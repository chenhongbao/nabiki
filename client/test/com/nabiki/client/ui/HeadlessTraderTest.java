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

import com.nabiki.objects.CCandle;
import com.nabiki.objects.CDepthMarketData;
import com.nabiki.objects.CombOffsetFlagType;
import com.nabiki.objects.DirectionType;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class HeadlessTraderTest {
  private static class MyHeadlessTrader extends HeadlessTrader {
    @Override
    public String getAlgoName() {
      return "Random Buy/Sell";
    }

    @Override
    public String getAlgoVersion() {
      return "0.0.1";
    }

    @Override
    public String getAlgoDescription() {
      return "Test case for headless client";
    }

    private Thread daemon;

    @Override
    public void onStart() {
      setUser("0001", "1234");
      getLogger().info("set user login");

      daemon = new Thread(new Runnable() {
        @Override
        public void run() {
          try {
            TimeUnit.SECONDS.sleep(5);
            orderInsert(
                "c2101",
                "DCE",
                2450,
                1,
                DirectionType.DIRECTION_BUY,
                CombOffsetFlagType.OFFSET_OPEN);
            getLogger().info("buy open");
            orderInsert(
                "c2101",
                "DCE",
                2350,
                1,
                DirectionType.DIRECTION_SELL,
                CombOffsetFlagType.OFFSET_CLOSE);
            getLogger().info("sell close");
            orderInsert(
                "c2101",
                "DCE",
                2350,
                1,
                DirectionType.DIRECTION_SELL,
                CombOffsetFlagType.OFFSET_OPEN);
            getLogger().info("sell open");
            orderInsert(
                "c2101",
                "DCE",
                2450,
                1,
                DirectionType.DIRECTION_BUY,
                CombOffsetFlagType.OFFSET_CLOSE);
            getLogger().info("buy close");
          } catch (Throwable th) {
            getLogger().warning("can't trade");
            th.printStackTrace();
          }
        }
      });
      daemon.start();
    }

    @Override
    public void onDepthMarketData(CDepthMarketData depthMarketData, boolean isTrading) {

    }

    @Override
    public void onCandle(CCandle candle, boolean isTrading) {

    }

    @Override
    public void onStop() {

    }
  }

  @Test
  public void only_trade() {
    var client = new Client();

    System.out.println("start trader");

    client.start(new MyHeadlessTrader(), new InetSocketAddress("106.54.254.193", 9038));

    System.out.println("wait trader exits");
    client.exitAt(LocalDateTime.now().plusMinutes(1));
    System.out.println("only_trade exits");
  }
}
