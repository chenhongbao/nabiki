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
import com.nabiki.iop.x.SystemStream;
import com.nabiki.objects.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public abstract class AbstractTrader implements Trader {
    private TradeClient client;
    private String userID, password;
    private final Collection<String> instruments = new LinkedList<>();
    protected final Logger logger = Logger.getLogger(this.getClass().getName());

    protected AbstractTrader() {
        prepare();
    }

    private void prepare() {
        try {
            logger.addHandler(new SimpleFileHandler());
            logger.setUseParentHandlers(false);
            // Set default err/out.
            SystemStream.setErr("err.log");
            SystemStream.setOut("out.log");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getRequestID() {
        return UUID.randomUUID().toString();
    }

    private <T> Collection<T> get(Response<T> rsp, int timeout, TimeUnit unit) {
        var r = new LinkedList<T>();
        var lock = new ReentrantLock();
        var cond = lock.newCondition();
        rsp.consume((object, rspInfo, currentCount, totalCount) -> {
            r.add(object);
            if (r.size() == totalCount) {
                lock.lock();
                try {
                    cond.signal();
                } finally {
                    lock.unlock();
                }
            }
        });
        // Wait signal.
        lock.lock();
        try {
            if (!cond.await(timeout, unit))
                throw new RuntimeException("wait rsp timeout");
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }
        return r;
    }

    private COrder queryOrder(String instrumentID, String exchangeID,
                              String orderID) {
        var qry = new CQryOrder();
        qry.ExchangeID = exchangeID;
        qry.InstrumentID = instrumentID;
        qry.OrderSysID = orderID;
        var rsp = get(
                this.client.queryOrder(qry, getRequestID()),
                5,
                TimeUnit.SECONDS);
        if (rsp.size() != 1)
            throw new RuntimeException("invalid rsp size: " + rsp.size());
        return rsp.iterator().next();
    }

    private boolean isOrderDone(COrder order) {
        return order.OrderStatus == OrderStatusType.ALL_TRADED
                || order.OrderStatus == OrderStatusType.CANCELED;
    }

    private char waitTrade(String instrumentID, String exchangeID,
                           String orderID)  {
        COrder rsp = null;
        while (rsp == null || !isOrderDone(rsp)) {
            rsp = queryOrder(instrumentID, exchangeID, orderID);
            try {
                TimeUnit.MILLISECONDS.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        return (char)rsp.OrderStatus;
    }

    @Override
    public void setUser(String userID, String password) {
        this.userID = userID;
        this.password = password;
    }

    @Override
    public String getUserID() {
        return userID;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public void subscribe(String... instruments) {
        this.instruments.addAll(Arrays.asList(instruments));
    }

    @Override
    public Collection<String> getSubscribe() {
        return instruments;
    }

    @Override
    public char orderInsert(String instrumentID, String exchangeID, double price,
                            int volume, char direction, char offset) {
        var req = new CInputOrder();
        req.InstrumentID = instrumentID;
        req.ExchangeID = exchangeID;
        req.LimitPrice = price;
        req.VolumeTotalOriginal = volume;
        req.Direction = (byte) direction;
        req.CombOffsetFlag = (byte) offset;
        // Process rsp.
        var rsp = get(
                this.client.orderInsert(req, getRequestID()),
                5,
                TimeUnit.SECONDS);
        if (rsp.size() != 1)
            throw new RuntimeException("invalid rsp size: " + rsp.size());
        var rspOrder = rsp.iterator().next();
        return waitTrade(instrumentID, exchangeID, rspOrder.OrderLocalID);
    }

    @Override
    public Collection<CInvestorPosition> getPosition() {
        return getPosition("", "");
    }

    @Override
    public Collection<CInvestorPosition> getPosition(
            String instrumentID, String exchangeID) {
        var qry = new CQryInvestorPosition();
        qry.ExchangeID = exchangeID;
        qry.InstrumentID = instrumentID;
        return get(this.client.queryPosition(qry, getRequestID()),
                5, TimeUnit.SECONDS);
    }

    @Override
    public CTradingAccount getAccount()  {
        var qry = new CQryTradingAccount();
        var rsp = get(
                this.client.queryAccount(qry, getRequestID()),
                5,
                TimeUnit.SECONDS);
        if (rsp.size() != 1)
            throw new RuntimeException("invalid rsp size: " + rsp.size());
        return rsp.iterator().next();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public TradeClient getClient() {
        return this.client;
    }

    @Override
    public void setClient(TradeClient client) {
        this.client = client;
    }

    static class SimpleFileHandler extends FileHandler {
        public SimpleFileHandler() throws IOException, SecurityException {
            super("default.log");
            super.setFormatter(new SimpleFormatter());
        }
    }
}
