/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

package com.nabiki.centre.md;

import com.nabiki.commons.ctpobj.CCandle;
import com.nabiki.commons.ctpobj.CDepthMarketData;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingleCandle {
  private final Duration minuteDuration = Duration.ofMinutes(1);
  private final String instrumentID;
  private final Map<Duration, CandleProgress> progress = new ConcurrentHashMap<>();

  SingleCandle(String instrumentID) {
    this.instrumentID = instrumentID;
  }

  void update(CDepthMarketData md) {
    if (md.InstrumentID.compareTo(instrumentID) != 0)
      throw new IllegalArgumentException("wrong instrument");
    for (var c : progress.values()) {
      c.update(md);
    }
  }

  void register(Duration du) {
    progress.computeIfAbsent(
        du,
        d -> new CandleProgress(instrumentID, (int) d.dividedBy(minuteDuration)));
  }

  CCandle peak(Duration du, String tradingDay) {
    synchronized (progress) {
      if (progress.containsKey(du))
        return progress.get(du).peak(tradingDay);
      else
        throw new IllegalArgumentException("duration not found");
    }
  }

  CCandle pop(Duration du, String tradingDay) {
    synchronized (progress) {
      if (progress.containsKey(du))
        return progress.get(du).pop(tradingDay);
      else
        throw new IllegalArgumentException("duration not found");
    }
  }
}
