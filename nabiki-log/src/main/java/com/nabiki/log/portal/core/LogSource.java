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

package com.nabiki.log.portal.core;

import com.nabiki.log.server.XmlReceiver;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.LogRecord;

public class LogSource {
  private final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
  private final LogDisplay display;
  private final SAXHandler xmlHandler = new SAXHandler();

  private Socket client;
  private Thread recv;
  private final Thread updater;

  public LogSource(LogDisplay display) {
    this.display = display;
    this.updater = new Thread(() -> {
      SAXParser parser;
      try {
        parser = SAXParserFactory.newInstance().newSAXParser();
      } catch (SAXException | ParserConfigurationException e) {
        e.printStackTrace();
        return;
      }
      for(;;) {
        try {
          parser.reset();
          parser.parse(new ByteArrayInputStream(
              queue.take().getBytes(StandardCharsets.UTF_8)), xmlHandler);
          LogRecord record;
          while ((record = xmlHandler.pop()) != null) {
            display.append(record);
          }
        } catch (Throwable th) {
          th.printStackTrace();
        }
      }
    });
    this.updater.setDaemon(true);
    this.updater.start();
  }

  private InetSocketAddress getAddress(String address) {
    InetSocketAddress inetAddress;
    if (address.contains(":")) {
      inetAddress = new InetSocketAddress(
          address.substring(0, address.indexOf(":")).trim(),
          Integer.parseInt(address.substring(address.indexOf(":") + 1)));
    } else {
      inetAddress = new InetSocketAddress(Integer.parseInt(address.trim()));
    }
    return inetAddress;
  }

  public void open(String address) throws IOException {
    if (client != null && !client.isClosed()) {
      throw new IllegalStateException("re-open an open connection");
    }
    var inetAddress = getAddress(address);
    client = new Socket(inetAddress.getAddress(), inetAddress.getPort());
    if (recv != null && recv.isAlive()) {
      recv.interrupt();
    }
    recv = new Thread(new XmlReceiver(queue, client));
    recv.setDaemon(true);
    recv.start();
  }

  public void close() {
    if (client.isClosed()) {
      throw new IllegalStateException("close a closed connection");
    }
    try {
      client.close();
      recv.interrupt();
      recv = null;
      client = null;
    } catch (Throwable th) {
      throw new IllegalStateException("fail closing connection");
    }
  }
}
