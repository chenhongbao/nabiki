/*
 * Copyright (c) 2020 Hongbao Chen <chenhongbao@outlook.com>
 *
 * Licensed under the  GNU Affero General Public License v3.0 and you may not use
 * this file except in compliance with the  License. You may obtain a copy of the
 * License at
 *
 *                    https://www.gnu.org/licenses/agpl-3.0.txt
 *
 * Permission is hereby  granted, free of charge, to any  person obtaining a copy
 * of this software and associated  documentation files (the "Software"), to deal
 * in the Software  without restriction, including without  limitation the rights
 * to  use, copy,  modify, merge,  publish, distribute,  sublicense, and/or  sell
 * copies  of  the Software,  and  to  permit persons  to  whom  the Software  is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE  IS PROVIDED "AS  IS", WITHOUT WARRANTY  OF ANY KIND,  EXPRESS OR
 * IMPLIED,  INCLUDING BUT  NOT  LIMITED TO  THE  WARRANTIES OF  MERCHANTABILITY,
 * FITNESS FOR  A PARTICULAR PURPOSE AND  NONINFRINGEMENT. IN NO EVENT  SHALL THE
 * AUTHORS  OR COPYRIGHT  HOLDERS  BE  LIABLE FOR  ANY  CLAIM,  DAMAGES OR  OTHER
 * LIABILITY, WHETHER IN AN ACTION OF  CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE  OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.nabiki.centre.ctp;

import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.ConfigLoader;
import com.nabiki.centre.utils.Signal;
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.ctp4j.CThostFtdcMdApi;
import com.nabiki.objects.*;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class TickProvider {
    protected final Config config;
    protected final LoginConfig loginCfg;
    protected final CThostFtdcMdApi mdApi;
    protected final MessageWriter msgWriter;
    protected final Set<MarketDataRouter> routers = new HashSet<>();
    protected final Set<CandleEngine> engines = new HashSet<>();
    protected final Set<String> subscribed = new HashSet<>();

    protected boolean isConnected = false,
            isLogin = false;
    protected WorkingState workingState = WorkingState.STOPPED;

    // Login signal.
    protected final Signal loginSignal = new Signal();

    protected String actionDay;

    public TickProvider(Config cfg) {
        this.config = cfg;
        this.loginCfg = this.config.getLoginConfigs().get("md");
        this.mdApi = CThostFtdcMdApi.CreateFtdcMdApi(
                this.loginCfg.FlowDirectory,
                this.loginCfg.IsUsingUDP,
                this.loginCfg.IsMulticast);
        this.msgWriter = new MessageWriter(this.config);
        // Schedule action day updater.
        prepareActionDayUpdater();
    }

    private void prepareActionDayUpdater() {
        var m = TimeUnit.DAYS.toMillis(1);
        Timer dayUpdater = new Timer();
        dayUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateActionDay();
            }
        },  m - System.currentTimeMillis() % m, m);
    }

    public void register(CandleEngine engine) {
        synchronized (this.engines) {
            this.engines.add(engine);
        }
    }

    public void register(MarketDataRouter router) {
        synchronized (this.routers) {
            this.routers.add(router);
        }
    }

    public void subscribe(Collection<String> instr) {
        if (instr == null || instr.size() == 0)
            return;
        // Clear subscribed instruments in last call.
        this.subscribed.clear();
        // Prepare new subscription.
        var ins = new String[50];
        int count = -1;
        var iter = instr.iterator();
        while (true) {
            while (iter.hasNext() && ++count < 50) {
                var i= iter.next();
                // Initialize instrument ID in candle engine.
                registerInstrument(i);
                ins[count] = i;
                // Save instruments for reconnection and re-subscription.
                this.subscribed.add(i);
            }
            // Subscribe batch.
            subscribeBatch(ins, count);
            count = -1;
            if (!iter.hasNext())
                break;
            else {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(1));
                } catch (InterruptedException e) {
                    this.config.getLogger().warning(
                            Utils.formatLog("failed sleep", null,
                                    e.getMessage(), null));
                }
            }
        }
        // Setup durations.
        setupDurations();
    }

    public void initialize() {
        this.mdApi.RegisterSpi(new JniMdSpi(this));
        for (var addr : this.loginCfg.FrontAddresses)
            this.mdApi.RegisterFront(addr);
        this.mdApi.Init();
    }

    public void release() {
        // Set states.
        this.isLogin = false;
        this.isConnected = false;
        this.workingState = WorkingState.STOPPED;
        setWorking(false);
        // Release resources.
        this.mdApi.Release();
    }

    public void login() {
        if (!this.isConnected)
            throw new IllegalStateException("not connected");
        this.workingState = WorkingState.STARTING;
        doLogin();
    }

    public void logout() {
        if (!this.isLogin)
            throw new IllegalStateException("repeated logout");
        this.workingState = WorkingState.STOPPING;
        doLogout();
    }

    public WorkingState getWorkingState() {
        return this.workingState;
    }

    public WorkingState waitWorkingState(long millis) throws InterruptedException {
        this.loginSignal.waitSignal(millis);
        return this.workingState;
    }

    private void updateActionDay() {
        this.actionDay = Utils.getDay(LocalDate.now(), null);
    }

    private void subscribeBatch(String[] instr, int count) {
        this.mdApi.SubscribeMarketData(instr, count);
    }

    private void registerInstrument(String instrID) {
        synchronized (this.engines) {
            for (var e : this.engines)
                e.addInstrument(instrID);
        }
    }

    private void setupDurations() {
        synchronized (this.engines) {
            for (var e : this.engines)
                e.setupDurations();
        }
    }

    private void doLogin() {
        var req = new CReqUserLogin();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        req.Password = this.loginCfg.Password;
        var r = this.mdApi.ReqUserLogin(
                JNI.toJni(req),
                Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().severe(
                    Utils.formatLog("failed login request", null,
                            null, r));
    }

    private void doLogout() {
        var req = new CUserLogout();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        var r = this.mdApi.ReqUserLogout(
                JNI.toJni(req),
                Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().warning(
                    Utils.formatLog("failed logout request", null,
                            null, r));
    }

    private void setWorking(boolean working) {
        synchronized (this.engines) {
            for (var e : this.engines)
                e.setWorking(working);
        }
    }

    public void whenFrontConnected() {
        this.isConnected = true;
        if (this.workingState == WorkingState.STARTING
                || this.workingState == WorkingState.STARTED)
            doLogin();
    }

    public void whenFrontDisconnected(int reason) {
        this.config.getLogger().warning(
                Utils.formatLog("md disconnected", null, null,
                        reason));
        this.isLogin = false;
        this.isConnected = false;
        // If disconnected when or after provider stops, candle engine isn't working.
        // But if disconnected in work time, it is still working.
        if (this.workingState == WorkingState.STOPPING
                || this.workingState == WorkingState.STOPPED)
            setWorking(false);
    }

    public void whenRspError(CRspInfo rspInfo, int requestId,
                           boolean isLast) {
        this.msgWriter.writeErr(rspInfo);
        this.config.getLogger().severe(
                Utils.formatLog("unknown error", null, rspInfo.ErrorMsg,
                        rspInfo.ErrorID));
    }

    public void whenRspUserLogin(CRspUserLogin rspUserLogin,
                               CRspInfo rspInfo, int requestId,
                               boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.isLogin = true;
            this.workingState = WorkingState.STARTED;
            // Candle engine starts working.
            setWorking(true);
            // Signal login state changed.
            this.loginSignal.signal();
            updateActionDay();
            // If there are instruments to subscribe, do it.
            // The subscribe method clears the container, so the instruments must be
            // kept in another container.
            if (this.subscribed.size() > 0)
                subscribe(new HashSet<>(this.subscribed));
        } else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed login", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRspUserLogout(CUserLogout userLogout,
                                CRspInfo rspInfo, int nRequestID,
                                boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.isLogin = false;
            this.workingState = WorkingState.STOPPED;
            setWorking(false);
            // Signal login state changed to logout.
            this.loginSignal.signal();
            // Clear last subscription on successful logout.
            this.subscribed.clear();
        } else {
            this.config.getLogger().warning(
                    Utils.formatLog("failed logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRspSubMarketData(
            CSpecificInstrument specificInstrument,
            CRspInfo rspInfo, int requestId, boolean isLast) {
        if (rspInfo.ErrorID != 0) {
            this.config.getLogger().warning(Utils.formatLog(
                    "failed subscription", specificInstrument.InstrumentID,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRspUnSubMarketData(
            CSpecificInstrument specificInstrument,
            CRspInfo rspInfo, int nRequestID, boolean isLast) {
        if (rspInfo.ErrorID != 0) {
            this.config.getLogger().warning(Utils.formatLog(
                    "failed un-subscription", specificInstrument.InstrumentID,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRtnDepthMarketData(CDepthMarketData depthMarketData) {
        // Set action day to today.
        depthMarketData.ActionDay = this.actionDay;
        synchronized (this.routers) {
            for (var r : this.routers)
                r.route(depthMarketData);
        }
        synchronized (this.engines) {
            for (var e : this.engines)
                e.update(depthMarketData);
        }
        // Update config's depth.
        ConfigLoader.setDepthMarketData(depthMarketData);
    }
}
