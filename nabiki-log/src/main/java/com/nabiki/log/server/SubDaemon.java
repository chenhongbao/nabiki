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

package com.nabiki.log.server;

import com.nabiki.commons.utils.Utils;
import com.nabiki.commons.utils.frame.Frame;
import com.nabiki.commons.utils.frame.FrameType;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class SubDaemon implements Runnable {
  private final BlockingQueue<Frame> queue = new LinkedBlockingQueue<>();
  private final Set<Socket> clients;
  private final InetSocketAddress address;

  private Thread recvDaemon;

  public SubDaemon(InetSocketAddress recv, Set<Socket> c) {
    address = recv;
    clients = c;
  }

  private void startRecv() {
    recvDaemon = new Thread(new RecvDaemon(address, queue));
    recvDaemon.setDaemon(true);
    recvDaemon.start();
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
          send(f);
        } catch (Throwable ignored) {
        }
      }
    }, TimeUnit.MINUTES.toMillis(1));
  }

  private void send(Frame m) {
    synchronized (clients) {
      var iterator = clients.iterator();
      while (iterator.hasNext()) {
        var c = iterator.next();
        try {
          c.getOutputStream().write(m.getBytes());
        } catch (IOException e) {
          /* Don't print stack trace here, because it is very common to close
           * connection, and server catches io exception. */
          iterator.remove();
        }
      }
    }
  }

  @Override
  public void run() {
    try {
      startRecv();
      scheduleHeartbeat();
      for (; ; ) {
        try {
          send(queue.take());
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }
}
