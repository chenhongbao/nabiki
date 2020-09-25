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
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.Utils;
import com.nabiki.centre.utils.plain.InstrumentInfo;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ActiveRequest {
  private final Map<String, FrozenAccount> frozenAccount = new HashMap<>();
  private final Map<String, FrozenPositionDetail> frozenPosition = new HashMap<>();
  private final String uuid = UUID.randomUUID().toString();
  private final User user;
  private final OrderProvider orderProvider;
  private final Global global;
  private final CInputOrder order;
  private final CInputOrderAction action;
  private final CRspInfo execRsp = new CRspInfo();
  // Count total traded volume from all sub-orders.
  private final AtomicInteger tradedCount = new AtomicInteger(0);

  ActiveRequest(
      CInputOrder order,
      User user,
      OrderProvider provider,
      Global cfg) {
    this.user = user;
    this.orderProvider = provider;
    this.global = cfg;
    this.order = Utils.deepCopy(order);
    this.action = null;
  }

  ActiveRequest(
      CInputOrderAction action,
      User user,
      OrderProvider mgr,
      Global cfg) {
    this.user = user;
    this.orderProvider = mgr;
    this.global = cfg;
    this.order = null;
    this.action = Utils.deepCopy(action);
  }

  public boolean isAction() {
    return this.action != null;
  }

  public CInputOrder getOriginOrder() {
    return this.order;
  }

  public CInputOrderAction getOriginAction() {
    return this.action;
  }

  Map<String, FrozenPositionDetail> getFrozenPosition() {
    synchronized (this.user) {
      return this.frozenPosition;
    }
  }

  public User getUser() {
    return this.user;
  }

  FrozenAccount getFrozenAccount() {
    synchronized (this.user) {
      if (this.frozenAccount.size() == 0)
        return null;
      return this.frozenAccount.values().iterator().next();
    }
  }

  void execOrder() {
    synchronized (this.user) {
      directExecOrder();
    }
  }

  private void directExecOrder() {
    if (this.order == null)
      throw new IllegalStateException("no order to execute");
    // If the user is panic, some internal error occurred. Don't trade again.
    var usrState = this.user.getUserAccount().getParent().getState();
    if (usrState == UserState.PANIC) {
      this.execRsp.ErrorID = ErrorCodes.INCONSISTENT_INFORMATION;
      this.execRsp.ErrorMsg = "internal error caused account panic";
      return;
    }
    // User is settled, but not inited for next day.
    if (usrState == UserState.SETTLED) {
      this.execRsp.ErrorID = ErrorCodes.NOT_INITED;
      this.execRsp.ErrorMsg = ErrorMessages.NOT_INITED;
      return;
    }
    var instrInfo = this.global.getInstrInfo(this.order.InstrumentID);
    if (instrInfo == null) {
      this.execRsp.ErrorID = ErrorCodes.INSTRUMENT_NOT_FOUND;
      this.execRsp.ErrorMsg = ErrorMessages.INSTRUMENT_NOT_FOUND;
    } else {
      switch (this.order.CombOffsetFlag) {
        case CombOffsetFlagType.OFFSET_OPEN:
          insertOpen(Utils.deepCopy(this.order), instrInfo);
          break;
        case CombOffsetFlagType.OFFSET_CLOSE:
        case CombOffsetFlagType.OFFSET_CLOSE_TODAY:
        case CombOffsetFlagType.OFFSET_CLOSE_YESTERDAY:
        case CombOffsetFlagType.OFFSET_FORCE_CLOSE:
        case CombOffsetFlagType.OFFSET_FORCE_OFF:
        case CombOffsetFlagType.OFFSET_LOCAL_FORCE_CLOSE:
          insertClose(Utils.deepCopy(this.order), instrInfo);
          break;
        default:
          this.global.getLogger().warning("unknown offset flag: "
              + this.order.CombOffsetFlag);
          break;
      }
    }
  }

  void execAction() {
    synchronized (this.user) {
      directExecAction();
    }
  }

  private void directExecAction() {
    if (this.action == null)
      throw new IllegalStateException("no action to execute");
    if (this.action.OrderSysID == null || this.action.OrderSysID.length() < 1) {
      this.execRsp.ErrorID = ErrorCodes.BAD_ORDER_ACTION_FIELD;
      this.execRsp.ErrorMsg = ErrorMessages.BAD_ORDER_ACTION_FIELD;
      return;
    }
    var mapper = this.orderProvider.getMapper();
    var refs = mapper.getOrderRef(this.action.OrderSysID);
    if (refs == null || refs.size() < 1) {
      this.execRsp.ErrorID = ErrorCodes.ORDER_NOT_FOUND;
      this.execRsp.ErrorMsg = ErrorMessages.ORDER_NOT_FOUND;
      return;
    }
    boolean hasSent = false;
    for (var ref : refs) {
      var realAction = Utils.deepCopy(this.action);
      Objects.requireNonNull(realAction, "action deep copy null");
      // Check order return.
      var rtn = mapper.getRtnOrder(ref);
      if (rtn != null && (rtn.OrderStatus == OrderStatusType.CANCELED
          || rtn.OrderStatus == OrderStatusType.ALL_TRADED))
        continue;
      // Order ref to identify the sub-order to be canceled.
      realAction.OrderRef = ref;
      // Set instrument id and other info because validator doesn't check action.
      realAction.InstrumentID = rtn.InstrumentID;
      realAction.UserID = rtn.UserID;
      // Set mark.
      hasSent = true;
      if (send(realAction, this) != 0)
        break;
    }
    // If there's no action sent, return error.
    if (!hasSent) {
      this.execRsp.ErrorID = ErrorCodes.INSUITABLE_ORDER_STATUS;
      this.execRsp.ErrorMsg = ErrorMessages.INSUITABLE_ORDER_STATUS;
    }
  }

  private int send(CInputOrder order, ActiveRequest active) {
    int r = this.orderProvider.inputOrder(order, active);
    this.execRsp.ErrorID = r;
    this.execRsp.ErrorMsg = OP.getErrorMsg(r);
    return r;
  }

  private int send(CInputOrderAction action, ActiveRequest active) {
    int r = this.orderProvider.actionOrder(action, active);
    this.execRsp.ErrorID = r;
    this.execRsp.ErrorMsg = OP.getErrorMsg(r);
    return r;
  }

  private boolean isValidVolume(CInputOrder order) {
    int minVol, maxVol;
    var instrInfo = this.global.getInstrInfo(order.InstrumentID);
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

  private boolean isValidPrice(CInputOrder order) {
    var depth = this.global.getDepthMarketData(order.InstrumentID);
    if (depth == null)
      return true;
    else
      return depth.LowerLimitPrice <= order.LimitPrice
          && order.LimitPrice <= depth.UpperLimitPrice;
  }

  private boolean isValidField(CInputOrder order) {
    return isValidVolume(order) && isValidPrice(order);
  }

  private void insertOpen(CInputOrder order,
                          InstrumentInfo instrInfo) {
    if (!isValidField(order)) {
      this.execRsp.ErrorID = ErrorCodes.BAD_FIELD;
      this.execRsp.ErrorMsg = ErrorMessages.BAD_FIELD;
      return;
    }
    // Check info ready.
    Objects.requireNonNull(instrInfo.Instrument, "instrument null");
    Objects.requireNonNull(instrInfo.Margin, "margin null");
    Objects.requireNonNull(instrInfo.Commission, "commission null");
    var frzAccount = this.user.getUserAccount()
        .getOpenFrozenAccount(
            this.order,
            instrInfo.Instrument,
            instrInfo.Margin,
            instrInfo.Commission);
    if (frzAccount == null) {
      this.execRsp.ErrorID = ErrorCodes.INSUFFICIENT_MONEY;
      this.execRsp.ErrorMsg = ErrorMessages.INSUFFICIENT_MONEY;
    } else {
      // Set valid order ref.
      order.OrderRef = this.orderProvider.getOrderRef();
      this.frozenAccount.put(order.OrderRef, frzAccount);
      // Apply frozen account to parent account.
      if (send(order, this) == 0)
        getFrozenAccount().setFrozen();
    }
  }

  private void insertClose(CInputOrder order,
                           InstrumentInfo instrInfo) {
    Objects.requireNonNull(instrInfo.Instrument, "instrument null");
    Objects.requireNonNull(instrInfo.Commission, "commission null");
    var pds = this.user.getUserPosition()
        .peakCloseFrozen(
            order,
            instrInfo.Instrument,
            instrInfo.Commission,
            this.global.getTradingDay());
    if (pds == null || pds.size() == 0) {
      this.execRsp.ErrorID = ErrorCodes.OVER_CLOSE_POSITION;
      this.execRsp.ErrorMsg = ErrorMessages.OVER_CLOSE_POSITION;
      return;
    }
    // Send close request.
    for (var p : pds) {
      if (p.getFrozenVolume() == 0)
        continue;
      else if (p.getFrozenVolume() < 0)
        throw new IllegalStateException("negative frozen volume");
      var cls = toCloseOrder(p);
      cls.OrderRef = this.orderProvider.getOrderRef();
      var x = this.orderProvider.inputOrder(cls, this);
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

  public String getRequestUUID() {
    return this.uuid;
  }

  public CRspInfo getExecRsp() {
    synchronized (this.user) {
      return this.execRsp;
    }
  }

  private CInputOrder toCloseOrder(FrozenPositionDetail pd) {
    var cls = Utils.deepCopy(getOriginOrder());
    Objects.requireNonNull(cls, "failed deep copy");
    cls.VolumeTotalOriginal = (int) pd.getFrozenVolume();
    if (pd.getSingleFrozenPosition().TradingDay
        .compareTo(this.global.getTradingDay()) != 0) {
      // Yesterday.
      cls.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE_YESTERDAY;
    } else {
      // Today.
      cls.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE_TODAY;
    }
    return cls;
  }

  /**
   * Update return order. The only flag it cares is {@code CANCEL} because
   * canceling an order affects the position and frozen money.
   *
   * @param rtn return order
   */
  public void updateRtnOrder(COrder rtn) {
    synchronized (this.user) {
      directUpdateRtnOrder(rtn);
    }
  }

  private void directUpdateRtnOrder(COrder rtn) {
    if (rtn == null)
      throw new NullPointerException("return order null");
    switch ((char) rtn.OrderStatus) {
      case OrderStatusType.CANCELED:
        cancelOrder(rtn);
        break;
      case OrderStatusType.ALL_TRADED:
      case OrderStatusType.NO_TRADE_NOT_QUEUEING:
      case OrderStatusType.NO_TRADE_QUEUEING:
      case OrderStatusType.PART_TRADED_NOT_QUEUEING:
      case OrderStatusType.PART_TRADED_QUEUEING:
      case OrderStatusType.NOT_TOUCHED:
      case OrderStatusType.TOUCHED:
      case OrderStatusType.UNKNOWN:
        break;
      default:
        this.global.getLogger().warning("unknown order status: "
            + this.order.CombOffsetFlag);
        break;
    }
  }

  private void cancelOrder(COrder rtn) {
    switch (rtn.CombOffsetFlag) {
      case CombOffsetFlagType.OFFSET_OPEN:
        // Cancel cash.
        if (this.frozenAccount == null) {
          this.global.getLogger().severe(
              Utils.formatLog("no frozen cash",
                  rtn.OrderRef, null, null));
          this.user.getUserAccount().getParent().setPanic(
              ErrorCodes.INCONSISTENT_INFORMATION,
              "frozen cash null");
          return;
        }
        getFrozenAccount().cancel();
        break;
      case CombOffsetFlagType.OFFSET_CLOSE:
      case CombOffsetFlagType.OFFSET_CLOSE_TODAY:
      case CombOffsetFlagType.OFFSET_CLOSE_YESTERDAY:
      case CombOffsetFlagType.OFFSET_FORCE_CLOSE:
      case CombOffsetFlagType.OFFSET_FORCE_OFF:
      case CombOffsetFlagType.OFFSET_LOCAL_FORCE_CLOSE:
        if (this.frozenPosition == null || this.frozenPosition.size() == 0) {
          this.global.getLogger().severe(
              Utils.formatLog("no frozen position",
                  rtn.OrderRef, null, null));
          this.user.setPanic(
              ErrorCodes.INCONSISTENT_INFORMATION,
              "frozen position null");
          return;
        }
        // Cancel position.
        var p = this.frozenPosition.get(rtn.OrderRef);
        if (p == null) {
          this.global.getLogger().severe(
              Utils.formatLog("frozen position not found",
                  rtn.OrderRef, null, null));
          this.user.setPanic(
              ErrorCodes.INCONSISTENT_INFORMATION,
              "frozen position not found for order ref");
          return;
        }
        p.cancel();
        break;
      default:
        this.global.getLogger().warning("unknown offset flag: "
            + this.order.CombOffsetFlag);
        break;
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
  public void updateTrade(CTrade trade) {
    if (order == null) {
      global.getLogger().severe(Utils.formatLog(
          "null original input order",
          trade.OrderRef,
          null,
          null));
      return;
    }
    // Increase return trade counter.
    tradedCount.addAndGet(trade.Volume);
    // If trade more than input, it is an order from other client, otherwise update
    // the trade into account.
    if (tradedCount.get() > order.VolumeTotalOriginal) {
      global.getLogger().severe(String.format(
          "update more traded volume than input, expected %d, count %d. [%s][%s]",
          order.VolumeTotalOriginal,
          tradedCount.get(),
          trade.OrderRef,
          getRequestUUID()));
    } else {
      synchronized (this.user) {
        directUpdateTrade(trade);
      }
    }
  }

  private void directUpdateTrade(CTrade trade) {
    if (trade == null)
      throw new NullPointerException("return trade null");
    var instrInfo = this.global.getInstrInfo(trade.InstrumentID);
    Objects.requireNonNull(instrInfo, "instr info null");
    Objects.requireNonNull(instrInfo.Instrument, "instrument null");
    Objects.requireNonNull(instrInfo.Margin, "margin null");
    Objects.requireNonNull(instrInfo.Commission, "commission null");
    switch (trade.OffsetFlag) {
      case CombOffsetFlagType.OFFSET_OPEN:
        openTrade(trade, instrInfo);
        break;
      case CombOffsetFlagType.OFFSET_CLOSE:
      case CombOffsetFlagType.OFFSET_CLOSE_TODAY:
      case CombOffsetFlagType.OFFSET_CLOSE_YESTERDAY:
      case CombOffsetFlagType.OFFSET_FORCE_CLOSE:
      case CombOffsetFlagType.OFFSET_FORCE_OFF:
      case CombOffsetFlagType.OFFSET_LOCAL_FORCE_CLOSE:
        closeTrade(trade, instrInfo);
        break;
      default:
        this.global.getLogger().warning("unknown offset flag: "
            + this.order.CombOffsetFlag);
        break;
    }
  }

  private void openTrade(CTrade trade, InstrumentInfo instrInfo) {
    if (this.frozenAccount == null) {
      this.global.getLogger().severe(
          Utils.formatLog("no frozen cash",
              trade.OrderRef, null, null));
      this.user.setPanic(
          ErrorCodes.INCONSISTENT_INFORMATION,
          "frozen cash null");
      return;
    }
    var depth = this.global.getDepthMarketData(trade.InstrumentID);
    Objects.requireNonNull(depth, "depth market data null");
    // Update frozen account, user account and user position.
    // The frozen account handles the update of user account.
    getFrozenAccount().applyOpenTrade(
        trade,
        instrInfo.Instrument,
        instrInfo.Commission);
    this.user.getUserPosition().applyOpenTrade(
        trade,
        instrInfo.Instrument,
        instrInfo.Margin,
        depth.PreSettlementPrice);
  }

  private void closeTrade(CTrade trade, InstrumentInfo instrInfo) {
    if (this.frozenPosition == null || this.frozenPosition.size() == 0) {
      this.global.getLogger().severe(
          Utils.formatLog("no frozen position",
              trade.OrderRef, null, null));
      this.user.setPanic(
          ErrorCodes.INCONSISTENT_INFORMATION,
          "frozen position null");
      return;
    }
    // Update user position, frozen position and user account.
    // The frozen position handles the update of user position.
    var p = this.frozenPosition.get(trade.OrderRef);
    if (p == null) {
      this.global.getLogger().severe(
          Utils.formatLog("frozen position not found",
              trade.OrderRef, null, null));
      this.user.setPanic(
          ErrorCodes.INCONSISTENT_INFORMATION,
          "frozen position not found for order ref");
      return;
    }
    if (p.getFrozenVolume() < trade.Volume) {
      this.global.getLogger().severe(
          Utils.formatLog("not enough frozen position",
              trade.OrderRef, null, null));
      this.user.setPanic(
          ErrorCodes.OVER_CLOSE_POSITION,
          "not enough frozen position for trade");
      return;
    }
    // Check the frozen position OK, here won't throw exception.
    p.applyCloseTrade(trade, instrInfo.Instrument);
    this.user.getUserAccount().applyTrade(
        trade,
        instrInfo.Instrument,
        instrInfo.Commission);
  }
}
