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

package com.nabiki.client.ui;

import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class UIPrintStream extends PrintStream {
  private static final DateTimeFormatter formatter
      = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ");

  public UIPrintStream(UIPrinter printer, boolean isOut) {
    super(new UIMessageOutputStream(printer, isOut), true);
  }

  private static String format(LocalDateTime timeStamp) {
    if (timeStamp == null)
      timeStamp = LocalDateTime.now();
    return timeStamp.format(formatter);
  }

  @Override
  public void println(boolean x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(char x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(int x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(long x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(float x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(double x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(char[] x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(String x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }

  @Override
  public void println(Object x) {
    super.print(format(LocalDateTime.now()));
    super.println(x);
  }
}
