/*
 * Copyright (c) 2020-2021. Hongbao Chen <chenhongbao@outlook.com>
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
import com.nabiki.centre.config.UncaughtWriter;
import com.nabiki.centre.config.plain.LoginConfig;
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.user.core.ActiveRequest;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.utils.Signal;
import com.nabiki.commons.utils.Utils;
import com.nabiki.ctp4j.CThostFtdcTraderApi;
import com.nabiki.ctp4j.THOST_TE_RESUME_TYPE;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code AliveOrderManager} keeps the status of all alive orders, interacts with
 * JNI interfaces and invoke callback methods to process responses.
 */
public class OrderProvider {
  private final OrderMapper mapper = new OrderMapper();
  private final AtomicInteger orderRef = new AtomicInteger(0);
  private final Global global;
  private final CandleEngine candleEngine;
  private final LoginConfig loginCfg;
  private final ReqRspWriter msgWriter;
  private final Set<String> instrumentIDs = new HashSet<>();
  private final Map<String, CInstrument> activeInstruments = new ConcurrentHashMap<>();
  private final BlockingDeque<PendingRequest> pendingReqs;

  // Offset for order ref, try to avoid duplication.
  private final Integer orderRefOffset;
  // Wait and signaling.
  private final Signal stateSignal = new Signal(), qryLastInstrSignal = new Signal();
  private boolean isConfirmed = false, isConnected = false, qryInstrLast = false;
  private CRspUserLogin rspLogin;
  // Query instrument info.
  private QueryTask qryTask;
  private Thread qryDaemon;

  // Request daemon.
  private RequestDaemon reqTask;
  private Thread orderDaemon;

  // State.
  private WorkingState workingState = WorkingState.STOPPED;

  private CThostFtdcTraderApi api;
  private JniTraderSpi spi;

  public OrderProvider(CandleEngine cdl, Global glb) {
    global = glb;
    candleEngine = cdl;
    loginCfg = global.getLoginConfigs().get("trader");
    msgWriter = new ReqRspWriter(mapper, global);
    pendingReqs = new LinkedBlockingDeque<>();
    orderRefOffset = getOrderRefOffset();
    startOrderDaemonOnce();
  }

  private int getOrderRefOffset() {
    if (orderRefOffset != null) {
      return orderRefOffset;
    } else {
      var i = new Random().nextInt(100000) << 16;
      if (i < 0) {
        return -i;
      } else {
        return i;
      }
    }
  }

  public void settle() {
    getMapper().settle();
    /* Clear all pending requests because their frozen resources will be freed,
     * then settled. They become invalid after settlement. */
    getPendingRequests().clear();
  }

  private void startOrderDaemonOnce() {
    // In case the method is called more than once, throwing exception.
    if (orderDaemon != null && orderDaemon.isAlive())
      return;
    // Start order daemon.
    reqTask = new RequestDaemon(this, global);
    orderDaemon = new Thread(reqTask);
    orderDaemon.setDaemon(true);
    orderDaemon.setUncaughtExceptionHandler(UncaughtWriter.getDefault());
    orderDaemon.start();
  }

  private void startQryDaemonOnce() {
    // Don't start more than once.
    if (qryDaemon != null && qryDaemon.isAlive())
      return;
    // Start the qry daemon whatever because this object is init only once and
    // instruments are updated on every login.
    qryTask = new QueryTask(this, global);
    qryDaemon = new Thread(qryTask);
    qryDaemon.setDaemon(true);
    qryDaemon.setUncaughtExceptionHandler(UncaughtWriter.getDefault());
    qryDaemon.start();
  }

  BlockingDeque<PendingRequest> getPendingRequests() {
    return pendingReqs;
  }

  ReqRspWriter getMsgWriter() {
    return msgWriter;
  }

  CThostFtdcTraderApi getApi() {
    return api;
  }

  CRspUserLogin getLoginRsp() {
    return rspLogin;
  }

  LoginConfig getLoginCfg() {
    return loginCfg;
  }

  boolean isQryLast() {
    return qryInstrLast;
  }

  private void setQryLast(boolean last) {
    qryInstrLast = last;
    if (last) {
      global.getLogger().info("qry last instrument: " + isQryLast());
    }
  }

  public boolean isConfirmed() {
    return isConfirmed;
  }

  private void setConfirmed(boolean confirmed) {
    if (isConfirmed != confirmed) {
      global.getLogger().info("trader confirmed: " + confirmed);
    }
    isConfirmed = confirmed;
  }

  public boolean isConnected() {
    return this.isConnected;
  }

  private void setConnected(boolean connected) {
    if (isConnected != connected) {
      global.getLogger().info("trader connected: " + connected);
    }
    isConnected = connected;
  }

  public boolean waitConnected(long millis) {
    try {
      this.stateSignal.waitSignal(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    return isConnected();
  }

  /**
   * Get order mapper.
   *
   * @return {@link OrderMapper}
   */
  public OrderMapper getMapper() {
    return this.mapper;
  }

  public boolean isInit() {
    return api != null;
  }

  /**
   * Initialize connection to remote counter.
   */
  public void init() {
    if (isInit()) {
      throw new IllegalStateException("trader duplicated init");
    }
    this.api = CThostFtdcTraderApi
            .CreateFtdcTraderApi(this.loginCfg.FlowDirectory);
    this.spi = new JniTraderSpi(this, global);
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
  public void release() {
    // Set states.
    setConfirmed(false);
    setConnected(false);
    setWorkingState(WorkingState.STOPPED);
    // Release resources.
    this.api.Release();
    this.api = null;
    this.spi = null;
  }

  /**
   * Request login.
   */
  public void login() {
    if (!isConnected()) {
      throw new IllegalStateException("not connected");
    }
    if (isConfirmed()) {
      throw new IllegalStateException("repeated login");
    }
    setWorkingState(WorkingState.STARTING);
    // Reset qry last instrument to false so it will wait for the completion
    // of qry instruments.
    setQryLast(false);
    doAuthentication();
  }

  /**
   * Request logout;
   */
  public void logout() {
    if (!isConfirmed()) {
      throw new IllegalStateException("repeated logout");
    }
    setWorkingState(WorkingState.STOPPING);
    // Set candle working state.
    this.candleEngine.setWorking(false);
    doLogout();
  }

  public WorkingState getWorkingState() {
    return workingState;
  }

  private void setWorkingState(WorkingState state) {
    workingState = state;
    global.getLogger().info("trader state: " + getWorkingState());
  }

  public boolean waitLastInstrument(long millis) throws InterruptedException {
    if (!isQryLast()) {
      this.qryLastInstrSignal.waitSignal(millis);
    }
    return isQryLast();
  }

  public boolean waitWorkingState(WorkingState stateToWait, long millis) {
    var wait = millis;
    while (getWorkingState() != stateToWait && wait > 0) {
      var beg = System.currentTimeMillis();
      try {
        this.stateSignal.waitSignal(wait);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      wait -= (System.currentTimeMillis() - beg);
    }
    return getWorkingState() == stateToWait;
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
    // Check order ref only after it is time for sending requests.
    if (!isOrderRefUnique(input.OrderRef)) {
      global.getLogger().warning(
              Utils.formatLog("duplicated order", input.OrderRef, null, null));
      doInsertError(input,
                    ErrorCodes.DUPLICATE_ORDER_REF,
                    ErrorMessages.DUPLICATE_ORDER_REF);
      return ErrorCodes.DUPLICATE_ORDER_REF;
    }
    if (!this.pendingReqs.offer(new PendingRequest(input, active))) {
      doInsertError(input,
                    ErrorCodes.NEED_RETRY,
                    ErrorMessages.NEED_RETRY);
      return ErrorCodes.NEED_RETRY;
    } else {
      // Only after request is sent successfully, initialize rtn order.
      // Otherwise, it looks like request is sent, actually hasn't, when there's
      // error like exception.
      registerInitialOrderInsert(input, active);
      return ErrorCodes.NONE;
    }
  }

  private boolean isOrderRefUnique(String orderRef) {
    return this.mapper.getInputOrder(orderRef) == null;
  }

  private void registerInitialOrderInsert(CInputOrder input, ActiveRequest active) {
    var o = toRtnOrder(input);
    o.OrderLocalID = active.getRequestUUID();
    o.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
    o.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
    // Register order.
    this.mapper.register(input, active);
    this.mapper.register(o);
  }

  private void doInsertError(CInputOrder input, int code, String msg) {
    var rsp = new CRspInfo();
    rsp.ErrorID = code;
    rsp.ErrorMsg = msg;
    this.global.getLogger().severe(Utils.formatLog(
            "failed order insertion", input.OrderRef, msg, code));
    // Failed order results in canceling the order.
    cancelInputOrder(input, rsp);
    this.msgWriter.writeErr(input, rsp);
  }

  private void doInsertError(CInputOrderAction action, int code, String msg) {
    var rsp = new CRspInfo();
    rsp.ErrorID = code;
    rsp.ErrorMsg = msg;
    this.global.getLogger().warning(Utils.formatLog(
            "failed action", action.OrderRef, msg, code));
    this.msgWriter.writeErr(action, rsp);
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
    if (!this.pendingReqs.offer(new PendingRequest(action, active))) {
      doInsertError(action,
                    ErrorCodes.NEED_RETRY,
                    ErrorMessages.NEED_RETRY);
      return ErrorCodes.NEED_RETRY;
    } else {
      return ErrorCodes.NONE;
    }
  }

  public Collection<String> getInstrumentIDs() {
    return this.instrumentIDs;
  }

  public synchronized String getOrderRef() {
    if (orderRef.get() == Integer.MAX_VALUE) {
      orderRef.set(0);
      reqTask.clearOrderRef();
    }
    return String.valueOf(orderRef.incrementAndGet() + orderRefOffset);
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
    var r = api.ReqAuthenticate(
            JNI.toJni(req),
            Utils.getIncrementID());
    if (r != 0) {
      global.getLogger().severe(Utils.formatLog("failed authentication",
                                                null,
                                                null,
                                                r));
    }
    global.getLogger().info(
            String.format("Send auth. %s/%s, AppID: %s, AuthCode: %s, %s",
                          req.BrokerID,
                          req.UserID,
                          req.AppID,
                          req.AuthCode,
                          req.UserProductInfo));
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
              Utils.formatLog("missing previous rtn order", ref, null, null));
      return true;
    }
    if (rtn0.OrderStatus == OrderStatusType.ALL_TRADED
            || rtn0.OrderStatus == OrderStatusType.CANCELED) {
      this.global.getLogger().warning(
              Utils.formatLog("no update to a completed order", ref, null, null));
      return true;
    } else
      return false;
  }

  protected void doOrder(COrder rtn) {
    if (isOrderCompleted(rtn.OrderRef)) {
      return;
    }
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
    // Signal request daemon that last order rsp has arrived.
    this.reqTask.signalOrderRef(rtn.OrderRef);
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
    // Clear old data only after settlement confirm and right before sending qry.
    // If it fails login to trader, the instruments kept from previous day will not
    // be clear so it still has instruments to subscribe.
    this.instrumentIDs.clear();
    this.activeInstruments.clear();
    // Send qry.
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
    setConnected(true);
    this.stateSignal.signal();
    if (getWorkingState() == WorkingState.STARTED) {
      doAuthentication();
    }
  }

  public void whenFrontDisconnected(int reason) {
    setConnected(false);
    setConfirmed(false);
    // If the state is starting or stopping, remote disconnects, it is service not
    // not available or auto-disconnect after sending logout.
    if (getWorkingState() == WorkingState.STARTING
            || getWorkingState() == WorkingState.STOPPING) {
      setWorkingState(WorkingState.STOPPED);
    }
    // Don't change working state when it is started here
    // because it may disconnect in half way.
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
    // Process the error only once in rsp-order-insert.
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
      global.getLogger().severe(
              String.format("%s/%s, AppID: %s, AppType: %d, %s",
                            rspAuthenticateField.BrokerID,
                            rspAuthenticateField.UserID,
                            rspAuthenticateField.AppID,
                            rspAuthenticateField.AppType,
                            rspAuthenticateField.UserProductInfo));
      this.msgWriter.writeErr(rspInfo);
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

  public void whenRspOrderAction(CInputOrderAction inputOrderAction,
                                 CRspInfo rspInfo, int requestId,
                                 boolean isLast) {
    // Process error only in err-rtn-order-action.
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
        this.activeInstruments.put(instrument.InstrumentID, instrument);
        this.instrumentIDs.add(instrument.InstrumentID);
      }
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("failed instrument query", null,
                              rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
    // Don't signal qry task for instrument rsp because it doesn't qry instruments
    // and it qry commission and margin.
    // Signal last rsp.
    if (isLast) {
      // Set active instruments into config, and remove obsolete ones.
      GlobalConfig.resetInstrConfig(this.activeInstruments.values());
      // First update config instrument info, then signal. So other waiting
      // thread can get the correct data.
      setQryLast(true);
      this.qryLastInstrSignal.signal();
      // Start qry task.
      startQryDaemonOnce();
    } else {
      setQryLast(false);
    }
  }

  private void setCommission(CInstrumentCommissionRate commission) {
    // Commission is set per product, needs to convert to instrument.
    var pid = Utils.getProductID(commission.InstrumentID);
    if (pid != null && pid.equals(commission.InstrumentID)) {
      var instruments = global.getProduct(pid);
      if (instruments == null) {
        global.getLogger().warning("no instrument in product: " + pid);
        return;
      }
      for (var i : instruments) {
        var c = Utils.deepCopy(commission);
        c.InstrumentID = i;
        this.msgWriter.writeInfo(c);
        GlobalConfig.setCommissionConfig(c);
      }
    } else {
      this.msgWriter.writeInfo(commission);
      GlobalConfig.setCommissionConfig(commission);
    }
  }

  public void whenRspQryInstrumentCommissionRate(
          CInstrumentCommissionRate instrumentCommissionRate,
          CRspInfo rspInfo, int requestID, boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      setCommission(instrumentCommissionRate);
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
      GlobalConfig.setMarginConfig(instrumentMarginRate);
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
      setConfirmed(true);
      setWorkingState(WorkingState.STARTED);
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
    } else {
      this.global.getLogger().severe(
              Utils.formatLog("trader failed login", null,
                              rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
      setWorkingState(WorkingState.STOPPED);
    }
  }

  public void whenRspUserLogout(CUserLogout userLogout,
                                CRspInfo rspInfo, int requestId,
                                boolean isLast) {
    if (rspInfo.ErrorID == 0) {
      setConfirmed(false);
      setWorkingState(WorkingState.STOPPED);
      // Signal logout.
      this.stateSignal.signal();
    } else {
      this.global.getLogger().warning(
              Utils.formatLog("failed logout", null, rspInfo.ErrorMsg, rspInfo.ErrorID));
      this.msgWriter.writeErr(rspInfo);
    }
  }

  public void whenRtnOrder(COrder order) {
    // Measure performance.
    var max = this.global.getPerformance().start("when.order.max");
    var cur = this.global.getPerformance().start("when.order.cur");
    // Process order.
    doOrder(order);
    // End measurement.
    max.endWithMax();
    cur.end();
  }

  public void whenRtnTrade(CTrade trade) {
    // Measure performance.
    var max = this.global.getPerformance().start("when.trade.max");
    var cur = this.global.getPerformance().start("when.trade.cur");
    // Process order.
    doTrade(trade);
    // End measurement.
    max.endWithMax();
    cur.end();
  }
}
