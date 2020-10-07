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

package com.nabiki.commons.iop.x;

import org.junit.Assert;
import org.junit.Test;

import java.time.LocalTime;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class PerformanceMeasureTest {
  @Test
  public void basic() {
    var measure = new PerformanceMeasure();
    var diff = measure.start("wait.lag");

    long sleep = Math.abs(new Random().nextInt(500));
    try {
      TimeUnit.MILLISECONDS.sleep(sleep);
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      diff.end();
    }

    Assert.assertEquals(sleep * 1.0D, measure.getMeasure("wait.lag").toMillis(), 15.0D);

    LocalTime s = LocalTime.now();
    LocalTime e;
    try {
      TimeUnit.MILLISECONDS.sleep(sleep);
    } catch (InterruptedException ex) {
      ex.printStackTrace();
    } finally {
      e = LocalTime.now();
    }

    Assert.assertEquals(sleep * 1.0D, measure.measure("wait.local", s, e).toMillis(), 15.0D);
  }
}