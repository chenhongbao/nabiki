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

package com.nabiki.centre.user.core;

import com.nabiki.centre.active.ActiveUser;
import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.ConfigLoader;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.*;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;
import org.junit.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class UserTest {

    private Config config;
    private User user;
    private OrderProvider provider;
    private final String flowDir = "C:\\Users\\chenh\\Desktop\\.root";
    private CThostFtdcTradingAccountField account;
    private LocalDate tradingDay;
    private CThostFtdcRspUserLoginField rspLogin;

    private void prepare() {
        ConfigLoader.rootPath = flowDir;

        try {
            this.config = ConfigLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        //
        // Set instrument information.
        //
        var instrument = new CThostFtdcInstrumentField();
        instrument.ExchangeID = "DCE";
        instrument.InstrumentID = "c2101";
        instrument.ExchangeInstID = "c2101";
        instrument.ProductID = "c";
        instrument.VolumeMultiple = 10;
        instrument.PriceTick = 1.0D;
        instrument.MaxLimitOrderVolume = 10000;
        instrument.MinLimitOrderVolume = 1;

        var commission = new CThostFtdcInstrumentCommissionRateField();
        commission.InstrumentID = "c2101";
        commission.ExchangeID = "DCE";
        commission.CloseRatioByMoney = 0.0D;
        commission.CloseRatioByVolume = 0.0D;
        commission.CloseTodayRatioByMoney = 0.0D;
        commission.CloseTodayRatioByVolume = 1.2D;
        commission.OpenRatioByMoney = 0.0D;
        commission.OpenRatioByVolume = 1.2D;

        var margin = new CThostFtdcInstrumentMarginRateField();
        margin.InstrumentID = "c2101";
        margin.ExchangeID = "DCE";
        margin.HedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;
        margin.ShortMarginRatioByVolume = 0.0D;
        margin.ShortMarginRatioByMoney = 0.05D;
        margin.LongMarginRatioByVolume = 0.0D;
        margin.LongMarginRatioByMoney = 0.05D;

        ConfigLoader.setInstrConfig(instrument);
        ConfigLoader.setInstrConfig(commission);
        ConfigLoader.setInstrConfig(margin);

        //
        // Set trading day.
        //
        tradingDay = LocalDate.now();
        if (LocalTime.now().getHour() >= 21)
            tradingDay = tradingDay.plusDays(1);

        ConfigLoader.setTradingDay(Utils.getDay(tradingDay, null));

        //
        // Set depth market data.
        //
        var depth = new CThostFtdcDepthMarketDataField();
        depth.InstrumentID = "c2101";
        depth.ExchangeInstID = "c2101";
        depth.ExchangeID = "DCE";
        depth.PreSettlementPrice = 2100;
        depth.PreClosePrice = 2100;
        depth.PreOpenInterest = 100000;
        depth.AskPrice1 = 2110;
        depth.AskVolume1 = 1000;
        depth.BidPrice1 = 2109;
        depth.BidVolume1 = 1000;
        depth.LowestPrice = 2100;
        depth.HighestPrice = 2120;
        depth.OpenPrice = 2100;
        depth.LastPrice = 2110;
        depth.UpperLimitPrice = 2150;
        depth.LowerLimitPrice = 2050;
        depth.ActionDay = Utils.getDay(LocalDate.now(), null);
        depth.TradingDay = Utils.getDay(tradingDay, null);
        depth.UpdateTime = Utils.getTime(LocalTime.now(), null);
        depth.UpdateMillisec = 500;

        ConfigLoader.setDepthMarketData(depth);

        //
        // Set position.
        //
        var openDate0 = LocalDate.of(2020, 5, 13);
        var openDate1 = LocalDate.now();

        Map<String, List<UserPositionDetail>> map = new HashMap<>();
        map.put("c2101", new LinkedList<>());

        var currMargin = 0.0D;

        var p = new CThostFtdcInvestorPositionDetailField();
        p.InstrumentID = "c2101";
        p.ExchangeID = "DCE";
        p.OpenPrice = 2050;
        p.Volume = 20;
        p.CloseVolume = 0;
        p.CloseAmount = 0;
        p.CloseProfitByDate = 0;
        p.CloseProfitByTrade = 0;
        p.LastSettlementPrice = config.getDepthMarketData(p.InstrumentID).PreSettlementPrice;
        p.HedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;

        // History position.
        p.OpenDate = Utils.getDay(openDate0, null);
        p.TradingDay = Utils.getDay(openDate0, null);
        p.Direction = TThostFtdcDirectionType.DIRECTION_BUY;
        p.MarginRateByVolume = margin.LongMarginRatioByVolume;
        p.MarginRateByMoney = margin.LongMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        p.OpenDate = Utils.getDay(openDate0.plusDays(1), null);
        p.TradingDay = Utils.getDay(openDate0.plusDays(1), null);
        p.Direction = TThostFtdcDirectionType.DIRECTION_SELL;
        p.MarginRateByVolume = margin.ShortMarginRatioByVolume;
        p.MarginRateByMoney = margin.ShortMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        // Today position.
        p.OpenDate = Utils.getDay(openDate1, null);
        p.TradingDay = Utils.getDay(openDate1, null);
        p.Direction = TThostFtdcDirectionType.DIRECTION_SELL;
        p.MarginRateByVolume = margin.ShortMarginRatioByVolume;
        p.MarginRateByMoney = margin.ShortMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        p.Direction = TThostFtdcDirectionType.DIRECTION_BUY;
        p.MarginRateByVolume = margin.LongMarginRatioByVolume;
        p.MarginRateByMoney = margin.LongMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        //
        // Trading account.
        //
        account = new CThostFtdcTradingAccountField();
        account.BrokerID = "9999";
        account.AccountID = "0001";
        account.PreBalance = 800000.0D;
        account.PreDeposit = 0.0D;
        account.PreMargin = currMargin;

        //
        // Create user.
        //
        user = new User(account, map);

        //
        // Create provider.
        //
        provider = new OrderProvider(
                CThostFtdcTraderApi.CreateFtdcTraderApi(flowDir), config);
        var rspInfo = new CThostFtdcRspInfoField();
        rspInfo.ErrorID = 0;
        provider.OnRspSettlementInfoConfirm(
                new CThostFtdcSettlementInfoConfirmField(),
                rspInfo,
                0,
                true);

        // Fake login.
        rspLogin = new CThostFtdcRspUserLoginField();
        rspLogin.UserID = "0001";
        rspLogin.BrokerID = "9999";
        rspLogin.MaxOrderRef = "0";
        rspLogin.TradingDay = Utils.getDay(tradingDay, null);
        rspLogin.LoginTime = Utils.getTime(LocalTime.now(), null);
        rspLogin.SessionID = 2;
        rspLogin.FrontID = 1;
        provider.OnRspUserLogin(rspLogin, rspInfo, 1, true);
    }

    @Test
    public void checkInitAccount() {
        // Prepare account.
        prepare();

        // Test user state.
        assertEquals("user state should be renew",
                user.getState(), UserState.RENEW);

        // Test account info.
        var userAccount = user.getFeaturedAccount();
        assertEquals(userAccount.PreBalance, account.PreBalance, 0.0);
        assertEquals(userAccount.PreMargin, account.PreMargin, 0.0);
        assertEquals(userAccount.PreCredit, account.PreCredit, 0.0);
        assertEquals(userAccount.PreDeposit, account.PreDeposit, 0.0);
    }

    @Test
    public void open() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 10;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_OPEN;
        order.Direction = TThostFtdcDirectionType.DIRECTION_SELL;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        // Test order exec state.
        var rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.NONE);

        // Test frozen volume.
        var frz = active.getFrozenAccount(uuid);
        assertEquals(frz.getFrozenVolume(), order.VolumeTotalOriginal, 0.0D);

        // Test frozen cash.
        var cash = frz.getSingleFrozenCash();
        var frzCash = order.LimitPrice
                * config.getInstrInfo("c2101").Instrument.VolumeMultiple
                * config.getInstrInfo("c2101").Margin.ShortMarginRatioByMoney;
        var frzCommission
                = config.getInstrInfo("c2101").Commission.OpenRatioByVolume;
        assertEquals(cash.FrozenCash, frzCash, 0.0);
        assertEquals(cash.FrozenCommission, frzCommission, 0.0);

        // Test account.
        var userAccount = active.getTradingAccount();
        assertEquals(userAccount.CurrMargin, account.PreMargin, 0.0);
        assertEquals(userAccount.FrozenCash,
                frzCash * order.VolumeTotalOriginal, 0.0);
        assertEquals(userAccount.FrozenCommission,
                frzCommission * order.VolumeTotalOriginal, 0.0);
        assertEquals(userAccount.FrozenMargin, 0.0, 0.0);
        assertEquals(
                userAccount.Available,
                userAccount.Balance - userAccount.CurrMargin
                        - (frzCash + frzCommission) * order.VolumeTotalOriginal,
                0.0);
    }

    @Test
    public void open_fail() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        // Test bad price.
        order.LimitPrice = 5000;
        order.VolumeTotalOriginal = 10;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_OPEN;
        order.Direction = TThostFtdcDirectionType.DIRECTION_SELL;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        // Check order exec state.
        var rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.BAD_FIELD);

        // Test bad volume.
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = Integer.MAX_VALUE;

        active = new ActiveUser(user, provider, config);
        uuid = active.insertOrder(order);

        // Check order exec state.
        rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.BAD_FIELD);

        // Test insufficient money.
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 5000;

        active = new ActiveUser(user, provider, config);
        uuid = active.insertOrder(order);

        // Check order exec state.
        rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.INSUFFICIENT_MONEY);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void open_rtn() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 10;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_OPEN;
        order.CombHedgeFlag = TThostFtdcCombHedgeFlagType.SPECULATION;
        order.Direction = TThostFtdcDirectionType.DIRECTION_SELL;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        // Sleep and wait for the thread to take request.
        sleep(1000);

        var orderRefs = provider.getMapper().getDetailRef(uuid);
        assertEquals(orderRefs.size(), 1);

        // Get order ref.
        var orderRef = orderRefs.iterator().next();

        // Construct rtn order.
        var rtnOrder = new CThostFtdcOrderField();
        rtnOrder.InstrumentID = "c2101";
        rtnOrder.ExchangeInstID = "c2101";
        rtnOrder.ExchangeID = "DCE";
        rtnOrder.OrderRef = orderRef;
        rtnOrder.OrderLocalID = "test_order_local_id";
        rtnOrder.OrderSysID = "test_order_sys_id";
        rtnOrder.LimitPrice = order.LimitPrice;
        rtnOrder.VolumeTotalOriginal = order.VolumeTotalOriginal;
        rtnOrder.VolumeTraded = 0;
        rtnOrder.VolumeTotal = rtnOrder.VolumeTotalOriginal;
        rtnOrder.Direction = order.Direction;
        rtnOrder.CombOffsetFlag = order.CombOffsetFlag;
        rtnOrder.CombHedgeFlag = order.CombHedgeFlag;
        rtnOrder.OrderSubmitStatus = TThostFtdcOrderSubmitStatusType.ACCEPTED;
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.NO_TRADE_QUEUEING;
        rtnOrder.InsertDate = Utils.getDay(LocalDate.now(), null);
        rtnOrder.InsertTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.TradingDay = config.getTradingDay();

        // Update rtn order.
        provider.OnRtnOrder(rtnOrder);

        // Construct a traded return order.
        rtnOrder.VolumeTraded = 3;
        rtnOrder.VolumeTotal = rtnOrder.VolumeTotalOriginal - rtnOrder.VolumeTraded;
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.PART_TRADED_QUEUEING;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        // Update order again.
        provider.OnRtnOrder(rtnOrder);

        // Check rtn order.
        assertEquals(active.getRtnOrder(uuid).size(), 1);

        var rtnOrder0 = active.getRtnOrder(uuid).iterator().next();
        assertNotNull(rtnOrder0);
        assertEquals(rtnOrder0.UpdateTime, rtnOrder.UpdateTime);

        // Construct rtn trade.
        var rtnTrade = new CThostFtdcTradeField();
        rtnTrade.InstrumentID = "c2101";
        rtnTrade.ExchangeInstID = "c2101";
        rtnTrade.ExchangeID = "DCE";
        rtnTrade.OrderRef = orderRef;
        rtnTrade.OrderLocalID = "test_order_local_id";
        rtnTrade.OrderSysID = "test_order_sys_id";
        rtnTrade.Price = order.LimitPrice;
        rtnTrade.Volume = 3;
        rtnTrade.Direction = order.Direction;
        rtnTrade.OffsetFlag = order.CombOffsetFlag;
        rtnTrade.HedgeFlag = order.CombHedgeFlag;
        rtnTrade.TradingDay = Utils.getDay(tradingDay, null);
        rtnTrade.TradeTime = Utils.getTime(LocalTime.now(), null);
        rtnTrade.TradeDate = Utils.getDay(LocalDate.now(), null);

        // Update rtn trade.
        provider.OnRtnTrade(rtnTrade);

        // Check rtn trade.
        // Test frozen volume.
        var frz = active.getFrozenAccount(uuid);
        assertEquals(frz.getFrozenVolume(), rtnOrder.VolumeTotal, 0.0D);

        var userAccount = active.getTradingAccount();

        // Test frozen cash.
        var frzCash = order.LimitPrice
                * config.getInstrInfo("c2101").Instrument.VolumeMultiple
                * config.getInstrInfo("c2101").Margin.ShortMarginRatioByMoney;
        var frzCommission
                = config.getInstrInfo("c2101").Commission.OpenRatioByVolume;

        // Calculate used margin for new open position.
        var settlePrice = config.getDepthMarketData("c2101").PreSettlementPrice;
        var volumeMultiple = config.getInstrInfo("c2101").Instrument.VolumeMultiple;
        var marginRate = config.getInstrInfo("c2101").Margin.ShortMarginRatioByMoney;
        var singleMargin = settlePrice * volumeMultiple * marginRate;

        // Check fields.
        assertEquals(userAccount.FrozenCash,
                frzCash * rtnOrder.VolumeTotal, 0.0);
        assertEquals(userAccount.FrozenCommission,
                frzCommission * rtnOrder.VolumeTotal, 0.0);
        assertEquals(userAccount.FrozenMargin, 0.0, 0.0);
        assertEquals(userAccount.CurrMargin,
                singleMargin * rtnOrder.VolumeTraded + userAccount.PreMargin,
                0.0);
        assertEquals(
                userAccount.Available,
                userAccount.Balance - userAccount.CurrMargin
                        - (frzCash + frzCommission) * rtnOrder.VolumeTotal,
                0.0);

        // Complete trades.
        // Construct a traded return order.
        rtnOrder.VolumeTraded = rtnOrder.VolumeTotalOriginal;
        rtnOrder.VolumeTotal = 0;
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.ALL_TRADED;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        // Update order again.
        provider.OnRtnOrder(rtnOrder);

        // Check rtn order.
        assertEquals(active.getRtnOrder(uuid).size(), 1);

        rtnOrder0 = active.getRtnOrder(uuid).iterator().next();
        assertNotNull(rtnOrder0);
        assertEquals(rtnOrder0.VolumeTraded, rtnOrder.VolumeTotalOriginal);

        // Construct trade.
        rtnTrade.Volume = rtnOrder.VolumeTotalOriginal - rtnTrade.Volume;
        rtnTrade.TradeTime = Utils.getTime(LocalTime.now(), null);
        rtnTrade.TradeDate = Utils.getDay(LocalDate.now(), null);

        // Update trade.
        provider.OnRtnTrade(rtnTrade);

        // Test frozen volume.
        frz = active.getFrozenAccount(uuid);
        assertEquals(frz.getFrozenVolume(), 0, 0.0D);

        userAccount = active.getTradingAccount();

        // Test frozen cash.
        assertEquals(userAccount.FrozenCash, 0, 0.0);
        assertEquals(userAccount.FrozenCommission, 0, 0.0);
        assertEquals(userAccount.FrozenMargin, 0.0, 0.0);
        assertEquals(userAccount.CurrMargin,
                singleMargin * order.VolumeTotalOriginal + userAccount.PreMargin,
                0.0);
        assertEquals(userAccount.Commission, frzCommission * order.VolumeTotalOriginal, 0.0);
        assertEquals(
                userAccount.Available,
                userAccount.Balance - userAccount.CurrMargin,
                0.0);

        // Suppose exception.
        try {
            provider.OnRtnTrade(rtnTrade);
            fail("should throw exception");
        } catch (Throwable ignored) {
        }
    }
}