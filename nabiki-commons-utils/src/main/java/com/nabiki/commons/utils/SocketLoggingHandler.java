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

package com.nabiki.commons.utils;

import com.nabiki.commons.utils.frame.Frame;
import com.nabiki.commons.utils.frame.FrameType;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.XMLFormatter;

public class SocketLoggingHandler extends Handler {
  private final String host;
  private final int port;
  private Socket sock;
  private BufferedOutputStream bout;

  public SocketLoggingHandler(String host, int port) throws IOException {
    this.host = host;
    this.port = port;
    connect();
    scheduleHeartbeat();
    setFormatter(new XMLFormatter());
  }

  private void scheduleHeartbeat() {
    var f = new Frame();
    f.Type = FrameType.HEARTBEAT;
    f.Length = 1;
    f.Body = new byte[]{0};
    Utils.schedule(new TimerTask() {
      @Override
      public void run() {
        try {
          synchronized (this) {
            if (bout != null) {
              bout.write(f.getBytes());
            }
          }
        } catch (Throwable ignored) {
        }
      }
    }, TimeUnit.MINUTES.toMillis(1));
  }

  private void connect() throws IOException {
    if (port == 0) {
      throw new IllegalArgumentException("Bad port: " + port);
    }
    if (host == null) {
      throw new IllegalArgumentException("Null host name: " + host);
    }
    synchronized (this) {
      sock = new Socket(host, port);
      bout = new BufferedOutputStream(sock.getOutputStream());
    }
  }

  @Override
  public boolean isLoggable(LogRecord record) {
    if (bout == null || record == null) {
      return false;
    }
    return super.isLoggable(record);
  }

  private void write(String s) throws IOException {
    Charset cs = null;
    var encoding = getEncoding();
    if (encoding == null) {
      // Figure out the default encoding.
      cs = java.nio.charset.Charset.defaultCharset();
    } else {
      try {
        cs = Charset.forName(encoding);
      } catch (Exception ex) {
        cs = java.nio.charset.Charset.defaultCharset();
      }
    }
    try {
      var bytes = s.getBytes(cs);
      Frame f = new Frame();
      f.Type = FrameType.REQUEST;
      f.Length = bytes.length;
      f.Body = bytes;
      synchronized (this) {
        bout.write(f.getBytes());
      }
    } catch (IOException ex) {
      reportError("Fail writing bytes in " + encoding, ex,
          ErrorManager.WRITE_FAILURE);
      // Try reconnect.
      connect();
    } catch (Exception ex) {
      reportError(ex.getMessage(), ex, ErrorManager.GENERIC_FAILURE);
    }
  }

  @Override
  public void publish(LogRecord record) {
    if (!isLoggable(record)) {
      return;
    }
    String msg;
    try {
      msg = getFormatter().getHead(this) + "\n" + getFormatter().format(record)
          + "\n" + getFormatter().getTail(this);
    } catch (Exception ex) {
      reportError("Format failed.", ex, ErrorManager.FORMAT_FAILURE);
      return;
    }
    try {
      write(msg);
    } catch (IOException ex) {
      reportError(ex.getMessage(), ex, ErrorManager.WRITE_FAILURE);
    }
  }

  @Override
  public void flush() {
    synchronized (this) {
      try {
        bout.flush();
      } catch (IOException e) {
        reportError(e.getMessage(), e, ErrorManager.FLUSH_FAILURE);
      }
    }
  }

  private void closeConnection() {
    if (sock != null) {
      try {
        sock.close();
      } catch (IOException ignored) {
      }
    }
    sock = null;
  }

  private void closeStream() {
    if (bout != null) {
      try {
        bout.close();
      } catch (IOException ignored) {
      }
    }
    bout = null;
  }

  @Override
  public void close() throws SecurityException {
    synchronized (this) {
      closeStream();
      closeConnection();
    }
  }
}
