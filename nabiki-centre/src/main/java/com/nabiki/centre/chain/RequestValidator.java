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

package com.nabiki.centre.chain;

import com.nabiki.centre.user.auth.OrderOffset;
import com.nabiki.centre.user.auth.UserAuthProfile;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.Utils;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.iop.ServerSession;
import com.nabiki.commons.iop.x.OP;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class RequestValidator extends RequestSuper {
  private final Global global;
  private final ParkedRequestManager parked;

  public RequestValidator(ParkedRequestManager parked, Global global) {
    this.parked = parked;
    this.global = global;
  }

  private boolean isAllowed(
      UserAuthProfile.InstrumentAuth auth,
      String instrumentID,
      byte offset) {
    var rightTrade = (auth.AllowOffset == OrderOffset.OPEN_CLOSE ||
        (auth.AllowOffset == OrderOffset.ONLY_CLOSE &&
            offset != CombOffsetFlagType.OFFSET_OPEN));
    return auth.InstrumentID.compareTo(instrumentID) == 0 && rightTrade;
  }

  private void reply(
      ServerSession session,
      Object request,
      String requestID,
      MessageType type,
      int errorCode,
      String errorMsg) {
    Message m = new Message();
    m.RequestID = requestID;
    m.ResponseID = UUID.randomUUID().toString();
    m.CurrentCount = m.TotalCount = 1;
    m.Type = type;
    m.Body = request;
    m.RspInfo = new CRspInfo();
    m.RspInfo.ErrorID = errorCode;
    m.RspInfo.ErrorMsg = errorMsg;
    session.sendResponse(m);
  }

  protected boolean isOver(String instrID) {
    if (global.getTradingDay() == null) {
      return true;
    }
    var today = LocalDate.now();
    var tradingDay = Utils.parseDay(global.getTradingDay(), null);
    // Holiday check.
    if (tradingDay.isBefore(today)) {
      return true;
    }
    var time = LocalTime.now();
    var hour = global.getTradingHour(null, instrID);
    // There's night trading.
    if (hour.getBeginOfDay().isAfter(hour.getEndOfDay())) {
      if (time.isAfter(hour.getBeginOfDay())) {
        // The night before holiday.
        return tradingDay.equals(today);
      }
    }
    // Workday.
    // Just handle working day here. Because holiday is checked before.
    // Normal weekend is just a break between night of last Friday and day
    // of next Monday, so it is not over.
    var dayOfWeek = today.getDayOfWeek();
    return dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY &&
        hour.isEndOfDay(time);
  }

  @Override
  public void doReqOrderInsert(
      ServerSession session,
      CInputOrder request,
      String requestID,
      int current,
      int total) {
    var attr = session.getAttribute(UserLoginManager.FRONT_AUTH_KEY);
    if (attr == null) {
      reply(session,
          toRtnOrder(request),
          requestID,
          MessageType.RSP_REQ_ORDER_INSERT,
          ErrorCodes.NOT_AUTHENT,
          OP.getErrorMsg(ErrorCodes.NOT_AUTHENT));
    } else {
      var auth = (UserAuthProfile) attr;
      if (request.UserID == null
          || auth.UserID.compareTo(request.UserID) != 0) {
        reply(session,
            toRtnOrder(request),
            requestID,
            MessageType.RSP_REQ_ORDER_INSERT,
            ErrorCodes.USER_NOT_ACTIVE,
            OP.getErrorMsg(ErrorCodes.USER_NOT_ACTIVE));
      } else {
        for (var instrAuth : auth.InstrumentAuths) {
          var instrumentID = request.InstrumentID;
          var offset = request.CombOffsetFlag;
          if (isAllowed(instrAuth, instrumentID, offset)) {
            if (isOver(request.InstrumentID)) {
              parked.offer(request);
              // Set mark to indicate the order is inserted after market is closed.
              // It turns to parked order.
              var rsp = toRtnOrder(request);
              rsp.OrderSubmitStatus = OrderSubmitStatusType.INSERT_SUBMITTED;
              rsp.OrderStatus = OrderStatusType.NOT_TOUCHED;
              reply(session,
                  rsp,
                  requestID,
                  MessageType.RSP_REQ_ORDER_INSERT,
                  ErrorCodes.NONE,
                  OP.getErrorMsg(ErrorCodes.NONE));
            } else {
              // Allow the request goes to next handler on the chain.
              return;
            }
          }
        }
        reply(session,
            toRtnOrder(request),
            requestID,
            MessageType.RSP_REQ_ORDER_INSERT,
            ErrorCodes.NO_TRADING_RIGHT,
            OP.getErrorMsg(ErrorCodes.NO_TRADING_RIGHT));
      }
    }
    session.done();
  }

  // No need to validate an order action because when request reaches here, user
  // must be login. The insert is sent so it means the user has right to trade
  // the instrument.
}
