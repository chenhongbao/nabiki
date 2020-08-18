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
import com.nabiki.centre.user.core.*;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.InstrumentInfo;
import com.nabiki.ctp4j.jni.flag.TThostFtdcCombOffsetFlagType;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorMessage;
import com.nabiki.ctp4j.jni.flag.TThostFtdcOrderStatusType;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.x.OP;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class ActiveRequest {
    private final Map<String, FrozenAccount> frozenAccount = new HashMap<>();
    private final Map<String, FrozenPositionDetail> frozenPosition  = new HashMap<>();

    private final String uuid = UUID.randomUUID().toString();
    private final UserAccount userAccount;
    private final UserPosition userPos;
    private final OrderProvider orderProvider;
    private final Config config;
    private final CThostFtdcInputOrderField order;
    private final CThostFtdcInputOrderActionField action;

    private final CThostFtdcRspInfoField execRsp = new CThostFtdcRspInfoField();

    ActiveRequest(CThostFtdcInputOrderField order, User user, OrderProvider provider,
                         Config cfg) {
        this.userAccount = user.getUserAccount();
        this.userPos = user.getUserPosition();
        this.orderProvider = provider;
        this.config = cfg;
        this.order = Utils.deepCopy(order);
        this.action = null;
    }

    ActiveRequest(CThostFtdcInputOrderActionField action, User user,
                  OrderProvider mgr, Config cfg) {
        this.userAccount = user.getUserAccount();
        this.userPos = user.getUserPosition();
        this.orderProvider = mgr;
        this.config = cfg;
        this.order = null;
        this.action = Utils.deepCopy(action);
    }

    public boolean isAction() {
        return this.action != null;
    }

    public CThostFtdcInputOrderField getOriginOrder() {
        return this.order;
    }

    public CThostFtdcInputOrderActionField getOriginAction() {
        return this.action;
    }

    Map<String, FrozenPositionDetail> getFrozenPosition() {
        return this.frozenPosition;
    }

    FrozenAccount getFrozenAccount() {
        if (this.frozenAccount.size() == 0)
            return null;
        return this.frozenAccount.values().iterator().next();
    }

    void execOrder() {
        if (this.order == null)
            throw new IllegalStateException("no order to execute");
        // If the user is panic, some internal error occurred. Don't trade again.
        var usrState = this.userAccount.getParent().getState();
        if (usrState == UserState.PANIC) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.INCONSISTENT_INFORMATION;
            this.execRsp.ErrorMsg = "internal error caused account panic";
            return;
        }
        // User is settled, but not inited for next day.
        if (usrState == UserState.SETTLED) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.NOT_INITED;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.NOT_INITED;
            return;
        }
        var instrInfo = this.config.getInstrInfo(this.order.InstrumentID);
        if (instrInfo == null) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND;
        } else {
            if (this.order.CombOffsetFlag == TThostFtdcCombOffsetFlagType.OFFSET_OPEN)
                insertOpen(this.order, instrInfo);
            else
                insertClose(this.order, instrInfo);
        }
    }

    void execAction() {
        if (this.action == null)
            throw new IllegalStateException("no action to execute");
        if (this.action.OrderSysID == null || this.action.OrderSysID.length() < 1) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.BAD_ORDER_ACTION_FIELD;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.BAD_ORDER_ACTION_FIELD;
            return;
        }
        var mapper = this.orderProvider.getMapper();
        var refs = mapper.getDetailRef(this.action.OrderSysID);
        if (refs == null || refs.size() < 1) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.ORDER_NOT_FOUND;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.ORDER_NOT_FOUND;
            return;
        }
        for (var ref : refs) {
            var realAction = Utils.deepCopy(this.action);
            Objects.requireNonNull(realAction, "action deep copy null");
            realAction.OrderRef = ref;
            // Check order return.
            var rtn = mapper.getRtnOrder(ref);
            if (rtn != null && (rtn.OrderStatus == TThostFtdcOrderStatusType.CANCELED
                    || rtn.OrderStatus == TThostFtdcOrderStatusType.ALL_TRADED))
                continue;
            if (send(realAction, this) != 0)
                break;
        }
    }

    private int send(CThostFtdcInputOrderField order, ActiveRequest active) {
        int r = this.orderProvider.sendDetailOrder(order, active);
        this.execRsp.ErrorID = r;
        this.execRsp.ErrorMsg = OP.getErrorMsg(r);
        return r;
    }

    private int send(CThostFtdcInputOrderActionField action, ActiveRequest active) {
        int r = this.orderProvider.sendOrderAction(action, active);
        this.execRsp.ErrorID = r;
        this.execRsp.ErrorMsg = OP.getErrorMsg(r);
        return r;
    }

    private boolean isValidVolume(CThostFtdcInputOrderField order) {
        int minVol, maxVol;
        var instrInfo = this.config.getInstrInfo(order.InstrumentID);
        if (instrInfo == null || instrInfo.Instrument == null) {
            minVol = 1;
            maxVol = Integer.MAX_VALUE;
        } else {
            minVol = instrInfo.Instrument.MinLimitOrderVolume;
            maxVol = instrInfo.Instrument.MaxLimitOrderVolume;
        }
        return minVol <= order.VolumeTotalOriginal
                && order.VolumeTotalOriginal <= maxVol;
    }

    private boolean isValidPrice(CThostFtdcInputOrderField order) {
        var depth = this.config.getDepthMarketData(order.InstrumentID);
        if (depth == null)
            return true;
        else
            return depth.LowerLimitPrice <= order.LimitPrice
                    && order.LimitPrice <= depth.UpperLimitPrice;
    }

    private boolean isValidField(CThostFtdcInputOrderField order) {
        return isValidVolume(order) && isValidPrice(order);
    }

    private void insertOpen(CThostFtdcInputOrderField order,
                            InstrumentInfo instrInfo) {
        if (!isValidField(order)) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.BAD_FIELD;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.BAD_FIELD;
            return;
        }
        // Check info ready.
        Objects.requireNonNull(instrInfo.Instrument, "instrument null");
        Objects.requireNonNull(instrInfo.Margin, "margin null");
        Objects.requireNonNull(instrInfo.Commission, "commission null");
        var frzAccount = this.userAccount.getOpenFrozenAccount(this.order,
                instrInfo.Instrument, instrInfo.Margin, instrInfo.Commission);
        if (frzAccount == null) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.INSUFFICIENT_MONEY;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.INSUFFICIENT_MONEY;
        } else {
            // Set valid order ref.
            order.OrderRef = this.orderProvider.getOrderRef();
            this.frozenAccount.put(order.OrderRef, frzAccount);
            // Apply frozen account to parent account.
            if (send(order, this) == 0)
                getFrozenAccount().setFrozen();
        }
    }

    private void insertClose(CThostFtdcInputOrderField order,
                             InstrumentInfo instrInfo) {
        Objects.requireNonNull(instrInfo.Instrument, "instrument null");
        Objects.requireNonNull(instrInfo.Commission, "commission null");
        var pds = this.userPos.peakCloseFrozen(order, instrInfo.Instrument,
                instrInfo.Commission, this.config.getTradingDay());
        if (pds == null || pds.size() == 0) {
            this.execRsp.ErrorID = TThostFtdcErrorCode.OVER_CLOSE_POSITION;
            this.execRsp.ErrorMsg = TThostFtdcErrorMessage.OVER_CLOSE_POSITION;
            return;
        }
        // Send close request.
        for (var p : pds) {
            var cls = toCloseOrder(p);
            cls.OrderRef = this.orderProvider.getOrderRef();
            var x = this.orderProvider.sendDetailOrder(cls, this);
            if (x == 0) {
                // Map order reference to frozen position.
                this.frozenPosition.put(cls.OrderRef, p);
                // Apply frozen position to parent position.
                // Because the order has been sent, the position must be frozen to
                // ensure no over-close position.
                p.setFrozen();
            } else break;
        }
    }

    public String getOrderUUID() {
        return this.uuid;
    }

    public CThostFtdcRspInfoField getExecRsp() {
        return this.execRsp;
    }

    private CThostFtdcInputOrderField toCloseOrder(FrozenPositionDetail pd) {
        var cls = Utils.deepCopy(getOriginOrder());
        Objects.requireNonNull(cls, "failed deep copy");
        cls.VolumeTotalOriginal = (int) pd.getFrozenVolume();
        if (pd.getSingleFrozenPosition().TradingDay
                .compareTo(this.config.getTradingDay()) != 0) {
            // Yesterday.
            cls.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE_YESTERDAY;
        } else {
            // Today.
            cls.CombOffsetFlag = TThostFtdcCombOffsetFlagType.OFFSET_CLOSE_TODAY;
        }
        return cls;
    }

    /**
     * Update return order. The only flag it cares is {@code CANCEL} because
     * canceling an order affects the position and frozen money.
     *
     * @param rtn return order
     */
    public void updateRtnOrder(CThostFtdcOrderField rtn) {
        if (rtn == null)
            throw new NullPointerException("return order null");
        char flag = (char) rtn.OrderStatus;
        if (flag == TThostFtdcOrderStatusType.CANCELED) {
            if (rtn.CombOffsetFlag == TThostFtdcCombOffsetFlagType.OFFSET_OPEN) {
                // Cancel cash.
                if (this.frozenAccount == null) {
                    this.config.getLogger().severe(
                            Utils.formatLog("no frozen cash",
                                    rtn.OrderRef, null, null));
                    this.userAccount.getParent().setPanic(
                            TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                            "frozen cash null");
                    return;
                }
                getFrozenAccount().cancel();
            } else {
                if (this.frozenPosition == null || this.frozenPosition.size() == 0) {
                    this.config.getLogger().severe(
                            Utils.formatLog("no frozen position",
                                    rtn.OrderRef, null, null));
                    this.userAccount.getParent().setPanic(
                            TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                            "frozen position null");
                    return;
                }
                // Cancel position.
                var p = this.frozenPosition.get(rtn.OrderRef);
                if (p == null) {
                    this.config.getLogger().severe(
                            Utils.formatLog("frozen position not found",
                                    rtn.OrderRef, null, null));
                    this.userAccount.getParent().setPanic(
                            TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                            "frozen position not found for order ref");
                    return;
                }
                p.cancel();
            }
        }
    }

    /**
     * Update position and cash when trade happens. For cash, just update commission.
     * For position, update both frozen position and user position.
     *
     * <p>When query account, calculate the fields from yesterday's settlement and
     * current position.
     * </p>
     *
     * @param trade trade response
     */
    public void updateTrade(CThostFtdcTradeField trade) {
        if (trade == null)
            throw new NullPointerException("return trade null");
        var instrInfo = this.config.getInstrInfo(trade.InstrumentID);
        Objects.requireNonNull(instrInfo, "instr info null");
        Objects.requireNonNull(instrInfo.Instrument, "instrument null");
        Objects.requireNonNull(instrInfo.Margin, "margin null");
        Objects.requireNonNull(instrInfo.Commission, "commission null");
        if (trade.OffsetFlag == TThostFtdcCombOffsetFlagType.OFFSET_OPEN) {
            // Open.
            if (this.frozenAccount == null) {
                this.config.getLogger().severe(
                        Utils.formatLog("no frozen cash",
                                trade.OrderRef, null, null));
                this.userAccount.getParent().setPanic(
                        TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                        "frozen cash null");
                return;
            }
            var depth = this.config.getDepthMarketData(trade.InstrumentID);
            Objects.requireNonNull(depth, "depth market data null");
            // Update frozen account, user account and user position.
            // The frozen account handles the update of user account.
            getFrozenAccount().applyOpenTrade(trade, instrInfo.Instrument,
                    instrInfo.Commission);
            this.userPos.applyOpenTrade(trade, instrInfo.Instrument,
                    instrInfo.Margin, depth.PreSettlementPrice);
        } else {
            // Close.
            if (this.frozenPosition == null || this.frozenPosition.size() == 0) {
                this.config.getLogger().severe(
                        Utils.formatLog("no frozen position",
                                trade.OrderRef, null, null));
                this.userAccount.getParent().setPanic(
                        TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                        "frozen position null");
                return;
            }
            // Update user position, frozen position and user account.
            // The frozen position handles the update of user position.
            var p = this.frozenPosition.get(trade.OrderRef);
            if (p == null) {
                this.config.getLogger().severe(
                        Utils.formatLog("frozen position not found",
                                trade.OrderRef, null, null));
                this.userAccount.getParent().setPanic(
                        TThostFtdcErrorCode.INCONSISTENT_INFORMATION,
                        "frozen position not found for order ref");
                return;
            }
            if (p.getFrozenVolume() < trade.Volume) {
                this.config.getLogger().severe(
                        Utils.formatLog("not enough frozen position",
                                trade.OrderRef, null, null));
                this.userAccount.getParent().setPanic(
                        TThostFtdcErrorCode.OVER_CLOSE_POSITION,
                        "not enough frozen position for trade");
                return;
            }
            // Check the frozen position OK, here won't throw exception.
            p.applyCloseTrade(trade, instrInfo.Instrument);
            this.userAccount.applyTrade(trade, instrInfo.Instrument,
                    instrInfo.Commission);
        }
    }
}
