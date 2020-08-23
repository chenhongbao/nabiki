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
import com.nabiki.centre.user.core.plain.PositionTradedCash;
import com.nabiki.centre.user.core.plain.ProcessStage;
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.CInstrument;
import com.nabiki.objects.CInvestorPositionDetail;
import com.nabiki.objects.CTrade;
import com.nabiki.objects.DirectionType;

import java.util.Objects;

public class FrozenPositionDetail {
    // The original position that own this frozen position.
    private final UserPositionDetail parent;
    private final AccountFrozenCash frozenSingleCash;
    // The share is asset change for each share before real trade happens.
    // When trade returns, use the return trade to generate another share info
    // to update the position.
    // This share info is used to calculate frozen margin or commission.
    private final CInvestorPositionDetail frozenSinglePosition;

    private final long totalFrozenCount;
    private ProcessStage stage = ProcessStage.ONGOING;
    private long tradedCount = 0;

    public FrozenPositionDetail(UserPositionDetail parent,
                                CInvestorPositionDetail frzSinglePos,
                                AccountFrozenCash frzCash,
                                long frzCount) {
        this.parent = parent;
        this.frozenSinglePosition = frzSinglePos;
        this.frozenSingleCash = frzCash;
        this.totalFrozenCount = frzCount;
    }

    /**
     * Get frozen volume of this position detail.
     *
     * @return frozen volume
     */
    public long getFrozenVolume() {
        if (this.stage == ProcessStage.CANCELED)
            return 0;
        else
            return this.totalFrozenCount - tradedCount;
    }

    /**
     * Close some volume(a part or all) of a close order.
     *
     * <p>When a close order trades, its frozen volume is decreased. If a close
     * order is canceled, all its frozen volume is released.
     * </p>
     *
     * @param trade trade response
     * @param instr instrument
     */
    public void applyCloseTrade(CTrade trade,
                                CInstrument instr) {
        if (trade.Volume < 0)
            throw new IllegalArgumentException("negative traded share count");
        if (getFrozenVolume() < trade.Volume)
            throw new IllegalStateException("not enough frozen shares");
        this.tradedCount += trade.Volume;
        // Update parent.
        var single = toSingleTradedCash(this.frozenSinglePosition, trade, instr);
        this.parent.closePosition(single, trade.Volume);
    }

    /**
     * Cancel a close order whose frozen volume is all released.
     */
    public void cancel() {
        this.stage = ProcessStage.CANCELED;
    }

    /**
     * Get the position detail represents 1 share of the closed position, whose
     * close profit and close amount are pre-calculated. And the margin and exchange
     * margin are set for 1 volume, and close volume is set to 1.
     *
     * @return pre-calculated closed position detail for 1 volume.
     */
    public CInvestorPositionDetail getSingleFrozenPosition() {
        return Utils.deepCopy(this.frozenSinglePosition);
    }

    /**
     * Add this frozen position detail to its parent's frozen list. Then this
     * position is calculated as frozen.
     */
    public void setFrozen() {
        this.parent.addFrozenPosition(this);
    }

    AccountFrozenCash getSingleFrozenCash() {
        return Utils.deepCopy(this.frozenSingleCash);
    }

    private PositionTradedCash toSingleTradedCash(
            CInvestorPositionDetail p,
            CTrade trade,
            CInstrument instr) {
        var r = new PositionTradedCash();
        Objects.requireNonNull(r, "failed deep copy");
        // Calculate position detail.
        r.CloseAmount = trade.Price * instr.VolumeMultiple;
        r.CloseVolume = 1;
        double token;
        if (p.Direction == DirectionType.DIRECTION_BUY)
            token = 1.0D;   // Long position.
        else
            token = -1.0D;  // Short position.
        r.CloseProfitByTrade = token * (trade.Price - p.OpenPrice)
                * instr.VolumeMultiple;
        if (p.TradingDay.compareTo(trade.TradingDay) == 0)
            // Today's position.
            r.CloseProfitByDate = r.CloseProfitByTrade;
        else
            // History position.
            r.CloseProfitByDate = token * (trade.Price - p.LastSettlementPrice)
                    * instr.VolumeMultiple;
        return r;
    }
}
