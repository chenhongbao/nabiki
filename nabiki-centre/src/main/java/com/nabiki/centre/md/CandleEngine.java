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

import com.nabiki.centre.config.Global;
import com.nabiki.commons.ctpobj.CCandle;
import com.nabiki.commons.ctpobj.CDepthMarketData;
import com.nabiki.commons.utils.Utils;

import java.time.Duration;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CandleEngine extends TimerTask {
  private final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

  private final Global global;
  private final Map<String, Product> products = new ConcurrentHashMap<>();
  private final Map<String, Product> instrProducts = new ConcurrentHashMap<>();
  private final MarketDataRouter router;

  private final AtomicBoolean working = new AtomicBoolean(false);
  private final AtomicBoolean recvTick = new AtomicBoolean(false);

  public CandleEngine(MarketDataRouter router, Global cfg) {
    this.global = cfg;
    this.router = router;
    prepare();
  }

  private void prepare() {
    Utils.schedule(this, MILLIS);
  }

  // Time point is stored as key of a hash map.
  // Avoid missing hash key because of fractional nanoseconds.
  private LocalTime getRoundTime(LocalTime now, int seconds) {
    var nearSec = (int) Math.round(now.toSecondOfDay() / (double) seconds) * seconds;
    return LocalTime.ofSecondOfDay(nearSec);
  }

  public void setWorking(boolean working) {
    global.getLogger().info("candle engine working: " + working);
    this.working.set(working);
    if (!working) {
      recvTick.set(false);
    }
  }

  /**
   * Create facilities before market is open. This can save much time of
   * construction a large set of objects.
   *
   * @param instrumentID instrument ID
   */
  public void addInstrument(String instrumentID) {
    acquireMapProduct(instrumentID).acquireInstrument(instrumentID);
  }

  public void clearProducts() {
    instrProducts.clear();
    products.clear();
  }

  public void setupDurations() {
    for (var p : this.products.values()) {
      for (var du : this.global.getDurations()) {
        p.registerDuration(du);
      }
    }
  }

  private Product acquireMapProduct(String instrumentID) {
    if (instrumentID == null || instrumentID.length() == 0) {
      throw new IllegalArgumentException("illegal instrument ID");
    }
    return instrProducts.computeIfAbsent(
        instrumentID,
        i -> products.computeIfAbsent(Utils.getProductID(i), p -> new Product()));
  }

  private void setTickRecv() {
    if (!recvTick.get()) {
      recvTick.set(true);
    }
  }

  public void update(CDepthMarketData md) {
    acquireMapProduct(md.InstrumentID).update(md);
    setTickRecv();
  }

  private boolean checkNowOK(LocalTime now) {
    var s = now.getSecond();
    return s == 0 || s == 59;
  }

  @Override
  public void run() {
    // Not working or no tick recv, don't generate candles.
    // Fix date: 2021-02-14
    // In some cases, the broker front is up in holiday, but the market is not open.
    // So we need to confirm here the market is open and trading.
    if (!this.working.get() || !this.recvTick.get()) {
      return;
    }
    var now = LocalTime.now();
    // Check now time stamp is precisely at the point of one minute.
    if (!checkNowOK(now)) {
      global.getLogger().warning("timer not precise: " + now.toString());
    }
    // Working now.
    now = getRoundTime(now, (int) TimeUnit.MILLISECONDS.toSeconds(MILLIS));
    var hours = this.global.getAllTradingHour();
    // Measure performance.
    var max = global.getPerformance().start("candle.run.max");
    var cur = global.getPerformance().start("candle.run.cur");
    // Generate candles.
    for (var e : products.entrySet()) {
      var h = hours.get(e.getKey());
      if (h == null) {
        this.global.getLogger().warning(
            Utils.formatLog("trading hour global null", e.getKey(),
                null, null));
        continue;
      }
      for (var du : global.getDurations()) {
        if (h.contains(du, now))
          try {
            router.route(e.getValue().pop(du));
          } catch (Throwable th) {
            th.printStackTrace();
            global.getLogger().severe(th.getMessage());
          }
      }
    }
    // End measurement.
    max.endWithMax();
    cur.end();
  }

  class Product {
    private final Map<String, SingleCandle> candles = new ConcurrentHashMap<>();

    Product() {
    }

    public SingleCandle acquireInstrument(String instrID) {
      return candles.computeIfAbsent(instrID, SingleCandle::new);
    }

    public void removeInstrument(String instrID) {
      this.candles.remove(instrID);
    }

    public void registerDuration(Duration du) {
      if (du == null) {
        throw new NullPointerException("duration null");
      }
      synchronized (this.candles) {
        for (var c : this.candles.values()) {
          c.register(du);
        }
      }
    }

    public Set<CCandle> peak(Duration du) {
      if (du == null) {
        throw new NullPointerException("duration null");
      }
      var r = new HashSet<CCandle>();
      synchronized (this.candles) {
        for (var c : this.candles.values()) {
          r.add(c.peak(du, global.getTradingDay()));
        }
      }
      return r;
    }

    public Set<CCandle> pop(Duration du) {
      if (du == null) {
        throw new IllegalArgumentException("duration null");
      }
      var r = new HashSet<CCandle>();
      synchronized (this.candles) {
        for (var c : this.candles.values()) {
          var candle = c.pop(du, global.getTradingDay());
          r.add(candle);
        }
      }
      return r;
    }

    public void update(CDepthMarketData md) {
      acquireInstrument(md.InstrumentID).update(md);
    }
  }
}
