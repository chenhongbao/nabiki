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

import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.ConfigLoader;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.TThostFtdcCombHedgeFlagType;
import com.nabiki.ctp4j.jni.flag.TThostFtdcDirectionType;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;
import org.junit.Assert;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UserAccountTest {

    private Config config;
    private User user;
    private OrderProvider provider;
    private final String flowDir = "C:\\Users\\chenh\\Desktop";

    private void prepare() {
        try {
            this.config = ConfigLoader.load();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
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
        depth.TradingDay = Utils.getDay(LocalDate.now(), null);
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
        var account = new CThostFtdcTradingAccountField();
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
    }

}