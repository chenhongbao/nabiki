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
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.ClientMessageAdaptor;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class TradeClientAdaptor extends ClientMessageAdaptor {
  private class DefaultDepthListener implements MarketDataListener {
    @Override
    public void onDepthMarketData(CDepthMarketData depth) {
    }

    @Override
    public void onCandle(CCandle candle) {
    }
  }

  private final Map<String, ResponseImpl<?>> responses = new ConcurrentHashMap<>();
  private final AtomicReference<MarketDataListener> listener
      = new AtomicReference<>(new DefaultDepthListener());

  private String tradingDay;

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
      CRspInfo info,
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

  public String getTradingDay() {
    return this.tradingDay;
  }

  @Override
  public void doRspReqLogin(
      CRspUserLogin rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
    tradingDay = rsp.TradingDay;
  }

  @Override
  public void doRspReqOrderInsert(
      COrder rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspReqOrderAction(
      COrderAction rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryDepthMarketData(
      CDepthMarketData rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryAccount(
      CTradingAccount rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryOrder(
      COrder rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryPosition(
      CInvestorPosition rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryPositionDetail(
      CInvestorPositionDetail rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryInstrument(
      CInstrument rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryCommission(
      CInstrumentCommissionRate rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspQryMargin(
      CInstrumentMarginRate rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspSubscribeMarketData(
      CSpecificInstrument rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    doRsp(rsp, info, requestID, current, total);
  }

  @Override
  public void doRspDepthMarketData(
      CDepthMarketData rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    this.listener.get().onDepthMarketData(rsp);
  }

  @Override
  public void doRspCandle(
      CCandle rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
    this.listener.get().onCandle(rsp);
  }
}
