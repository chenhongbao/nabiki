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

package com.nabiki.centre;

import com.nabiki.client.MarketDataListener;
import com.nabiki.client.TradeClientFactory;
import com.nabiki.client.TradeClientListener;
import com.nabiki.client.internal.TradeClientFactoryImpl;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.*;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.LocalTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class PlatformTest {
    private void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    class ClientMdListener implements MarketDataListener {
        ClientMdListener() {
        }

        @Override
        public void onDepthMarketData(CDepthMarketData depth) {
            System.out.println(OP.toJson(depth));
        }

        @Override
        public void onCandle(CCandle candle) {
            System.out.println(OP.toJson(candle));
        }
    }

    class ClientListener implements TradeClientListener {
        ClientListener() {
        }

        @Override
        public void onError(Throwable th) {
            th.printStackTrace();
        }

        @Override
        public void onClose() {
            System.out.println("onClose()");
        }

        @Override
        public void onOpen() {
            System.out.println("onOpen()");
        }
    }

    @Test
    public void platformUp() {
        Thread main = new Thread(() -> {
            Platform.main(new String[]{
                    "--root", "C:\\Users\\chenh\\Desktop\\.root",
                    "--host", "localhost",
                    "--port", "9038"});
        });
        main.start();

        sleep((int)TimeUnit.MINUTES.toMillis(2));

        // Client up.
        TradeClientFactory factory = new TradeClientFactoryImpl();
        var client = factory.get();
        client.setListener(new ClientMdListener());
        client.setListener(new ClientListener());

        // Connect to server.
        try {
            client.open(new InetSocketAddress("localhost", 9038));
        } catch (Throwable e) {
            fail(e.getMessage());
        }

        int hour;
        do {
            sleep((int) TimeUnit.SECONDS.toMillis(5));
            hour = LocalTime.now().getHour();
        } while ((15 < hour && hour < 21) || (3 < hour && hour < 9));

        // Test login.
        System.out.println("Test login");

        var login = new CReqUserLogin();
        login.Password = "1234";
        login.UserID = "0001";
        var rsp = client.login(login, UUID.randomUUID().toString());

        rsp.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);
        });

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        // Query account.
        System.out.println("Query account");

        var qryAccount = new CQryTradingAccount();
        qryAccount.CurrencyID = "CNY";
        qryAccount.AccountID = "0001";
        qryAccount.InvestorID = "0001";
        var rsp1 = client.queryAccount(qryAccount, UUID.randomUUID().toString());

        rsp1.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println("Rsp query account");
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);
        });

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        // Query position.
        System.out.println("Query position");

        var qryPosition = new CQryInvestorPosition();
        qryPosition.InstrumentID = "c2101";
        qryPosition.InvestorID = "0001";
        var rsp2 = client.queryPosition(qryPosition, UUID.randomUUID().toString());

        rsp2.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println("Rsp query position");
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);
        });

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        // Insert order.
        System.out.println("Insert order");

        var order = new CInputOrder();
        order.InstrumentID = "c2101";
        order.BrokerID = "9999";
        order.ExchangeID = "DCE";
        order.LimitPrice = 2350;
        order.VolumeTotalOriginal = 1;
        order.CombOffsetFlag = CombOffsetFlagType.OFFSET_OPEN;
        order.Direction = DirectionType.DIRECTION_SELL;

        var rsp5 = client.orderInsert(order, UUID.randomUUID().toString());
        String[] id = new String[1];

        rsp5.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println("Rsp insert order");
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);

            // Save ID.
            id[0] = object.OrderLocalID;
        });

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        // Query order.
        System.out.println("Query order");

        if (id[0] == null)
            System.err.println("no order id");
        else {
            var qryOrder = new CQryOrder();
            qryOrder.OrderSysID = id[0];
            var rsp6 = client.queryOrder(qryOrder, UUID.randomUUID().toString());

            rsp6.consume((object, rspInfo, currentCount, totalCount) -> {
                System.out.println("Rsp query order");
                System.out.println(OP.toJson(object));
                System.out.println(OP.toJson(rspInfo));
                System.out.println(currentCount + "/" + totalCount);
            });
        }

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        System.out.println("Action order");

        if (id[0] == null)
            System.err.println("no order id");
        else {
            // Cancel the order.
            var action = new CInputOrderAction();

            action.UserID = "0001";
            action.BrokerID = "9999";
            action.InstrumentID = "c2101";
            action.OrderSysID = id[0];

            var rsp7 = client.orderAction(action, UUID.randomUUID().toString());
            rsp7.consume((object, rspInfo, currentCount, totalCount) -> {
                System.out.println("Rsp order action");
                System.out.println(OP.toJson(object));
                System.out.println(OP.toJson(rspInfo));
                System.out.println(currentCount + "/" + totalCount);
            });
        }

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        System.out.println("Query canceled order");

        if (id[0] == null)
            System.err.println("no order id");
        else {
            var qryOrder = new CQryOrder();
            qryOrder.OrderSysID = id[0];
            var rsp8 = client.queryOrder(qryOrder, UUID.randomUUID().toString());

            rsp8.consume((object, rspInfo, currentCount, totalCount) -> {
                System.out.println("Rsp query canceled order");
                System.out.println(OP.toJson(object));
                System.out.println(OP.toJson(rspInfo));
                System.out.println(currentCount + "/" + totalCount);
            });
        }

        sleep((int)TimeUnit.SECONDS.toMillis(5));

        // Subscribe md.
        System.out.println("Subscribe md");

        client.setListener(new MarketDataListener() {
            @Override
            public void onDepthMarketData(CDepthMarketData depth) {
                System.out.println("depth update time: " + depth.UpdateTime);
            }

            @Override
            public void onCandle(CCandle candle) {
                System.out.println("candle(" + candle.Minute + ") update time: " + candle.UpdateTime);
            }
        });

        var sub = new CSubMarketData();
        sub.InstrumentID = new String[] {"c2101", "c2105" };
        var rsp3 = client.subscribeMarketData(sub, UUID.randomUUID().toString());

        rsp3.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println("Rsp subscribe");
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);
        });

        // Un-subscribe md.
        /*
        sleep((int)TimeUnit.SECONDS.toMillis(10));

        System.out.println("Un-subscribe md");

        var unsub = new CUnsubMarketData();
        unsub.InstrumentID = new String[] {"c2101", "c2105" };
        var rsp4 = client.unSubscribeMarketData(unsub, UUID.randomUUID().toString());

        rsp4.consume((object, rspInfo, currentCount, totalCount) -> {
            System.out.println("Rsp un-subscribe");
            System.out.println(OP.toJson(object));
            System.out.println(OP.toJson(rspInfo));
            System.out.println(currentCount + "/" + totalCount);
        });
        */

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
    }
}