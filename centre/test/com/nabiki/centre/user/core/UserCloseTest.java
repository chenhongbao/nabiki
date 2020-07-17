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
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.*;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInputOrderField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcOrderField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcTradeField;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class UserCloseTest extends UserSuperTest {

    @Test
    public void checkInitPosition() {
        // Prepare account.
        prepare();

        // Test user state.
        assertEquals("user state should be renew",
                user.getState(), UserState.RENEW);

        // Test account info.
        var userPosition = user.getUserPosition();
        var frzCash = userPosition.getPositionFrozenCash();

        assertEquals(frzCash.FrozenCommission, 0, 0.0);
        assertEquals(frzCash.FrozenMargin, 0, 0.0);

        // Test position info.
        var active = new ActiveUser(user, provider, config);
        var positions = active.getPosition("c2101");
        for (var p : positions) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG) {
                assertEquals(p.Position, 40);
                assertEquals(p.YdPosition, 20);
                assertEquals(p.TodayPosition, 20);
                assertEquals(p.PreSettlementPrice,
                        config.getDepthMarketData("c2101").PreSettlementPrice,
                        0.0);
            } else {
                assertEquals(p.Position, 40);
                assertEquals(p.YdPosition, 20);
                assertEquals(p.TodayPosition, 20);
                assertEquals(p.PreSettlementPrice,
                        config.getDepthMarketData("c2101").PreSettlementPrice,
                        0.0);
            }
        }
    }

    @Test
    public void close() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 10;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE;
        order.Direction = TThostFtdcDirectionType.DIRECTION_BUY;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        // Test order exec state.
        var rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.NONE);

        // Test frozen volume.
        var frz = active.getFrozenAccount(uuid);
        assertNull(frz);

        // Test frozen position.
        var frzPosition = active.getFrozenPositionDetail(uuid);
        assertEquals(frzPosition.values().size(), 1);

        var p = frzPosition.values().iterator().next();
        assertEquals(p.getFrozenVolume(), 10);

        var sgFrzPos = p.getSingleFrozenPosition();
        assertEquals(sgFrzPos.Volume, 1);
        assertEquals(sgFrzPos.Margin,
                sgFrzPos.MarginRateByMoney * sgFrzPos.LastSettlementPrice
                        * config.getInstrInfo("c2101").Instrument.VolumeMultiple,
                0.0);

        var frzCash = p.getSingleFrozenCash();
        assertEquals(frzCash.FrozenCommission,
                order.VolumeTotalOriginal
                        * config.getInstrInfo("c2101").Commission.CloseRatioByVolume,
                0.0);
    }

    @Test
    public void close_fail() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 100;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE;
        order.Direction = TThostFtdcDirectionType.DIRECTION_BUY;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        sleep(1000);

        // Check rsp info.
        var rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.OVER_CLOSE_POSITION);

        var frzPosition = active.getFrozenPositionDetail(uuid);
        assertEquals(frzPosition.values().size(), 0);
    }

    @Test
    public void close_rtn() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 30;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE;
        order.Direction = TThostFtdcDirectionType.DIRECTION_BUY;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        sleep(1000);

        // Test order exec state.
        var rspInfo = active.getExecRsp(uuid);
        assertEquals(rspInfo.ErrorID, TThostFtdcErrorCode.NONE);

        // Test frozen volume.
        var frz = active.getFrozenAccount(uuid);
        assertNull(frz);

        // Test frozen position.
        var frzPosition = active.getFrozenPositionDetail(uuid);
        assertEquals(frzPosition.values().size(), 2);

        int totalCloseVolume = 0;
        var iter = frzPosition.keySet().iterator();
        while (iter.hasNext()) {
            var key = iter.next();
            var p = frzPosition.get(key);

            var sgFrzPos = p.getSingleFrozenPosition();
            var sgFrzCash = p.getSingleFrozenCash();

            if (sgFrzPos.TradingDay.compareTo(config.getTradingDay()) == 0) {
                assertEquals(p.getFrozenVolume(), 10);
                assertEquals(sgFrzCash.FrozenCommission,
                        config.getInstrInfo("c2101").Commission.CloseTodayRatioByVolume,
                        0.0);
            } else {
                assertEquals(p.getFrozenVolume(), 20);
                assertEquals(sgFrzCash.FrozenCommission, 0.0, 0.0);
            }

            assertEquals(sgFrzPos.Volume, 1);
            assertEquals(sgFrzPos.Margin,
                    sgFrzPos.MarginRateByMoney * sgFrzPos.LastSettlementPrice
                            * config.getInstrInfo("c2101").Instrument.VolumeMultiple,
                    0.0);
            // Count total.
            totalCloseVolume += p.getFrozenVolume();
        }

        assertEquals(totalCloseVolume, order.VolumeTotalOriginal);

        // Rtn order.
        var refIter = frzPosition.keySet().iterator();
        var orderRef = refIter.next();

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

        // Check account and position.
        var position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG)
                continue;
            // Close yd position first.
            assertEquals(p.CloseVolume, rtnOrder.VolumeTraded);
            assertEquals(p.Position, 40 - rtnOrder.VolumeTraded);
            assertEquals(p.YdPosition, 20);
        }

        // Check account.
        var sgMargin = active.getFrozenPositionDetail(uuid)
                .values()
                .iterator()
                .next()
                .getSingleFrozenPosition().Margin;

        var account = active.getTradingAccount();
        assertEquals(account.Commission, 0, 0.0);
        assertEquals(account.CurrMargin,
                account.PreMargin - sgMargin * 3,
                0.0);
        assertEquals(account.FrozenMargin, sgMargin * 27, 0.0);

        // Complete a frozen position.
        // Update order.
        rtnOrder.VolumeTraded = 20;
        rtnOrder.VolumeTotal = 0;
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.ALL_TRADED;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        provider.OnRtnOrder(rtnOrder);

        // Update trade.
        // **********************
        // Here is a WRONG trade.
        // **********************
        rtnTrade.Volume = 20;
        rtnTrade.TradeTime = Utils.getTime(LocalTime.now(), null);
        rtnTrade.TradeDate = Utils.getDay(LocalDate.now(), null);

        try {
            provider.OnRtnTrade(rtnTrade);
            fail("should throw exception");
        } catch (Throwable ignored) {
        }

        // Good trade.
        rtnTrade.Volume = 17;
        provider.OnRtnTrade(rtnTrade);

        // Check account and position.
        position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG)
                continue;
            // Close yd position first.
            assertEquals(p.CloseVolume, rtnOrder.VolumeTraded);
            assertEquals(p.Position, 40 - rtnOrder.VolumeTraded);
            assertEquals(p.YdPosition, 20);
        }

        // Complete the last frozen position.
        // Get the second order ref.
        orderRef = refIter.next();

        // *************************
        // It is a today's position.
        // *************************

        // Update order.
        rtnOrder.OrderRef = orderRef;
        rtnOrder.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE_TODAY;
        rtnOrder.VolumeTraded = 10;
        rtnOrder.VolumeTotal = 0;
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.ALL_TRADED;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        provider.OnRtnOrder(rtnOrder);

        // Update trade.
        rtnTrade.OrderRef = orderRef;
        rtnTrade.OffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE_TODAY;
        rtnTrade.Volume = 10;
        rtnTrade.TradeTime = Utils.getTime(LocalTime.now(), null);
        rtnTrade.TradeDate = Utils.getDay(LocalDate.now(), null);

        provider.OnRtnTrade(rtnTrade);

        // Check account and position.
        position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG)
                continue;
            // Close yd position first.
            assertEquals(p.CloseVolume, 30);
            assertEquals(p.Position, 10);
            assertEquals(p.YdPosition, 20);
        }

        // Check account.
        account = active.getTradingAccount();
        assertEquals(account.Commission, 12.0, 0.0);
        assertEquals(account.CurrMargin, sgMargin * 50, 0.0);
        assertEquals(account.FrozenMargin, 0.0, 0.0);
    }

    @Test
    public void close_rtn_cancel() {
        prepare();

        var order = new CThostFtdcInputOrderField();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 30;
        order.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE;
        order.Direction = TThostFtdcDirectionType.DIRECTION_BUY;

        var active = new ActiveUser(user, provider, config);
        var uuid = active.insertOrder(order);

        sleep(1000);
        // Test frozen position.
        var frzPosition = active.getFrozenPositionDetail(uuid);
        assertEquals(frzPosition.values().size(), 2);

        // Rtn order.
        var refIter = frzPosition.keySet().iterator();
        var orderRef = refIter.next();

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

        // Check account and position.
        var position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG)
                continue;
            // Close yd position first.
            assertEquals(p.CloseVolume, rtnOrder.VolumeTraded);
            assertEquals(p.Position, 40 - rtnOrder.VolumeTraded);
            assertEquals(p.YdPosition, 20);
        }

        // Check account.
        var sgMargin = active.getFrozenPositionDetail(uuid)
                .values()
                .iterator()
                .next()
                .getSingleFrozenPosition().Margin;

        var account = active.getTradingAccount();
        assertEquals(account.Commission, 0, 0.0);
        assertEquals(account.CurrMargin,
                account.PreMargin - sgMargin * 3,
                0.0);
        assertEquals(account.FrozenMargin, sgMargin * 27, 0.0);

        // Cancel the order.
        rtnOrder.OrderStatus = TThostFtdcOrderStatusType.CANCELED;
        rtnOrder.CancelTime = Utils.getTime(LocalTime.now(), null);

        provider.OnRtnOrder(rtnOrder);

        // Cancel the other order.
        orderRef = refIter.next();
        rtnOrder.OrderRef = orderRef;
        rtnOrder.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE_TODAY;

        provider.OnRtnOrder(rtnOrder);

        // Check order status.
        var status = active.getRtnOrder(uuid);
        assertEquals(status.size(), 1);

        var s = status.iterator().next();
        assertEquals(s.OrderStatus, TThostFtdcOrderStatusType.CANCELED);

        frzPosition = active.getFrozenPositionDetail(uuid);
        for (var p : frzPosition.values())
            assertEquals(p.getFrozenVolume(), 0);

        // Check account and position.
        position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == TThostFtdcPosiDirectionType.LONG)
                continue;
            // Close yd position first.
            assertEquals(p.CloseVolume, rtnOrder.VolumeTraded);
            assertEquals(p.Position, 40 - rtnOrder.VolumeTraded);
            assertEquals(p.YdPosition, 20);
        }

        // Check account.
        account = active.getTradingAccount();
        assertEquals(account.Commission, 0, 0.0);
        assertEquals(account.CurrMargin, sgMargin * 77, 0.0);
        assertEquals(account.FrozenMargin, 0, 0.0);
    }
}