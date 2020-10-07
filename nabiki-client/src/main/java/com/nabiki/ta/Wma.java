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

/**
 * Weighted moving average indicator computed with the following equation:<br/>
 * <code>
 * n-th day WMA(n) and n-th day input p(n). Given window w, the WMA(n) over the
 * latest window elements:<br/>
 * deno(n) = w x  p(n) + (w-1) x p(n-1) + ... + 2 x p(n-w+2) + p(n-w+1)<br/>
 * nume(n) = w + (w-1) + ... + 2 + 1 = n x (n+1) / 2<br/>
 * WMA(n) = deno(n) / nume(n)
 * </code>
 */
public class Wma extends Ma {
  public Wma(int window) {
    super(window);
  }

  @Override
  public boolean add(Double d) {
    base.add(d);
    var n = Math.min(base.size(), getDays());
    var idx = base.size();
    var t = 0.0D;
    do {
      t += n * base.get(--idx);
    } while (--n > 0);
    t /= n * (n + 1) / 2.0D;
    return super.add(t);
  }
}
