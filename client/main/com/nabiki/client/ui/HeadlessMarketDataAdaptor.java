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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class HeadlessMarketDataAdaptor implements MarketDataTraderAdaptor {
  protected final MarketDataHandler handler;
  protected final DateTimeFormatter formatter
      = DateTimeFormatter.ofPattern("yyyyMMddHH:mm");
  protected final Map<String, Set<Integer>> subscribes = new ConcurrentHashMap<>();

  HeadlessMarketDataAdaptor(MarketDataHandler h) {
    handler = h;
  }

  private boolean isCandleTrading(String actionDay, String endTime, int minute) {
    var dateTime = LocalDateTime.parse(
        actionDay + endTime,
        formatter);
    var epocDiff = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        - dateTime.toEpochSecond(ZoneOffset.UTC);
    // All real-time trading market data must arrive in minutes, or it
    // is taken as history data.
    return epocDiff < TimeUnit.MINUTES.toSeconds(minute);
  }

  @Override
  public void setSubscribeMinute(String instrument, int... minutes) {
    var set = subscribes.computeIfAbsent(
        instrument, k -> new HashSet<>());
    for (var i : minutes)
      set.add(i);
  }

  @Override
  public void onDepthMarketData(CDepthMarketData depth) {
    if (!subscribes.containsKey(depth.InstrumentID)) {
      return;
    }
    try {
      // All md is trading.
      handler.onDepthMarketData(depth, true);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  @Override
  public void onCandle(CCandle candle) {
    if (candle == null || candle.InstrumentID == null ||
        !subscribes.containsKey(candle.InstrumentID)) {
      return;
    } else {
      var set = subscribes.get(candle.InstrumentID);
      if (set == null || !set.contains(candle.Minute))
        return;
    }
    try {
      handler.onCandle(
          candle,
          isCandleTrading(candle.ActionDay, candle.EndTime, candle.Minute));
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }
}
