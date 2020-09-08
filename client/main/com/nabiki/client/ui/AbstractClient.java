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

import com.nabiki.client.sdk.ResponseConsumer;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.client.sdk.internal.TradeClientFactoryImpl;
import com.nabiki.objects.*;

import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

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

    private void reqLogin(Trade trade) {
        var lock = new ReentrantLock();
        var condition = lock.newCondition();
        var info = new AtomicReference<CRspInfo>();
        // Send login rsp.
        var login = new CReqUserLogin();
        login.UserID = trade.getUserID();
        login.Password = trade.getPassword();
        var rsp = client.login(
                login,
                UUID.randomUUID().toString());
        rsp.consume((object, rspInfo, currentCount, totalCount) -> {
            info.set(rspInfo);
            lock.lock();
            try {
                condition.signal();
            } finally {
                lock.unlock();
            }
        });
        // Wait login rsp.
        lock.lock();
        try {
            condition.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        // Check login rsp.
        var infoRsp = info.get();
        if (infoRsp == null)
            throw new RuntimeException("null login rsp");
        else if (info.get().ErrorID != ErrorCodes.NONE)
            throw new RuntimeException(
                    "login failure[" + infoRsp.ErrorID + "], " + infoRsp.ErrorMsg);
    }

    private void reqSubscription(Trade trade) {
        var lock = new ReentrantLock();
        var condition = lock.newCondition();
        var info = new AtomicReference<CRspInfo>();
        var in = new AtomicReference<CSpecificInstrument>();
        // Request subscription.
        var reqSub = new CSubMarketData();
        reqSub.InstrumentID = trade.getSubscribe().toArray(new String[0]);
        var rsp = client.subscribeMarketData(
                reqSub,
                UUID.randomUUID().toString());
        rsp.consume(new ResponseConsumer<>() {
            final AtomicInteger recvCount = new AtomicInteger(0);

            @Override
            public void accept(
                    CSpecificInstrument object,
                    CRspInfo rspInfo,
                    int currentCount,
                    int totalCount) {
                recvCount.incrementAndGet();
                if (totalCount == recvCount.get()
                        || (rspInfo != null && rspInfo.ErrorID != ErrorCodes.NONE)) {
                    info.set(rspInfo);
                    in.set(object);
                    lock.lock();
                    try {
                        condition.signal();
                    } finally {
                        lock.unlock();
                    }
                }
            }
        });
        // Wait rsp.
        lock.lock();
        try {
            condition.await(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        // Check rsp.
        var infoRsp = info.get();
        var inRsp = in.get();
        if (infoRsp != null && infoRsp.ErrorID != 0)
            if (inRsp != null)
                throw new RuntimeException(
                        "subscription failure[" + infoRsp.ErrorID + "]: "
                                + inRsp.InstrumentID + ", " + infoRsp.ErrorMsg);
            else
                throw new RuntimeException(
                        "subscription failure[" + infoRsp.ErrorID + "], "
                                + infoRsp.ErrorMsg);
    }

    private void closeConnection() {
        try {
            client.close();
            eventAdaptor.waitClose(TimeUnit.MINUTES.toMillis(1));
        } catch (Throwable th) {
            throw new RuntimeException("fail closing connection" , th);
        }
    }

    protected void startHeadless(HeadlessTrader trader, InetSocketAddress address) {
        setHeadlessListeners(trader);
        openConnection(address);
        // Set trade client into trader.
        trader.setClient(client);
        callStart(trader);
        reqLogin(trader);
        reqSubscription(trader);
    }

    protected void startFigure(FigureTrader trader, InetSocketAddress address) {
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
