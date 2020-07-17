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

import com.nabiki.centre.user.core.plain.AccountFrozenCash;
import com.nabiki.centre.user.core.plain.PositionFrozenCash;
import com.nabiki.centre.user.core.plain.PositionTradedCash;
import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.TThostFtdcDirectionType;
import com.nabiki.ctp4j.jni.struct.*;

import java.util.*;

public class UserPosition {
    private final User parent;
    // Instrument ID -> Position detail.
    private final Map<String, List<UserPositionDetail>> positionMap = new HashMap<>();

    public UserPosition(Map<String, List<UserPositionDetail>> map, User parent) {
        this.positionMap.putAll(map);
        this.parent = parent;
    }

    User getParent() {
        return this.parent;
    }

    /**
     * Get the specified position details of an instrument. If user doesn't have
     * position of the instrument, return {@code null}.
     *
     * @param instrID instrument ID
     * @return position details of the instrument
     */
    public List<UserPositionDetail> getSpecificPosition(String instrID) {
        return this.positionMap.get(instrID);
    }

    public Map<String, List<UserPositionDetail>> getPositionMap() {
        return this.positionMap;
    }

    PositionFrozenCash getPositionFrozenCash() {
        var r = new PositionFrozenCash();
        r.FrozenCommission = 0;
        r.FrozenMargin = 0;
        for (var lst : this.positionMap.values())
            for (var p : lst) {
                r.FrozenCommission = p.getFrozenCommission();
                r.FrozenMargin += p.getFrozenMargin();
            }
        return r;
    }

    public PositionTradedCash getMoneyAfterTrade() {
        var r = new PositionTradedCash();
        r.Margin = 0.;
        r.CloseProfitByTrade = 0.;
        r.CloseProfitByDate = 0;
        r.PositionProfitByTrade = 0;
        r.PositionProfitByDate = 0;
        for (var lst : this.positionMap.values())
            for (var c : lst) {
                var p = c.copyRawPosition();
                r.Margin += p.Margin;
                r.CloseProfitByDate += p.CloseProfitByDate;
                r.CloseProfitByTrade += p.CloseProfitByTrade;
                r.PositionProfitByDate += p.PositionProfitByDate;
                r.PositionProfitByTrade += p.PositionProfitByTrade;
            }
        return r;
    }

    public void applyOpenTrade(CThostFtdcTradeField trade,
                               CThostFtdcInstrumentField instr,
                               CThostFtdcInstrumentMarginRateField margin,
                               double preSettlementPrice) {
        getSpecificPosition(trade.InstrumentID)
                .add(toUserPosition(trade, instr, margin, preSettlementPrice));
    }

    /**
     * Calculate the frozen position. But the frozen position is not written to
     * the user position detail. Only after the request is sent successfully, the
     * frozen position is added to the frozen list.
     *
     * @param order input order, must be close order
     * @param instr instrument
     * @param comm commission
     * @param tradingDay trading day
     * @return list of frozen position detail if the order is sent successfully
     */
    public List<FrozenPositionDetail> peakCloseFrozen(
            CThostFtdcInputOrderField order,
            CThostFtdcInstrumentField instr,
            CThostFtdcInstrumentCommissionRateField comm,
            String tradingDay) {
        // Get position details.
        var avail = getSpecificPosition(order.InstrumentID);
        Objects.requireNonNull(avail, "user position null");
        // Trading day not null.
        Objects.requireNonNull(tradingDay, "trading day null");
        // Calculate frozen position detail.
        int volume = order.VolumeTotalOriginal;
        var r = new LinkedList<FrozenPositionDetail>();
        for (var a : avail) {
            // Calculate shares.
            // No need to calculate close profits and amount. They will be updated
            // on return trade.
            var rawPos = a.copyRawPosition();
            if (rawPos.Direction == order.Direction)
                continue;
            // Buy open -> sell close, sell open -> buy close.
            // The directions must be different.
            rawPos.ExchMargin /= 1.0D * rawPos.Volume;
            rawPos.Margin /= 1.0D * rawPos.Volume;
            rawPos.Volume
                    = rawPos.CloseVolume = 1;
            // Commission.
            var frzCash = new AccountFrozenCash();
            if (rawPos.TradingDay.compareTo(tradingDay) == 0) {
                // Today position.
                if (comm.CloseTodayRatioByMoney > 0)
                    frzCash.FrozenCommission = order.LimitPrice
                            * instr.VolumeMultiple * comm.CloseTodayRatioByMoney;
                else
                    frzCash.FrozenCommission = comm.CloseTodayRatioByVolume;
            } else {
                // YD position.
                if (comm.CloseRatioByMoney > 0)
                    frzCash.FrozenCommission = order.LimitPrice
                            * instr.VolumeMultiple * comm.CloseRatioByMoney;
                else
                    frzCash.FrozenCommission = comm.CloseRatioByVolume;
            }
            // Keep frozen position.
            long vol = Math.min(a.getAvailableVolume(), volume);
            var frz = new FrozenPositionDetail(a, rawPos, frzCash, vol);
            r.add(frz);
            // Reduce volume to zero.
            if ((volume -= vol) <= 0)
                break;
        }
        if (volume > 0)
            return null; // Failed to ensure position to close.
        else
            return r;
    }

    /**
     * Settle position.
     *
     * @param prep settlement information
     */
    public void settle(SettlementPreparation prep) {
        if (this.positionMap.size() == 0)
            return;
        // Place to keep the settled position.
        var settledPos = new HashMap<String, List<UserPositionDetail>>();
        // Settle all positions.
        for (var entry : this.positionMap.entrySet()) {
            var id = entry.getKey();
            // Acquire settlement price.
            var depthSettle = prep.getDepth(id);
            Objects.requireNonNull(depthSettle, "depth settlement null");
            var settlementPrice = depthSettle.SettlementPrice;
            if (!Utils.validPrice(settlementPrice))
                throw new IllegalStateException("no settlement price for " + id);
            // Get information.
            var instr = prep.getInstrument(id);
            var margin = prep.getMargin(id);
            Objects.requireNonNull(instr, "instrument null");
            Objects.requireNonNull(margin, "margin null");
            // Settle specific instrument.
            var r = settleSpecificInstrument(entry.getValue(), settlementPrice,
                    instr.VolumeMultiple, prep.getTradingDay());
            // Only keep the non-zero position.
            if (r.size() > 0)
                settledPos.put(id, r);
        }
        this.positionMap.clear();
        this.positionMap.putAll(settledPos);
    }

    /**
     * Get all own instrument IDs.
     *
     * @return set of instrument ID
     */
    public Set<String> getPositionInstrID() {
        return this.positionMap.keySet();
    }

    private List<UserPositionDetail> settleSpecificInstrument(
            Collection<UserPositionDetail> position,
            double settlementPrice,
            int volumeMultiple,
            String tradingDay) {
        // Settled position to bre return.
        var settledPosition =  new LinkedList<UserPositionDetail>();
        for (var p : position) {
            // Unset frozen position.
            p.cancel();
            /*
            Keep original volume because the close volume is also kept.
            When the settled position loaded for next day, the volume and close
            volume/amount/profit will be adjusted:
            1. volume -= close volume
            2. close volume = 0
            3. close amount = 0;
            4/ close profits = 0;
             */
            var origin = p.copyRawPosition();
            origin.SettlementPrice = settlementPrice;
            var volume = origin.Volume - origin.CloseVolume;
            if (volume == 0)
                continue;
            if (volume < 0)
                throw new IllegalStateException("position volume less than zero");
            // Calculate new position detail, the close profit/volume/amount are
            // updated on return trade, just calculate the position's fields.
            double token;
            if (origin.Direction == TThostFtdcDirectionType.DIRECTION_BUY) {
                // Margin.
                if (origin.MarginRateByMoney > 0)
                    origin.Margin = volume * origin.SettlementPrice
                            * volumeMultiple * origin.MarginRateByMoney;
                else
                    origin.Margin = volume * origin.MarginRateByVolume;
                // Long position, token is positive.
                token = 1.0D;
            } else {
                // Margin.
                if (origin.MarginRateByMoney > 0)
                    origin.Margin = volume * origin.SettlementPrice
                            * volumeMultiple * origin.MarginRateByMoney;
                else
                    origin.Margin = volume * origin.MarginRateByVolume;
                // Short position, token is negative.
                token = -1.0D;
            }
            // ExchMargin.
            origin.ExchMargin = origin.Margin;
            // Position profit.
            origin.PositionProfitByTrade = token * volume *
                    (origin.SettlementPrice - origin.OpenPrice) * volumeMultiple;
            if (origin.TradingDay.compareTo(tradingDay) == 0)
                // Today position, open price is real open price.
                origin.PositionProfitByDate = origin.PositionProfitByTrade;
            else
                // History position, open price is last settlement price.
                origin.PositionProfitByDate = token * volume *
                        (origin.SettlementPrice - origin.LastSettlementPrice)
                        * volumeMultiple;
            // Save settled position.
            settledPosition.add(new UserPositionDetail(origin));
        }
        return settledPosition;
    }

    private UserPositionDetail toUserPosition(
            CThostFtdcTradeField trade,
            CThostFtdcInstrumentField instr,
            CThostFtdcInstrumentMarginRateField margin,
            double preSettlementPrice) {
        var d = new CThostFtdcInvestorPositionDetailField();
        d.InvestorID = trade.InvestorID;
        d.BrokerID = trade.BrokerID;
        d.Volume = trade.Volume;
        d.OpenPrice = trade.Price;
        d.Direction = trade.Direction;
        d.ExchangeID = trade.ExchangeID;
        d.HedgeFlag = trade.HedgeFlag;
        d.InstrumentID = trade.InstrumentID;
        d.OpenDate = trade.TradeDate;
        d.TradeID = trade.TradeID;
        d.TradeType = trade.TradeType;
        d.TradingDay = trade.TradingDay;
        d.InvestUnitID = trade.InvestUnitID;
        d.SettlementID = trade.SettlementID;
        // Calculate margin.
        // Decide margin rates.
        if (d.Direction == TThostFtdcDirectionType.DIRECTION_BUY) {
            d.MarginRateByMoney = margin.LongMarginRatioByMoney;
            d.MarginRateByVolume = margin.LongMarginRatioByVolume;
        } else {
            d.MarginRateByMoney = margin.ShortMarginRatioByMoney;
            d.MarginRateByVolume = margin.ShortMarginRatioByVolume;
        }
        // Calculate margin.
        d.LastSettlementPrice = preSettlementPrice;
        if (d.MarginRateByMoney > 0)
            d.Margin = d.Volume * instr.VolumeMultiple
                    * d.LastSettlementPrice * d.MarginRateByMoney;
        else
            d.Margin = d.Volume * d.MarginRateByVolume;
        // Default values.
        d.CloseVolume = 0;
        d.CloseAmount = 0.0D;
        d.PositionProfitByDate = 0.0D;
        d.PositionProfitByTrade = 0.0D;
        d.CloseProfitByDate = 0.0D;
        d.CloseProfitByTrade = 0.0D;
        d.SettlementPrice = 0.0D;
        d.TimeFirstVolume = 0;
        d.CombInstrumentID = "";
        return new UserPositionDetail(d);
    }
}
