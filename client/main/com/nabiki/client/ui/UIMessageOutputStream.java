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

import java.io.*;
import java.nio.ByteBuffer;

public class UIMessageOutputStream extends OutputStream {
  private final UIPrinter printer;
  private final boolean isOut;
  private final byte[] bytes = new byte[1024 * 16];
  private final ByteBuffer buffer = ByteBuffer.wrap(bytes);
  private final File outFile = new File("out.log"),
      errFile = new File("err.log");

  public UIMessageOutputStream(UIPrinter printer, boolean isOut) {
    this.isOut = isOut;
    this.printer = printer;
  }

  @Override
  public void write(int b) throws IOException {
    buffer.put((byte) b);
  }

  @Override
  public void flush() throws IOException {
    buffer.flip();
    var str = new String(bytes, buffer.position(), buffer.remaining());
    buffer.clear();
    if (isOut) {
      printer.appendOut(str);
      writeFile(str, outFile);
    } else {
      printer.appendErr(str);
      writeFile(str, errFile);
    }
  }

  private void writeFile(String msg, File file) {
    try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
      pw.print(msg);
      pw.flush();
    } catch (IOException ignored) {
    }
  }
}
