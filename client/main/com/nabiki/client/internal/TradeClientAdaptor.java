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
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.ClientMessageAdaptor;
import com.nabiki.iop.Message;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class TradeClientAdaptor extends ClientMessageAdaptor {
    private class DefaultDepthListener implements MarketDataListener {
        @Override
        public void onDepthMarketData(CThostFtdcDepthMarketDataField depth) {
        }

        @Override
        public void onCandle(CThostFtdcCandleField candle) {
        }
    }

    private final Map<UUID, ResponseImpl<?>> responses = new ConcurrentHashMap<>();
    private final AtomicReference<MarketDataListener> listener
            = new AtomicReference<>(new DefaultDepthListener());

    TradeClientAdaptor() {}

    void setResponse(ResponseImpl<?> response, UUID requestID) {
        Objects.requireNonNull(response, "response future null");
        Objects.requireNonNull(requestID, "request ID null");
        if (this.responses.containsKey(requestID))
            throw new IllegalArgumentException("duplicated request ID");
        this.responses.put(requestID, response);
    }

    void setListener(MarketDataListener listener) {
        if (listener != null)
            this.listener.set(listener);
    }

    private void checkCompletion(ResponseImpl<?> response, UUID requestID) {
        // Check the completion of response.
        if (response.getArrivalCount() == response.getTotalCount()
                && response.getTotalCount() != 0)
            this.responses.remove(requestID);
    }

    private void requireMessage(Message message) {
        Objects.requireNonNull(message, "message null");
        Objects.requireNonNull(message.RequestID, "request ID null");
        if (!this.responses.containsKey(message.RequestID))
            throw new IllegalStateException(
                    "no such request ID and response mapping");
    }

    @SuppressWarnings("unchecked")
    private <T> void doRsp(Message message, Class<T> clz) {
        requireMessage(message);
        if (!(message.Body instanceof CThostFtdcRspUserLoginField))
            throw new IllegalArgumentException("wrong message body");
        // Call response.
        var response = (ResponseImpl<T>)this.responses.get(message.RequestID);
        response.put((T)message.Body, message.RspInfo, message.CurrentCount,
                message.TotalCount);
        // Remove mapping if all responses arrive.
        checkCompletion(response, message.RequestID);
    }

    @Override
    public void doRspReqLogin(Message message) {
        doRsp(message, CThostFtdcRspUserLoginField.class);
    }

    @Override
    public void doRspReqOrderInsert(Message message) {
        doRsp(message, CThostFtdcInputOrderField.class);
    }

    @Override
    public void doRspReqOrderAction(Message message) {
        doRsp(message, CThostFtdcInputOrderActionField.class);
    }

    @Override
    public void doRspQryAccount(Message message) {
        doRsp(message, CThostFtdcTradingAccountField.class);
    }

    @Override
    public void doRspQryOrder(Message message) {
        doRsp(message, CThostFtdcOrderField.class);
    }

    @Override
    public void doRspQryPosition(Message message) {
        doRsp(message, CThostFtdcInvestorPositionField.class);
    }

    @Override
    public void doRspSubscribeMarketData(Message message) {
        doRsp(message, CThostFtdcSpecificInstrumentField.class);
    }

    @Override
    public void doRspDepthMarketData(Message message) {
        Objects.requireNonNull(message, "message null");
        Objects.requireNonNull(message.Body, "depth market data body null");
        this.listener.get().onDepthMarketData(
                (CThostFtdcDepthMarketDataField)message.Body);
    }

    @Override
    public void doRspCandle(Message message) {
        Objects.requireNonNull(message, "message null");
        Objects.requireNonNull(message.Body, "candle body null");
        this.listener.get().onCandle((CThostFtdcCandleField)message.Body);
    }
}
