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

package com.nabiki.client.sdk;

import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.IOPClient;

import java.io.IOException;
import java.net.InetSocketAddress;

public interface TradeClient {
  void setListener(TradeClientListener clientListener);

  void setListener(MarketDataListener listener);

  void open(InetSocketAddress address) throws IOException;

  void close();

  boolean isClosed();

  String getTradingDay();

  long getLag();

  IOPClient getIOP();

  Response<CRspUserLogin> login(CReqUserLogin request) throws Exception;

  Response<COrder> orderInsert(CInputOrder order) throws Exception;

  Response<COrderAction> orderAction(CInputOrderAction action) throws Exception;

  Response<CDepthMarketData> queryDepthMarketData(CQryDepthMarketData query) throws Exception;

  Response<CInvestorPosition> queryPosition(CQryInvestorPosition query) throws Exception;

  Response<CInvestorPositionDetail> queryPositionDetail(CQryInvestorPositionDetail query) throws Exception;

  Response<CTradingAccount> queryAccount(CQryTradingAccount query) throws Exception;

  Response<COrder> queryOrder(CQryOrder query) throws Exception;

  Response<CSpecificInstrument> subscribeMarketData(CSubMarketData subscription) throws Exception;

  Response<CSpecificInstrument> unSubscribeMarketData(CUnsubMarketData subscription) throws Exception;

  Response<CInstrument> queryInstrument(CQryInstrument query) throws Exception;

  Response<CInstrumentMarginRate> queryMargin(CQryInstrumentMarginRate query) throws Exception;

  Response<CInstrumentCommissionRate> queryCommission(CQryInstrumentCommissionRate query) throws Exception;
}
