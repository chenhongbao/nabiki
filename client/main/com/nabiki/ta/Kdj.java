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

package com.nabiki.ta;

import java.util.Collection;
import java.util.Comparator;

/**
 * Stochastic oscillator, also named KDJ, is computed by the following equation:<br/>
 * <code>
 * Given close, high and low prices of the latest n days, latest k days for K,
 * latest d days for D, highest value of prices c of latest x day is hhv(c, x),
 * lowest is llv(c, x), the indicator is computed:<br/>
 * RSV(n) = (close - llv(low, n)) / (hhv(high, n) - llv(low, n)) * 100<br/>
 * K(n) = SMA(RSV, k, 1)<br/>
 * D(n) = SMA(K, d, 1)<br/>
 * J(n) = 3 x K(n) - 2 x D(n)
 * </code>
 */
public class Kdj extends Series<KdjElement> {
  private final Comparator<Double> comparator = new Comparator<Double>() {
    @Override
    public int compare(Double o1, Double o2) {
      var r = o1 - o2;
      return r > 0 ? 1 : (r < 0 ? -1 : 0);
    }
  };

  private static final int DEFAULT_N_DAYS = 9;
  private static final int DEFAULT_K_DAYS = 3;
  private static final int DEFAULT_D_DAYS = 3;

  private final Series<Double> high = new Series<>();
  private final Series<Double> low = new Series<>();
  private final Sma k;
  private final Sma d;
  private final int nDays;

  /**
   * KDJ with default parameters:<br/>
   * <code>
   * days = 9<br/>
   * k-days = 3<br/>
   * d-days = 3<br/>
   * </code>
   */
  public Kdj() {
    this(DEFAULT_N_DAYS, DEFAULT_K_DAYS, DEFAULT_D_DAYS);
  }

  public Kdj(int nDays, int kDays, int dDays) {
    this.nDays = nDays;
    this.k = new Sma(kDays, 1);
    this.d = new Sma(dDays, 1);
  }

  /**
   * Add and compute KDJ indicator on the specified inputs.
   *
   * @param close close price
   * @param h     high price
   * @param l     low price
   * @return {@code true} if new element is computed and added, {@code false}
   * otherwise.
   */
  public boolean add(Double close, Double h, Double l) {
    high.add(h);
    low.add(l);
    var vh = high.getHigh(nDays, comparator).getValue();
    var vl = low.getLow(nDays, comparator).getValue();
    var rsv = (close - vl) / (vh - vl) * 100.0D;
    k.add(rsv);
    d.add(k.getTail());
    var vj = 3 * k.getTail() - 2 * d.getTail();
    return super.add(new KdjElement(k.getTail(), d.getTail(), vj));
  }

  @Deprecated(since = "0.1")
  @Override
  public boolean add(KdjElement kdjElement) {
    throw new UnsupportedOperationException();
  }

  @Deprecated(since = "0.1")
  @Override
  public boolean addAll(Collection<? extends KdjElement> c) {
    throw new UnsupportedOperationException();
  }
}
