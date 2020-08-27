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
import com.nabiki.centre.user.core.plain.AccountTradedCash;
import com.nabiki.centre.user.core.plain.PositionTradedCash;
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.*;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class UserAccount {
    private final User parent;
    private final CTradingAccount raw;
    private final List<FrozenAccount> frozenAccount = new LinkedList<>();

    UserAccount(CTradingAccount raw, User parent) {
        this.raw = Utils.deepCopy(raw);
        this.parent = parent;
    }

    /**
     * Get the {@link User} that owns this account.
     *
     * @return user object that owns this account
     */
    User getParent() {
        return this.parent;
    }

    CTradingAccount copyRawAccount() {
        return Utils.deepCopy(this.raw);
    }

    /**
     * Add commission for traded order. The order can be open or close.
     *
     * @param trade the trade response
     * @param instr instrument
     * @param comm  commission
     */
    void applyTrade(CTrade trade,
                           CInstrument instr,
                           CInstrumentCommissionRate comm) {
        var cash = toTradedCash(trade, instr, comm);
        this.raw.Commission += cash.Commission;
    }

    /**
     * Cancel an open order and release all frozen cashes and commission.
     */
    void cancel() {
        for (var acc : this.frozenAccount)
            acc.cancel();
    }

    AccountFrozenCash getAccountFrozenCash() {
        var r = new com.nabiki.centre.user.core.plain.AccountFrozenCash();
        r.FrozenCash = 0;
        r.FrozenCommission = 0;
        for (var c : this.frozenAccount) {
            r.FrozenCash += c.getFrozenVolume() * c.getSingleFrozenCash().FrozenCash;
            r.FrozenCommission += c.getFrozenVolume()
                    * c.getSingleFrozenCash().FrozenCommission;
        }
        return r;
    }

    /**
     * Add frozen account for a new open order.
     *
     * @param frz new frozen account
     */
    void addFrozenAccount(FrozenAccount frz) {
        this.frozenAccount.add(frz);
    }

    FrozenAccount getOpenFrozenAccount(
            CInputOrder order, CInstrument instr,
            CInstrumentMarginRate margin,
            CInstrumentCommissionRate comm) {
        Objects.requireNonNull(instr, "instrument null");
        Objects.requireNonNull(margin, "margin null");
        Objects.requireNonNull(comm, "commission null");
        // Calculate commission, cash.
        var c = new AccountFrozenCash();
        if (order.Direction == DirectionType.DIRECTION_BUY) {
            if (margin.LongMarginRatioByMoney > 0)
                c.FrozenCash = order.LimitPrice * instr.VolumeMultiple
                        * margin.LongMarginRatioByMoney;
            else
                c.FrozenCash = margin.LongMarginRatioByVolume;
        } else {
            if (margin.ShortMarginRatioByMoney > 0)
                c.FrozenCash = order.LimitPrice * instr.VolumeMultiple
                        * margin.ShortMarginRatioByMoney;
            else
                c.FrozenCash = margin.ShortMarginRatioByVolume;
        }
        if (comm.OpenRatioByMoney > 0)
            c.FrozenCommission = order.LimitPrice * instr.VolumeMultiple
                    * comm.OpenRatioByMoney;
        else
            c.FrozenCommission = comm.OpenRatioByVolume;
        // Check if available money is enough.
        var needMoney = (c.FrozenCash + c.FrozenCommission)
                * order.VolumeTotalOriginal;
        var account = this.parent.getFeaturedAccount();
        if (account.Available < needMoney)
            return null;
        else
            return new FrozenAccount(this, c, order.VolumeTotalOriginal);
    }

    void settle(PositionTradedCash settlement, String tradingDay) {
        // Unset frozen account.
        cancel();
        // Calculate fields.
        this.raw.CurrMargin = settlement.Margin;
        this.raw.CloseProfit = settlement.CloseProfitByDate;
        this.raw.PositionProfit = settlement.PositionProfitByDate;
        this.raw.Balance = this.raw.PreBalance + (this.raw.Deposit - this.raw.Withdraw)
                + (this.raw.CloseProfit + this.raw.PositionProfit) - this.raw.Commission;
        this.raw.Available = this.raw.Balance - this.raw.CurrMargin;
        // Trading day.
        this.raw.TradingDay = tradingDay;
    }

    // Only calculate commission.
    private AccountTradedCash toTradedCash(
            CTrade trade,
            CInstrument instr,
            CInstrumentCommissionRate comm) {
        Objects.requireNonNull(comm, "commission null");
        Objects.requireNonNull(instr, "instrument null");
        var r = new AccountTradedCash();
        if (trade.OffsetFlag == CombOffsetFlagType.OFFSET_OPEN) {
            if (comm.OpenRatioByMoney > 0)
                r.Commission = comm.OpenRatioByMoney * instr.VolumeMultiple
                        * trade.Price * trade.Volume;
            else
                r.Commission = comm.OpenRatioByVolume * trade.Volume;
        } else {
            if (trade.OffsetFlag ==
                    CombOffsetFlagType.OFFSET_CLOSE_TODAY) {
                if (comm.CloseRatioByMoney > 0)
                    r.Commission = comm.CloseTodayRatioByMoney
                            * instr.VolumeMultiple * trade.Price * trade.Volume;
                else
                    r.Commission = comm.CloseTodayRatioByVolume * trade.Volume;
            } else {
                // close = close yesterday
                if (comm.CloseRatioByMoney > 0)
                    r.Commission = comm.CloseRatioByMoney
                            * instr.VolumeMultiple * trade.Price * trade.Volume;
                else
                    r.Commission = comm.CloseRatioByVolume * trade.Volume;
            }
        }
        return r;
    }
}
