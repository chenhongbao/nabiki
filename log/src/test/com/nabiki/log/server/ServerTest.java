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

import com.nabiki.log.Main;
import org.junit.Test;

import java.io.IOException;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.SocketHandler;

public class ServerTest {

  private void sleep(int value, TimeUnit unit){
    try {
      unit.sleep(value);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  @Test
  public void basic() {
    new Thread(() -> {
      try {
        sleep(1, TimeUnit.SECONDS);
        var logger = Logger.getLogger("writer");
        logger.addHandler(new SocketHandler("localhost", 9039));
        for (;;) {
          sleep(2, TimeUnit.SECONDS);
          logger.info(LocalDateTime.now().toString());
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();

    new Thread(() -> {
      try {
        sleep(1, TimeUnit.SECONDS);
        var queue = new LinkedBlockingQueue<String>();
        Socket client = new Socket("localhost", 9031);
        new Thread(new XmlReceiver(queue, client)).start();
        for(;;) {
          try {
            var m = queue.take();
            System.out.println(m);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }).start();

    Main.main(new String[]{
        "--portal",
        "--server",
        "--src-listen", "9039",
        "--sub-listen", "9031"
    });
  }
}