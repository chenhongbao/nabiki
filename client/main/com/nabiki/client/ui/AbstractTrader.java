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
    public Response<COrder> orderInsert(
            String instrumentID, String exchangeID, double price, int volume,
            char direction, char offset) throws Exception {
        var req = new CInputOrder();
        req.InstrumentID = instrumentID;
        req.ExchangeID = exchangeID;
        req.LimitPrice = price;
        req.VolumeTotalOriginal = volume;
        req.Direction = (byte) direction;
        req.CombOffsetFlag = (byte) offset;
        return this.client.orderInsert(req, getRequestID());
    }

    @Override
    public Response<CInvestorPosition> getPosition() throws Exception {
        return getPosition("", "");
    }

    @Override
    public Response<CInvestorPosition> getPosition(
            String instrumentID, String exchangeID) throws Exception {
        var qry = new CQryInvestorPosition();
        qry.ExchangeID = exchangeID;
        qry.InstrumentID = instrumentID;
        return this.client.queryPosition(qry, getRequestID());
    }

    @Override
    public Response<CTradingAccount> getAccount() throws Exception {
        return this.client.queryAccount(new CQryTradingAccount(), getRequestID());
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
            super("default.log", true);
            super.setFormatter(new SimpleFormatter());
        }
    }
}
