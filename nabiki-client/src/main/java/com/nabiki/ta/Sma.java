/*
 * Copyright (c) 2020-2021. Hongbao Chen <chenhongbao@outlook.com>
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

/**
 * Simple Moving Average is computed with the following equation:<br/>
 * <code>
 * SMA of the n-th days, with a weight w to current input C and m inputs,
 * is equal to:<br/>
 * SMA(n) = (w x C + SMA(n-1) x (m - w)) / m<br/>
 * or<br/>
 * SMA(n) = alpha x C + SMA(n-1) x (1 - alpha)<br/>
 * alpha = w / m
 * </code>
 */
public class Sma extends Series<Double> {
  private static final double ZERO_DAY_SMA = 0.0D;

  private final int w;
  private final int m;

  public Sma(int days, int weight) {
    if (weight <= 0 || days <= weight) {
      throw new InvalidValueException(String.format("(%d, %d)", days, weight));
    }
    w = weight;
    m = days;
  }

  @Override
  public boolean add(Double d) {
    var prev = ZERO_DAY_SMA;
    if (size() > 0) {
      prev = get(size() - 1);
    }
    var t = (w * d + (m - w) * prev) / m;
    return super.add(t);
  }
}
