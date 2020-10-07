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

/**
 * (Moving Average Convergence/Divergence indicator is computed with the following
 * equation:<br/>
 * <code>
 * DIF(n) is the n-th different between short-term line and long-term line, DEA(n)
 * is EMA of DIF, and MACD(n) is the red/green bar.<br/>
 * EMA(close, w, n) is the n-th day EMA of input close computed over window w,
 * then we have <br/>
 * DIF(n) = EMA(close, short_term, n) - EMA(close, long_term, n)<br/>
 * DEA(n) = EMA(DIF, mid_term, n)<br/>
 * MACD(n) = (DIF(n) - DEA(n)) x 2<br/>
 * </code>
 */
public class Macd extends Series<MacdElement> {
  private static final int DEFAULT_SHORT_TERM = 12;
  private static final int DEFAULT_LONG_TERM = 26;
  private static final int DEFAULT_MID_TERM = 9;
  private final Ema difShort, difLong, dea;

  /**
   * MACD with default parameters:<br/>
   * <code>
   * short_term = 12<br/>
   * long_term = 26<br/>
   * mid_term = 9
   * </code>
   */
  public Macd() {
    this(DEFAULT_SHORT_TERM, DEFAULT_LONG_TERM, DEFAULT_MID_TERM);
  }

  public Macd(int shortTerm, int longTerm, int midTerm) {
    difShort = new Ema(shortTerm);
    difLong = new Ema(longTerm);
    dea = new Ema(midTerm);
  }

  /**
   * Compute and add the new {@link MacdElement} with the input close price.
   *
   * @param close close price
   * @return {@code true} if new element is computed and added, {@code false}
   * otherwise.
   */
  public boolean add(Double close) {
    difShort.add(close);
    difLong.add(close);
    var dif = difShort.getTail() - difLong.getTail();
    dea.add(dif);
    return super.add(new MacdElement(
        (dif - dea.getTail()) * 2.0D,
        dif,
        dea.getTail()));
  }

  /**
   * @deprecated Element is computed internally, not appended.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean add(MacdElement macdElement) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Element is computed internally, not appended.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean addAll(Collection<? extends MacdElement> c) {
    return super.addAll(c);
  }
}
