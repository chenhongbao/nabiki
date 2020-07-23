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
import com.nabiki.iop.ClientMessageAdaptor;
import com.nabiki.iop.ClientMessageHandler;
import com.nabiki.iop.ClientSession;
import com.nabiki.iop.Message;

public class DefaultClientMessageHandler implements ClientMessageHandler {
    static class DefaultClientMessageAdaptor extends ClientMessageAdaptor {
    }

    private ClientMessageAdaptor msgAdaptor = new DefaultClientMessageAdaptor();

    DefaultClientMessageHandler() {
    }

    void setAdaptor(ClientMessageAdaptor adaptor) {
        this.msgAdaptor = adaptor;
    }

    @Override
    public void onMessage(ClientSession session, Message message) {
        handleMessage(this.msgAdaptor, message);
    }

    private void handleMessage(ClientMessageAdaptor adaptor, Message message) {
        switch (message.Type) {
            case RSP_SUB_MD:
                adaptor.doRspSubscribeMarketData(
                        (CThostFtdcSpecificInstrumentField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_UNSUB_MD:
                adaptor.doRspUnsubscribeMarketData(
                        (CThostFtdcSpecificInstrumentField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case FLOW_DEPTH:
                adaptor.doRspDepthMarketData(
                        (CThostFtdcDepthMarketDataField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case FLOW_CANDLE:
                adaptor.doRspCandle(
                        (CThostFtdcCandleField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_AUTHENTICATE:
                adaptor.doRspAuthenticate(
                        (CThostFtdcRspAuthenticateField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_LOGIN:
                adaptor.doRspReqLogin(
                        (CThostFtdcRspUserLoginField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_LOGOUT:
                adaptor.doRspReqLogout(
                        (CThostFtdcUserLogoutField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_SETTLEMENT:
                adaptor.doRspReqSettlementConfirm(
                        (CThostFtdcSettlementInfoConfirmField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_ORDER_INSERT:
                adaptor.doRspReqOrderInsert(
                        (CThostFtdcOrderField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_REQ_ORDER_ACTION:
                adaptor.doRspReqOrderAction(
                        (CThostFtdcOrderActionField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_ACCOUNT:
                adaptor.doRspQryAccount(
                        (CThostFtdcTradingAccountField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_ORDER:
                adaptor.doRspQryOrder(
                        (CThostFtdcOrderField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_POSITION:
                adaptor.doRspQryPosition(
                        (CThostFtdcInvestorPositionField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_POSI_DETAIL:
                adaptor.doRspQryPositionDetail(
                        (CThostFtdcInvestorPositionDetailField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_INSTRUMENT:
                adaptor.doRspQryInstrument(
                        (CThostFtdcInstrumentField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_COMMISSION:
                adaptor.doRspQryCommission(
                        (CThostFtdcInstrumentCommissionRateField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_QRY_MARGIN:
                adaptor.doRspQryMargin(
                        (CThostFtdcInstrumentMarginRateField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RTN_ORDER:
                adaptor.doRtnOrder(
                        (CThostFtdcOrderField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RTN_TRADE:
                adaptor.doRtnTrade(
                        (CThostFtdcTradeField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RTN_ORDER_ACTION:
                adaptor.doRtnOrderAction(
                        (CThostFtdcOrderActionField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RTN_ORDER_INSERT:
                adaptor.doRtnOrderInsert(
                        (CThostFtdcInputOrderField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_ORDER_ACTION:
                adaptor.doRspOrderAction(
                        (CThostFtdcInputOrderActionField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_ORDER_INSERT:
                adaptor.doRspOrderInsert(
                        (CThostFtdcInputOrderField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_ERROR:
                adaptor.doRspError(
                        (CThostFtdcRspInfoField) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_CONNECT:
                adaptor.doRspConnect(
                        (CThostFtdcConnect) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case RSP_DISCONNECT:
                adaptor.doRspDisconnect(
                        (CThostFtdcDisconnect) message.Body,
                        message.RspInfo,
                        message.RequestID,
                        message.ResponseID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            default:
                throw new IllegalStateException(
                        "unknown message type " + message.Type);
        }
    }
}
