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
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.ctp4j.jni.flag.*;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;
import com.nabiki.ctp4j.trader.CThostFtdcTraderSpi;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * {@code AliveOrderManager} keeps the status of all alive orders, interacts with
 * JNI interfaces and invoke callback methods to process responses.
 */
public class OrderProvider extends CThostFtdcTraderSpi {
    protected final OrderMapper mapper = new OrderMapper();
    protected final AtomicInteger orderRef = new AtomicInteger(0);
    protected final Config config;
    protected final LoginConfig loginCfg;
    protected final MessageWriter msgWriter;
    protected final CThostFtdcTraderApi traderApi;
    protected final Thread orderDaemon = new Thread(new RequestDaemon());
    protected final Timer qryTimer = new Timer();
    protected final List<String> instruments = new LinkedList<>();
    protected final BlockingQueue<PendingRequest> pendingReqs;
    protected final TimeAligner timeAligner = new TimeAligner();

    protected boolean isConfirmed = false,
            isConnected = false,
            qryInstrLast = false;
    protected CThostFtdcRspUserLoginField rspLogin;

    // Wait last instrument.
    protected ReentrantLock lock = new ReentrantLock();
    protected Condition cond = lock.newCondition();

    // State.
    protected WorkingState workingState = WorkingState.STOPPED;

    public OrderProvider(Config cfg) {
        this.config = cfg;
        this.loginCfg = this.config.getLoginConfigs().get("trader");
        this.traderApi = CThostFtdcTraderApi
                .CreateFtdcTraderApi(this.loginCfg.FlowDirectory);
        this.msgWriter = new MessageWriter(this.config);
        this.pendingReqs = new LinkedBlockingQueue<>();
        // Start query timer task.
        this.qryTimer.scheduleAtFixedRate(new QueryTask(), 0, 3000);
        // Start order daemon.
        this.orderDaemon.start();
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
            this.traderApi.RegisterFront(fa);
        this.traderApi.SubscribePrivateTopic(ThostTeResumeType.THOST_TERT_RESUME);
        this.traderApi.SubscribePublicTopic(ThostTeResumeType.THOST_TERT_RESUME);
        this.traderApi.RegisterSpi(this);
    }

    /**
     * Initialize connection to remote counter.
     */
    public void initialize() {
        configTrader();
        this.traderApi.Init();
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
        this.qryTimer.cancel();
        this.orderDaemon.interrupt();
        try {
            this.orderDaemon.join(5000);
        } catch (InterruptedException e) {
            this.config.getLogger().warning(
                    Utils.formatLog("failed join order daemon",
                            null, e.getMessage(), null));
        }
        // Release resources.
        this.traderApi.Release();
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

    public boolean waitLastInstrument(long millis) {
        this.lock.lock();
        try {
            if (!this.qryInstrLast)
                this.cond.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            this.config.getLogger().warning(ex.getMessage());
        } finally {
            this.lock.unlock();
        }
        return this.qryInstrLast;
    }

    private void signalLastInstrument() {
        this.lock.lock();
        try {
            this.cond.signal();
        } finally {
            this.lock.unlock();
        }
    }

    public WorkingState waitWorkingState(long millis) {
        this.lock.lock();
        try {
            this.cond.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            this.config.getLogger().warning(ex.getMessage());
        } finally {
            this.lock.unlock();
        }
        return this.workingState;
    }

    private void signalWorkingState() {
        signalLastInstrument();
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
     * @param input input order
     * @param active alive order
     * @return always return 0
     */
    public int sendDetailOrder(
            CThostFtdcInputOrderField input,
            ActiveRequest active) {
        // Set the initial rtn order.
        registerInitialOrderInsert(input, active);
        // Check time.
        if (isOver(input.InstrumentID)) {
            rspError(input, TThostFtdcErrorCode.FRONT_NOT_ACTIVE,
                    TThostFtdcErrorMessage.FRONT_NOT_ACTIVE);
            return TThostFtdcErrorCode.FRONT_NOT_ACTIVE;
        } else {
            if (!this.pendingReqs.offer(new PendingRequest(input, active)))
                return (-2);
            else
                return TThostFtdcErrorCode.NONE;
        }
    }

    protected void registerInitialOrderInsert(
            CThostFtdcInputOrderField detail,
            ActiveRequest active) {
        var o = toRtnOrder(detail);
        o.OrderLocalID = active.getRequestUUID();
        o.OrderSubmitStatus = TThostFtdcOrderSubmitStatusType.ACCEPTED;
        o.OrderStatus = TThostFtdcOrderStatusType.NO_TRADE_QUEUEING;
        // Register order.
        this.mapper.register(detail, active);
        this.mapper.register(o);
    }

    protected void rspError(CThostFtdcInputOrderField order, int code,
                            String msg) {
        var rsp = new CThostFtdcRspInfoField();
        rsp.ErrorID = code;
        rsp.ErrorMsg = msg;
        OnErrRtnOrderInsert(order, rsp);
    }

    protected void rspError(CThostFtdcInputOrderActionField action, int code,
                            String msg) {
        var rsp = new CThostFtdcRspInfoField();
        rsp.ErrorID = code;
        rsp.ErrorMsg = msg;
        OnRspOrderAction(action, rsp, 0, true);
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
            CThostFtdcInputOrderActionField action,
            ActiveRequest active) {
        if (isOver(action.InstrumentID)) {
            rspError(action, TThostFtdcErrorCode.FRONT_NOT_ACTIVE,
                    TThostFtdcErrorMessage.FRONT_NOT_ACTIVE);
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
        var req = new CThostFtdcReqUserLoginField();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        req.Password = this.loginCfg.Password;
        var r = this.traderApi.ReqUserLogin(req, Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().severe(
                    Utils.formatLog("failed login request", null,
                            null, r));
    }

    protected void doLogout() {
        var req = new CThostFtdcUserLogoutField();
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        var r = this.traderApi.ReqUserLogout(req, Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().warning(
                    Utils.formatLog("failed logout request", null,
                            null, r));
    }

    protected void doAuthentication() {
        var req = new CThostFtdcReqAuthenticateField();
        req.AppID = this.loginCfg.AppID;
        req.AuthCode = this.loginCfg.AuthCode;
        req.BrokerID = this.loginCfg.BrokerID;
        req.UserID = this.loginCfg.UserID;
        req.UserProductInfo = this.loginCfg.UserProductInfo;
        var r = this.traderApi.ReqAuthenticate(req, Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().severe(
                    Utils.formatLog("failed authentication", null,
                            null, r));
    }

    protected void doSettlement() {
        var req = new CThostFtdcSettlementInfoConfirmField();
        req.BrokerID = this.loginCfg.BrokerID;
        req.AccountID = this.loginCfg.UserID;
        req.InvestorID = this.loginCfg.UserID;
        req.CurrencyID = "CNY";
        var r = this.traderApi.ReqSettlementInfoConfirm(req, Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().severe(
                    Utils.formatLog("failed confirm settlement", null,
                            null, r));
    }

    protected void doRspLogin(CThostFtdcRspUserLoginField rsp) {
        this.rspLogin = rsp;
        // Update order ref if max order ref goes after it.
        var maxOrderRef = Integer.parseInt(this.rspLogin.MaxOrderRef);
        if (maxOrderRef > this.orderRef.get())
            this.orderRef.set(maxOrderRef);
    }

    /*
     Construct a return order from the specified error order.
     */
    CThostFtdcOrderField toRtnOrder(CThostFtdcInputOrderField rtn) {
        var r = new CThostFtdcOrderField();
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

    protected void doRtnOrder(CThostFtdcOrderField rtn) {
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
            this.config.getLogger().severe(
                    Utils.formatLog("failed update rtn order", rtn.OrderRef,
                            th.getMessage(), null));
        }
    }

    protected void doRtnTrade(CThostFtdcTradeField trade) {
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
            this.config.getLogger().severe(
                    Utils.formatLog("failed update rtn trade", trade.OrderRef,
                            th.getMessage(), null));
        }
    }

    protected void doQueryInstr() {
        var req = new CThostFtdcQryInstrumentField();
        var r = this.traderApi.ReqQryInstrument(req, Utils.getIncrementID());
        if (r != 0)
            this.config.getLogger().warning(
                    Utils.formatLog("failed query instrument", null,
                            null, r));
    }

    protected void cancelInputOrder(CThostFtdcInputOrderField inputOrder) {
        var cancel = toRtnOrder(inputOrder);
        // Order status.
        cancel.OrderStatus = TThostFtdcOrderStatusType.CANCELED;
        cancel.OrderSubmitStatus = TThostFtdcOrderSubmitStatusType.CANCEL_SUBMITTED;
        doRtnOrder(cancel);
        // Write input order as normal cancel order.
        this.msgWriter.writeRtn(cancel);
    }

    @Override
    public void OnFrontConnected() {
        this.isConnected = true;
        if (this.workingState == WorkingState.STARTING
                || this.workingState == WorkingState.STARTED)
            doAuthentication();
    }

    @Override
    public void OnFrontDisconnected(int reason) {
        this.config.getLogger().warning(
                Utils.formatLog("trader disconnected", null,
                        null, reason));
        this.isConnected = false;
        this.isConfirmed = false;
        // Clear trading day.
        ConfigLoader.setTradingDay(null);
        // Don't change working state here because it may disconnect in half way.
    }

    @Override
    public void OnErrRtnOrderAction(CThostFtdcOrderActionField orderAction,
                                    CThostFtdcRspInfoField rspInfo) {
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

    @Override
    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField inputOrder,
                                    CThostFtdcRspInfoField rspInfo) {
        this.config.getLogger().severe(
                Utils.formatLog("failed order insertion", inputOrder.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
        // Failed order results in canceling the order.
        cancelInputOrder(inputOrder);
        this.msgWriter.writeErr(inputOrder);
        this.msgWriter.writeErr(rspInfo);
    }

    @Override
    public void OnRspAuthenticate(
            CThostFtdcRspAuthenticateField rspAuthenticateField,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {
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

    @Override
    public void OnRspError(CThostFtdcRspInfoField rspInfo, int requestId,
                           boolean isLast) {
        this.msgWriter.writeErr(rspInfo);
        this.config.getLogger().severe(
                Utils.formatLog("unknown error", null, rspInfo.ErrorMsg,
                        rspInfo.ErrorID));
    }

    @Override
    public void OnRspOrderAction(CThostFtdcInputOrderActionField inputOrderAction,
                                 CThostFtdcRspInfoField rspInfo, int requestId,
                                 boolean isLast) {
        this.msgWriter.writeErr(inputOrderAction);
        this.msgWriter.writeErr(rspInfo);
        this.config.getLogger().warning(
                Utils.formatLog("failed action", inputOrderAction.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
    }

    @Override
    public void OnRspOrderInsert(CThostFtdcInputOrderField inputOrder,
                                 CThostFtdcRspInfoField rspInfo, int requestId,
                                 boolean isLast) {
        this.config.getLogger().severe(
                Utils.formatLog("failed order insertion", inputOrder.OrderRef,
                        rspInfo.ErrorMsg, rspInfo.ErrorID));
        // Failed order results in canceling the order.
        cancelInputOrder(inputOrder);
        this.msgWriter.writeErr(inputOrder);
        this.msgWriter.writeErr(rspInfo);
    }

    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField instrument,
                                   CThostFtdcRspInfoField rspInfo, int requestId,
                                   boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            var accepted = InstrumentFilter.accept(instrument.InstrumentID);
            if (accepted) {
                this.msgWriter.writeRsp(instrument);
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
        if (this.qryInstrLast)
            signalLastInstrument();
    }

    @Override
    public void OnRspQryInstrumentCommissionRate(
            CThostFtdcInstrumentCommissionRateField instrumentCommissionRate,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.msgWriter.writeRsp(instrumentCommissionRate);
            ConfigLoader.setInstrConfig(instrumentCommissionRate);
        } else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed commission query", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    @Override
    public void OnRspQryInstrumentMarginRate(
            CThostFtdcInstrumentMarginRateField instrumentMarginRate,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.msgWriter.writeRsp(instrumentMarginRate);
            ConfigLoader.setInstrConfig(instrumentMarginRate);
        } else {
            this.config.getLogger().severe(
                    Utils.formatLog("failed margin query", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    @Override
    public void OnRspSettlementInfoConfirm(
            CThostFtdcSettlementInfoConfirmField settlementInfoConfirm,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {
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

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField rspUserLogin,
                               CThostFtdcRspInfoField rspInfo, int requestId,
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
            this.workingState =  WorkingState.STOPPED;
        }
    }

    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField userLogout,
                                CThostFtdcRspInfoField rspInfo, int requestId,
                                boolean isLast) {
        if (rspInfo.ErrorID == 0) {
            this.config.getLogger().fine(
                    Utils.formatLog("successful logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.isConfirmed = false;
            this.workingState = WorkingState.STOPPED;
            // Clear trading day.
            ConfigLoader.setTradingDay(null);
        } else {
            this.config.getLogger().warning(
                    Utils.formatLog("failed logout", null,
                            rspInfo.ErrorMsg, rspInfo.ErrorID));
            this.msgWriter.writeErr(rspInfo);
        }
    }

    @Override
    public void OnRtnOrder(CThostFtdcOrderField order) {
        doRtnOrder(order);
        // The codes below follow the doXXX method because the parameter's fields
        // were rewritten by the method, with local IDs.
        this.msgWriter.writeRtn(order);
        this.mapper.register(order);
    }

    @Override
    public void OnRtnTrade(CThostFtdcTradeField trade) {
        doRtnTrade(trade);
        // The writing method must follow the doXXX method because the fields are
        // rewritten with local IDs.
        this.msgWriter.writeRtn(trade);
    }

    protected static class PendingRequest {
        final ActiveRequest active;
        final CThostFtdcInputOrderField order;
        final CThostFtdcInputOrderActionField action;

        PendingRequest(CThostFtdcInputOrderField order, ActiveRequest active) {
            this.order = order;
            this.action = null;
            this.active = active;
        }

        PendingRequest(CThostFtdcInputOrderActionField action, ActiveRequest active) {
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

        protected int fillAndSendOrder(CThostFtdcInputOrderField detail) {
            // Set correct users.
            detail.BrokerID = rspLogin.BrokerID;
            detail.UserID = rspLogin.UserID;
            detail.InvestorID = rspLogin.UserID;
            // Adjust flags.
            detail.CombHedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;
            detail.ContingentCondition = TThostFtdcContingentConditionType.IMMEDIATELY;
            detail.ForceCloseReason = TThostFtdcForceCloseReasonType.NOT_FORCE_CLOSE;
            detail.IsAutoSuspend = 0;
            detail.MinVolume = 1;
            detail.OrderPriceType = TThostFtdcOrderPriceTypeType.LIMIT_PRICE;
            detail.StopPrice = 0;
            detail.TimeCondition = TThostFtdcTimeConditionType.GFD;
            detail.VolumeCondition = TThostFtdcVolumeConditionType.ANY_VOLUME;
            return traderApi.ReqOrderInsert(detail, Utils.getIncrementID());
        }

        protected int fillAndSendAction(CThostFtdcInputOrderActionField action) {
            var instrInfo = config.getInstrInfo(action.InstrumentID);
            var rtn = mapper.getRtnOrder(action.OrderRef);
            if (rtn != null) {
                action.BrokerID = rspLogin.BrokerID;
                action.InvestorID = rspLogin.UserID;
                action.UserID = rspLogin.UserID;
                // Use order sys ID as first choice.
                action.OrderSysID = rtn.OrderSysID;
                // Adjust flags.
                action.ActionFlag = TThostFtdcActionFlagType.DELETE;
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
                action.ActionFlag = TThostFtdcActionFlagType.DELETE;
                // Adjust other info.
                action.OrderSysID = null;
                action.ExchangeID = (instrInfo.Instrument != null)
                        ? instrInfo.Instrument.ExchangeID : null;
            }
            return traderApi.ReqOrderAction(action, Utils.getIncrementID());
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
            else
                return pend.order.InstrumentID;
        }

        protected void warn(int r, PendingRequest pend) {
            String ref, hint;
            if (pend.order != null) {
                ref = pend.order.OrderRef;
                hint = "failed sending order";
            } else {
                ref = pend.action.OrderRef;
                hint = "failed sending action";
            }
            config.getLogger().warning(
                    Utils.formatLog(hint, ref, null, r));
        }
    }

    protected class QueryTask extends TimerTask {
        protected final Random rand = new Random();

        @Override
        public void run() {
            if (!qryInstrLast || !isConfirmed)
                return;
            try {
                doQuery();
            } catch (Throwable th) {
                th.printStackTrace();
                config.getLogger().warning(th.getMessage());
            }
        }

        protected void doQuery() {
            String ins = randomGet();
            var in = config.getInstrInfo(ins);
            // Query margin.
            if (in.Margin == null) {
                var req = new CThostFtdcQryInstrumentMarginRateField();
                req.BrokerID = loginCfg.BrokerID;
                req.InvestorID = loginCfg.UserID;
                req.HedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;
                req.InstrumentID = ins;
                int r = traderApi.ReqQryInstrumentMarginRate(req,
                        Utils.getIncrementID());
                if (r != 0)
                    config.getLogger().warning(
                            Utils.formatLog("failed query margin",
                                    null, ins, r));
                // Sleep for 1.5 seconds
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    config.getLogger().warning(
                            Utils.formatLog("failed sleep", null,
                                    e.getMessage(), null));
                }
            }
            // Query commission.
            if (in.Commission == null) {
                var req0 = new CThostFtdcQryInstrumentCommissionRateField();
                req0.BrokerID = loginCfg.BrokerID;
                req0.InvestorID = loginCfg.UserID;
                req0.InstrumentID = ins;
                var r = traderApi.ReqQryInstrumentCommissionRate(req0,
                        Utils.getIncrementID());
                if (r != 0)
                    config.getLogger().warning(
                            Utils.formatLog("failed query commission",
                                    null, ins, r));
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
