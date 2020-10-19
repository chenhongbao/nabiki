/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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
import com.nabiki.commons.iop.ClientMessageAdaptor;
import com.nabiki.commons.iop.ClientMessageHandler;
import com.nabiki.commons.iop.ClientSession;
import com.nabiki.commons.iop.Message;

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
            (CSpecificInstrument) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_UNSUB_MD:
        adaptor.doRspUnsubscribeMarketData(
            (CSpecificInstrument) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case FLOW_DEPTH:
        adaptor.doRspDepthMarketData(
            (CDepthMarketData) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case FLOW_CANDLE:
        adaptor.doRspCandle(
            (CCandle) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_AUTHENTICATE:
        adaptor.doRspAuthenticate(
            (CRspAuthenticate) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_LOGIN:
        adaptor.doRspReqLogin(
            (CRspUserLogin) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_LOGOUT:
        adaptor.doRspReqLogout(
            (CUserLogout) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_SETTLEMENT:
        adaptor.doRspReqSettlementConfirm(
            (CSettlementInfoConfirm) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_ORDER_INSERT:
        adaptor.doRspReqOrderInsert(
            (COrder) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_REQ_ORDER_ACTION:
        adaptor.doRspReqOrderAction(
            (COrderAction) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_MD:
        adaptor.doRspQryDepthMarketData(
            (CDepthMarketData) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_ACCOUNT:
        adaptor.doRspQryAccount(
            (CTradingAccount) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_ORDER:
        adaptor.doRspQryOrder(
            (COrder) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_POSITION:
        adaptor.doRspQryPosition(
            (CInvestorPosition) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_POSI_DETAIL:
        adaptor.doRspQryPositionDetail(
            (CInvestorPositionDetail) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_INSTRUMENT:
        adaptor.doRspQryInstrument(
            (CInstrument) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_COMMISSION:
        adaptor.doRspQryCommission(
            (CInstrumentCommissionRate) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_QRY_MARGIN:
        adaptor.doRspQryMargin(
            (CInstrumentMarginRate) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RTN_ORDER:
        adaptor.doRtnOrder(
            (COrder) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RTN_TRADE:
        adaptor.doRtnTrade(
            (CTrade) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RTN_ORDER_ACTION:
        adaptor.doRtnOrderAction(
            (COrderAction) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RTN_ORDER_INSERT:
        adaptor.doRtnOrderInsert(
            (CInputOrder) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_ORDER_ACTION:
        adaptor.doRspOrderAction(
            (CInputOrderAction) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_ORDER_INSERT:
        adaptor.doRspOrderInsert(
            (CInputOrder) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_ERROR:
        adaptor.doRspError(
            (CRspInfo) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_CONNECT:
        adaptor.doRspConnect(
            (CConnect) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      case RSP_DISCONNECT:
        adaptor.doRspDisconnect(
            (CDisconnect) message.Body,
            message.RspInfo,
            message.RequestID,
            message.ResponseID,
            message.CurrentCount,
            message.TotalCount);
        break;
      default:
        throw new IllegalStateException(String.format("unknown frame type %s", message.Type));
    }
  }
}
