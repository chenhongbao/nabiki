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

package com.nabiki.client.ui;

import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.commons.ctpobj.CInvestorPosition;
import com.nabiki.commons.ctpobj.COrder;
import com.nabiki.commons.ctpobj.CTradingAccount;

import java.util.Collection;
import java.util.logging.Logger;

public interface Trader {
  void setUser(String userID, String password);

  String getUserID();

  String getPassword();

  String getAlgoName();

  String getAlgoVersion();

  String getAlgoDescription();

  void subscribe(String instrument, int... minutes);

  Collection<String> getSubscribe();

  Response<COrder> orderInsert(
      String instrumentID, String exchangeID, double price, int volume,
      char direction, char offset) throws Exception;

  Response<CInvestorPosition> getPosition() throws Exception;

  Response<CInvestorPosition> getPosition(
      String instrumentID, String exchangeID) throws Exception;

  Response<CTradingAccount> getAccount() throws Exception;

  Logger getLogger();

  void setLoggingServer(String host, int port);

  TradeClient getClient();

  void setClient(TradeClient client);

  void stop();
}