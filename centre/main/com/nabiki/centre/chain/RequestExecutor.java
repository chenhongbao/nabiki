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

import com.nabiki.centre.active.ActiveUser;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.flag.TThostFtdcOrderStatusType;
import com.nabiki.ctp4j.jni.flag.TThostFtdcOrderSubmitStatusType;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.Message;
import com.nabiki.iop.MessageType;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.x.OP;

import java.util.UUID;

public class RequestExecutor extends RequestSuper {
    RequestExecutor() {
    }

    @Override
    public void doReqOrderInsert(
            ServerSession session,
            CThostFtdcInputOrderField request,
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
            rsp.Body = new CThostFtdcOrderField();
            rsp.RspInfo = new CThostFtdcRspInfoField();
            rsp.RspInfo.ErrorID = TThostFtdcErrorCode.USER_NOT_ACTIVE;
            rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        } else {
            var activeUser = (ActiveUser)attr;
            var uuid = activeUser.insertOrder(request);
            // Build response.
            var o = toRtnOrder(request);
            o.OrderLocalID = uuid;
            o.OrderSubmitStatus = TThostFtdcOrderSubmitStatusType.ACCEPTED;
            o.OrderStatus = TThostFtdcOrderStatusType.UNKNOWN;
            rsp.Body = o;
            rsp.RspInfo = activeUser.getExecRsp(uuid);
        }
        session.sendResponse(rsp);
        session.done();
    }

    @Override
    public void doReqOrderAction(
            ServerSession session,
            CThostFtdcInputOrderActionField request,
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
            rsp.Body = new CThostFtdcOrderActionField();
            rsp.RspInfo = new CThostFtdcRspInfoField();
            rsp.RspInfo.ErrorID = TThostFtdcErrorCode.USER_NOT_ACTIVE;
            rsp.RspInfo.ErrorMsg = OP.getErrorMsg(rsp.RspInfo.ErrorID);
        } else {
            var activeUser = (ActiveUser)attr;
            var uuid = activeUser.orderAction(request);
            // Build response.
            var o = toOrderAction(request);
            o.OrderLocalID = uuid;
            rsp.Body = o;
            rsp.RspInfo = activeUser.getExecRsp(uuid);
        }
        session.sendResponse(rsp);
        session.done();
    }
}
