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

package com.nabiki.client.sdk.internal;

import com.nabiki.client.sdk.MarketDataListener;
import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.client.sdk.TradeClientListener;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.*;
import com.nabiki.commons.utils.Utils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

class TradeClientImpl implements TradeClient {
  private final IOPClient client = IOP.createClient();
  private final TradeClientAdaptor clientAdaptor = new TradeClientAdaptor();
  private final TradeClientSessionAdaptor sessionAdaptor
      = new TradeClientSessionAdaptor();

  public final static String USER_PRODUCT_INFO = "SDK";

  private CReqUserLogin lastLoginReq;

  public TradeClientImpl() {
  }

  private ClientSession getSession() {
    var session = this.client.getSession();
    if (session == null || session.isClosed())
      throw new IllegalStateException("session closed");
    else
      return session;
  }

  private <T> Message toMessage(MessageType type, T object, String requestID) {
    Objects.requireNonNull(object, "request null");
    Objects.requireNonNull(requestID, "request ID null");
    var message = new Message();
    message.Type = type;
    message.Body = object;
    message.RequestID = requestID;
    message.CurrentCount = 1;
    message.TotalCount = 1;
    return message;
  }

  private <T, V> Response<T> send(
      MessageType type,
      V request,
      String requestID,
      Class<T> clz) throws InterruptedException {
    var rsp = new ResponseImpl<T>();
    this.clientAdaptor.setResponse(rsp, requestID);
    getSession().sendRequest(toMessage(type, request, requestID));
    return rsp;
  }

  private void requireLogin() {
    if (lastLoginReq == null)
      throw new NullPointerException("need login");
  }

  @Override
  public Response<CRspUserLogin> login(CReqUserLogin request) {
    var requestID = Utils.getUID();
    var rsp = new ResponseImpl<CRspUserLogin>();
    this.clientAdaptor.setResponse(rsp, requestID);
    // Set user product info.
    if (request.UserProductInfo == null ||
        request.UserProductInfo.trim().length() == 0) {
      request.UserProductInfo = USER_PRODUCT_INFO;
    }
    getSession().sendLogin(
        toMessage(MessageType.REQ_LOGIN, request, requestID));
    // Preserve login request.
    this.lastLoginReq = request;
    return rsp;
  }

  @Override
  public void setListener(TradeClientListener listener) {
    if (listener != null)
      this.sessionAdaptor.setListener(listener);
  }

  @Override
  public void setListener(MarketDataListener listener) {
    if (listener != null)
      this.clientAdaptor.setListener(listener);
  }

  @Override
  public void open(InetSocketAddress address) throws IOException {
    this.client.setMessageAdaptor(this.clientAdaptor);
    this.client.setSessionAdaptor(this.sessionAdaptor);
    this.client.connect(address);
  }

  @Override
  public void close() {
    this.client.disconnect();
  }

  @Override
  public boolean isClosed() {
    return !this.client.isConnected();
  }

  @Override
  public String getTradingDay() {
    return this.clientAdaptor.getTradingDay();
  }

  @Override
  public long getLag() {
    return client.getSession().getLag();
  }

  @Override
  public IOPClient getIOP() {
    return client;
  }

  @Override
  public Response<COrder> orderInsert(CInputOrder order) throws Exception {
    requireLogin();
    order.InvestorID
        = order.UserID
        = order.AccountID
        = this.lastLoginReq.UserID;
    order.BrokerID = this.lastLoginReq.BrokerID;
    return send(
        MessageType.REQ_ORDER_INSERT,
        order,
        Utils.getUID(),
        COrder.class);
  }

  @Override
  public Response<COrderAction> orderAction(
      CInputOrderAction action) throws Exception {
    requireLogin();
    action.InvestorID
        = action.UserID
        = this.lastLoginReq.UserID;
    action.BrokerID = this.lastLoginReq.BrokerID;
    return send(
        MessageType.REQ_ORDER_ACTION,
        action,
        Utils.getUID(),
        COrderAction.class);
  }

  @Override
  public Response<CDepthMarketData> queryDepthMarketData(
      CQryDepthMarketData query) throws Exception {
    requireLogin();
    return send(
        MessageType.QRY_MD,
        query,
        Utils.getUID(),
        CDepthMarketData.class);
  }

  @Override
  public Response<CInvestorPosition> queryPosition(
      CQryInvestorPosition query) throws Exception {
    requireLogin();
    query.InvestorID = this.lastLoginReq.UserID;
    query.BrokerID = this.lastLoginReq.BrokerID;
    return send(
        MessageType.QRY_POSITION,
        query,
        Utils.getUID(),
        CInvestorPosition.class);
  }

  @Override
  public Response<CInvestorPositionDetail> queryPositionDetail(
      CQryInvestorPositionDetail query) throws Exception {
    requireLogin();
    query.InvestorID = this.lastLoginReq.UserID;
    query.BrokerID = this.lastLoginReq.BrokerID;
    return send(
        MessageType.QRY_POSI_DETAIL,
        query,
        Utils.getUID(),
        CInvestorPositionDetail.class);
  }

  @Override
  public Response<CTradingAccount> queryAccount(
      CQryTradingAccount query) throws Exception {
    requireLogin();
    query.InvestorID
        = query.AccountID
        = this.lastLoginReq.UserID;
    query.BrokerID = this.lastLoginReq.BrokerID;
    query.CurrencyID = "CNY";
    return send(
        MessageType.QRY_ACCOUNT,
        query,
        Utils.getUID(),
        CTradingAccount.class);
  }

  @Override
  public Response<COrder> queryOrder(CQryOrder query) throws Exception {
    requireLogin();
    query.InvestorID = this.lastLoginReq.UserID;
    query.BrokerID = this.lastLoginReq.BrokerID;
    return send(
        MessageType.QRY_ORDER,
        query,
        Utils.getUID(),
        COrder.class);
  }

  @Override
  public Response<CSpecificInstrument> subscribeMarketData(
      CSubMarketData subscription) throws Exception {
    requireLogin();
    return send(
        MessageType.SUB_MD,
        subscription,
        Utils.getUID(),
        CSpecificInstrument.class);
  }

  @Override
  public Response<CSpecificInstrument> unSubscribeMarketData(
      CUnsubMarketData subscription) throws Exception {
    requireLogin();
    return send(
        MessageType.UNSUB_MD,
        subscription,
        Utils.getUID(),
        CSpecificInstrument.class);
  }

  @Override
  public Response<CInstrument> queryInstrument(CQryInstrument query) throws Exception {
    return send(
        MessageType.QRY_INSTRUMENT,
        query,
        Utils.getUID(),
        CInstrument.class);
  }

  @Override
  public Response<CInstrumentMarginRate> queryMargin(
      CQryInstrumentMarginRate query) throws Exception {
    return send(
        MessageType.QRY_MARGIN,
        query,
        Utils.getUID(),
        CInstrumentMarginRate.class);
  }

  @Override
  public Response<CInstrumentCommissionRate> queryCommission(
      CQryInstrumentCommissionRate query) throws Exception {
    return send(
        MessageType.QRY_COMMISSION,
        query,
        Utils.getUID(),
        CInstrumentCommissionRate.class);
  }
}
