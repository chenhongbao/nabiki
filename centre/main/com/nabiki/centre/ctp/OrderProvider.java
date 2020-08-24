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

import com.nabiki.centre.active.ActiveRequest;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.ConfigLoader;
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
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@code AliveOrderManager} keeps the status of all alive orders, interacts with
 * JNI interfaces and invoke callback methods to process responses.
 */
public class OrderProvider {
    protected final OrderMapper mapper = new OrderMapper();
    protected final AtomicInteger orderRef = new AtomicInteger(0);
    protected final Config config;
    protected final LoginConfig loginCfg;
    protected final ReqRspWriter msgWriter;
    protected final CThostFtdcTraderApi api;
    protected final Thread orderDaemon = new Thread(new RequestDaemon());
    protected final List<String> instruments = new LinkedList<>();
    protected final BlockingQueue<PendingRequest> pendingReqs;
    protected final TimeAligner timeAligner = new TimeAligner();

    protected boolean isConfirmed = false,
            isConnected = false,
            qryInstrLast = false;
    protected CRspUserLogin rspLogin;

    // Wait last instrument.
    protected final Signal lastRspSignal = new Signal();

    // Query instrument info.
    protected final QueryTask qryTask = new QueryTask();
    protected final Thread qryDaemon = new Thread(this.qryTask);
    protected final long qryWaitMillis = TimeUnit.SECONDS.toMillis(10);

    // State.
    protected WorkingState workingState = WorkingState.STOPPED;

    // SPI.
    protected JniTraderSpi spi;

    public OrderProvider(Config cfg) {
        this.config = cfg;
        this.loginCfg = this.config.getLoginConfigs().get("trader");
        this.api = CThostFtdcTraderApi
                .CreateFtdcTraderApi(this.loginCfg.FlowDirectory);
        this.msgWriter = new ReqRspWriter(this.mapper, this.config);
        this.pendingReqs = new LinkedBlockingQueue<>();
        // Start order daemon.
        this.orderDaemon.start();
        // Start query timer task if it needs to query some info.
        if (estimateQueryCount())
            this.qryDaemon.start();

    }

    private boolean estimateQueryCount() {
        int estimatedQueryCount = 0;
        for (var i : config.getAllInstrInfo()) {
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
            config.getLogger().info(
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

    protected void configTrader() {
        for (var fa : this.loginCfg.FrontAddresses)
            this.api.RegisterFront(fa);
        this.api.SubscribePrivateTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESUME);
        this.api.SubscribePublicTopic(THOST_TE_RESUME_TYPE.THOST_TERT_RESUME);
        /*
         IMPORTANT!
         Must kept reference to SPi explicitly so GC won't release the underlining
         C++ objects.
         */
        this.spi = new JniTraderSpi(this);
        this.api.RegisterSpi(spi);
    }

    /**
     * Initialize connection to remote counter.
     */
    public void initialize() {
        configTrader();
        this.api.Init();
    }

    /**
     * Disconnect the trader api and release resources.
     */
    public void release() {
        // Set states.
        this.isConfirmed = false;
        this.isConnected = false;
        this.workingState = WorkingState.STOPPED;
        // Cancel threads.
        this.qryDaemon.interrupt();
        this.orderDaemon.interrupt();
        try {
            this.orderDaemon.join(5000);
        } catch (InterruptedException e) {
            this.config.getLogger().warning(
                    Utils.formatLog("failed join order daemon",
                            null, e.getMessage(), null));
        }
        // Release resources.
        this.api.Release();
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
        doAuthentication();
    }

    /**
     * Request logout;
     */
    public void logout() {
        if (!this.isConfirmed)
            throw new IllegalStateException("repeated logout");
        this.workingState = WorkingState.STOPPING;
        doLogout();
    }

    public WorkingState getWorkingState() {
        return this.workingState;
    }

    public boolean waitLastInstrument(long millis) throws InterruptedException {
        if (!this.qryInstrLast)
            this.lastRspSignal.waitSignal(millis);
        return this.qryInstrLast;
    }

    public WorkingState waitWorkingState(long millis) throws InterruptedException {
        this.lastRspSignal.waitSignal(millis);
        return this.workingState;
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
    public int sendDetailOrder(
            CInputOrder input,
            ActiveRequest active) {
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

    protected void registerInitialOrderInsert(
            CInputOrder detail,
            ActiveRequest active) {
        var o = toRtnOrder(detail);
        o.OrderLocalID = active.getRequestUUID();
        o.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
        o.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
        // Register order.
        this.mapper.register(detail, active);
        this.mapper.register(o);
    }

    protected void rspError(CInputOrder order, int code,
                            String msg) {
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
        var hour = this.config.getTradingHour(null, instrID);
        if (hour == null)
            throw new IllegalArgumentException("invalid instr for trading hour");
        var depth = this.config.getDepthMarketData(instrID);
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
    public int sendOrderAction(
            CInputOrderAction action,
            ActiveRequest active) {
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

    public List<String> getInstruments() {
        return new LinkedList<>(this.instruments);
    }

    public String getOrderRef() {
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
            this.config.getLogger().severe(
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
            this.config.getLogger().warning(
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
            this.config.getLogger().severe(
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
            this.config.getLogger().severe(
                    Utils.formatLog("failed confirm settlement", null,
                            null, r));
    }

    protected void doRspLogin(CRspUserLogin rsp) {
        this.rspLogin = rsp;
        // Update order ref if max order ref goes after it.
        var maxOrderRef = Integer.parseInt(this.rspLogin.MaxOrderRef);
        if (maxOrderRef > this.orderRef.get())
            this.orderRef.set(maxOrderRef);
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

    protected void doOrder(COrder rtn) {
        var active = this.mapper.getActiveOrder(rtn.OrderRef);
        if (active == null) {
            this.config.getLogger().warning(
                    Utils.formatLog("active order not found", rtn.OrderRef,
                            null, null));
            return;
        }
        // Adjust IDs.
        rtn.BrokerID = active.getOriginOrder().BrokerID;
        rtn.UserID = active.getOriginOrder().UserID;
        rtn.InvestorID = active.getOriginOrder().InvestorID;
        rtn.AccountID = active.getOriginOrder().AccountID;
        rtn.OrderLocalID = active.getRequestUUID();

        try {
            active.updateRtnOrder(rtn);
        } catch (Throwable th) {
            th.printStackTrace();
            this.config.getLogger().severe(
                    Utils.formatLog("failed update rtn order", rtn.OrderRef,
                            th.getMessage(), null));
        }
    }

    protected void doTrade(CTrade trade) {
        var active = this.mapper.getActiveOrder(trade.OrderRef);
        if (active == null) {
            this.config.getLogger().warning(
                    Utils.formatLog("active order not found", trade.OrderRef,
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
            this.config.getLogger().severe(
                    Utils.formatLog("failed update rtn trade", trade.OrderRef,
                            th.getMessage(), null));
        }
    }

    protected void doQueryInstr() {
        var req = new CQryInstrument();
        var r = this.api.ReqQryInstrument(
                JNI.toJni(req),
                Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().warning(
                    Utils.formatLog("failed query instrument", null,
                            null, r));
    }

    protected void cancelInputOrder(CInputOrder inputOrder) {
        var cancel = toRtnOrder(inputOrder);
        // Order status.
        cancel.OrderStatus = OrderStatusType.CANCELED;
        cancel.OrderSubmitStatus = OrderSubmitStatusType.CANCEL_SUBMITTED;
        doOrder(cancel);
        // Write input order as normal cancel order.
        this.msgWriter.writeRtn(cancel);
    }

    public void whenFrontConnected() {
        this.isConnected = true;
        if (this.workingState == WorkingState.STARTING
                || this.workingState == WorkingState.STARTED)
            doAuthentication();
    }

    public void whenFrontDisconnected(int reason) {
        this.config.getLogger().warning(
                Utils.formatLog("trader disconnected", null,
                        null, reason));
        this.isConnected = false;
        this.isConfirmed = false;
        // Clear trading day.
        ConfigLoader.setTradingDay(null);
        // Don't change working state here because it may disconnect in half way.
    }

    public void whenErrRtnOrderAction(COrderAction orderAction,
                                    CRspInfo rspInfo) {
        this.config.getLogger().warning(
                Utils.formatLog("failed action", orderAction.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
        // Rewrite the local ID.
        var active = this.mapper.getActiveOrder(orderAction.OrderRef);
        if (active != null)
            orderAction.OrderLocalID = active.getRequestUUID();
        this.msgWriter.writeErr(orderAction);
        this.msgWriter.writeErr(rspInfo);
    }

    public void whenErrRtnOrderInsert(CInputOrder inputOrder,
                                      CRspInfo rspInfo) {
        this.config.getLogger().severe(
                Utils.formatLog("failed order insertion", inputOrder.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
        // Failed order results in canceling the order.
        cancelInputOrder(inputOrder);
        this.msgWriter.writeErr(inputOrder);
        this.msgWriter.writeErr(rspInfo);
    }

    public void whenRspAuthenticate(
            CRspAuthenticate rspAuthenticateField,
            CRspInfo rspInfo, int requestId, boolean isLast) {
        if (rspInfo.ErrorID == 0)
            doLogin();
        else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed authentication", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
            this.workingState = WorkingState.STOPPED;
        }
    }

    public void whenRspError(CRspInfo rspInfo, int requestId,
                           boolean isLast) {
        this.msgWriter.writeErr(rspInfo);
        this.config.getLogger().severe(
                Utils.formatLog("unknown error", null, rspInfo.ErrorMsg,
                        rspInfo.ErrorID));
    }

    public void whenRspOrderAction(CInputOrderAction inputOrderAction,
                                   CRspInfo rspInfo, int requestId,
                                   boolean isLast) {
        this.msgWriter.writeErr(inputOrderAction);
        this.msgWriter.writeErr(rspInfo);
        this.config.getLogger().warning(
                Utils.formatLog("failed action", inputOrderAction.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
    }

    public void whenRspOrderInsert(CInputOrder inputOrder,
                                 CRspInfo rspInfo, int requestId,
                                 boolean isLast) {
        this.config.getLogger().severe(
                Utils.formatLog("failed order insertion", inputOrder.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
        // Failed order results in canceling the order.
        cancelInputOrder(inputOrder);
        this.msgWriter.writeErr(inputOrder);
        this.msgWriter.writeErr(rspInfo);
    }

    public void whenRspQryInstrument(CInstrument instrument,
                                   CRspInfo rspInfo, int requestID,
                                   boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            var accepted = InstrumentFilter.accept(instrument.InstrumentID);
            if (accepted) {
                this.msgWriter.writeInfo(instrument);
                ConfigLoader.setInstrConfig(instrument);
            }
            // Sync on instrument set.
            synchronized (this.instruments) {
                if (this.qryInstrLast)
                    this.instruments.clear();
                if (accepted)
                    this.instruments.add(instrument.InstrumentID);
                this.qryInstrLast = isLast;
            }
        } else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed instrument query", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
        // Signal request rtn.
        this.qryTask.signalRequest(requestID);
        // Signal last rsp.
        if (this.qryInstrLast)
            this.lastRspSignal.signal();
    }

    public void whenRspQryInstrumentCommissionRate(
            CInstrumentCommissionRate instrumentCommissionRate,
            CRspInfo rspInfo, int requestID, boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.msgWriter.writeInfo(instrumentCommissionRate);
            ConfigLoader.setInstrConfig(instrumentCommissionRate);
        } else {
            this.config.getLogger().severe(
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
            ConfigLoader.setInstrConfig(instrumentMarginRate);
        } else {
            this.config.getLogger().severe(
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
            this.config.getLogger().fine(
                    Utils.formatLog("successful login", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.isConfirmed = true;
            this.workingState = WorkingState.STARTED;
            // Query instruments.
            doQueryInstr();
        } else {
            this.config.getLogger().severe(
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
            ConfigLoader.setTradingDay(rspUserLogin.TradingDay);
            // Align time.
            this.timeAligner.align("SHFE", LocalTime.now(), rspLogin.SHFETime);
            this.timeAligner.align("CZCE", LocalTime.now(), rspLogin.CZCETime);
            this.timeAligner.align("DCE", LocalTime.now(), rspLogin.DCETime);
            this.timeAligner.align("FFEX", LocalTime.now(), rspLogin.FFEXTime);
            this.timeAligner.align("INE", LocalTime.now(), rspLogin.INETime);
        } else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed login", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
            this.workingState = WorkingState.STOPPED;
        }
    }

    public void whenRspUserLogout(CUserLogout userLogout,
                                CRspInfo rspInfo, int requestId,
                                boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.config.getLogger().fine(
                    Utils.formatLog("successful logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.isConfirmed = false;
            this.workingState = WorkingState.STOPPED;
            // Clear trading day.
            ConfigLoader.setTradingDay(null);
            // Signal logout.
            this.lastRspSignal.signal();
        } else {
            this.config.getLogger().warning(
                    Utils.formatLog("failed logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    public void whenRtnOrder(COrder order) {
        doOrder(order);
        // The codes below follow the doXXX method because the parameter's fields
        // were rewritten by the method, with local IDs.
        this.msgWriter.writeRtn(order);
        this.mapper.register(order);
    }

    public void whenRtnTrade(CTrade trade) {
        doTrade(trade);
        // The writing method must follow the doXXX method because the fields are
        // rewritten with local IDs.
        this.msgWriter.writeRtn(trade);
    }

    protected static class PendingRequest {
        final ActiveRequest active;
        final CInputOrder order;
        final CInputOrderAction action;

        PendingRequest(CInputOrder order, ActiveRequest active) {
            this.order = order;
            this.action = null;
            this.active = active;
        }

        PendingRequest(CInputOrderAction action, ActiveRequest active) {
            this.order = null;
            this.action = action;
            this.active = active;
        }
    }

    protected class RequestDaemon implements Runnable {
        protected final int MAX_REQ_PER_SEC = 5;
        protected int sendCnt = 0;
        protected long threshold = TimeUnit.SECONDS.toMillis(1);
        protected long timeStamp = System.currentTimeMillis();

        @Override
        public void run() {

            while (!Thread.interrupted()) {
                try {
                    var pend = trySendRequest();
                    // Pending request is not sent, enqueue the request for next
                    // loop.
                    if (pend != null) {
                        Thread.sleep(threshold);
                        pendingReqs.offer(pend);
                    }
                } catch (InterruptedException e) {
                    if (workingState == WorkingState.STOPPING
                            || workingState == WorkingState.STOPPED)
                        break;
                    else
                        config.getLogger().warning(
                                Utils.formatLog("order daemon interrupted",
                                        null, e.getMessage(),
                                        null));
                }
            }
        }

        private PendingRequest trySendRequest() throws InterruptedException {
            PendingRequest pend = null;
            while (pend == null)
                pend = pendingReqs.poll(1, TimeUnit.DAYS);
            // Await time out, or notified by new request.
            // Instrument not trading.
            if (!canTrade(getInstrID(pend))) {
                return pend;
            }
            int r = 0;
            // Send order or action.
            // Fill and send order at first place so its fields are filled.
            if (pend.action != null) {
                r = fillAndSendAction(pend.action);
                if (r == 0)
                    msgWriter.writeReq(pend.action);
            } else if (pend.order != null) {
                r = fillAndSendOrder(pend.order);
                if (r == 0)
                    msgWriter.writeReq(pend.order);
            }
            // Check send ret code.
            // If fail sending the request, add it back to queue and sleep
            // for some time.
            if (r != 0) {
                warn(r, pend);
                return pend;
            }
            // Flow control.
            long curTimeStamp = System.currentTimeMillis();
            long diffTimeStamp = threshold - (curTimeStamp - timeStamp);
            if (diffTimeStamp > 0) {
                ++sendCnt;
                if (sendCnt > MAX_REQ_PER_SEC) {
                    Thread.sleep(diffTimeStamp);
                    timeStamp = System.currentTimeMillis();
                }
            } else {
                sendCnt = 0;
                timeStamp = System.currentTimeMillis();
            }
            // Return null, indicates the request has been sent.
            // Otherwise, enqueue the request and wait.
            return null;
        }

        protected int fillAndSendOrder(CInputOrder input) {
            // Set correct users.
            input.BrokerID = rspLogin.BrokerID;
            input.UserID = rspLogin.UserID;
            input.InvestorID = rspLogin.UserID;
            // Adjust flags.
            input.CombHedgeFlag = CombHedgeFlagType.SPECULATION;
            input.ContingentCondition = ContingentConditionType.IMMEDIATELY;
            input.ForceCloseReason = ForceCloseReasonType.NOT_FORCE_CLOSE;
            input.IsAutoSuspend = 0;
            input.MinVolume = 1;
            input.OrderPriceType = OrderPriceTypeType.LIMIT_PRICE;
            input.StopPrice = 0;
            input.TimeCondition = TimeConditionType.GFD;
            input.VolumeCondition = VolumeConditionType.ANY_VOLUME;
            return api.ReqOrderInsert(
                    JNI.toJni(input),
                    Utils.getIncrementID());
        }

        protected int fillAndSendAction(CInputOrderAction action) {
            var instrInfo = config.getInstrInfo(action.InstrumentID);
            var rtn = mapper.getRtnOrder(action.OrderRef);
            if (rtn != null) {
                action.BrokerID = rspLogin.BrokerID;
                action.InvestorID = rspLogin.UserID;
                action.UserID = rspLogin.UserID;
                // Use order sys ID as first choice.
                action.OrderSysID = rtn.OrderSysID;
                // Adjust flags.
                action.ActionFlag = ActionFlagType.DELETE;
                // Adjust other info.
                action.OrderRef = null;
                action.FrontID = 0;
                action.SessionID = 0;
                // Must need exchange id.
                action.ExchangeID = rtn.ExchangeID;
            } else {
                action.BrokerID = rspLogin.BrokerID;
                action.InvestorID = rspLogin.UserID;
                action.UserID = rspLogin.UserID;
                // Use order ref + front ID + session ID.
                // Keep original order ref and instrument ID.
                action.FrontID = rspLogin.FrontID;
                action.SessionID = rspLogin.SessionID;
                // Adjust flags.
                action.ActionFlag = ActionFlagType.DELETE;
                // Adjust other info.
                action.OrderSysID = null;
                action.ExchangeID = (instrInfo.Instrument != null)
                        ? instrInfo.Instrument.ExchangeID : null;
            }
            return api.ReqOrderAction(
                    JNI.toJni(action),
                    Utils.getIncrementID());
        }

        protected boolean canTrade(String instrID) {
            var hour = config.getTradingHour(null, instrID);
            if (hour == null) {
                config.getLogger().warning(
                        Utils.formatLog("trading hour config null", instrID,
                                null, null));
                return false;
            }
            LocalTime now;
            var ins = config.getInstrInfo(instrID);
            if (ins != null && ins.Instrument != null)
                now = timeAligner.getAlignTime(ins.Instrument.ExchangeID,
                        LocalTime.now());
            else
                now = LocalTime.now();
            return isConfirmed && hour.contains(now.minusSeconds(1));
        }

        protected String getInstrID(PendingRequest pend) {
            if (pend.action != null)
                return pend.action.InstrumentID;
            else if (pend.order != null)
                return pend.order.InstrumentID;
            else
                return null;
        }

        protected void warn(int r, PendingRequest pend) {
            String ref, hint;
            if (pend.order != null) {
                ref = pend.order.OrderRef;
                hint = "failed sending order";
            } else if (pend.action != null) {
                ref = pend.action.OrderRef;
                hint = "failed sending action";
            } else
                return;
            config.getLogger().warning(
                    Utils.formatLog(hint, ref, null, r));
        }
    }

    protected class QueryTask implements Runnable {
        protected final Random rand = new Random();

        // Wait last request return.
        protected final Signal lastRtn = new Signal();
        protected final AtomicInteger lastID = new AtomicInteger(0);

        QueryTask() {
        }

        void signalRequest(int requestID) {
            if (this.lastID.get() == requestID)
                this.lastRtn.signal();
        }

        private boolean waitRequestRsp(
                long millis, int requestID) throws InterruptedException {
            this.lastID.set(requestID);
            return this.lastRtn.waitSignal(millis);
        }


        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                if (qryInstrLast && isConfirmed) {
                    try {
                        doQuery();
                    } catch (Throwable th) {
                        th.printStackTrace();
                        config.getLogger().warning(th.getMessage());
                    }
                }
                // Sleep 1 second between queries.
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void doQuery() {
            String ins = randomGet();
            int reqID;
            var in = config.getInstrInfo(ins);
            // Query margin.
            if (in.Margin == null) {
                var req = new CQryInstrumentMarginRate();
                req.BrokerID = loginCfg.BrokerID;
                req.InvestorID = loginCfg.UserID;
                req.HedgeFlag = CombHedgeFlagType.SPECULATION;
                req.InstrumentID = ins;
                reqID = Utils.getIncrementID();
                int r = api.ReqQryInstrumentMarginRate(
                        JNI.toJni(req),
                        reqID);
                if (r != 0) {
                    config.getLogger().warning(
                            Utils.formatLog("failed query margin",
                                    null, ins, r));
                } else {
                    // Sleep up tp some seconds.
                    try {
                        if (!waitRequestRsp(qryWaitMillis, reqID))
                            config.getLogger().warning("query margin timeout");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            // Sleep 1 second between queries.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            // Query commission.
            if (in.Commission == null) {
                var req0 = new CQryInstrumentCommissionRate();
                req0.BrokerID = loginCfg.BrokerID;
                req0.InvestorID = loginCfg.UserID;
                req0.InstrumentID = ins;
                reqID = Utils.getIncrementID();
                var r = api.ReqQryInstrumentCommissionRate(
                        JNI.toJni(req0),
                        reqID);
                if (r != 0) {
                    config.getLogger().warning(
                            Utils.formatLog("failed query commission",
                                    null, ins, r));
                } else {
                    // Sleep up tp some seconds.
                    try {
                        if (!waitRequestRsp(qryWaitMillis, reqID))
                            config.getLogger().warning("query margin timeout");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        protected String randomGet() {
            synchronized (instruments) {
                return instruments.get(
                        Math.abs(rand.nextInt()) % instruments.size());
            }
        }
    }
}
