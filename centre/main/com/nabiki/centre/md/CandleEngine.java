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

package com.nabiki.centre.md;

import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.struct.CThostFtdcCandleField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class CandleEngine extends TimerTask {
    private final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

    private final Config config;
    private final Timer timer = new Timer();
    private final Map<String, Product> products = new ConcurrentHashMap<>();
    private final Set<MarketDataRouter> routers = new HashSet<>();

    private final AtomicBoolean working = new AtomicBoolean(false);

    public CandleEngine(Config cfg) {
        this.config = cfg;
        prepare();
    }

    private void prepare() {
        // Schedule.
        this.timer.scheduleAtFixedRate(this,
                MILLIS - System.currentTimeMillis() % MILLIS, MILLIS);
    }

    // Time point is stored as key of a hash map.
    // Avoid missing hash key because of fractional nanoseconds.
    private LocalTime getRoundTime(LocalTime now, int seconds) {
        var nearSec = (int)Math.round(now.toSecondOfDay() / (double)seconds) * seconds;
        return LocalTime.ofSecondOfDay(nearSec);
    }

    public void setWorking(boolean working) {
        this.working.set(working);
    }

    /**
     * Create facilities before market is open. This can save much time of
     * construction a large set of objects.
     *
     * @param instrID instrument ID
     */
    public void addInstrument(String instrID) {
        if (instrID == null || instrID.length() == 0)
            throw new IllegalArgumentException("illegal instr ID");
        ensureProduct(Utils.getProductID(instrID)).registerInstr(instrID);
    }

    public void removeInstrument(String instrID) {
        if (instrID == null || instrID.length() == 0)
            throw new IllegalArgumentException("illegal instr ID");
        ensureProduct(Utils.getProductID(instrID)).unregisterInstr(instrID);
    }

    public void setupDurations() {
        for (var p : this.products.values())
            for (var du : this.config.getDurations())
                p.registerDuration(du);
    }

    private Product ensureProduct(String product) {
        Product p;
        synchronized (this.products) {
            if (this.products.containsKey(product))
                return this.products.get(product);
            else {
                this.products.put(product, new Product());
                p = this.products.get(product);
            }
        }
        return p;
    }

    public void registerRouter(MarketDataRouter router) {
        if (router != null)
            synchronized (this.routers) {
                this.routers.add(router);
            }
    }

    public void update(CThostFtdcDepthMarketDataField md) {
        ensureProduct(Utils.getProductID(md.InstrumentID)).update(md);
    }

    @Override
    public void run() {
        // Not working, don't generate candles.
        if (!this.working.get())
            return;
        // Working now.
        var now = getRoundTime(
                LocalTime.now(),
                (int)TimeUnit.MILLISECONDS.toSeconds(MILLIS));
        var hours = this.config.getAllTradingHour();
        for (var e : this.products.entrySet()) {
            var h = hours.get(e.getKey());
            if (h == null) {
                this.config.getLogger().warning(
                        Utils.formatLog("trading hour config null", e.getKey(),
                                null, null));
                continue;
            }
            for (var du : this.config.getDurations()) {
                if (h.contains(du, now))
                    for (var r : this.routers) {
                        try {
                            r.route(e.getValue().pop(du));
                        } catch (Throwable th) {
                            th.printStackTrace();
                            config.getLogger().severe(th.getMessage());
                        }
                    }
            }
        }
    }

    class Product {
        private final Map<String, SingleCandle> candles = new HashMap<>();

        Product() {
        }

        public void registerInstr(String instrID) {
            if (!this.candles.containsKey(instrID))
                this.candles.put(instrID, new SingleCandle(instrID));
        }

        public void unregisterInstr(String instrID) {
            this.candles.remove(instrID);
        }

        public void registerDuration(Duration du) {
            if (du == null)
                throw new NullPointerException("duration null");
            synchronized (this.candles) {
                for (var c : this.candles.values())
                    c.register(du);
            }
        }

        public Set<CThostFtdcCandleField> peak(Duration du) {
            if (du == null)
                throw new NullPointerException("duration null");
            var r = new HashSet<CThostFtdcCandleField>();
            synchronized (this.candles) {
                for (var c : this.candles.values())
                    r.add(c.peak(du, config.getTradingDay()));
            }
            return r;
        }

        public Set<CThostFtdcCandleField> pop(Duration du) {
            if (du == null)
                throw new IllegalArgumentException("duration null");
            var r = new HashSet<CThostFtdcCandleField>();
            synchronized (this.candles) {
                for (var c : this.candles.values())
                    r.add(c.pop(du, config.getTradingDay()));
            }
            return r;
        }

        public void update(CThostFtdcDepthMarketDataField md) {
            synchronized (this.candles) {
                if (!this.candles.containsKey(md.InstrumentID))
                    this.candles.put(md.InstrumentID,
                            new SingleCandle(md.InstrumentID));
                this.candles.get(md.InstrumentID).update(md);
            }
        }
    }
}
