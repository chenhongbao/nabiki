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

package com.nabiki.client.ui;

import com.nabiki.client.portal.Constants;
import com.nabiki.client.sdk.MarketDataListener;
import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.client.sdk.internal.TradeClientFactoryImpl;
import com.nabiki.commons.ctpobj.CReqUserLogin;
import com.nabiki.commons.ctpobj.CRspUserLogin;
import com.nabiki.commons.ctpobj.CSpecificInstrument;
import com.nabiki.commons.ctpobj.CSubMarketData;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClient {
  private final TradeClient client;
  private final ClientEventAdaptor eventAdaptor = new ClientEventAdaptor();

  protected Response<CRspUserLogin> loginRsp;
  protected Response<CSpecificInstrument> subRsp;

  public final static String USER_PRODUCT_INFO = "TRADER";

  AbstractClient() {
    client = new TradeClientFactoryImpl().get();
  }

  private void setListener(MarketDataListener listener) {
    client.setListener(listener);
    client.setListener(eventAdaptor);
  }

  private void setInOutLogger() {
    client.getIOP().setMessageHandlerIn(new InputFromServerLogger());
    client.getIOP().setMessageHandlerOut(new OutputToServerLogger());
  }

  private void openConnection(InetSocketAddress address) {
    // Open connection to server.
    try {
      client.open(address);
      eventAdaptor.waitOpen(
          TimeUnit.SECONDS.toMillis(Constants.GLOBAL_WAIT_SECONDS));
    } catch (Throwable th) {
      throw new RuntimeException("fail opening connection to " + address, th);
    }
  }

  private void callStart(MarketDataHandler handler) {
    try {
      handler.onStart();
    } catch (Throwable th) {
      throw new RuntimeException("uncaught error: " + th.getMessage(), th);
    }
  }

  private void reqLogin(String userID, String password) throws Exception {
    // Send login rsp.
    var login = new CReqUserLogin();
    login.UserID = userID;
    login.Password = password;
    login.UserProductInfo = USER_PRODUCT_INFO;
    loginRsp = client.login(login);
  }

  private void reqSubscription(Trader trader) throws Exception {
    // Request subscription.
    var reqSub = new CSubMarketData();
    reqSub.InstrumentID = trader.getSubscribe().toArray(new String[0]);
    subRsp = client.subscribeMarketData(reqSub);
    // Query instrument information.
    for (var i : reqSub.InstrumentID) {
      trader.getMargin(i);
      trader.getCommission(i);
      trader.getInstrument(i);
    }
  }

  private void closeConnection() {
    try {
      client.close();
      eventAdaptor.waitClose(TimeUnit.MINUTES.toMillis(1));
    } catch (Throwable th) {
      throw new RuntimeException("fail closing connection", th);
    }
  }

  protected void init(Trader trader,
                      MarketDataHandler handler,
                      String userID,
                      String password,
                      InetSocketAddress address) throws Exception {
    setInOutLogger();
    openConnection(address);
    trader.setClient(client);
    callStart(handler);
    reqLogin(userID, password);
    reqSubscription(trader);
  }

  protected void initTrader(HeadlessTrader trader,
                            String userID,
                            String password,
                            InetSocketAddress etrade) throws Exception {
    setListener(trader.getDefaultAdaptor());
    init(trader, trader, userID, password, etrade);
  }

  protected void initTrader(FigureTrader trader,
                            String userID,
                            String password,
                            InetSocketAddress etrade) throws Exception {
    setListener(trader.getDefaultAdaptor());
    init(trader, trader, userID, password, etrade);
  }

  protected void stop() {
    closeConnection();
  }
}
