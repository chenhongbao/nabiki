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
import com.nabiki.iop.*;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class AdaptorChainImpl implements AdaptorChain {
    private final Queue<ServerMessageAdaptor> adaptors
            = new ConcurrentLinkedQueue<>();
    private ServerSessionAdaptor sessionAdaptor;

    AdaptorChainImpl() {}

    @Override
    public void addAdaptor(ServerMessageAdaptor adaptor) {
        if (adaptor == null)
            throw new NullPointerException("adaptor null");
        this.adaptors.add(adaptor);
    }

    void setSessionAdaptor(ServerSessionAdaptor adaptor) {
        this.sessionAdaptor = adaptor;
    }

    void invoke(ServerSessionImpl session, Message message) {
        // Set response not yet.
        session.setResponseState(SessionResponseState.NOT_YET);
        // Go through all adaptors.
        for(var adaptor : this.adaptors) {
            var state = session.getResponseState();
            if (state == SessionResponseState.DONE
                    || state == SessionResponseState.ERROR)
                return;
            // Invoke adaptors.
            try {
                handleMessage(adaptor, session, message);
            } catch (Throwable th) {
                whenError(session, SessionEvent.ERROR, th);
            }
        }
        // The message goes through all adaptors and not done yet.
        whenError(session, SessionEvent.MESSAGE_NOT_DONE, message);
    }

    private void whenError(ServerSessionImpl session, SessionEvent event, Object obj) {
        try {
            if (sessionAdaptor != null)
                sessionAdaptor.doEvent(session, event, obj);
        } catch (Throwable ignored) {}
    }

    private void handleMessage(ServerMessageAdaptor adaptor,
                               ServerSessionImpl session, Message message) {
        switch (message.Type) {
            case SUB_MD:
                adaptor.doSubDepthMarketData(
                        session,
                        (CThostFtdcSubMarketDataField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case UNSUB_MD:
                adaptor.doUnsubDepthMarketData(
                        session,
                        (CThostFtdcUnsubMarketDataField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_AUTHENTICATE:
                adaptor.doReqAuthenticate(
                        session,
                        (CThostFtdcReqAuthenticateField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_LOGIN:
                adaptor.doReqLogin(
                        session,
                        (CThostFtdcReqUserLoginField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_LOGOUT:
                adaptor.doReqLogout(
                        session,
                        (CThostFtdcUserLogoutField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_SETTLEMENT:
                adaptor.doReqSettlementConfirm(
                        session,
                        (CThostFtdcSettlementInfoConfirmField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_ORDER_INSERT:
                adaptor.doReqOrderInsert(
                        session,
                        (CThostFtdcInputOrderField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case REQ_ORDER_ACTION:
                adaptor.doReqOrderAction(
                        session,
                        (CThostFtdcInputOrderActionField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_ACCOUNT:
                adaptor.doQryAccount(
                        session,
                        (CThostFtdcQryTradingAccountField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_ORDER:
                adaptor.doQryOrder(
                        session,
                        (CThostFtdcQryOrderField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_POSITION:
                adaptor.doQryPosition(
                        session,
                        (CThostFtdcQryInvestorPositionField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_POSI_DETAIL:
                adaptor.doQryPositionDetail(
                        session,
                        (CThostFtdcQryInvestorPositionDetailField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_INSTRUMENT:
                adaptor.doQryInstrument(
                        session,
                        (CThostFtdcQryInstrumentField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_COMMISSION:
                adaptor.doQryCommission(
                        session,
                        (CThostFtdcQryInstrumentCommissionRateField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            case QRY_MARGIN:
                adaptor.doQryMargin(
                        session,
                        (CThostFtdcQryInstrumentMarginRateField) message.Body,
                        message.RequestID,
                        message.CurrentCount,
                        message.TotalCount);
                break;
            default:
                session.setResponseState(SessionResponseState.ERROR);
                whenError(session, SessionEvent.STRANGE_MESSAGE, message);
                break;
        }
    }
}
