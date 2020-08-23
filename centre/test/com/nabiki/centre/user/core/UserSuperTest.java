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
import com.nabiki.objects.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

public class UserSuperTest {

    protected Config config;
    protected User user;
    protected OrderProvider provider;
    protected final String flowDir = "C:\\Users\\chenh\\Desktop\\.root";
    protected CTradingAccount account;
    protected LocalDate tradingDay;
    protected CRspUserLogin rspLogin;

    protected void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void prepare() {
        ConfigLoader.rootPath = flowDir;

        try {
            this.config = ConfigLoader.config();
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        //
        // Set instrument information.
        //
        var instrument = new CInstrument();
        instrument.ExchangeID = "DCE";
        instrument.InstrumentID = "c2101";
        instrument.ExchangeInstID = "c2101";
        instrument.ProductID = "c";
        instrument.VolumeMultiple = 10;
        instrument.PriceTick = 1.0D;
        instrument.MaxLimitOrderVolume = 10000;
        instrument.MinLimitOrderVolume = 1;

        var commission = new CInstrumentCommissionRate();
        commission.InstrumentID = "c2101";
        commission.ExchangeID = "DCE";
        commission.CloseRatioByMoney = 0.0D;
        commission.CloseRatioByVolume = 0.0D;
        commission.CloseTodayRatioByMoney = 0.0D;
        commission.CloseTodayRatioByVolume = 1.2D;
        commission.OpenRatioByMoney = 0.0D;
        commission.OpenRatioByVolume = 1.2D;

        var margin = new CInstrumentMarginRate();
        margin.InstrumentID = "c2101";
        margin.ExchangeID = "DCE";
        margin.HedgeFlag = CombHedgeFlagType.SPECULATION;
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
        var depth = new CDepthMarketData();
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

        Map<String, List<UserPositionDetail>> map = new HashMap<>();
        map.put("c2101", new LinkedList<>());

        var currMargin = 0.0D;

        var p = new CInvestorPositionDetail();
        p.InstrumentID = "c2101";
        p.ExchangeID = "DCE";
        p.OpenPrice = 2050;
        p.Volume = 20;
        p.CloseVolume = 0;
        p.CloseAmount = 0;
        p.CloseProfitByDate = 0;
        p.CloseProfitByTrade = 0;
        p.LastSettlementPrice = config.getDepthMarketData(p.InstrumentID).PreSettlementPrice;
        p.HedgeFlag = CombHedgeFlagType.SPECULATION;

        // History position.
        p.OpenDate = Utils.getDay(openDate0, null);
        p.TradingDay = Utils.getDay(openDate0, null);
        p.Direction = DirectionType.DIRECTION_BUY;
        p.MarginRateByVolume = margin.LongMarginRatioByVolume;
        p.MarginRateByMoney = margin.LongMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        p.OpenDate = Utils.getDay(openDate0.plusDays(1), null);
        p.TradingDay = Utils.getDay(openDate0.plusDays(1), null);
        p.Direction = DirectionType.DIRECTION_SELL;
        p.MarginRateByVolume = margin.ShortMarginRatioByVolume;
        p.MarginRateByMoney = margin.ShortMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        // Today position.
        p.OpenDate = Utils.getDay(tradingDay, null);
        p.TradingDay = Utils.getDay(tradingDay, null);
        p.Direction = DirectionType.DIRECTION_SELL;
        p.MarginRateByVolume = margin.ShortMarginRatioByVolume;
        p.MarginRateByMoney = margin.ShortMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        p.Direction = DirectionType.DIRECTION_BUY;
        p.MarginRateByVolume = margin.LongMarginRatioByVolume;
        p.MarginRateByMoney = margin.LongMarginRatioByMoney;
        p.Margin = p.LastSettlementPrice * instrument.VolumeMultiple * p.Volume * p.MarginRateByMoney;

        currMargin += p.Margin;

        map.get("c2101").add(new UserPositionDetail(p));

        //
        // Trading account.
        //
        account = new CTradingAccount();
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
        provider = new OrderProvider(config);
        var rspInfo = new CRspInfo();
        rspInfo.ErrorID = 0;
        provider.whenRspSettlementInfoConfirm(
                new CSettlementInfoConfirm(),
                rspInfo,
                0,
                true);

        // Fake login.
        rspLogin = new CRspUserLogin();
        rspLogin.UserID = "0001";
        rspLogin.BrokerID = "9999";
        rspLogin.MaxOrderRef = "0";
        rspLogin.TradingDay = Utils.getDay(tradingDay, null);
        rspLogin.LoginTime = Utils.getTime(LocalTime.now(), null);
        rspLogin.SessionID = 2;
        rspLogin.FrontID = 1;
        provider.whenRspUserLogin(rspLogin, rspInfo, 1, true);
    }
}
