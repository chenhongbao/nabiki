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

package com.nabiki.client.sdk.internal;

import com.nabiki.client.sdk.MarketDataListener;
import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.client.sdk.TradeClientListener;
import com.nabiki.iop.*;
import com.nabiki.objects.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;

class TradeClientImpl implements TradeClient {
    private final IOPClient client = IOP.createClient();
    private final TradeClientAdaptor clientAdaptor = new TradeClientAdaptor();
    private final TradeClientSessionAdaptor sessionAdaptor
            = new TradeClientSessionAdaptor();

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
            Class<T> clz) {
        var rsp = new ResponseImpl<T>();
        this.clientAdaptor.setResponse(rsp, requestID);
        getSession().sendRequest(toMessage(type, request, requestID));
        return rsp;
    }

    @Override
    public Response<CRspUserLogin> login(
            CReqUserLogin request, String requestID) {
        var rsp = new ResponseImpl<CRspUserLogin>();
        this.clientAdaptor.setResponse(rsp, requestID);
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
    public Response<COrder> orderInsert(
            CInputOrder order, String requestID) {
        order.InvestorID
                = order.UserID
                = order.AccountID
                = this.lastLoginReq.UserID;
        order.BrokerID = this.lastLoginReq.BrokerID;
        return send(
                MessageType.REQ_ORDER_INSERT,
                order,
                requestID,
                COrder.class);
    }

    @Override
    public Response<COrderAction> orderAction(
            CInputOrderAction action, String requestID) {
        action.InvestorID
                = action.UserID
                = this.lastLoginReq.UserID;
        action.BrokerID = this.lastLoginReq.BrokerID;
        return send(
                MessageType.REQ_ORDER_ACTION,
                action,
                requestID,
                COrderAction.class);
    }

    @Override
    public Response<CDepthMarketData> queryDepthMarketData(
            CQryDepthMarketData query, String requestID) {
        return send(
                MessageType.QRY_MD,
                query,
                requestID,
                CDepthMarketData.class);
    }

    @Override
    public Response<CInvestorPosition> queryPosition(
            CQryInvestorPosition query, String requestID) {
        query.InvestorID = this.lastLoginReq.UserID;
        query.BrokerID  = this.lastLoginReq.BrokerID;
        return send(
                MessageType.QRY_POSITION,
                query,
                requestID,
                CInvestorPosition.class);
    }

    @Override
    public Response<CTradingAccount> queryAccount(
            CQryTradingAccount query, String requestID) {
        query.InvestorID
                = query.AccountID
                = this.lastLoginReq.UserID;
        query.BrokerID  = this.lastLoginReq.BrokerID;
        query.CurrencyID = "CNY";
        return send(
                MessageType.QRY_ACCOUNT,
                query,
                requestID,
                CTradingAccount.class);
    }

    @Override
    public Response<COrder> queryOrder(
            CQryOrder query, String requestID) {
        query.InvestorID = this.lastLoginReq.UserID;
        query.BrokerID  = this.lastLoginReq.BrokerID;
        return send(
                MessageType.QRY_ORDER,
                query,
                requestID,
                COrder.class);
    }

    @Override
    public Response<CSpecificInstrument> subscribeMarketData(
            CSubMarketData subscription, String requestID) {
        return send(
                MessageType.SUB_MD,
                subscription,
                requestID,
                CSpecificInstrument.class);
    }

    @Override
    public Response<CSpecificInstrument> unSubscribeMarketData(
            CUnsubMarketData subscription, String requestID) {
        return send(
                MessageType.UNSUB_MD,
                subscription,
                requestID,
                CSpecificInstrument.class);
    }
}
