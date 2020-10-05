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

package com.nabiki.log;

import com.nabiki.log.portal.ui.LogMainWin;
import com.nabiki.log.server.Server;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
  private static final ExecutorService es = Executors.newCachedThreadPool();

  private static String getOption(String prefix, String[] args) {
    if (args.length < 1) {
      return null;
    }
    int i = 0;
    do {
      if (args[i].equals(prefix)) {
        break;
      }
    } while (++i < args.length);
    if (i == args.length) {
      return null;
    } else if (i == args.length - 1) {
      return "";
    } else {
      return args[i + 1];
    }
  }

  private static InetSocketAddress parseInetAddress(String address) {
    if (address == null) {
      return null;
    }
    int idx = address.indexOf(":");
    if (idx < 0) {
      return new InetSocketAddress(Integer.parseInt(address.trim()));
    } else if (idx < address.length() - 1) {
      return new InetSocketAddress(
          address.substring(0, idx),
          Integer.parseInt(address.substring(idx + 1)));
    } else {
      printHelp("invalid address");
      return null;
    }
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
      var src = parseInetAddress(getOption("--src-listen", args));
      var sub = parseInetAddress(getOption("--sub-listen", args));
      new Server(src, sub).serve();
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (getOption("--help", args) != null || args.length == 0) {
      printHelp(null);
      return;
    }
    if (getOption("--portal", args) != null) {
      es.submit(LogMainWin::work);
    }
    if (getOption("--server", args) != null) {
      runServer(args);
    }
  }
}
