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

package com.nabiki.log.server;

import java.util.Queue;

public class SimpleParser {
  private final byte[] buffer = new byte[1024 * 16];
  private final Queue<String> queue;
  private int position = 0;

  public SimpleParser(Queue<String> q) {
    queue = q;
  }

  private String get() {
    var str = new String(buffer, 0, position);
    int s = str.indexOf("<record>");
    if (str.endsWith("</record>") && s >= 0) {
      return str.substring(s);
    } else {
      return null;
    }
  }

  private void tryGet() {
    var m = get();
    if (m != null) {
      queue.offer(m);
      position = 0;
    }
  }

  public void parse(byte[] bytes) {
    if (bytes == null) {
      return;
    }
    for (var b : bytes) {
      parse(b);
    }
  }

  public void parse(byte b) {
    char c = (char) b;
    if (position == buffer.length) {
      tryGet();
      position = 0;
    } else {
      buffer[position++] = b;
      if (c == '>') {
        tryGet();
      }
    }
  }
}
