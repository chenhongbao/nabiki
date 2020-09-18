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
import com.nabiki.centre.user.core.ActiveRequest;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.centre.utils.Signal;
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.ctp4j.CThostFtdcTraderApi;
import com.nabiki.ctp4j.THOST_TE_RESUME_TYPE;
import com.nabiki.objects.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code AliveOrderManager} keeps the status of all alive orders, interacts with
 * JNI interfaces and invoke callback methods to process responses.
 */
public class OrderProvider implements Connectable {
  protected final OrderMapper mapper = new OrderMapper();
  protected final AtomicInteger orderRef = new AtomicInteger(0);
  protected final Global global;
  protected final CandleEngine candleEngine;
  protected final LoginConfig loginCfg;
  protected final ReqRspWriter msgWriter;
  protected final List<String> instrumentIDs = new LinkedList<>();
  protected final List<CInstrument> activeInstruments = new LinkedList<>();
  protected final BlockingQueue<PendingRequest> pendingReqs;
  protected final TimeAligner timeAligner = new TimeAligner();

  protected boolean isConfirmed = false,
          isConnected = false,
          qryInstrLast = false;
  protected CRspUserLogin rspLogin;

  // Wait and signaling.
  protected final Signal stateSignal = new Signal(),
          qryLastInstrSignal = new Signal();

  // Query instrument info.
  protected final QueryTask qryTask = new QueryTask(this);
  protected final Thread qryDaemon = new Thread(this.qryTask);
  protected final long qryWaitMillis = TimeUnit.SECONDS.toMillis(10);

  // Request daemon.
  protected Thread orderDaemon;
  protected final RequestDaemon reqDaemon = new RequestDaemon(this);

  // State.
  protected WorkingState workingState = WorkingState.STOPPED;

  protected CThostFtdcTraderApi api;
  protected JniTraderSpi spi;

  public OrderProvider(CandleEngine cdl, Global global) {
    this.global = global;
    this.candleEngine = cdl;
    this.loginCfg = this.global.getLoginConfigs().get("trader");
    this.msgWriter = new ReqRspWriter(this.mapper, this.global);
    this.pendingReqs = new LinkedBlockingQueue<>();
    daemon();
  }

  private void daemon() {
    // Start order daemon.
    this.orderDaemon = new Thread(this.reqDaemon);
    this.orderDaemon.setDaemon(true);
    this.orderDaemon.start();
    // Count how many requests to send for info.
    estimateQueryCount();
    // Start the qry daemon whatever because this object is init only once and
    // instruments are updated on every login.
    this.qryDaemon.setDaemon(true);
    this.qryDaemon.start();
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

  private boolean estimateQueryCount() {
    int estimatedQueryCount = 0;
    for (var i : global.getAllInstrInfo()) {
      if (i == null)
        continue;
      if (i.Instrument == null)
        ++estimatedQueryCount;
      if (i.Commission == null)
        ++estimatedQueryCount;
      if (i.Margin == null)
        ++estimatedQueryCount;
    }
    if (estimatedQueryCount > 0)
      global.getLogger().info(
              "estimated query count: " + estimatedQueryCount);
    return estimatedQueryCount != 0;
  }

  /**
   * Get order mapper.
   *
   * @return {@link OrderMapper}
   */
  public OrderMapper getMapper() {
    return this.mapper;
  }

  /**
   * Initialize connection to remote counter.
   */
  @Override
  public void connect() {
    if (this.api != null)
      throw new IllegalStateException("need disconnect before connect");
        /*
         IMPORTANT!
         Must kept reference to SPi explicitly so GC won't release the underlining
         C++ objects.
         */
    this.api = CThostFtdcTraderApi
            .CreateFtdcTraderApi(this.loginCfg.FlowDirectory);
    this.spi = new JniTraderSpi(this);
    for (var fa : this.loginCfg.FrontAddresses)
      this.api.RegisterFront(fa);
    this.api.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESUME);
    this.api.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESUME);
    this.api.RegisterSpi(spi);
    this.api.Init();
  }

  /**
   * Disconnect the trader api and release resources.
   */
  @Override
  public void disconnect() {
    // Set states.
    this.isConfirmed = false;
    this.isConnected = false;
    this.workingState = WorkingState.STOPPED;
    // Release resources.
    this.api.Release();
    this.api = null;
    this.spi = null;
  }

  /**
   * Request login.
   */
  public void login() {
    if (!this.isConnected)
      throw new IllegalStateException("not connected");
    if (this.isConfirmed)
      throw new IllegalStateException("repeated login");
    this.workingState = WorkingState.STARTING;
    // Reset qry last instrument to false so it will wait for the completion
    // of qry instruments.
    this.qryInstrLast = false;
    doAuthentication();
  }

  /**
   * Request logout;
   */
  public void logout() {
    if (!this.isConfirmed)
      throw new IllegalStateException("repeated logout");
    this.workingState = WorkingState.STOPPING;
    // Set candle working state.
    this.candleEngine.setWorking(false);
    doLogout();
  }

  public WorkingState getWorkingState() {
    return this.workingState;
  }

  public boolean waitLastInstrument(long millis) throws InterruptedException {
    if (!this.qryInstrLast)
      this.qryLastInstrSignal.waitSignal(millis);
    return this.qryInstrLast;
  }

  public boolean waitWorkingState(WorkingState stateToWait, long millis) {
    var wait = millis;
    while (this.workingState != stateToWait && wait > 0) {
      var beg = System.currentTimeMillis();
      try {
        this.stateSignal.waitSignal(wait);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      wait -= (System.currentTimeMillis() - beg);
    }
    return this.workingState == stateToWait;
  }

  /**
   * Save the mapping from the specified input order to the specified alive order,
   * then send the specified input order to remote server.
   *
   * <p>If the remote service is temporarily unavailable within a trading day,
   * the order is saved to send at next market open. If the trading day is over,
   * return error.
   * </p>
   *
   * @param input  input order
   * @param active alive order
   * @return always return 0
   */
  public synchronized int inputOrder(CInputOrder input, ActiveRequest active) {
    if (!isOrderRefUnique(input.OrderRef)) {
      global.getLogger().warning(
              Utils.formatLog("duplicated order",
                      input.OrderRef, null, null));
      return -1;
    }
    // Set the initial rtn order.
    registerInitialOrderInsert(input, active);
    // Check time.
    if (isOver(input.InstrumentID)) {
      rspError(input, ErrorCodes.FRONT_NOT_ACTIVE,
              ErrorMessages.FRONT_NOT_ACTIVE);
      return ErrorCodes.FRONT_NOT_ACTIVE;
    } else {
      if (!this.pendingReqs.offer(new PendingRequest(input, active)))
        return (-2);
      else
        return ErrorCodes.NONE;
    }
  }

  private boolean isOrderRefUnique(String orderRef) {
    return this.mapper.getInputOrder(orderRef) == null;
  }

  protected void registerInitialOrderInsert(CInputOrder input, ActiveRequest active) {
    var o = toRtnOrder(input);
    o.OrderLocalID = active.getRequestUUID();
    o.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
    o.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
    // Register order.
    this.mapper.register(input, active);
    this.mapper.register(o);
  }

  protected void rspError(CInputOrder order, int code, String msg) {
    var rsp = new CRspInfo();
    rsp.ErrorID = code;
    rsp.ErrorMsg = msg;
    whenErrRtnOrderInsert(order, rsp);
  }

  protected void rspError(CInputOrderAction action, int code,
                          String msg) {
    var rsp = new CRspInfo();
    rsp.ErrorID = code;
    rsp.ErrorMsg = msg;
    whenRspOrderAction(action, rsp, 0, true);
  }

  /*
  Beginning hour of continuous trading, at night, 21 pm.
   */
  protected static final int CONT_TRADE_BEG = 21;

  protected boolean isOver(String instrID) {
    var hour = this.global.getTradingHour(null, instrID);
    if (hour == null)
      throw new IllegalArgumentException("invalid instr for trading hour");
    var depth = this.global.getDepthMarketData(instrID);
    if (depth == null)
      throw new IllegalStateException("depth market data not found");
    var depthTradingDay = Utils.parseDay(depth.TradingDay, null);
    var day = LocalDate.now();
    var time = LocalTime.now();
    // Holiday.
    if (depthTradingDay.isBefore(day))
      return true;
    if (CONT_TRADE_BEG <= time.getHour()) {
      // The night before holiday.
      return depthTradingDay.equals(day);
    } else {
      // Workday.
      var dayOfWeek = day.getDayOfWeek();
      return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
              && hour.isEndDay(LocalTime.now());
    }
  }

  /**
   * Send action request to remote server. The method first checks the type
   * of the specified order to be canceled. If it is an order, just cancel it. If
   * an action and action can't be canceled, return error.
   *
   * <p>If the remote service is temporarily unavailable within a trading day,
   * the action is saved to send at next market open. If the trading day is over,
   * return error.
   * </p>
   *
   * @param action action to send
   * @param active alive order
   * @return always return 0
   */
  public synchronized int actionOrder(CInputOrderAction action, ActiveRequest active) {
    if (isOver(action.InstrumentID)) {
      rspError(action, ErrorCodes.FRONT_NOT_ACTIVE,
              ErrorMessages.FRONT_NOT_ACTIVE);
      return (-1);
    } else {
      if (!this.pendingReqs.offer(new PendingRequest(action, active)))
        return (-2);
      else
        return 0;
    }
  }

  public List<String> getInstrumentIDs() {
    return new LinkedList<>(this.instrumentIDs);
  }

  public synchronized String getOrderRef() {
    if (this.orderRef.get() == Integer.MAX_VALUE)
      this.orderRef.set(0);
    return String.valueOf(this.orderRef.incrementAndGet());
  }

  protected void doLogin() {
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

  protected void doLogout() {
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

  protected void doAuthentication() {
    var req = new CReqAuthenticate();
    req.AppID = this.loginCfg.AppID;
    req.AuthCode = this.loginCfg.AuthCode;
    req.BrokerID = this.loginCfg.BrokerID;
    req.UserID = this.loginCfg.UserID;
    req.UserProductInfo = this.loginCfg.UserProductInfo;
    var r = this.api.ReqAuthenticate(
            JNI.toJni(req),
            Utils.getIncrementID());
    if (r != 0)
      this.global.getLogger().severe(
              Utils.formatLog("failed authentication", null,
                      null, r));
  }

  protected void doSettlement() {
    var req = new CSettlementInfoConfirm();
    req.BrokerID = this.loginCfg.BrokerID;
    req.AccountID = this.loginCfg.UserID;
    req.InvestorID = this.loginCfg.UserID;
    req.CurrencyID = "CNY";
    var r = this.api.ReqSettlementInfoConfirm(
            JNI.toJni(req),
            Utils.getIncrementID());
    if (r != 0)
      this.global.getLogger().severe(
              Utils.formatLog("failed confirm settlement", null,
                      null, r));
  }

  protected void doRspLogin(CRspUserLogin rsp) {
    rspLogin = rsp;
    // Update order ref if max order ref goes after it.
    var maxOrderRef = Integer.parseInt(rspLogin.MaxOrderRef);
    if (maxOrderRef > orderRef.get()) {
      orderRef.set(maxOrderRef);
      reqDaemon.clearOrderRef();
    }
  }

  /*
   Construct a return order from the specified error order.
   */
  COrder toRtnOrder(CInputOrder rtn) {
    var r = new COrder();
    r.AccountID = rtn.AccountID;
    r.BrokerID = rtn.BrokerID;
    r.BusinessUnit = rtn.BusinessUnit;
    r.ClientID = rtn.ClientID;
    r.CombHedgeFlag = rtn.CombHedgeFlag;
    r.CombOffsetFlag = rtn.CombOffsetFlag;
    r.ContingentCondition = rtn.ContingentCondition;
    r.CurrencyID = rtn.CurrencyID;
    r.Direction = rtn.Direction;
    r.ExchangeID = rtn.ExchangeID;
    r.ForceCloseReason = rtn.ForceCloseReason;
    r.GTDDate = rtn.GTDDate;
    r.InstrumentID = rtn.InstrumentID;
    r.InvestorID = rtn.InvestorID;
    r.InvestUnitID = rtn.InvestUnitID;
    r.IPAddress = rtn.IPAddress;
    r.IsAutoSuspend = rtn.IsAutoSuspend;
    r.IsSwapOrder = rtn.IsSwapOrder;
    r.LimitPrice = rtn.LimitPrice;
    r.MacAddress = rtn.MacAddress;
    r.MinVolume = rtn.MinVolume;
    r.OrderPriceType = rtn.OrderPriceType;
    r.OrderRef = rtn.OrderRef;
    r.RequestID = rtn.RequestID;
    r.StopPrice = rtn.StopPrice;
    r.TimeCondition = rtn.TimeCondition;
    r.UserForceClose = rtn.UserForceClose;
    r.UserID = rtn.UserID;
    r.VolumeCondition = rtn.VolumeCondition;
    r.VolumeTotalOriginal = rtn.VolumeTotalOriginal;
    return r;
  }

  protected boolean isOrderCompleted(String ref) {
    var rtn0 = this.mapper.getRtnOrder(ref);
    if (rtn0 == null) {
      this.global.getLogger().warning(
              Utils.formatLog(
                      "missing previous rtn order",
                      ref, null, null));
      return true;
    }
    if (rtn0.OrderStatus == OrderStatusType.ALL_TRADED
            || rtn0.OrderStatus == OrderStatusType.CANCELED) {
      this.global.getLogger().warning(
              Utils.formatLog(
                      "no update to a completed order",
                      ref, null, null));
      return true;
    } else
      return false;
  }

  protected void doOrder(COrder rtn) {
    if (isOrderCompleted(rtn.OrderRef))
      return;
    var active = this.mapper.getActiveRequest(rtn.OrderRef);
    if (active == null) {
      this.global.getLogger().warning(
              Utils.formatLog("active request not found", rtn.OrderRef,
                      null, null));
      return;
    }
    // Adjust IDs.
    rtn.BrokerID = active.getOriginOrder().BrokerID;
    rtn.UserID = active.getOriginOrder().UserID;
    rtn.InvestorID = active.getOriginOrder().InvestorID;
    rtn.AccountID = active.getOriginOrder().AccountID;
    rtn.OrderLocalID = active.getRequestUUID();
    // Set date and time if it is not set.
    if (rtn.UpdateTime == null || rtn.UpdateTime.length() == 0)
      rtn.UpdateTime = Utils.getTime(LocalTime.now(), null);
    if (rtn.OrderStatus == OrderStatusType.CANCELED) {
      if (rtn.CancelTime == null || rtn.CancelTime.length() == 0)
        rtn.CancelTime = Utils.getTime(LocalTime.now(), null);
    }
    if (rtn.InsertDate == null || rtn.InsertDate.length() == 0)
      rtn.InsertDate = Utils.getDay(LocalDate.now(), null);
    try {
      active.updateRtnOrder(rtn);
    } catch (Throwable th) {
      th.printStackTrace();
      this.global.getLogger().severe(
              Utils.formatLog("failed update rtn order", rtn.OrderRef,
                      th.getMessage(), null));
    }
    // The codes below follow the doXXX method because the parameter's fields
    // were rewritten by the method, with local IDs.
    this.msgWriter.writeRtn(rtn);
    this.mapper.register(rtn);
  }

  protected void doTrade(CTrade trade) {
    // Don't filter completed order here because if return order arrives earlier
    // than trade, the trade is not updated into system. So position is wrong.
    var active = this.mapper.getActiveRequest(trade.OrderRef);
    if (active == null) {
      this.global.getLogger().warning(
              Utils.formatLog("active request not found", trade.OrderRef,
                      null, null));
      return;
    }
    // Adjust IDs.
    trade.BrokerID = active.getOriginOrder().BrokerID;
    trade.UserID = active.getOriginOrder().UserID;
    trade.InvestorID = active.getOriginOrder().InvestorID;
    trade.OrderLocalID = active.getRequestUUID();
    try {
      active.updateTrade(trade);
    } catch (Throwable th) {
      th.printStackTrace();
      this.global.getLogger().severe(
              Utils.formatLog("failed update rtn trade", trade.OrderRef,
                      th.getMessage(), null));
    }
    // The writing method must follow the doXXX method because the fields are
    // rewritten with local IDs.
    this.msgWriter.writeRtn(trade);
  }

  protected void doQueryInstr() {
    // Clear old data.
    this.instrumentIDs.clear();
    this.activeInstruments.clear();
    var req = new CQryInstrument();
    var r = this.api.ReqQryInstrument(
            JNI.toJni(req),
            Utils.getIncrementID());
    if (r != 0)
      this.global.getLogger().warning(
              Utils.formatLog("failed query instrument", null,
                      null, r));
  }

  protected void cancelInputOrder(CInputOrder inputOrder, CRspInfo info) {
    var cancel = toRtnOrder(inputOrder);
    // Order status.
    cancel.OrderStatus = OrderStatusType.CANCELED;
    cancel.StatusMsg = info.ErrorMsg;
    cancel.OrderSubmitStatus = OrderSubmitStatusType.CANCEL_SUBMITTED;
    doOrder(cancel);
  }

  public void whenFrontConnected() {
    this.isConnected = true;
    this.stateSignal.signal();
    if (this.workingState == WorkingState.STARTING
            || this.workingState == WorkingState.STARTED) {
      doAuthentication();
      this.global.getLogger().info("trader reconnected");
    } else
      this.global.getLogger().info("trader connected");
  }

  public void whenFrontDisconnected(int reason) {
    this.global.getLogger().warning("trader disconnected");
    this.isConnected = false;
    this.isConfirmed = false;
    // Don't change working state here because it may disconnect in half way.
  }

  public void whenErrRtnOrderAction(COrderAction orderAction,
                                    CRspInfo rspInfo) {
    this.global.getLogger().warning(
            Utils.formatLog("failed action", orderAction.OrderRef,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
    // Rewrite the local ID.
    var active = this.mapper.getActiveRequest(orderAction.OrderRef);
    if (active != null)
      orderAction.OrderLocalID = active.getRequestUUID();
    this.msgWriter.writeErr(orderAction, rspInfo);
  }

  public void whenErrRtnOrderInsert(CInputOrder inputOrder,
                                    CRspInfo rspInfo) {
    // NO NEED to process the same error again.
    // this.global.getLogger().severe(
    //        Utils.formatLog("failed order insertion", inputOrder.OrderRef,
    //                rspInfo.ErrorMsg, rspInfo.ErrorID));
    // Failed order results in canceling the order.
    // cancelInputOrder(inputOrder, rspInfo);
    // this.msgWriter.writeErr(inputOrder, rspInfo);
  }

  public void whenRspAuthenticate(
          CRspAuthenticate rspAuthenticateField,
          CRspInfo rspInfo, int requestId, boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      doLogin();
      this.global.getLogger().info("trader authenticated");
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed authentication", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
      this.workingState = WorkingState.STOPPED;
    }
  }

  public void whenRspError(CRspInfo rspInfo, int requestId,
                           boolean isLast) {
    this.msgWriter.writeErr(rspInfo);
    this.global.getLogger().severe(
            Utils.formatLog("unknown error", null, rspInfo.ErrorMsg,
                    rspInfo.ErrorID));
  }

  public void whenRspOrderAction(CInputOrderAction inputOrderAction,
                                 CRspInfo rspInfo, int requestId,
                                 boolean isLast) {
    // NO NEED to process the same error again.
    // this.msgWriter.writeErr(inputOrderAction, rspInfo);
    // this.global.getLogger().warning(
    //        Utils.formatLog("failed action", inputOrderAction.OrderRef,
    //               rspInfo.ErrorMsg, rspInfo.ErrorID));
  }

  public void whenRspOrderInsert(CInputOrder inputOrder,
                                 CRspInfo rspInfo, int requestId,
                                 boolean isLast) {
    this.global.getLogger().severe(
            Utils.formatLog("failed order insertion", inputOrder.OrderRef,
                    rspInfo.ErrorMsg, rspInfo.ErrorID));
    // Failed order results in canceling the order.
    // Set order status msg to error message.
    cancelInputOrder(inputOrder, rspInfo);
    this.msgWriter.writeErr(inputOrder, rspInfo);
  }

  public void whenRspQryInstrument(CInstrument instrument,
                                   CRspInfo rspInfo, int requestID,
                                   boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      if (InstrumentFilter.accept(instrument)) {
        this.msgWriter.writeInfo(instrument);
        this.activeInstruments.add(instrument);
        this.instrumentIDs.add(instrument.InstrumentID);
      }
      this.qryInstrLast = isLast;
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed instrument query", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
    // Signal request rtn.
    this.qryTask.signalRequest(requestID);
    // Signal last rsp.
    if (this.qryInstrLast) {
      // Set active instruments into config, and remove obsolete ones.
      GlobalConfig.resetInstrConfig(this.activeInstruments);
      // First update config instrument info, then signal. So other waiting
      // thread can get the correct data.
      this.qryLastInstrSignal.signal();
      this.global.getLogger().info("get last qry instrument rsp");
    }
  }

  public void whenRspQryInstrumentCommissionRate(
          CInstrumentCommissionRate instrumentCommissionRate,
          CRspInfo rspInfo, int requestID, boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      this.msgWriter.writeInfo(instrumentCommissionRate);
      GlobalConfig.setInstrConfig(instrumentCommissionRate);
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed commission query", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
    // Signal request rtn.
    this.qryTask.signalRequest(requestID);
  }

  public void whenRspQryInstrumentMarginRate(
          CInstrumentMarginRate instrumentMarginRate,
          CRspInfo rspInfo, int requestID, boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      this.msgWriter.writeInfo(instrumentMarginRate);
      GlobalConfig.setInstrConfig(instrumentMarginRate);
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed margin query", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
    // Signal request rtn.
    this.qryTask.signalRequest(requestID);
  }

  public void whenRspSettlementInfoConfirm(
          CSettlementInfoConfirm settlementInfoConfirm,
          CRspInfo rspInfo, int requestId, boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      this.global.getLogger().info("trader confirm settlement");
      this.isConfirmed = true;
      this.workingState = WorkingState.STARTED;
      // Set candle working state.
      this.candleEngine.setWorking(true);
      // Query instruments.
      doQueryInstr();
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed settlement confirm", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
      // Confirm settlement fails, logout.
      logout();
    }
  }

  public void whenRspUserLogin(CRspUserLogin rspUserLogin,
                               CRspInfo rspInfo, int requestId,
                               boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      doSettlement();
      doRspLogin(rspUserLogin);
      // Set trading day.
      GlobalConfig.setTradingDay(rspUserLogin.TradingDay);
      this.global.getLogger().info("trader login");
      // Align time.
      this.timeAligner.align("SHFE", LocalTime.now(), rspLogin.SHFETime);
      this.timeAligner.align("CZCE", LocalTime.now(), rspLogin.CZCETime);
      this.timeAligner.align("DCE", LocalTime.now(), rspLogin.DCETime);
      this.timeAligner.align("FFEX", LocalTime.now(), rspLogin.FFEXTime);
      this.timeAligner.align("INE", LocalTime.now(), rspLogin.INETime);
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("trader failed login", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
      this.workingState = WorkingState.STOPPED;
    }
  }

  public void whenRspUserLogout(CUserLogout userLogout,
                                CRspInfo rspInfo, int requestId,
                                boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      this.global.getLogger().info("trader logout");
      this.isConfirmed = false;
      this.workingState = WorkingState.STOPPED;
      // Signal logout.
      this.stateSignal.signal();
    } else {
      this.global.getLogger().warning(
              Utils.formatLog("failed logout", null,
                      rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
  }

  public void whenRtnOrder(COrder order) {
    // Measure performance.
    var max = this.global.getPerformanceMeasure().start("when.order.max");
    var cur = this.global.getPerformanceMeasure().start("when.order.cur");
    // Process order.
    doOrder(order);
    // End measurement.
    max.endWithMax();
    cur.end();
  }

  public void whenRtnTrade(CTrade trade) {
    // Measure performance.
    var max = this.global.getPerformanceMeasure().start("when.trade.max");
    var cur = this.global.getPerformanceMeasure().start("when.trade.cur");
    // Process order.
    doTrade(trade);
    // End measurement.
    max.endWithMax();
    cur.end();
  }
}
