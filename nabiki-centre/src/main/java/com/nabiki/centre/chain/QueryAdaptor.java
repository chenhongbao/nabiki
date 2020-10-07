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

import com.nabiki.centre.user.core.ActiveUser;
import com.nabiki.centre.utils.Global;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.iop.ServerMessageAdaptor;
import com.nabiki.commons.iop.ServerSession;
import com.nabiki.commons.iop.x.OP;

import java.util.UUID;

public class QueryAdaptor extends ServerMessageAdaptor {
  private final Global global;

  public QueryAdaptor(Global global) {
    this.global = global;
  }

  @Override
  public void doQryAccount(
      ServerSession session,
      CQryTradingAccount query,
      String requestID,
      int current,
      int total) {
    var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
    Message rsp = new Message();
    rsp.Type = MessageType.RSP_QRY_ACCOUNT;
    rsp.CurrentCount = 1;
    rsp.TotalCount = 1;
    rsp.RequestID = requestID;
    rsp.ResponseID = UUID.randomUUID().toString();
    rsp.RspInfo = new CRspInfo();
    if (attr == null) {
      rsp.Body = new CTradingAccount();
      rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
    } else {
      var activeUser = (ActiveUser) attr;
      // Performance measurement.
      var max = this.global.getPerformanceMeasure().start("qry.account.max");
      var cur = this.global.getPerformanceMeasure().start("qry.account.cur");
      // Qry account.
      rsp.Body = activeUser.getTradingAccount();
      // End measurement.
      max.endWithMax();
      cur.end();
      rsp.RspInfo.ErrorID = ErrorCodes.NONE;
    }
    rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
    session.sendResponse(rsp);
    session.done();
  }

  @Override
  public void doQryOrder(
      ServerSession session,
      CQryOrder query,
      String requestID,
      int current,
      int total) {
    var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
    Message rsp = new Message();
    rsp.Type = MessageType.RSP_QRY_ORDER;
    rsp.RequestID = requestID;
    rsp.ResponseID = UUID.randomUUID().toString();
    rsp.RspInfo = new CRspInfo();
    if (attr == null) {
      rsp.CurrentCount = 1;
      rsp.TotalCount = 1;
      rsp.Body = new COrder();
      rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
      rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
      session.sendResponse(rsp);
    } else {
      var activeUser = (ActiveUser) attr;
      // Performance measurement.
      var max = this.global.getPerformanceMeasure().start("qry.rtnorder.max");
      var cur = this.global.getPerformanceMeasure().start("qry.rtnorder.cur");
      // Qry rtn order.
      var orders = activeUser.getRtnOrder(query.OrderSysID);
      // End measurement.
      max.endWithMax();
      cur.end();
      if (orders == null || orders.size() == 0) {
        // No rtn orders found.
        rsp.CurrentCount = 1;
        rsp.TotalCount = 1;
        rsp.Body = new COrder();
        rsp.RspInfo.ErrorID = ErrorCodes.ORDER_NOT_FOUND;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        session.sendResponse(rsp);
      } else {
        // Send rtn orders.
        rsp.CurrentCount = 0;
        rsp.TotalCount = orders.size();
        rsp.RspInfo.ErrorID = ErrorCodes.NONE;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        // Send rtn orders.
        for (COrder order : orders) {
          ++rsp.CurrentCount;
          rsp.Body = order;
          session.sendResponse(rsp);
        }
      }
    }
    session.done();
  }

  @Override
  public void doQryPosition(
      ServerSession session,
      CQryInvestorPosition query,
      String requestID,
      int current,
      int total) {
    var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
    Message rsp = new Message();
    rsp.Type = MessageType.RSP_QRY_POSITION;
    rsp.RequestID = requestID;
    rsp.ResponseID = UUID.randomUUID().toString();
    rsp.RspInfo = new CRspInfo();
    if (attr == null) {
      // Try to provide as much information as possible.
      var p = new CInvestorPosition();
      p.InstrumentID = query.InstrumentID;
      p.TradingDay = global.getTradingDay();
      // Set rsp.
      rsp.CurrentCount = 1;
      rsp.TotalCount = 1;
      rsp.Body = p;
      rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
      rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
      session.sendResponse(rsp);
    } else {
      var activeUser = (ActiveUser) attr;
      // Performance measurement.
      var max = this.global.getPerformanceMeasure().start("qry.position.max");
      var cur = this.global.getPerformanceMeasure().start("qry.position.cur");
      // Qry position.
      var positions = activeUser.getPosition(query.InstrumentID);
      // End measurement.
      max.endWithMax();
      cur.end();
      if (positions == null || positions.size() == 0) {
        // Try to provide as much information as possible.
        var p = new CInvestorPosition();
        p.InstrumentID = query.InstrumentID;
        p.TradingDay = global.getTradingDay();
        // Set rsp.
        rsp.CurrentCount = 1;
        rsp.TotalCount = 1;
        rsp.Body = p;
        // It's very common that there is no position of spec instrument.
        // Don't report error.
        rsp.RspInfo.ErrorID = ErrorCodes.NONE;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        session.sendResponse(rsp);
      } else {
        rsp.CurrentCount = 0;
        rsp.TotalCount = positions.size();
        rsp.RspInfo.ErrorID = ErrorCodes.NONE;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        // Send positions.
        for (CInvestorPosition position : positions) {
          ++rsp.CurrentCount;
          rsp.Body = position;
          session.sendResponse(rsp);
        }
      }
    }
    session.done();
  }

  @Override
  public void doQryPositionDetail(
      ServerSession session,
      CQryInvestorPositionDetail query,
      String requestID,
      int current,
      int total) {
    var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
    Message rsp = new Message();
    rsp.Type = MessageType.RSP_QRY_POSI_DETAIL;
    rsp.RequestID = requestID;
    rsp.ResponseID = UUID.randomUUID().toString();
    rsp.RspInfo = new CRspInfo();
    if (attr == null) {
      // Try to provide as much information as possible.
      var p = new CInvestorPositionDetail();
      p.InstrumentID = query.InstrumentID;
      p.TradingDay = global.getTradingDay();
      // Set rsp.
      rsp.CurrentCount = 1;
      rsp.TotalCount = 1;
      rsp.Body = p;
      rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
      rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
      session.sendResponse(rsp);
    } else {
      var activeUser = (ActiveUser) attr;
      // Performance measurement.
      var max = this.global.getPerformanceMeasure().start("qry.posidetail.max");
      var cur = this.global.getPerformanceMeasure().start("qry.posidetail.cur");
      // Qry position.
      var details = activeUser.getPositionDetail(query.InstrumentID);
      // End measurement.
      max.endWithMax();
      cur.end();
      if (details == null || details.size() == 0) {
        // Try to provide as much information as possible.
        var p = new CInvestorPositionDetail();
        p.InstrumentID = query.InstrumentID;
        p.TradingDay = global.getTradingDay();
        // Set rsp.
        rsp.CurrentCount = 1;
        rsp.TotalCount = 1;
        rsp.Body = p;
        // It's very common that there is no position of spec instrument.
        // Don't report error.
        rsp.RspInfo.ErrorID = ErrorCodes.NONE;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        session.sendResponse(rsp);
      } else {
        rsp.CurrentCount = 0;
        rsp.TotalCount = details.size();
        rsp.RspInfo.ErrorID = ErrorCodes.NONE;
        rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        // Send positions.
        for (var detail : details) {
          ++rsp.CurrentCount;
          rsp.Body = detail;
          session.sendResponse(rsp);
        }
      }
    }
    session.done();
  }

  @Override
  public void doQryDepthMarketData(
      ServerSession session,
      CQryDepthMarketData query,
      String requestID,
      int current,
      int total) {
    var depth = this.global.getDepthMarketData(query.InstrumentID);
    var rsp = new Message();
    rsp.Type = MessageType.RSP_QRY_MD;
    rsp.RequestID = requestID;
    rsp.ResponseID = UUID.randomUUID().toString();
    rsp.RspInfo = new CRspInfo();
    rsp.CurrentCount = 1;
    rsp.TotalCount = 1;
    if (depth == null) {
      rsp.Body = new CDepthMarketData();
      rsp.RspInfo.ErrorID = ErrorCodes.INSTRUMENT_NOT_FOUND;
    } else {
      rsp.Body = depth;
      rsp.RspInfo.ErrorID = ErrorCodes.NONE;
    }
    rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
    session.sendResponse(rsp);
  }
}