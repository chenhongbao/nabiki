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

package com.nabiki.iop.internal;

import com.nabiki.iop.Message;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;

import java.io.IOException;

class MessageImpl extends Message {
    protected MessageImpl() {
    }

    static Message toMessage(Body body) throws IOException {
        var msg = new Message();
        // Decode object.
        if (body.Body != null && body.Body.length() > 0) {
            switch (body.Type) {
                case HEARTBEAT:
                    msg.Body = null;
                    break;
                case SUB_MD:
                    msg.Body = OP.fromJson(body.Body, CSubMarketData.class);
                    break;
                case RSP_SUB_MD:
                case RSP_UNSUB_MD:
                    msg.Body = OP.fromJson(body.Body, CSpecificInstrument.class);
                    break;
                case UNSUB_MD:
                    msg.Body = OP.fromJson(body.Body, CUnsubMarketData.class);
                    break;
                case FLOW_DEPTH:
                case RSP_QRY_MD:
                    msg.Body = OP.fromJson(body.Body, CDepthMarketData.class);
                    break;
                case FLOW_CANDLE:
                    msg.Body = OP.fromJson(body.Body, CCandle.class);
                    break;
                case REQ_AUTHENTICATE:
                    msg.Body = OP.fromJson(body.Body, CReqAuthenticate.class);
                    break;
                case RSP_REQ_AUTHENTICATE:
                    msg.Body = OP.fromJson(body.Body, CRspAuthenticate.class);
                    break;
                case REQ_LOGIN:
                    msg.Body = OP.fromJson(body.Body, CReqUserLogin.class);
                    break;
                case RSP_REQ_LOGIN:
                    msg.Body = OP.fromJson(body.Body, CRspUserLogin.class);
                    break;
                case REQ_LOGOUT:
                case RSP_REQ_LOGOUT:
                    msg.Body = OP.fromJson(body.Body, CUserLogout.class);
                    break;
                case REQ_SETTLEMENT:
                    msg.Body = OP.fromJson(body.Body, CSettlementInfoConfirm.class);
                    break;
                case RSP_REQ_SETTLEMENT:
                    msg.Body = OP.fromJson(body.Body, CSettlementInfoConfirm.class);
                    break;
                case QRY_MD:
                    msg.Body = OP.fromJson(body.Body, CQryDepthMarketData.class);
                    break;
                case REQ_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body, CInputOrder.class);
                    break;
                case RSP_REQ_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body, COrder.class);
                    break;
                case REQ_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body, CInputOrderAction.class);
                    break;
                case RSP_REQ_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body, COrderAction.class);
                    break;
                case QRY_ACCOUNT:
                    msg.Body = OP.fromJson(body.Body, CQryTradingAccount.class);
                    break;
                case RSP_QRY_ACCOUNT:
                    msg.Body = OP.fromJson(body.Body, CTradingAccount.class);
                    break;
                case QRY_ORDER:
                    msg.Body = OP.fromJson(body.Body, CQryOrder.class);
                    break;
                case RSP_QRY_ORDER:
                    msg.Body = OP.fromJson(body.Body, COrder.class);
                    break;
                case QRY_POSITION:
                    msg.Body = OP.fromJson(body.Body, CQryInvestorPosition.class);
                    break;
                case RSP_QRY_POSITION:
                    msg.Body = OP.fromJson(body.Body, CInvestorPosition.class);
                    break;
                case QRY_POSI_DETAIL:
                    msg.Body = OP.fromJson(body.Body, CQryInvestorPositionDetail.class);
                    break;
                case RSP_QRY_POSI_DETAIL:
                    msg.Body = OP.fromJson(body.Body, CInvestorPositionDetail.class);
                    break;
                case QRY_INSTRUMENT:
                    msg.Body = OP.fromJson(body.Body, CQryInstrument.class);
                    break;
                case RSP_QRY_INSTRUMENT:
                    msg.Body = OP.fromJson(body.Body, CInstrument.class);
                    break;
                case QRY_COMMISSION:
                    msg.Body = OP.fromJson(body.Body, CQryInstrumentCommissionRate.class);
                    break;
                case RSP_QRY_COMMISSION:
                    msg.Body = OP.fromJson(body.Body, CInstrumentCommissionRate.class);
                    break;
                case QRY_MARGIN:
                    msg.Body = OP.fromJson(body.Body, CQryInstrumentMarginRate.class);
                    break;
                case RSP_QRY_MARGIN:
                    msg.Body = OP.fromJson(body.Body, CInstrumentMarginRate.class);
                    break;
                case RTN_ORDER:
                    msg.Body = OP.fromJson(body.Body, COrder.class);
                    break;
                case RTN_TRADE:
                    msg.Body = OP.fromJson(body.Body, CTrade.class);
                    break;
                case RTN_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body, COrderAction.class);
                    break;
                case RTN_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body, CInputOrder.class);
                    break;
                case RSP_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body, CInputOrderAction.class);
                    break;
                case RSP_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body, CInputOrder.class);
                    break;
                case RSP_ERROR:
                    msg.Body = OP.fromJson(body.Body, CRspInfo.class);
                    break;
                case RSP_CONNECT:
                    msg.Body = OP.fromJson(body.Body, CConnect.class);
                    break;
                case RSP_DISCONNECT:
                    msg.Body = OP.fromJson(body.Body, CDisconnect.class);
                    break;
                default:
                    throw new IOException("unknown message type " + body.Type);
            }
        }
        if (body.RspInfo != null && body.RspInfo.length() > 0)
            msg.RspInfo = OP.fromJson(body.RspInfo,
                    CRspInfo.class);
        // Copy other info.
        msg.Type = body.Type;
        msg.RequestID = body.RequestID;
        msg.ResponseID = body.ResponseID;
        msg.CurrentCount = body.CurrentCount;
        msg.TotalCount = body.TotalCount;
        return msg;
    }
}
