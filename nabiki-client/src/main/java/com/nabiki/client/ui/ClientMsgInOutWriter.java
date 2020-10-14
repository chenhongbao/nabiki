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

import com.nabiki.commons.iop.Message;
import com.nabiki.commons.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ClientMsgInOutWriter {
  private final Path inDir, outDir;
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

  public ClientMsgInOutWriter() {
    this.inDir = Path.of(".in");
    this.outDir = Path.of(".out");
  }

  private void write(String text, File file) {
    try {
      Utils.writeText(text, file, StandardCharsets.UTF_8, false);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private File ensureFile(Path root, String fn) {
    try {
      var r = Path.of(root.toAbsolutePath().toString(), fn);
      Utils.createFile(root, true);
      Utils.createFile(r, false);
      return r.toFile();
    } catch (IOException e) {
      e.printStackTrace();
      return new File(".failover");
    }
  }

  void writeOut(Message out) {
    write(Utils.toJson(out), ensureFile(this.outDir,
        out.Type + "." + LocalDateTime.now().format(this.formatter) + "." + out.RequestID + ".json"));
  }

  void writeIn(Message in) {
    write(Utils.toJson(in), ensureFile(this.inDir,
        in.Type + "." + LocalDateTime.now().format(this.formatter) + "." + in.RequestID + ".json"));
  }
}
