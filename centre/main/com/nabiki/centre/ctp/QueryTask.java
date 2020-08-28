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

package com.nabiki.centre.ctp;

import com.nabiki.centre.utils.Signal;
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.CQryInstrumentCommissionRate;
import com.nabiki.objects.CQryInstrumentMarginRate;
import com.nabiki.objects.CombHedgeFlagType;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class QueryTask implements Runnable {
    private final OrderProvider orderProvider;
    protected final Random rand = new Random();

    // Wait last request return.
    protected final Signal lastRtn = new Signal();
    protected final AtomicInteger lastID = new AtomicInteger(0);

    QueryTask(OrderProvider orderProvider) {
        this.orderProvider = orderProvider;
    }

    void signalRequest(int requestID) {
        if (this.lastID.get() == requestID)
            this.lastRtn.signal();
    }

    private boolean waitRequestRsp(
            long millis, int requestID) throws InterruptedException {
        this.lastID.set(requestID);
        return this.lastRtn.waitSignal(millis);
    }


    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            if (orderProvider.qryInstrLast && orderProvider.isConfirmed) {
                try {
                    doQuery();
                } catch (Throwable th) {
                    th.printStackTrace();
                    orderProvider.global.getLogger().warning(th.getMessage());
                }
            }
            // Sleep 1 second between queries.
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    protected void doQuery() {
        String ins = randomGet();
        int reqID;
        var in = orderProvider.global.getInstrInfo(ins);
        // Query margin.
        if (in.Margin == null) {
            var req = new CQryInstrumentMarginRate();
            req.BrokerID = orderProvider.loginCfg.BrokerID;
            req.InvestorID = orderProvider.loginCfg.UserID;
            req.HedgeFlag = CombHedgeFlagType.SPECULATION;
            req.InstrumentID = ins;
            reqID = Utils.getIncrementID();
            int r = orderProvider.api.ReqQryInstrumentMarginRate(
                    JNI.toJni(req),
                    reqID);
            if (r != 0) {
                orderProvider.global.getLogger().warning(
                        Utils.formatLog("failed query margin",
                                null, ins, r));
            } else {
                // Sleep up tp some seconds.
                try {
                    if (!waitRequestRsp(orderProvider.qryWaitMillis, reqID))
                        orderProvider.global.getLogger().warning("query margin timeout");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        // Sleep 1 second between queries.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // Query commission.
        if (in.Commission == null) {
            var req0 = new CQryInstrumentCommissionRate();
            req0.BrokerID = orderProvider.loginCfg.BrokerID;
            req0.InvestorID = orderProvider.loginCfg.UserID;
            req0.InstrumentID = ins;
            reqID = Utils.getIncrementID();
            var r = orderProvider.api.ReqQryInstrumentCommissionRate(
                    JNI.toJni(req0),
                    reqID);
            if (r != 0) {
                orderProvider.global.getLogger().warning(
                        Utils.formatLog("failed query commission",
                                null, ins, r));
            } else {
                // Sleep up tp some seconds.
                try {
                    if (!waitRequestRsp(orderProvider.qryWaitMillis, reqID))
                        orderProvider.global.getLogger().warning("query margin timeout");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    protected String randomGet() {
        synchronized (orderProvider.instruments) {
            return orderProvider.instruments.get(
                    Math.abs(rand.nextInt()) % orderProvider.instruments.size());
        }
    }
}
