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
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.*;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInputOrderField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcOrderField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcTradeField;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class UserSettleTest extends UserSuperTest {
    private void checkSettle() {
        double settlementPrice = 2120.0D;

        var active = new ActiveUser(user, provider, config);
        config.getDepthMarketData("c2101").SettlementPrice = settlementPrice;

        try {
            active.settle();
        } catch (Throwable th) {
            fail(th.getMessage());
        }

        // Get instrument info.
        var instrument = config.getInstrInfo("c2101").Instrument;
        var margin = config.getInstrInfo("c2101").Margin;
        int totalVolume = 0;

        var positions = user.getUserPosition().getSpecificPosition("c2101");
        for (var position : positions) {
            var rawPosition = position.copyRawPosition();
            double profitByDate, profitByTrade;
            if (rawPosition.TradingDay.compareTo(config.getTradingDay()) != 0)
                profitByDate = (rawPosition.SettlementPrice - rawPosition.LastSettlementPrice)
                        * rawPosition.Volume * instrument.VolumeMultiple;
            else
                profitByDate = (rawPosition.SettlementPrice - rawPosition.OpenPrice)
                        * rawPosition.Volume * instrument.VolumeMultiple;
            profitByTrade = (rawPosition.SettlementPrice - rawPosition.OpenPrice)
                    * rawPosition.Volume * instrument.VolumeMultiple;
            if (rawPosition.Direction == TThostFtdcDirectionType.DIRECTION_SELL) {
                profitByDate *= -1;
                profitByTrade *= -1;
            }
            // Check settled fields.
            assertEquals(rawPosition.SettlementPrice, settlementPrice, 0.0);
            assertEquals(
                    rawPosition.Margin,
                    rawPosition.Volume * rawPosition.MarginRateByMoney
                            * instrument.VolumeMultiple
                            * rawPosition.SettlementPrice,
                    0.01);
            assertEquals(rawPosition.PositionProfitByDate, profitByDate, 0.0);
            assertEquals(rawPosition.PositionProfitByTrade, profitByTrade, 0.0);
            // Update total volume for checking account.
            totalVolume += rawPosition.Volume;
        }

        var account = active.getTradingAccount();
        assertEquals(
                account.CurrMargin,
                totalVolume * settlementPrice * instrument.VolumeMultiple
                        * margin.LongMarginRatioByMoney,
                0.0);
    }

    @Test
    public void just_settle() {
        prepare();
        checkSettle();
    }

    @Test
    public void open_settle() {
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

        checkSettle();
    }

    @Test
    public void close_settle() {
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

        checkSettle();
    }
}
