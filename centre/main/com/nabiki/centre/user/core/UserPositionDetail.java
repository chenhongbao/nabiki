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

import com.nabiki.centre.user.core.plain.PositionTradedCash;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.TThostFtdcDirectionType;
import com.nabiki.ctp4j.jni.flag.TThostFtdcPosiDirectionType;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInvestorPositionDetailField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInvestorPositionField;

import java.util.LinkedList;
import java.util.List;

public class UserPositionDetail {
    private final CThostFtdcInvestorPositionDetailField raw;
    private final List<FrozenPositionDetail> frozenPosition = new LinkedList<>();

    public UserPositionDetail(CThostFtdcInvestorPositionDetailField raw) {
        this.raw = Utils.deepCopy(raw);
    }

    /**
     * Close frozen position. The method updates the fields in original position
     * mainly the closeXXXX info and margin.
     *
     * @param share close info for 1 volume
     * @param tradeCnt closed volume
     */
    public void closePosition(PositionTradedCash share,
                              long tradeCnt) {
        this.raw.CloseAmount += share.CloseAmount * tradeCnt;
        this.raw.CloseProfitByDate += share.CloseProfitByDate * tradeCnt;
        this.raw.CloseProfitByTrade += share.CloseProfitByTrade * tradeCnt;
        this.raw.CloseVolume += share.CloseVolume * tradeCnt;
        this.raw.ExchMargin -= share.ExchMargin * tradeCnt;
        this.raw.Margin -= share.Margin * tradeCnt;
    }

    /**
     * Cancel an close order whose frozen volume is released.
     */
    public void cancel() {
        for (var frz : this.frozenPosition)
            frz.cancel();
    }

    public int getFrozenVolume() {
        int frozen = 0;
        for (var pd : frozenPosition)
            frozen += pd.getFrozenCount();
        return frozen;
    }

    /**
     * The currently available volume to close.
     *
     * @return available volume to close
     */
    public int getAvailableVolume() {
        return this.raw.Volume - this.raw.CloseVolume - getFrozenVolume();
    }

    double getFrozenMargin() {
        double frz = 0.0D;
        for (var c : this.frozenPosition)
            frz += c.getFrozenCount() * c.getSingleFrozenPosition().Margin;
        return frz;
    }

    double getFrozenCommission() {
        double frz = 0.0D;
        for (var c : this.frozenPosition)
            frz += c.getFrozenCount() * c.getSingleFrozenCash().FrozenCommission;
        return frz;
    }

    /**
     * Get a deep copy of the original position detail.
     *
     * @return a deep copy of original position detail
     */
    public CThostFtdcInvestorPositionDetailField copyRawPosition() {
        return Utils.deepCopy(this.raw);
    }

    /**
     * Summarize the position details and generate position report. The method needs
     * today's trading day to decide if the position is YD.
     *
     * @param tradingDay today's trading day
     * @return {@link CThostFtdcInvestorPositionField}
     */
    public CThostFtdcInvestorPositionField getInvestorPosition(String tradingDay) {
        var r = new CThostFtdcInvestorPositionField();
        // Only need the following 5 fields.
        r.YdPosition = 0;
        r.Position = 0;
        r.TodayPosition = 0;
        r.CloseVolume = 0;
        r.LongFrozen = 0;
        r.ShortFrozen = 0;
        // Prepare other fields.
        r.BrokerID = this.raw.BrokerID;
        r.ExchangeID = this.raw.ExchangeID;
        r.InvestorID = this.raw.InvestorID;
        r.PreSettlementPrice = this.raw.LastSettlementPrice;
        r.SettlementPrice = this.raw.SettlementPrice;
        r.InstrumentID = this.raw.InstrumentID;
        r.HedgeFlag = this.raw.HedgeFlag;
        // Calculate fields.
        if (this.raw.Direction == TThostFtdcDirectionType.DIRECTION_BUY) {
            r.PosiDirection = TThostFtdcPosiDirectionType.LONG;
            r.LongFrozen = getFrozenVolume();
        } else {
            r.PosiDirection = TThostFtdcPosiDirectionType.SHORT;
            r.ShortFrozen = getFrozenVolume();
        }
        r.CloseVolume = this.raw.CloseVolume;
        r.Position = this.raw.Volume - this.raw.CloseVolume;
        if (this.raw.TradingDay.compareTo(tradingDay) != 0)
            r.YdPosition = this.raw.Volume;
        else
            r.TodayPosition = r.Position;
        return r;
    }

    /**
     * Add frozen position for a close order.
     *
     * @param frzPosition new frozen position
     */
    public void addFrozenPosition(FrozenPositionDetail frzPosition) {
        this.frozenPosition.add(frzPosition);
    }
}
