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

import com.nabiki.client.sdk.ClientUtils;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.client.sdk.internal.TradeClientFactoryImpl;
import com.nabiki.objects.CReqUserLogin;
import com.nabiki.objects.CSubMarketData;
import com.nabiki.objects.ErrorCodes;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class AbstractClient {
    private final TradeClient client;
    private final ClientEventAdaptor eventAdaptor = new ClientEventAdaptor();

    AbstractClient() {
        client = new TradeClientFactoryImpl().get();
    }

    private void setHeadlessListeners(MarketDataHandler handler) {
        client.setListener(new HeadlessMarketDataAdaptor(handler));
        client.setListener(eventAdaptor);
    }

    private void setFigureListeners(FigureTrader trader) {
        client.setListener(new FigureMarketDataAdaptor(trader, trader));
        client.setListener(eventAdaptor);
    }

    private void openConnection(InetSocketAddress address) {
        // Open connection to server.
        try {
            client.open(address);
            eventAdaptor.waitOpen(TimeUnit.MINUTES.toMillis(1));
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

    private void reqLogin(Trader trader) throws Exception {
        // Send login rsp.
        var login = new CReqUserLogin();
        login.UserID = trader.getUserID();
        login.Password = trader.getPassword();
        var rsp = ClientUtils.get(
                client.login(login, UUID.randomUUID().toString()),
                15,
                TimeUnit.SECONDS);
        // Check login rsp.
        if (rsp.size() == 0)
            throw new RuntimeException("no login rsp");
        else {
            var rspInfo = rsp.values().iterator().next();
            if (rspInfo.ErrorID != ErrorCodes.NONE)
                throw new RuntimeException(
                        "login failure[" + rspInfo.ErrorID + "], " + rspInfo.ErrorMsg);
        }
    }

    private String getCommaList(List<String> values) {
        if (values == null || values.size() == 0)
            return "";
        if (values.size() == 1) {
            return values.iterator().next();
        } else {
            String r = values.get(0);
            for (int i = 1; i < values.size(); ++i)
                r += "," + values.get(i);
            return r;
        }
    }

    private void reqSubscription(Trader trader) throws Exception {
        // Request subscription.
        var reqSub = new CSubMarketData();
        reqSub.InstrumentID = trader.getSubscribe().toArray(new String[0]);
        var rsp = ClientUtils.get(
                client.subscribeMarketData(reqSub, UUID.randomUUID().toString()),
                15,
                TimeUnit.SECONDS);
        // Check rsp.
        if (rsp.size() == 0)
            throw new RuntimeException("no sub md rsp");
        else {
            var fail = new LinkedList<String>();
            for (var instr : rsp.keySet()) {
                var rspInfo = rsp.get(instr);
                if (rspInfo == null  || rspInfo.ErrorID != ErrorCodes.NONE)
                    fail.add(instr.InstrumentID);
            }
            if (fail.size() > 0)
                throw new RuntimeException("can't subscribe md: " + getCommaList(fail));
        }
    }

    private void closeConnection() {
        try {
            client.close();
            eventAdaptor.waitClose(TimeUnit.MINUTES.toMillis(1));
        } catch (Throwable th) {
            throw new RuntimeException("fail closing connection" , th);
        }
    }

    protected void startHeadless(HeadlessTrader trader, InetSocketAddress address) throws Exception {
        setHeadlessListeners(trader);
        openConnection(address);
        // Set trade client into trader.
        trader.setClient(client);
        callStart(trader);
        reqLogin(trader);
        reqSubscription(trader);
    }

    protected void startFigure(FigureTrader trader, InetSocketAddress address) throws Exception {
        setFigureListeners(trader);
        openConnection(address);
        // Set trade client into trader.
        trader.setClient(client);
        callStart(trader);
        reqLogin(trader);
        reqSubscription(trader);
    }

    protected void stop() {
        closeConnection();
    }
}
