/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

package com.nabiki.centre.chain;

import com.nabiki.centre.config.Global;
import com.nabiki.centre.user.core.ActiveUser;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.iop.ServerSession;
import com.nabiki.commons.utils.Utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class RequestExecutor extends RequestSuper {
  private final ActiveUserManager userMgr;
  private final Global global;

  public RequestExecutor(ActiveUserManager user, Global g) {
    userMgr = user;
    global = g;
  }

  private void reply(
      ServerSession session,
      Object rsp,
      String requestID,
      MessageType type,
      CRspInfo info) {
    Message m = new Message();
    m.RequestID = requestID;
    m.ResponseID = UUID.randomUUID().toString();
    m.CurrentCount = m.TotalCount = 1;
    m.Type = type;
    m.Body = rsp;
    m.RspInfo = info;
    session.sendResponse(m);
    if (rsp instanceof COrderAction) {
      var action = (COrderAction) rsp;
      if (info.ErrorID == ErrorCodes.NONE) {
        global.getLogger().info(String.format(
            "Accept action from %s for %s.",
            action.UserID,
            action.OrderLocalID));
      } else {
        global.getLogger().warning(String.format(
            "Reject action from %s for %s because of %s[%d].",
            action.UserID,
            action.OrderLocalID,
            info.ErrorMsg,
            info.ErrorID));
      }
    }
  }

  private void reply(
      ServerSession session,
      COrder rsp,
      String requestID,
      MessageType type,
      int errorCode,
      String errorMsg) {
    var info = new CRspInfo();
    info.ErrorID = errorCode;
    info.ErrorMsg = errorMsg;
    reply(session, rsp, requestID, type, info);
    // Logging.
    if (errorCode == ErrorCodes.NONE) {
      global.getLogger().info(String.format(
          "Accept order from %s on %s at %.1f.",
          rsp.UserID,
          rsp.InstrumentID,
          rsp.LimitPrice));
    } else {
      global.getLogger().warning(String.format(
          "Reject order from %s on %s because of %s[%d].",
          rsp.UserID,
          rsp.InstrumentID,
          errorMsg,
          errorCode));
    }
  }

  public ActiveUser getUser(ServerSession session) {
    var user = session.getAttribute(UserLoginManager.FRONT_USERID_KEY);
    return user == null ? null : userMgr.getActiveUser((String) user);
  }

  @Override
  public void doReqOrderInsert(
      ServerSession session,
      CInputOrder request,
      String requestID,
      int current,
      int total) {
    var user = getUser(session);
    if (user == null) {
      reply(
          session,
          new COrder(),
          requestID,
          MessageType.RSP_REQ_ORDER_INSERT,
          ErrorCodes.USER_NOT_ACTIVE,
          Utils.getErrorMsg(ErrorCodes.USER_NOT_ACTIVE));
    } else {
      // Measure performance.
      var max = this.global.getPerformance().start("order.insert.max");
      var cur = this.global.getPerformance().start("order.insert.cur");
      // Order insert.
      var uuid = user.insertOrder(request);
      // End measurement.
      max.endWithMax();
      cur.end();
      // Build response.
      var order = toRtnOrder(request);
      var info = user.getExecRsp(uuid);
      if (info.ErrorID == ErrorCodes.NONE) {
        order.OrderLocalID = uuid;
        order.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
        order.OrderStatus = OrderStatusType.NO_TRADE_QUEUEING;
      } else {
        order.OrderLocalID = null;
        order.OrderSubmitStatus = OrderSubmitStatusType.INSERT_REJECTED;
        order.OrderStatus = OrderStatusType.NO_TRADE_NOT_QUEUEING;
      }
      order.InsertDate = Utils.getDay(LocalDate.now(), null);
      order.InsertTime = Utils.getTime(LocalTime.now(), null);
      reply(
          session,
          order,
          requestID,
          MessageType.RSP_REQ_ORDER_INSERT,
          info.ErrorID,
          info.ErrorMsg);
    }
    session.done();
  }

  @Override
  public void doReqOrderAction(
      ServerSession session,
      CInputOrderAction request,
      String requestID,
      int current,
      int total) {
    var user = getUser(session);
    if (user == null) {
      var info = new CRspInfo();
      info.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
      info.ErrorMsg = Utils.getErrorMsg(info.ErrorID);
      reply(
          session,
          new COrderAction(),
          requestID,
          MessageType.RSP_REQ_ORDER_ACTION,
          info);
    } else {
      // Measure performance.
      var max = this.global.getPerformance().start("order.insert.max");
      var cur = this.global.getPerformance().start("order.insert.cur");
      // Order action.
      var uuid = user.orderAction(request);
      // End measurement.
      max.endWithMax();
      cur.end();
      // Build response.
      var action = toOrderAction(request);
      action.OrderLocalID = request.OrderSysID;
      action.OrderSysID = null;
      reply(
          session,
          action,
          requestID,
          MessageType.RSP_REQ_ORDER_ACTION,
          user.getExecRsp(uuid));
    }
    session.done();
  }
}
