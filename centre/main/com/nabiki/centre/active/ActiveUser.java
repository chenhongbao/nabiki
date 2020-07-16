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

package com.nabiki.centre.active;

import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.user.core.FrozenAccount;
import com.nabiki.centre.user.core.FrozenPositionDetail;
import com.nabiki.centre.user.core.User;
import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.flag.TThostFtdcPosiDirectionType;
import com.nabiki.ctp4j.jni.struct.*;

import java.util.*;

public class ActiveUser {
    private final User user;
    private final Config config;
    private final OrderProvider orderProvider;
    private final Map<String, ActiveRequest> requests = new HashMap<>();

    public ActiveUser(User user, OrderProvider orderProvider, Config cfg) {
        this.user = user;
        this.config = cfg;
        this.orderProvider = orderProvider;
    }

    public void settle() {
        // Prepare settlement prices.
        var prep = new SettlementPreparation();
        for (var instr : this.user.getUserPosition().getPositionInstrID()) {
            var depth = this.config.getDepthMarketData(instr);
            if (depth == null)
                throw new NullPointerException("depth market data null");
            if (!Utils.validPrice(depth.SettlementPrice))
                throw new IllegalArgumentException(
                        "no settlement price for " + instr);
            prep.prepare(depth);
        }
        // Prepare instrument info set.
        for (var instr : this.user.getUserPosition().getPositionInstrID()) {
            var instrInfo = this.config.getInstrInfo(instr);
            Objects.requireNonNull(instrInfo, "instr info null");
            Objects.requireNonNull(instrInfo.instrument, "instrument null");
            Objects.requireNonNull(instrInfo.margin, "margin null");
            Objects.requireNonNull(instrInfo.commission, "commission null");
            // Set info.
            prep.prepare(instrInfo.instrument);
            prep.prepare(instrInfo.margin);
            prep.prepare(instrInfo.commission);
        }
        this.user.settle(prep);
    }

    public CThostFtdcRspInfoField getExecRsp(String uuid) {
        var active = this.requests.get(uuid);
        if (active == null)
            return null;
        else
            return active.getExecRsp();
    }

    public String insertOrder(CThostFtdcInputOrderField order) {
        var active = new ActiveRequest(order, this.user, this.orderProvider,
                this.config);
        this.requests.put(active.getOrderUUID(), active);
        try {
            active.execOrder();
        } catch (Throwable th) {
            this.config.getLogger().severe(
                    Utils.formatLog("failed order insertion", order.UserID,
                            th.getMessage(), null));
        }
        return active.getOrderUUID();
    }

    public String orderAction(CThostFtdcInputOrderActionField action) {
        var active = new ActiveRequest(action, this.user, this.orderProvider,
                this.config);
        this.requests.put(active.getOrderUUID(), active);
        try {
            active.execAction();
        } catch (Throwable th) {
            this.config.getLogger().severe(
                    Utils.formatLog("failed order action", action.UserID,
                            th.getMessage(), null));
        }
        return active.getOrderUUID();
    }

    public Set<CThostFtdcOrderField> getRtnOrder(String uuid) {
        var r = new HashSet<CThostFtdcOrderField>();
        var refs = this.orderProvider.getMapper().getDetailRef(uuid);
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
        var o = this.requests.get(uuid);
        if (o != null)
            return o.getFrozenPosition();
        else
            return null;
    }

    public FrozenAccount getFrozenAccount(String uuid) {
        var o = this.requests.get(uuid);
        if (o != null)
            return o.getFrozenAccount();
        else
            return null;
    }

    public CThostFtdcTradingAccountField getTradingAccount() {
        var account = this.user.getFeaturedAccount();
        account.TradingDay = this.config.getTradingDay();
        return account;
    }

    public List<CThostFtdcInvestorPositionField> getPosition(String instrID) {
        if (instrID == null || instrID.length() == 0) {
            var ret = new LinkedList<CThostFtdcInvestorPositionField>();
            for (var instr : this.user.getUserPosition().getPositionMap().keySet())
                ret.addAll(getInstrPosition(instr));
            return ret;
        } else
            return getInstrPosition(instrID);
    }

    private List<CThostFtdcInvestorPositionField> getInstrPosition(String instrID) {
        var ret = new LinkedList<CThostFtdcInvestorPositionField>();
        if (instrID == null || instrID.length() == 0)
            return ret;
        var usrPos = this.user.getUserPosition().getSpecificPosition(instrID);
        if (usrPos == null || usrPos.size() == 0)
            return ret;
        var tradingDay = this.config.getTradingDay();
        if (tradingDay == null || tradingDay.length() == 0)
            throw new IllegalArgumentException("trading day null");
        CThostFtdcInvestorPositionField lp = null, sp = null;
        for (var p : usrPos) {
            var sum = p.getInvestorPosition(tradingDay);
            if (sum.PosiDirection == TThostFtdcPosiDirectionType.LONG) {
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
            lp.TradingDay = this.config.getTradingDay();
            ret.add(lp);
        }
        if (sp != null) {
            sp.TradingDay = this.config.getTradingDay();
            ret.add(sp);
        }
        return ret;
    }

    private CThostFtdcInvestorPositionField add(
            CThostFtdcInvestorPositionField a,
            CThostFtdcInvestorPositionField b) {
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
}
