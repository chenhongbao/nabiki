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

package com.nabiki.client.internal;

import com.nabiki.client.MarketDataListener;
import com.nabiki.client.Response;
import com.nabiki.client.TradeClient;
import com.nabiki.client.TradeClientListener;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.UUID;

class TradeClientImpl implements TradeClient {


    private final IOPClient client = IOP.createClient();
    private final TradeClientAdaptor clientAdaptor = new TradeClientAdaptor();
    private final TradeClientSessionAdaptor sessionAdaptor
            = new TradeClientSessionAdaptor();

    public TradeClientImpl() {
    }

    private ClientSession getSession() {
        var session = this.client.getSession();
        if (session == null || session.isClosed())
            throw new IllegalStateException("session closed");
        else
            return session;
    }

    private <T> Message toMessage(T object, UUID requestID) {
        Objects.requireNonNull(object, "request null");
        Objects.requireNonNull(requestID, "request ID null");
        var message = new Message();
        message.Type =  MessageType.REQ_ORDER_INSERT;
        message.Body = object;
        message.RequestID = requestID;
        message.CurrentCount  = 1;
        message.TotalCount = 1;
        return message;
    }

    private <T, V> Response<T> send(V request, UUID requestID, Class<T> clz) {
        var rsp = new ResponseImpl<T>();
        this.clientAdaptor.setResponse(rsp, requestID);
        getSession().sendRequest(toMessage(request, requestID));
        return rsp;
    }

    @Override
    public Response<CThostFtdcRspUserLoginField> login(
            CThostFtdcReqUserLoginField request, UUID requestID) {
        return send(request, requestID, CThostFtdcRspUserLoginField.class);
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
    public Response<CThostFtdcInputOrderField> orderInsert(
            CThostFtdcInputOrderField order, UUID requestID) {
        return send(order, requestID, CThostFtdcInputOrderField.class);
    }

    @Override
    public Response<CThostFtdcInputOrderActionField> orderAction(
            CThostFtdcInputOrderActionField action, UUID requestID) {
        return send(action, requestID, CThostFtdcInputOrderActionField.class);
    }

    @Override
    public Response<CThostFtdcInvestorPositionField> queryPosition(
            CThostFtdcQryInvestorPositionField query, UUID requestID) {
        return send(query, requestID, CThostFtdcInvestorPositionField.class);
    }

    @Override
    public Response<CThostFtdcTradingAccountField> queryAccount(
            CThostFtdcQryTradingAccountField query, UUID requestID) {
        return send(query, requestID, CThostFtdcTradingAccountField.class);
    }

    @Override
    public Response<CThostFtdcOrderField> queryOrder(
            CThostFtdcOrderUuidField uuid, UUID requestID) {
        return send(uuid, requestID, CThostFtdcOrderField.class);
    }

    @Override
    public Response<CThostFtdcSpecificInstrumentField> subscribeMarketData(
            CThostFtdcSubMarketDataField subscription, UUID requestID) {
        return send(subscription, requestID,
                CThostFtdcSpecificInstrumentField.class);
    }
}
