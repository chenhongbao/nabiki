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
import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUser {
    private final User user;
    private final Global global;
    private final OrderProvider orderProvider;
    private final Map<String, ActiveRequest> requests = new ConcurrentHashMap<>();

    public ActiveUser(User user, OrderProvider orderProvider, Global cfg) {
        this.user = user;
        this.global = cfg;
        this.orderProvider = orderProvider;
    }

    public CRspInfo getExecRsp(String uuid) {
        var active = this.requests.get(uuid);
        if (active == null)
            return null;
        else
            return active.getExecRsp();
    }

    public String insertOrder(CInputOrder order) {
        var active = new ActiveRequest(order, this.user, this.orderProvider,
                this.global);
        this.requests.put(active.getRequestUUID(), active);
        try {
            active.execOrder();
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe(
                    Utils.formatLog("failed order insertion", order.UserID,
                            th.getMessage(), null));
        }
        return active.getRequestUUID();
    }

    public String orderAction(CInputOrderAction action) {
        var active = new ActiveRequest(action, this.user, this.orderProvider,
                this.global);
        this.requests.put(active.getRequestUUID(), active);
        try {
            active.execAction();
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe(
                    Utils.formatLog("failed order action", action.UserID,
                            th.getMessage(), null));
        }
        return active.getRequestUUID();
    }

    public Set<COrder> getRtnOrder(String uuid) {
        var r = new HashSet<COrder>();
        if (uuid == null)
            return r;
        var refs = this.orderProvider.getMapper().getOrderRef(uuid);
        if (refs == null || refs.size() == 0)
            return r;
        for (var ref : refs) {
            var o = this.orderProvider.getMapper().getRtnOrder(ref);
            if (o != null)
                r.add(o);
        }
        return r;
    }

    public Map<String, FrozenPositionDetail> getFrozenPositionDetail(String uuid) {
        var active = this.requests.get(uuid);
        if (active != null)
            return active.getFrozenPosition();
        else
            return null;
    }

    public FrozenAccount getFrozenAccount(String uuid) {
        var active = this.requests.get(uuid);
        if (active != null)
            return active.getFrozenAccount();
        else
            return null;
    }

    public CTradingAccount getTradingAccount() {
        // Call user's method directly, sync here.
        synchronized (this.user) {
            var account = this.user.getFeaturedAccount();
            account.TradingDay = this.global.getTradingDay();
            return account;
        }
    }

    public List<CInvestorPosition> getPosition(String instrID) {
        // Manipulate user's internal data, so sync here.
        synchronized (this.user) {
            if (instrID == null || instrID.length() == 0) {
                var ret = new LinkedList<CInvestorPosition>();
                for (var instr : this.user.getUserPosition().getPositionMap().keySet())
                    ret.addAll(getInstrPosition(instr));
                return ret;
            } else
                return getInstrPosition(instrID);
        }
    }

    void settle() {
        // Call user's method directly, sync here.
        synchronized (this.user) {
            this.user.settle(prepare());
        }
    }

    private List<CInvestorPosition> getInstrPosition(String instrID) {
        var ret = new LinkedList<CInvestorPosition>();
        if (instrID == null || instrID.length() == 0)
            return ret;
        var usrPos = this.user.getUserPosition().getSpecificPosition(instrID);
        if (usrPos == null || usrPos.size() == 0)
            return ret;
        var tradingDay = this.global.getTradingDay();
        if (tradingDay == null || tradingDay.length() == 0)
            throw new IllegalArgumentException("trading day null");
        CInvestorPosition lp = null, sp = null;
        for (var p : usrPos) {
            var sum = p.getInvestorPosition(tradingDay);
            if (sum.PosiDirection == PosiDirectionType.LONG) {
                // Long position.
                if (lp  == null)
                    lp = sum;
                else
                    lp = add(lp, sum);
            } else {
                // Short position.
                if (sp == null)
                    sp = sum;
                else
                    sp = add(sp, sum);
            }
        }
        // Add to result set.
        if (lp != null) {
            lp.TradingDay = this.global.getTradingDay();
            ret.add(lp);
        }
        if (sp != null) {
            sp.TradingDay = this.global.getTradingDay();
            ret.add(sp);
        }
        return ret;
    }

    private CInvestorPosition add(
            CInvestorPosition a,
            CInvestorPosition b) {
        a.CloseAmount += b.CloseAmount;
        a.CloseProfit += b.CloseProfit;
        a.CloseProfitByDate += b.CloseProfitByDate;
        a.CloseProfitByTrade += b.CloseProfitByTrade;
        a.CloseVolume += b.CloseVolume;
        a.Commission += b.Commission;
        a.ExchangeMargin += b.ExchangeMargin;
        a.FrozenCash += b.FrozenCash;
        a.FrozenCommission += b.FrozenCommission;
        a.FrozenMargin += b.FrozenMargin;
        a.LongFrozen += b.LongFrozen;
        a.LongFrozenAmount += b.LongFrozenAmount;
        a.OpenAmount += b.OpenAmount;
        a.OpenVolume += b.OpenVolume;
        a.OpenCost += b.OpenCost;
        a.Position += b.Position;
        a.PositionCost += b.PositionCost;
        a.PositionProfit += b.PositionProfit;
        a.PreMargin += b.PreMargin;
        a.ShortFrozen += b.ShortFrozen;
        a.ShortFrozenAmount += b.ShortFrozenAmount;
        a.TodayPosition += b.TodayPosition;
        a.UseMargin += b.UseMargin;
        a.YdPosition += b.YdPosition;
        return Utils.deepCopy(a);
    }

    private SettlementPreparation prepare() {
        SettlementPreparation prep = new SettlementPreparation();
        prep.prepare(this.global.getTradingDay());
        for (var i : this.global.getAllInstrInfo()) {
            // There may be some info missing, but it doesn't matter if we don't
            // have that position.
            // It is possible for some instruments that don't have trade for whole
            // day whose depth md is null. Need to catch exception here and keep
            // settlement going.
            try {
                prep.prepare(i.Instrument);
                prep.prepare(i.Commission);
                prep.prepare(i.Margin);
                var depth = this.global.getDepthMarketData(i.Instrument.InstrumentID);
                Objects.requireNonNull(depth, "depth null");
                if (Utils.validPrice(depth.SettlementPrice))
                    prep.prepare(depth);
                else
                    this.global.getLogger()
                            .warning("no settlement price("
                                    + depth.SettlementPrice + "): "
                                    + i.Instrument.InstrumentID);
            } catch (Throwable th) {
                if (i != null && i.Instrument != null)
                    this.global.getLogger()
                            .warning("can't prepare settlement: "
                                    + i.Instrument.InstrumentID
                                    + ", " + th.getMessage());
            }
        }
        return prep;
    }
}
