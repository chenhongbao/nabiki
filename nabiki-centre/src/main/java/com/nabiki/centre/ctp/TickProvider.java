/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

import com.nabiki.centre.config.Global;
import com.nabiki.centre.config.GlobalConfig;
import com.nabiki.centre.config.plain.LoginConfig;
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.utils.Signal;
import com.nabiki.commons.utils.Utils;
import com.nabiki.ctp4j.CThostFtdcMdApi;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TickProvider {
  private final Global global;
  private final LoginConfig loginCfg;
  private final MarketDataRouter router;
  private final CandleEngine engine;
  private final ReqRspWriter msgWriter;
  private final Set<String> toSubscribe = new HashSet<>(),
      subscribed = new HashSet<>();
  private final ExecutorService es = Executors.newCachedThreadPool();

  private boolean isConnected = false, isLogin = false;
  private WorkingState workingState = WorkingState.STOPPED;

  // Login signal.
  private final Signal stateSignal = new Signal();

  private String actionDay;
  private CThostFtdcMdApi api;
  private JniMdSpi spi;

  public TickProvider(MarketDataRouter router, CandleEngine engine, Global global) {
    this.global = global;
    this.router = router;
    this.engine = engine;
    this.loginCfg = this.global.getLoginConfigs().get("md");
    this.msgWriter = new ReqRspWriter(null, this.global);
    daemon();
  }

  private void daemon() {
    Utils.schedule(new TimerTask() {
      @Override
      public void run() {
        updateActionDay();
      }
    }, TimeUnit.DAYS.toMillis(1));
  }

  public boolean isConnected() {
    return this.isConnected;
  }

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

  private void sleep(int value, TimeUnit unit) {
    try {
      unit.sleep(value);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public boolean isLogin() {
    return isLogin;
  }

  private void setLogin(boolean login) {
    isLogin = login;
    global.getLogger().info("md login: " + isLogin());
  }

  private void setConnected(boolean connected) {
    isConnected = connected;
    global.getLogger().info("md connected: " + isConnected());
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
        else
          sleep(1, TimeUnit.SECONDS);
      }
    }
    // Setup durations.
    setupDurations();
    // Wait for a while, then check subscription status.
    sleep(1, TimeUnit.MINUTES);
    checkSubscription();
  }

  public boolean isInit() {
    return api != null;
  }

  public void init() {
    if (isInit()) {
      throw new IllegalStateException("md duplicated init");
    }
    this.api = CThostFtdcMdApi.CreateFtdcMdApi(
        this.loginCfg.FlowDirectory,
        this.loginCfg.IsUsingUDP,
        this.loginCfg.IsMulticast);
    this.spi = new JniMdSpi(this, global);
    this.api.RegisterSpi(spi);
    for (var addr : this.loginCfg.FrontAddresses) {
      this.api.RegisterFront(addr);
    }
    this.api.Init();
  }

  public void release() {
    // Set states.
    setLogin(false);
    setConnected(false);
    setWorkingState(WorkingState.STOPPED);
    // Release resources.
    this.api.Release();
    this.api = null;
    this.spi = null;
  }

  public void login() {
    if (!isConnected()) {
      throw new IllegalStateException("not connected");
    }
    if (isLogin()) {
      throw new IllegalStateException("duplicated login");
    }
    setWorkingState(WorkingState.STARTING);
    doLogin();
  }

  public void logout() {
    if (!isLogin()) {
      throw new IllegalStateException("duplicated logout");
    }
    setWorkingState(WorkingState.STOPPING);
    doLogout();
  }

  public WorkingState getWorkingState() {
    return workingState;
  }

  private void setWorkingState(WorkingState state) {
    workingState = state;
    global.getLogger().info("md state: " + getWorkingState());
  }

  public boolean waitWorkingState(WorkingState stateToWait, long millis) {
    var wait = millis;
    var beg = System.currentTimeMillis();
    while (getWorkingState() != stateToWait) {
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
    return getWorkingState() == stateToWait;
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

  // Simply test against trading hours because no md during weekend or holiday.
  private boolean isTrading(String instrumentID) {
    var keeper = this.global.getTradingHour(
        null, instrumentID);
    return keeper != null && keeper.contains(LocalTime.now());
  }

  public void whenFrontConnected() {
    setConnected(true);
    this.stateSignal.signal();
    if (getWorkingState() == WorkingState.STARTED) {
      doLogin();
    }
  }

  public void whenFrontDisconnected(int reason) {
    setLogin(false);
    setConnected(false);
    // It doesn't start up, then it is disconnected. So remote is not available.
    if (getWorkingState() == WorkingState.STARTING) {
      setWorkingState(WorkingState.STOPPED);
    }
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
      setLogin(true);
      setWorkingState(WorkingState.STARTED);
      // Signal login state changed.
      this.stateSignal.signal();
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
      setLogin(false);
      setWorkingState(WorkingState.STOPPED);
      // Signal login state changed to logout.
      this.stateSignal.signal();
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
      var r = new HashSet<String>();
      for (var in0 : this.toSubscribe) {
        if (!this.subscribed.contains(in0))
          r.add(in0);
      }
      int count = 0;
      String msgBlock = System.lineSeparator();
      for (var i : r) {
        ++count;
        msgBlock += "\t" + i + ",";
        if (count >= 10) {
          msgBlock += System.lineSeparator();
          count = 0;
        }
      }
      global.getLogger().warning(
          r.size() + " instruments have no sub rsp: " + msgBlock);
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
