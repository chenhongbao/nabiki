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

import java.util.ArrayList;
import java.util.Collection;


/**
 * Moving average that simply computes the average value of the latest elements back
 * to the size of {@code window}.
 */
public class Ma extends Series<Double> {
  private final int days;
  protected final transient ArrayList<Double> base = new ArrayList<>();

  public Ma(int days) {
    if (days <= 0)
      throw new InvalidValueException("not positive");
    this.days = days;
  }

  public int getDays() {
    return days;
  }

  @Override
  public boolean add(Double d) {
    base.add(d);
    var w = Math.min(getDays(), base.size());
    var range = super.subList(base.size() - w, base.size());
    return super.add(Commons.average(range));
  }


  @Override
  public boolean addAll(Collection<? extends Double> c) {
    if (c.size() == 0)
      return false;
    for (var v : c)
      add(v);
    return true;
  }

}
