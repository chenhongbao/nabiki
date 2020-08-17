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

import java.util.Map;
import java.util.Objects;
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

    private final Map<String, ResponseImpl<?>> responses = new ConcurrentHashMap<>();
    private final AtomicReference<MarketDataListener> listener
            = new AtomicReference<>(new DefaultDepthListener());

    TradeClientAdaptor() {
    }

    void setResponse(ResponseImpl<?> response, String requestID) {
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

    private void checkCompletion(ResponseImpl<?> response, String requestID) {
        // Check the completion of response.
        if (response.getArrivalCount() == response.getTotalCount()
                && response.getTotalCount() != 0)
            this.responses.remove(requestID);
    }

    @SuppressWarnings("unchecked")
    private <T> void doRsp(
            T object,
            CThostFtdcRspInfoField info,
            String requestID,
            int current,
            int total) {
        if (!this.responses.containsKey(requestID))
            throw new IllegalStateException(
                    "no such request ID and response mapping");
        // Call response.
        var response = (ResponseImpl<T>) this.responses.get(requestID);
        response.put(object, info, current, total);
        // Remove mapping if all responses arrive.
        checkCompletion(response, requestID);
    }

    @Override
    public void doRspReqLogin(
            CThostFtdcRspUserLoginField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspReqOrderInsert(
            CThostFtdcOrderField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspReqOrderAction(
            CThostFtdcOrderActionField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspQryAccount(
            CThostFtdcTradingAccountField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspQryOrder(
            CThostFtdcOrderField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspQryPosition(
            CThostFtdcInvestorPositionField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspSubscribeMarketData(
            CThostFtdcSpecificInstrumentField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        doRsp(rsp, info, requestID, current, total);
    }

    @Override
    public void doRspDepthMarketData(
            CThostFtdcDepthMarketDataField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        this.listener.get().onDepthMarketData(rsp);
    }

    @Override
    public void doRspCandle(
            CThostFtdcCandleField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
        this.listener.get().onCandle(rsp);
    }
}
