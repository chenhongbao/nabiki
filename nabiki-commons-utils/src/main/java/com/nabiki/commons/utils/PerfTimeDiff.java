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

package com.nabiki.commons.utils;

import java.time.Duration;
import java.util.Map;

public class PerfTimeDiff {
  private final String name;
  private final long startNanos;
  private final Map<String, Duration> measures;

  PerfTimeDiff(String name, Map<String, Duration> measures) {
    this.name = name;
    this.measures = measures;
    this.startNanos = System.nanoTime();
  }

  public Duration end() {
    var duration = Duration.ofNanos(System.nanoTime() - this.startNanos);
    measures.put(this.name, duration);
    return duration;
  }

  public Duration compareAndEnd(int positive) {
    var duration = Duration.ofNanos(System.nanoTime() - this.startNanos);
    var old = measures.get(this.name);
    if (old == null || old.compareTo(duration) * positive < 0)
      measures.put(this.name, duration);
    else
      duration = measures.get(this.name);
    return duration;
  }

  public Duration endWithMax() {
    return compareAndEnd(1);
  }

  public Duration endWithMin() {
    return compareAndEnd(-1);
  }
}
