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

import java.awt.*;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FigureTraderTest {

  final static Path path = Path.of("candles/c2101_1.csv");

  CCandle parse(String line) {
    String[] vars = line.split(",");
    if (vars.length != 13)
      return null;
    CCandle candle = new CCandle();
    candle.InstrumentID = vars[0];
    candle.OpenPrice = Double.parseDouble(vars[1]);
    candle.HighestPrice = Double.parseDouble(vars[2]);
    candle.LowestPrice = Double.parseDouble(vars[3]);
    candle.ClosePrice = Double.parseDouble(vars[4]);
    candle.AveragePrice = Double.parseDouble(vars[5]);
    candle.OpenInterest = Double.parseDouble(vars[6]);
    candle.Volume = Integer.parseInt(vars[7]);
    candle.Minute = Integer.parseInt(vars[8]);
    candle.TradingDay = vars[9];
    candle.ActionDay = vars[10];
    candle.UpdateTime = vars[11];
    candle.EndTime = vars[12];
    return candle;
  }

  List<CCandle> loadFakeCandles(Path path) {
    var r = new LinkedList<CCandle>();
    try {
      var br = new BufferedReader(new InputStreamReader(new FileInputStream(path.toFile())));
      br.readLine();
      String line;
      while ((line = br.readLine()) != null) {
        r.add(parse(line));
      }
    } catch (Throwable th) {
      th.printStackTrace();
    }
    return r;
  }

  void sleep(int seconds) {
    try {
      TimeUnit.SECONDS.sleep(seconds);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  @Test
  public void basic() {
    var trader = new MyFigureTrader();
    var client = new Client();
    ///////////////////////////////////////////////////////////////////////////
    // Uncomment the following codes if you want to test client with real data.
    new Thread(() -> {
      sleep(5);
      for (var candle : loadFakeCandles(path)) {
        trader.getDefaultAdaptor().onCandle(candle);
      }
    }).start();
    ///////////////////////////////////////////////////////////////////////////
    client.start(trader, new InetSocketAddress("106.54.254.193", 9038));
    client.exitAt(LocalDateTime.now().plusDays(1));
  }

  private static class MyFigureTrader extends FigureTrader {
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
      return "Test case for UI client";
    }

    CDepthMarketData depth;
    int currentPosition = 0;

    private final List<Double> close01 = new LinkedList<>(), vol01 = new LinkedList<>();

    private Double average(int m, List<Double> close) {
      var rm = Math.min(m, close.size());
      if (rm < 1)
        return null;
      else {
        var total = 0.0D;
        var tail = close.size() - rm;
        for (int idx = close.size() - 1; tail <= idx; --idx)
          total += close.get(idx);
        return total / rm;
      }
    }

    private void open(CDepthMarketData depth) throws Exception {
      if (Math.random() > 0.5) {
        var r = orderInsert(
            "c2101",
            "DCE",
            depth.HighestPrice,
            1,
            DirectionType.DIRECTION_BUY,
            CombOffsetFlagType.OFFSET_OPEN);
        currentPosition += 1;
        getLogger().info("buy open");
      } else {
        var r = orderInsert(
            "c2101",
            "DCE",
            depth.LowestPrice,
            1,
            DirectionType.DIRECTION_SELL,
            CombOffsetFlagType.OFFSET_OPEN);
        currentPosition -= 1;
        getLogger().info("sell open");
      }
    }

    private void close(CDepthMarketData depth) throws Exception {
      if (currentPosition > 0) {
        var r = orderInsert(
            "c2101",
            "DCE",
            depth.LowerLimitPrice,
            currentPosition,
            DirectionType.DIRECTION_SELL,
            CombOffsetFlagType.OFFSET_CLOSE);
        currentPosition = 0;
        getLogger().info("sell close");
      } else {
        var r = orderInsert(
            "c2101",
            "DCE",
            depth.UpperLimitPrice,
            -currentPosition,
            DirectionType.DIRECTION_BUY,
            CombOffsetFlagType.OFFSET_CLOSE);
        currentPosition = 0;
        getLogger().info("buy close");
      }
    }

    @Override
    public void onStart() {
      subscribe("c2101", 1);
      setStickFigure(1, "c2101", 1);
      setLine(1, "close", Color.MAGENTA);

      setBarFigure(2, "c2101", 1);
      setLine(2, "volume", Color.MAGENTA);

      setUser("0001", "1234");
    }

    @Override
    public void onDepthMarketData(CDepthMarketData depthMarketData, boolean isTrading) {
      if (!isTrading || depthMarketData.InstrumentID.compareTo("c2101") != 0)
        return;
      depth = depthMarketData;
    }

    @Override
    public void onCandle(CCandle candle, boolean isTrading) {
      if (candle.Minute != 1)
        getLogger().warning("i didn't subscribe this minute: " + candle.Minute);

      if (candle.Minute == 1) {
        close01.add(candle.ClosePrice);
        vol01.add((double) candle.Volume);
        draw(1, "close", average(20, close01));
        bar(2, candle.Volume, candle.EndTime);
        draw(2, "volume", (double) candle.Volume);
      }
      if (candle.Minute == 1 && depth != null && isTrading) {
        try {
          if (currentPosition == 0)
            open(depth);
          else
            close(depth);
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }
    }

    @Override
    public void onStop() {
      System.out.println("----------- END ----------");
    }
  }


}