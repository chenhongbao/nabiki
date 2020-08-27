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
import com.nabiki.iop.Message;
import com.nabiki.iop.MessageType;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;

import java.util.UUID;

public class RequestExecutor extends RequestSuper {
    private final Global global;
    RequestExecutor(Global global) {
        this.global = global;
    }

    @Override
    public void doReqOrderInsert(
            ServerSession session,
            CInputOrder request,
            String requestID,
            int current,
            int total) {
        var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
        Message rsp = new Message();
        rsp.Type = MessageType.RSP_REQ_ORDER_INSERT;
        rsp.RequestID = requestID;
        rsp.ResponseID = UUID.randomUUID().toString();
        rsp.CurrentCount = 1;
        rsp.TotalCount = 1;
        if (attr == null) {
            rsp.Body = new COrder();
            rsp.RspInfo = new CRspInfo();
            rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
            rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        } else {
            var activeUser = (ActiveUser)attr;
            // Measure performance.
            var max = this.global.getPerformanceMeasure().start("order.insert.max");
            var avr = this.global.getPerformanceMeasure().start("order.insert.avr");
            // Order insert.
            var uuid = activeUser.insertOrder(request);
            // End measurement.
            max.endWithMax();
            avr.end();
            // Build response.
            var o = toRtnOrder(request);
            rsp.RspInfo = activeUser.getExecRsp(uuid);
            if (rsp.RspInfo.ErrorID == ErrorCodes.NONE)
                o.OrderLocalID = uuid;
            else
                o.OrderLocalID = null;
            o.OrderSubmitStatus = OrderSubmitStatusType.ACCEPTED;
            o.OrderStatus = OrderStatusType.UNKNOWN;
            rsp.Body = o;
        }
        session.sendResponse(rsp);
        session.done();
    }

    @Override
    public void doReqOrderAction(
            ServerSession session,
            CInputOrderAction request,
            String requestID,
            int current,
            int total) {
        var attr = session.getAttribute(UserLoginManager.FRONT_ACTIVEUSR_KEY);
        Message rsp = new Message();
        rsp.Type = MessageType.RSP_REQ_ORDER_ACTION;
        rsp.RequestID = requestID;
        rsp.ResponseID = UUID.randomUUID().toString();
        rsp.CurrentCount = 1;
        rsp.TotalCount = 1;
        if (attr == null) {
            rsp.Body = new COrderAction();
            rsp.RspInfo = new CRspInfo();
            rsp.RspInfo.ErrorID = ErrorCodes.USER_NOT_ACTIVE;
            rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        } else {
            var activeUser = (ActiveUser)attr;
            // Measure performance.
            var max = this.global.getPerformanceMeasure().start("order.insert.max");
            var avr = this.global.getPerformanceMeasure().start("order.insert.avr");
            // Order action.
            var uuid = activeUser.orderAction(request);
            // End measurement.
            max.endWithMax();
            avr.end();
            // Build response.
            var o = toOrderAction(request);
            o.OrderLocalID = request.OrderSysID;
            o.OrderSysID = null;
            rsp.Body = o;
            rsp.RspInfo = activeUser.getExecRsp(uuid);
        }
        session.sendResponse(rsp);
        session.done();
    }
}
