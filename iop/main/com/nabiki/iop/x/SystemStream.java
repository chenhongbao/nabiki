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

package com.nabiki.iop.x;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SystemStream extends PrintStream {
  private static final DateTimeFormatter formatter
      = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss ");

  private SystemStream(OutputStream out, boolean autoFlush,
                       Charset charset) {
    super(out, autoFlush, charset);
  }

  public static void setErr(String filepath)
      throws IOException {
    setErr(Path.of(filepath));
  }

  public static void setErr(Path path)
      throws IOException {
    setErr(path.toFile());
  }

  public static void setErr(File file)
      throws IOException {
    ensure(file);
    System.setErr(new SystemStream(new FileOutputStream(file, true),
        true, StandardCharsets.UTF_8));
  }

  public static void setOut(String filepath)
      throws IOException {
    setOut(Path.of(filepath));
  }

  public static void setOut(Path path)
      throws IOException {
    setOut(path.toFile());
  }

  public static void setOut(File file)
      throws IOException {
    ensure(file);
    System.setOut(new SystemStream(new FileOutputStream(file, true),
        true, StandardCharsets.UTF_8));
  }

  private static void ensure(File file) throws IOException {
    if (!file.exists()) {
      ensureDir(file.getParent());
      file.createNewFile();
    }
  }

  private static void ensureDir(String dir) throws IOException {
    if (dir == null || dir.trim().length() == 0)
      return;
    var parent = Path.of(dir);
    if (!Files.exists(parent))
      Files.createDirectories(parent);
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
