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

package com.nabiki.client.ui;

import javax.swing.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;

public class UITextOutputStream extends UIOutputStream {
  private final JTextArea tout;
  private final byte[] bytes = new byte[1024 * 16];
  private final ByteBuffer buffer = ByteBuffer.wrap(bytes);

  private File fout;
  private boolean autoScroll = true;

  public UITextOutputStream(JTextArea area) {
    this.tout = area;
  }

  public UITextOutputStream(JTextArea area, File file) {
    this.tout = area;
    this.fout = file;
  }

  @Override
  public void setFile(File f) {
    fout = f;
  }

  @Override
  public void setAutoScroll(boolean b) {
    autoScroll = b;
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
    tout.append(str);
    if (autoScroll) {
      scrollBottom(tout);
    }
    writeFile(str, fout);
  }

  private void writeFile(String msg, File file) {
    if (file == null) {
      return;
    }
    try (PrintWriter pw = new PrintWriter(new FileWriter(file, true))) {
      pw.print(msg);
      pw.flush();
    } catch (IOException ignored) {
    }
  }

  private void scrollBottom(JTextArea area) {
    area.setCaretPosition(area.getDocument().getLength());
  }
}
