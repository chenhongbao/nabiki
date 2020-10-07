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

package com.nabiki.commons.iop.internal;

import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.*;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

class AdaptorChainImpl implements AdaptorChain {
  private final Queue<ServerMessageAdaptor> adaptors
      = new ConcurrentLinkedQueue<>();
  private ServerSessionAdaptor sessionAdaptor;

  AdaptorChainImpl() {
  }

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
    for (var adaptor : this.adaptors) {
      var state = session.getResponseState();
      if (state == SessionResponseState.DONE
          || state == SessionResponseState.ERROR)
        return;
      // Invoke adaptors.
      try {
        handleMessage(adaptor, session, message);
      } catch (Throwable th) {
        th.printStackTrace();
        session.setResponseState(SessionResponseState.ERROR);
        whenError(session, SessionEvent.ERROR, th);
      }
    }
    // The message goes through all adaptors and not done yet.
    if (session.getResponseState() != SessionResponseState.DONE)
      whenError(session, SessionEvent.MESSAGE_NOT_DONE, message);
  }

  private void whenError(ServerSessionImpl session, SessionEvent event, Object obj) {
    try {
      if (sessionAdaptor != null)
        sessionAdaptor.doEvent(session, event, obj);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  private void handleMessage(ServerMessageAdaptor adaptor,
                             ServerSessionImpl session, Message message) {
    switch (message.Type) {
      case SUB_MD:
        adaptor.doSubDepthMarketData(
            session,
            (CSubMarketData) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case UNSUB_MD:
        adaptor.doUnsubDepthMarketData(
            session,
            (CUnsubMarketData) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_AUTHENTICATE:
        adaptor.doReqAuthenticate(
            session,
            (CReqAuthenticate) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_LOGIN:
        adaptor.doReqLogin(
            session,
            (CReqUserLogin) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_LOGOUT:
        adaptor.doReqLogout(
            session,
            (CUserLogout) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_SETTLEMENT:
        adaptor.doReqSettlementConfirm(
            session,
            (CSettlementInfoConfirm) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_ORDER_INSERT:
        adaptor.doReqOrderInsert(
            session,
            (CInputOrder) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case REQ_ORDER_ACTION:
        adaptor.doReqOrderAction(
            session,
            (CInputOrderAction) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_MD:
        adaptor.doQryDepthMarketData(
            session,
            (CQryDepthMarketData) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_ACCOUNT:
        adaptor.doQryAccount(
            session,
            (CQryTradingAccount) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_ORDER:
        adaptor.doQryOrder(
            session,
            (CQryOrder) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_POSITION:
        adaptor.doQryPosition(
            session,
            (CQryInvestorPosition) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_POSI_DETAIL:
        adaptor.doQryPositionDetail(
            session,
            (CQryInvestorPositionDetail) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_INSTRUMENT:
        adaptor.doQryInstrument(
            session,
            (CQryInstrument) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_COMMISSION:
        adaptor.doQryCommission(
            session,
            (CQryInstrumentCommissionRate) message.Body,
            message.RequestID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case QRY_MARGIN:
        adaptor.doQryMargin(
            session,
            (CQryInstrumentMarginRate) message.Body,
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
