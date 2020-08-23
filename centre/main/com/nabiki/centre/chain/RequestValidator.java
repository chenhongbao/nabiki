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
import com.nabiki.iop.Message;
import com.nabiki.iop.MessageType;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;

import java.util.UUID;

public class RequestValidator extends RequestSuper {
    RequestValidator() {
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

    @Override
    public void doReqOrderInsert(
            ServerSession session,
            CInputOrder request,
            String requestID,
            int current,
            int total) {
        var attr = session.getAttribute(UserLoginManager.FRONT_USERAUTH_KEY);
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
                    if (isAllowed(instrAuth, instrumentID, offset))
                        return;
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

    @Override
    public void doReqOrderAction(
            ServerSession session,
            CInputOrderAction request,
            String requestID,
            int current,
            int total) {
        var attr = session.getAttribute(UserLoginManager.FRONT_USERAUTH_KEY);
        if (attr == null) {
            reply(session,
                    toOrderAction(request),
                    requestID,
                    MessageType.RSP_REQ_ORDER_ACTION,
                    ErrorCodes.NOT_AUTHENT,
                    OP.getErrorMsg(ErrorCodes.NOT_AUTHENT));
        } else {
            var auth = (UserAuthProfile) attr;
            if (request.UserID == null
                    || auth.UserID.compareTo(request.UserID) != 0) {
                reply(session,
                        toOrderAction(request),
                        requestID,
                        MessageType.RSP_REQ_ORDER_ACTION,
                        ErrorCodes.USER_NOT_ACTIVE,
                        OP.getErrorMsg(ErrorCodes.USER_NOT_ACTIVE));
            } else {
                for (var instrAuth : auth.InstrumentAuths) {
                    var instrumentID = request.InstrumentID;
                    if (instrAuth.InstrumentID.compareTo(instrumentID) == 0)
                        return;
                }
                reply(session,
                        toOrderAction(request),
                        requestID,
                        MessageType.RSP_REQ_ORDER_ACTION,
                        ErrorCodes.NO_TRADING_RIGHT,
                        OP.getErrorMsg(ErrorCodes.NO_TRADING_RIGHT));
            }
        }
        session.done();
    }
}
