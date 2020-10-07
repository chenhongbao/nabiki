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

package com.nabiki.log.portal.core;

import javax.swing.*;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LogDisplay {
  private final JTextArea area;
  private final LogFilter filter;
  private final AtomicBoolean enabled = new AtomicBoolean(true);
  private final Vector<LogRecord> logs = new Vector<>();
  private final SimpleFormatter formatter = new SimpleFormatter();

  public LogDisplay(JTextArea area, LogFilter filter) {
    this.area = area;
    this.filter = filter;
  }

  private void display(LogRecord record) {
    synchronized (area) {
      area.append(formatter.format(record));
    }
  }

  private void scrollBottom() {
    synchronized (area) {
      area.setCaretPosition(area.getDocument().getLength());
    }
  }

  public void append(LogRecord record) {
    logs.add(record);
    if (enabled.get() && filter.passed(record)) {
      display(record);
      scrollBottom();
    }
  }

  /**
   * Re-filter and display all logs.
   */
  public void reset() {
    clearArea();
    int size = logs.size();
    for (int index = 0; index < size; ++index) {
      var record = logs.elementAt(index);
      if (filter.passed(record)) {
        display(record);
      }
    }
    scrollBottom();
  }

  private void clearArea() {
    synchronized (area) {
      area.setText("");
    }
  }

  /**
   * Clear logs.
   */
  public void clear() {
    clearArea();
    logs.clear();
  }
}
