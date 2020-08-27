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

import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Utils;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.*;

public class UserSettleTest extends UserSuperTest {
    private void checkSettle() {
        double settlementPrice = 2120.0D;

        var active = new ActiveUser(user, provider, global);
        global.getDepthMarketData("c2101").SettlementPrice = settlementPrice;

        var info = global.getInstrInfo("c2101");
        SettlementPreparation prep = new SettlementPreparation();
        prep.prepare(global.getTradingDay());
        prep.prepare(global.getDepthMarketData("c2101"));
        prep.prepare(info.Instrument);
        prep.prepare(info.Margin);
        prep.prepare(info.Commission);

        try {
            user.settle(prep);
        } catch (Throwable th) {
            fail(th.getMessage());
        }

        // Get instrument info.
        var instrument = global.getInstrInfo("c2101").Instrument;
        var margin = global.getInstrInfo("c2101").Margin;
        int totalVolume = 0;

        var positions = user.getUserPosition().getSpecificPosition("c2101");
        for (var position : positions) {
            var rawPosition = position.copyRawPosition();
            double profitByDate, profitByTrade;
            if (rawPosition.TradingDay.compareTo(global.getTradingDay()) != 0)
                profitByDate = (rawPosition.SettlementPrice - rawPosition.LastSettlementPrice)
                        * rawPosition.Volume * instrument.VolumeMultiple;
            else
                profitByDate = (rawPosition.SettlementPrice - rawPosition.OpenPrice)
                        * rawPosition.Volume * instrument.VolumeMultiple;
            profitByTrade = (rawPosition.SettlementPrice - rawPosition.OpenPrice)
                    * rawPosition.Volume * instrument.VolumeMultiple;
            if (rawPosition.Direction == DirectionType.DIRECTION_SELL) {
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

    private void writeSettledUser() {
        var todayDir = Path.of(flowDir, ".user", "0001",
                Utils.getDay(LocalDate.now(), null));
        var path = Path.of(todayDir.toString(), "account.0001.json");
        try {
            Utils.writeText(OP.toJson(user.getUserAccount().copyRawAccount()),
                    Utils.createFile(path, false),
                    StandardCharsets.UTF_8,
                    false);
            int count = 0;
            for (var position : user.getUserPosition().getSpecificPosition("c2101")) {
                path = Path.of(todayDir.toString(),
                        "position." + (++count) + ".json");
                Utils.writeText(OP.toJson(position.copyRawPosition()),
                        Utils.createFile(path, false),
                        StandardCharsets.UTF_8,
                        false);
            }
        } catch (IOException e) {
            fail(e.getMessage());
        }
    }

    private void checkRenewUser() {
        var userManager = UserManager.create(Path.of(flowDir, ".user"));
        try {
            userManager.load();
        } catch (Exception e) {
            fail(e.getMessage());
        }
        var renewUser = userManager.getUser("0001");

        // Check account.
        var renewActive = new ActiveUser(renewUser, provider, global);
        var renewAccount = renewActive.getTradingAccount();

        var active = new ActiveUser(user, provider, global);
        var account = active.getTradingAccount();

        assertEquals(renewAccount.PreMargin, account.CurrMargin, 0.01);
        assertEquals(renewAccount.PreBalance, account.Balance, 0.01);

        // Check positions.
        var renewPositions = renewActive.getPosition("c2101");
        var positions = active.getPosition("c2101");

        assertEquals(renewPositions.size(), positions.size());

        int renewVolume = 0, volume = 0;
        for (int i = 0; i < positions.size(); ++i) {
            renewVolume += renewPositions.get(i).Position;
            volume += positions.get(i).Position;
        }

        assertEquals(renewVolume, volume);
    }

    @Test
    public void just_settle() {
        prepare();
        checkSettle();
        // Test user manager.
        writeSettledUser();
        checkRenewUser();
    }

    @Test
    public void open_settle() {
        prepare();

        var order = new CInputOrder();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 10;
        order.CombOffsetFlag = CombOffsetFlagType.OFFSET_OPEN;
        order.CombHedgeFlag = CombHedgeFlagType.SPECULATION;
        order.Direction = DirectionType.DIRECTION_SELL;

        var active = new ActiveUser(user, provider, global);
        var uuid = active.insertOrder(order);

        // Sleep and wait for the thread to take request.
        sleep(1000);

        var orderRefs = provider.getMapper().getDetailRef(uuid);
        assertEquals(orderRefs.size(), 1);

        // Get order ref.
        var orderRef = orderRefs.iterator().next();

        // Construct rtn order.
        var rtnOrder = new COrder();
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
        rtnOrder.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
        rtnOrder.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
        rtnOrder.InsertDate = Utils.getDay(LocalDate.now(), null);
        rtnOrder.InsertTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.TradingDay = global.getTradingDay();

        // Update rtn order.
        provider.whenRtnOrder(rtnOrder);

        // Construct a traded return order.
        rtnOrder.VolumeTraded = 3;
        rtnOrder.VolumeTotal = rtnOrder.VolumeTotalOriginal - rtnOrder.VolumeTraded;
        rtnOrder.OrderStatus = OrderStatusType.PART_TRADED_QUEUEING;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        // Update order again.
        provider.whenRtnOrder(rtnOrder);

        // Check rtn order.
        assertEquals(active.getRtnOrder(uuid).size(), 1);

        var rtnOrder0 = active.getRtnOrder(uuid).iterator().next();
        assertNotNull(rtnOrder0);
        assertEquals(rtnOrder0.UpdateTime, rtnOrder.UpdateTime);

        // Construct rtn trade.
        var rtnTrade = new CTrade();
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
        provider.whenRtnTrade(rtnTrade);

        checkSettle();
    }

    @Test
    public void close_settle() {
        prepare();

        var order = new CInputOrder();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2120;
        order.VolumeTotalOriginal = 30;
        order.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE;
        order.Direction = DirectionType.DIRECTION_BUY;

        var active = new ActiveUser(user, provider, global);
        var uuid = active.insertOrder(order);

        sleep(1000);
        // Test frozen position.
        var frzPosition = active.getFrozenPositionDetail(uuid);
        assertEquals(frzPosition.values().size(), 2);

        // Rtn order.
        var refIter = frzPosition.keySet().iterator();
        var orderRef = refIter.next();

        // Construct rtn order.
        var rtnOrder = new COrder();
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
        rtnOrder.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
        rtnOrder.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
        rtnOrder.InsertDate = Utils.getDay(LocalDate.now(), null);
        rtnOrder.InsertTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);
        rtnOrder.TradingDay = global.getTradingDay();

        // Update rtn order.
        provider.whenRtnOrder(rtnOrder);

        // Construct a traded return order.
        rtnOrder.VolumeTraded = 3;
        rtnOrder.VolumeTotal = rtnOrder.VolumeTotalOriginal - rtnOrder.VolumeTraded;
        rtnOrder.OrderStatus = OrderStatusType.PART_TRADED_QUEUEING;
        rtnOrder.UpdateTime = Utils.getTime(LocalTime.now(), null);

        // Update order again.
        provider.whenRtnOrder(rtnOrder);

        // Construct rtn trade.
        var rtnTrade = new CTrade();
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
        provider.whenRtnTrade(rtnTrade);

        // Check account and position.
        var position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == PosiDirectionType.LONG)
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
        rtnOrder.OrderStatus = OrderStatusType.CANCELED;
        rtnOrder.CancelTime = Utils.getTime(LocalTime.now(), null);

        provider.whenRtnOrder(rtnOrder);

        // Cancel the other order.
        orderRef = refIter.next();
        rtnOrder.OrderRef = orderRef;
        rtnOrder.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE_TODAY;

        provider.whenRtnOrder(rtnOrder);

        // Check order status.
        var status = active.getRtnOrder(uuid);
        assertEquals(status.size(), 1);

        var s = status.iterator().next();
        assertEquals(s.OrderStatus, OrderStatusType.CANCELED);

        frzPosition = active.getFrozenPositionDetail(uuid);
        for (var p : frzPosition.values())
            assertEquals(p.getFrozenVolume(), 0);

        // Check account and position.
        position = active.getPosition("c2101");
        for (var p : position) {
            if (p.PosiDirection == PosiDirectionType.LONG)
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
