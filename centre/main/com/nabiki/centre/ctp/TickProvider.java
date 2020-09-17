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
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.centre.utils.Signal;
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.ctp4j.CThostFtdcMdApi;
import com.nabiki.objects.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TickProvider implements Connectable {
    protected final Global global;
    protected final LoginConfig loginCfg;
    protected final MarketDataRouter router;
    protected final CandleEngine engine;
    protected final ReqRspWriter msgWriter;
    protected final Set<String> toSubscribe = new HashSet<>(),
            subscribed = new HashSet<>();
    protected ExecutorService es = Executors.newCachedThreadPool();

    protected boolean isConnected = false,
            isLogin = false;
    protected WorkingState workingState = WorkingState.STOPPED;

    // Login signal.
    protected final Signal stateSignal = new Signal();

    protected String actionDay;
    protected CThostFtdcMdApi api;
    protected JniMdSpi spi;

    public TickProvider(MarketDataRouter router, CandleEngine engine, Global global) {
        this.global = global;
        this.router = router;
        this.engine = engine;
        this.loginCfg = this.global.getLoginConfigs().get("md");
        this.msgWriter = new ReqRspWriter(null, this.global);
        daemon();
    }

    private void daemon() {
        // Schedule action day updater.
        var m = TimeUnit.DAYS.toMillis(1);
        Timer dayUpdater = new Timer();
        dayUpdater.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateActionDay();
            }
        },  m - System.currentTimeMillis() % m, m);
    }

    @Override
    public boolean isConnected() {
        return this.isConnected;
    }

    @Override
    public boolean waitConnected(long millis) {
        try {
            this.stateSignal.waitSignal(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return isConnected();
    }

    public void setSubscription(Collection<String> instr) {
        synchronized (this.toSubscribe) {
            this.toSubscribe.clear();
            this.toSubscribe.addAll(instr);
        }
    }

    public void subscribe() {
        if (this.toSubscribe.size() == 0)
            throw new IllegalStateException("no instrument to subscribe");
        // Prepare new subscription.
        var ins = new String[50];
        int count = -1;
        synchronized (this.toSubscribe) {
            var iter = this.toSubscribe.iterator();
            while (true) {
                while (iter.hasNext() && ++count < 50) {
                    var i = iter.next();
                    // Initialize instrument ID in candle engine.
                    registerInstrument(i);
                    ins[count] = i;
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
                        this.global.getLogger().warning(
                                Utils.formatLog("failed sleep", null,
                                        e.getMessage(), null));
                    }
                }
            }
        }
        // Setup durations.
        setupDurations();
    }

    @Override
    public void connect() {
        if (this.api != null)
            throw new IllegalStateException("need disconnect before connect");
        /*
         IMPORTANT!
         Kept reference to SPI so GC won't disconnect the object, hence the underlining
         C++ objects.
         */
        this.api = CThostFtdcMdApi.CreateFtdcMdApi(
                this.loginCfg.FlowDirectory,
                this.loginCfg.IsUsingUDP,
                this.loginCfg.IsMulticast);
        this.spi = new JniMdSpi(this);
        this.api.RegisterSpi(spi);
        for (var addr : this.loginCfg.FrontAddresses)
            this.api.RegisterFront(addr);
        this.api.Init();
    }

    @Override
    public void disconnect() {
        // Set states.
        this.isLogin = false;
        this.isConnected = false;
        this.workingState = WorkingState.STOPPED;
        // Release resources.
        this.api.Release();
        this.api = null;
        this.spi = null;
    }

    public void login() {
        if (!this.isConnected)
            throw new IllegalStateException("not connected");
        if (isLogin)
            throw new IllegalStateException("duplicated login");
        this.workingState = WorkingState.STARTING;
        doLogin();
    }

    public void logout() {
        if (!this.isLogin)
            throw new IllegalStateException("duplicated logout");
        this.workingState = WorkingState.STOPPING;
        doLogout();
    }

    public WorkingState getWorkingState() {
        return this.workingState;
    }

    public boolean waitWorkingState(WorkingState stateToWait, long millis) {
        var wait = millis;
        var beg = System.currentTimeMillis();
        while (this.workingState != stateToWait) {
            try {
                this.stateSignal.waitSignal(wait);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            var elapse = System.currentTimeMillis() - beg;
            if (elapse < wait)
                wait -= elapse;
            else
                break;
        }
        return this.workingState == stateToWait;
    }

    private void updateActionDay() {
        this.actionDay = Utils.getDay(LocalDate.now(), null);
    }

    private void subscribeBatch(String[] instr, int count) {
        this.api.SubscribeMarketData(instr, count);
    }

    private void registerInstrument(String instrID) {
        engine.addInstrument(instrID);
    }

    private void setupDurations() {
        engine.setupDurations();
    }

    private void doLogin() {
        var req = new CReqUserLogin();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        req.Password = this.loginCfg.Password;
        var r = this.api.ReqUserLogin(
                JNI.toJni(req),
                Utils.getIncrementID());
        if (r != 0)
            this.global.getLogger().severe(
                    Utils.formatLog("failed login request", null,
                            null, r));
    }

    private void doLogout() {
        var req = new CUserLogout();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        var r = this.api.ReqUserLogout(
                JNI.toJni(req),
                Utils.getIncrementID());
        if (r != 0)
            this.global.getLogger().warning(
                    Utils.formatLog("failed logout request", null,
                            null, r));
    }

    private boolean isTrading(String instrumentID) {
        var keeper = this.global.getTradingHour(
                null, instrumentID);
        return keeper != null && keeper.contains(LocalTime.now());
    }

    public void whenFrontConnected() {
        this.isConnected = true;
        this.stateSignal.signal();
        if (this.workingState == WorkingState.STARTING
                || this.workingState == WorkingState.STARTED) {
            doLogin();
            this.global.getLogger().info("md reconnected");
        } else
            this.global.getLogger().info("md connected");
    }

    public void whenFrontDisconnected(int reason) {
        this.global.getLogger().warning("md disconnected");
        this.isLogin = false;
        this.isConnected = false;
    }

    public void whenRspError(CRspInfo rspInfo, int requestId,
                           boolean isLast) {
        this.msgWriter.writeErr(rspInfo);
        this.global.getLogger().severe(
                Utils.formatLog("unknown error", null, rspInfo.ErrorMsg,
                        rspInfo.ErrorID));
    }

    public void whenRspUserLogin(CRspUserLogin rspUserLogin,
                               CRspInfo rspInfo, int requestId,
                               boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.isLogin = true;
            this.workingState = WorkingState.STARTED;
            // Signal login state changed.
            this.stateSignal.signal();
            this.global.getLogger().info("md login");
            updateActionDay();
            // The subscribe method uses instruments set by order provider.
            // Because it needs to send requests many times, costing around 10 secs,
            // use a thread here so it won't block the underlying API.
            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        subscribe();
                    } catch (Throwable th) {
                        th.printStackTrace();
                        global.getLogger().severe(th.getMessage());
                    }
                }
            });
        } else {
            this.global.getLogger().severe(
                    Utils.formatLog("md failed login", null,
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
            // Signal login state changed to logout.
            this.stateSignal.signal();
            this.global.getLogger().info("md logout");
            // Clear last subscription on successful logout.
            this.toSubscribe.clear();
        } else {
            this.global.getLogger().warning(
                    Utils.formatLog("failed logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void checkSubscription() {
        if (this.subscribed.size() == this.toSubscribe.size())
            this.global.getLogger().info(
                    "subscribe all " + this.toSubscribe.size() + "instruments");
        else {
            for (var in0 : this.toSubscribe)
                if (!this.subscribed.contains(in0))
                    this.global.getLogger().warning(in0 + " not subscribed");
        }
        // Clear.
        this.subscribed.clear();
    }

    public void whenRspSubMarketData(
            CSpecificInstrument specificInstrument,
            CRspInfo rspInfo, int requestId, boolean isLast) {
        if (rspInfo.ErrorID != 0) {
            this.global.getLogger().warning(Utils.formatLog(
                    "failed subscription", specificInstrument.InstrumentID,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        } else {
            this.subscribed.add(specificInstrument.InstrumentID);
        }
    }

    public void whenRspUnSubMarketData(
            CSpecificInstrument specificInstrument,
            CRspInfo rspInfo, int nRequestID, boolean isLast) {
        if (rspInfo.ErrorID != 0) {
            this.global.getLogger().warning(Utils.formatLog(
                    "failed un-subscription", specificInstrument.InstrumentID,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRtnDepthMarketData(CDepthMarketData depthMarketData) {
        // Set day.
        // CZCE's trading day is natural day, here unify them. No need to test the
        // exchange id because directly assign the reference saves more time.
        depthMarketData.TradingDay = this.global.getTradingDay();
        depthMarketData.ActionDay = this.actionDay;
        // Update global depth.
        GlobalConfig.setDepthMarketData(depthMarketData);
        // Filter re-sent md of last night between subscription and market open.
        // It is not sent specially when you subscribe near market opens, but
        // it is always sent when you subscribe early before market opens.
        // [IMPORTANT]
        // But please note that this condition filters out the settlement ticks,
        // so need to save the tick before this clause.
        if (isTrading(depthMarketData.InstrumentID)) {
            // Route md and update candle engines.
            router.route(depthMarketData);
            engine.update(depthMarketData);
        }
    }
}
