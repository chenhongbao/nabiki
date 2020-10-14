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

package com.nabiki.log;

import com.nabiki.commons.utils.SystemStream;
import com.nabiki.commons.utils.Utils;
import com.nabiki.log.portal.ui.LogMainWin;
import com.nabiki.log.server.Server;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
  private static void prepareStd() {
    try {
      var dir = Path.of(".log");
      if (!dir.toFile().exists() || !dir.toFile().isDirectory()) {
        Files.createDirectories(dir);
      }
      SystemStream.setErr(Path.of(dir.toString(), "log.err.log"));
      SystemStream.setOut(Path.of(dir.toString(), "log.out.log"));
    } catch (IOException e) {
      e.printStackTrace();
    }
    // Check default charset.
    System.out.println("default charset: " + Charset.defaultCharset().name());
  }

  private static void printHelp(String hint) {
    if (hint != null) {
      System.out.println(hint);
    }
    System.out.println("java[w] -jar server.jar <options>");
    System.out.println();
    System.out.println("Options:");
    System.out.println();
    System.out.println("--server          Start server. When server starts, the listening");
    System.out.println("                  options are necessary.");
    System.out.println("--src-listen      Local address for log input like 9039 or");
    System.out.println("                  127.0.0.1:9039");
    System.out.println("--sub-listen      Local address to serve clients like 9031");
    System.out.println("                  127.0.0.1:9031");
    System.out.println("--portal          Start GUI portal to monitor or send logs");
  }

  private static void runServer(String[] args) {
    try {
      var src = Utils.parseInetAddress(Utils.getOption("--src-listen", args));
      var sub = Utils.parseInetAddress(Utils.getOption("--sub-listen", args));
      if (src == null || sub == null) {
        printHelp("Wrong parameters.");
        System.exit(1);
      }
      new Server(src, sub).serve();
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (Utils.getOption("--help", args) != null || args.length == 0) {
      printHelp(null);
      return;
    }
    prepareStd();
    var serverOpt = Utils.getOption("--server", args);
    if (Utils.getOption("--portal", args) != null) {
      if (serverOpt != null) {
        LogMainWin.work(JFrame.DO_NOTHING_ON_CLOSE);
      } else {
        LogMainWin.work(JFrame.DISPOSE_ON_CLOSE);
      }
    }
    if (serverOpt != null) {
      runServer(args);
    }
  }
}
