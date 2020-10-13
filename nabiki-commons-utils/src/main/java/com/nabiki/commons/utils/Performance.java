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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Performance {

  private final static DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
  private final Map<String, Duration> measures = new ConcurrentHashMap<>();

  public Performance() {
  }

  public PerfTimeDiff start(String name) {
    return new PerfTimeDiff(name, measures);
  }

  public Duration measure(String name, String from, String to) {
    return measure(
        name,
        LocalTime.parse(from, timeFormatter),
        LocalTime.parse(to, timeFormatter));
  }

  public Duration measure(String name, LocalTime from, LocalTime to) {
    var duration = Duration.between(from, to);
    this.measures.put(name, duration);
    return duration;
  }

  public Duration measureMax(String name, String from, String to) {
    return measureMax(
        name,
        LocalTime.parse(from, timeFormatter),
        LocalTime.parse(to, timeFormatter));
  }

  public Duration measureMax(String name, LocalTime from, LocalTime to) {
    return measureAndCompare(name, from, to, 1);
  }

  public Duration measureMin(String name, String from, String to) {
    return measureMin(
        name,
        LocalTime.parse(from, timeFormatter),
        LocalTime.parse(to, timeFormatter));
  }

  private Duration measureAndCompare(
      String name, LocalTime from, LocalTime to, int positive) {
    var duration = Duration.between(from, to);
    var old = this.measures.get(name);
    if (old == null || old.compareTo(duration) * positive < 0)
      this.measures.put(name, duration);
    else
      duration = this.measures.get(name);
    return duration;
  }

  public Duration measureMin(String name, LocalTime from, LocalTime to) {
    return measureAndCompare(name, from, to, -1);
  }

  public Duration getMeasure(String name) {
    return this.measures.get(name);
  }

  public Map<String, Duration> getAllMeasures() {
    return this.measures;
  }
}
