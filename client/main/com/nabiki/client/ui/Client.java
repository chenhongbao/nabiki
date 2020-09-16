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

import com.nabiki.objects.CSpecificInstrument;
import com.nabiki.objects.ErrorCodes;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Client extends AbstractClient {
    private Trader trader;
    private MarketDataHandler handler;

    private void checkLogin() {
        if (!loginRsp.hasResponse())
            throw new RuntimeException("login timeout");
        var info = loginRsp.getRspInfo(loginRsp.poll());
        if (info == null || info.ErrorID != ErrorCodes.NONE)
            throw new RuntimeException("login failed");
    }

    private void checkSubMd() {
        if (!subRsp.hasResponse())
            throw new RuntimeException("sub md timeout");
        if (subRsp.getTotalCount() != subRsp.getArrivalCount())
            throw new RuntimeException("sub msd rsp uncompleted");
        CSpecificInstrument in = null;
        while ((in = subRsp.poll()) != null) {
            var info = subRsp.getRspInfo(in);
            if (info == null || info.ErrorID != ErrorCodes.NONE)
                throw new RuntimeException("sub md " + in.InstrumentID + " failed");
        }
    }

    private void checkAll() throws InterruptedException {
        TimeUnit.SECONDS.sleep(5);
        checkLogin();
        checkSubMd();
    }

    public void start(HeadlessTrader trader, InetSocketAddress serverAddress) {
        this.trader = trader;
        this.handler = trader;
        try {
            super.startHeadless(trader, serverAddress);
            checkAll();
        } catch (Throwable th) {
            th.printStackTrace();
            trader.getLogger().severe(th.getMessage());
        }
    }

    public void start(FigureTrader trader, InetSocketAddress serverAddress) {
        this.trader = trader;
        this.handler = trader;
        try {
            super.startFigure(trader, serverAddress);
            checkAll();
        } catch (Throwable th) {
            th.printStackTrace();
            trader.getLogger().severe(th.getMessage());
        }
    }

    public void noExit() {
        while (true) {
            try {
                new CountDownLatch(1).await();
            } catch (InterruptedException e) {
                trader.getLogger().warning(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void exitAt(LocalDateTime dateTime) {
        if (dateTime == null) {
            trader.getLogger().severe(
                    "client exits immediately because exit time has passed");
            return;
        }
       while (LocalDateTime.now().isBefore(dateTime)) {
           try {
               TimeUnit.SECONDS.sleep(1);
           } catch (InterruptedException ignored) {
           }
       }
       // Stop.
       try {
           handler.onStop();
       } catch (Throwable th) {
           th.printStackTrace();
           trader.getLogger().warning(th.getMessage());
       } finally {
           super.stop();
           trader.stop();
       }
    }
}
