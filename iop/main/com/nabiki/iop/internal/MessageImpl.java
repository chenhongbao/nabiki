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

import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.Message;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.x.OP;

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
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcSubMarketDataField.class);
                    break;
                case RSP_SUB_MD:
                case RSP_UNSUB_MD:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcSpecificInstrumentField.class);
                    break;
                case UNSUB_MD:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcUnsubMarketDataField.class);
                    break;
                case FLOW_DEPTH:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcDepthMarketDataField.class);
                    break;
                case FLOW_CANDLE:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcCandleField.class);
                    break;
                case REQ_AUTHENTICATE:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcReqAuthenticateField.class);
                    break;
                case RSP_REQ_AUTHENTICATE:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcRspAuthenticateField.class);
                    break;
                case REQ_LOGIN:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcReqUserLoginField.class);
                    break;
                case RSP_REQ_LOGIN:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcRspUserLoginField.class);
                    break;
                case REQ_LOGOUT:
                case RSP_REQ_LOGOUT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcUserLogoutField.class);
                    break;
                case REQ_SETTLEMENT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcSettlementInfoConfirmField.class);
                    break;
                case RSP_REQ_SETTLEMENT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcSettlementInfoConfirmField.class);
                    break;
                case REQ_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInputOrderField.class);
                    break;
                case RSP_REQ_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcOrderField.class);
                    break;
                case REQ_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInputOrderActionField.class);
                    break;
                case RSP_REQ_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcOrderActionField.class);
                    break;
                case QRY_ACCOUNT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryTradingAccountField.class);
                    break;
                case RSP_QRY_ACCOUNT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcTradingAccountField.class);
                    break;
                case QRY_ORDER:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryOrderField.class);
                    break;
                case RSP_QRY_ORDER:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcOrderField.class);
                    break;
                case QRY_POSITION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryInvestorPositionField.class);
                    break;
                case RSP_QRY_POSITION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInvestorPositionField.class);
                    break;
                case QRY_POSI_DETAIL:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryInvestorPositionDetailField.class);
                    break;
                case RSP_REQ_POSI_DETAIL:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInvestorPositionDetailField.class);
                    break;
                case QRY_INSTRUMENT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryInstrumentField.class);
                    break;
                case RSP_QRY_INSTRUMENT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInstrumentField.class);
                    break;
                case QRY_COMMISSION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryInstrumentCommissionRateField.class);
                    break;
                case RSP_QRY_COMMISSION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInstrumentCommissionRateField.class);
                    break;
                case QRY_MARGIN:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcQryInstrumentMarginRateField.class);
                    break;
                case RSP_QRY_MARGIN:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInstrumentMarginRateField.class);
                    break;
                case RTN_ORDER:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcOrderField.class);
                    break;
                case RTN_TRADE:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcTradeField.class);
                    break;
                case RTN_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcOrderActionField.class);
                    break;
                case RTN_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInputOrderField.class);
                    break;
                case RSP_ORDER_ACTION:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInputOrderActionField.class);
                    break;
                case RSP_ORDER_INSERT:
                    msg.Body = OP.fromJson(body.Body,
                            CThostFtdcInputOrderField.class);
                    break;
                default:
                    throw new IOException("unknown message type " + body.Type);
            }
        }
        if (body.RspInfo != null && body.RspInfo.length() > 0)
            msg.RspInfo = OP.fromJson(body.RspInfo,
                    CThostFtdcRspInfoField.class);
        // Copy other info.
        msg.Type = body.Type;
        msg.RequestID = body.RequestID;
        msg.ResponseID = body.ResponseID;
        msg.CurrentCount = body.CurrentCount;
        msg.TotalCount = body.TotalCount;
        return msg;
    }
}
