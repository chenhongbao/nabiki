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

import com.nabiki.client.ui.Client;
import com.nabiki.client.ui.HeadlessTrader;
import com.nabiki.objects.CCandle;
import com.nabiki.objects.CDepthMarketData;
import com.nabiki.objects.CombOffsetFlagType;
import com.nabiki.objects.DirectionType;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

public class PlatformTest {

  private void launch_centre() {
    new Thread(new Runnable() {
      @Override
      public void run() {
        Platform.main(new String[]{
            "--root",
            "C:/Users/chenh/Desktop/.root",
            "--host",
            "localhost",
            "--port",
            "9038",
            "--start-now",
            "true"
        });
      }
    }).start();
  }

  private void sleep(long seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void launch_client() {
    var client = new Client();
    client.start(new HeadlessTrader() {
      @Override
      public String getAlgoName() {
        return "Test platform launch";
      }

      @Override
      public String getAlgoVersion() {
        return "0.0.1";
      }

      @Override
      public String getAlgoDescription() {
        return "Test the launch of platform";
      }

      private Thread daemon;

      @Override
      public void onStart() {
        setUser("0001", "1234");
        daemon = new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              while (true) {
                sleep(30);
                orderInsert(
                    "c2101",
                    "DCE",
                    2450,
                    1,
                    DirectionType.DIRECTION_BUY,
                    CombOffsetFlagType.OFFSET_OPEN);
                getLogger().info("buy open all traded");
                orderInsert(
                    "c2101",
                    "DCE",
                    2350,
                    1,
                    DirectionType.DIRECTION_SELL,
                    CombOffsetFlagType.OFFSET_CLOSE);
                getLogger().info("sell close all traded");
              }
            } catch (Throwable th) {
              getLogger().warning("not traded");
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
    }, new InetSocketAddress("localhost", 9038));
    client.exitAt(LocalDateTime.now().plusDays(1));
  }

  @Test
  public void headless() {
    launch_centre();
    sleep(TimeUnit.MINUTES.toSeconds(1));
    launch_client();
  }
}