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

import com.nabiki.commons.ctpobj.CSpecificInstrument;
import com.nabiki.commons.ctpobj.ErrorCodes;
import com.nabiki.commons.iop.x.OP;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Client extends AbstractClient {
  private Trader trader;
  private MarketDataHandler handler;
  private final Date startDate;

  public Client(String[] args) {
    if (OP.getOption("--help", args) != null) {
      printHelp();
      System.exit(1);
    }
    startDate = parseDate(OP.getOption("--start-at", args));
  }

  private void printHelp() {
    System.out.println("java[w] -jar your-app.jar <option>");
    System.out.println();
    System.out.println("Options:");
    System.out.println();
    System.out.println("--start-at      Set the time when the trader is started:");
    System.out.println("                HH:mm:ss or yyyy-MM-dd HH:mm:ss");
    System.out.println("--help          Print help message.");
  }

  private Date parseDate(String str) {
    Date r = null;
    try {
      var time = LocalTime.parse(str, DateTimeFormatter.ofPattern("HH:mm:ss"));
      var c = Calendar.getInstance();
      c.set(Calendar.HOUR_OF_DAY, time.getHour());
      c.set(Calendar.MINUTE, time.getMinute());
      c.set(Calendar.SECOND, time.getSecond());
      r = c.getTime();
    } catch (Throwable ignored) {
      try {
        var dateTime = LocalDateTime.parse(str, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        var c = Calendar.getInstance();
        c.set(Calendar.YEAR, dateTime.getYear());
        c.set(Calendar.MONTH, dateTime.getMonthValue() - 1);
        c.set(Calendar.DAY_OF_MONTH, dateTime.getDayOfMonth());
        c.set(Calendar.HOUR_OF_DAY, dateTime.getHour());
        c.set(Calendar.MINUTE, dateTime.getMinute());
        c.set(Calendar.SECOND, dateTime.getSecond());
        r = c.getTime();
      } catch (Throwable th) {
        r = Calendar.getInstance().getTime();
      }
    }
    return r;
  }

  private void checkLogin() {
    if (!loginRsp.hasResponse())
      throw new RuntimeException("login timeout");
    var info = loginRsp.getRspInfo(loginRsp.poll());
    if (info == null || info.ErrorID != ErrorCodes.NONE)
      throw new RuntimeException("login failed");
  }

  private void checkSubMd() {
    if (!subRsp.hasResponse())
      throw new RuntimeException("sub md timeout");
    if (subRsp.getTotalCount() != subRsp.getArrivalCount())
      throw new RuntimeException("sub msd rsp uncompleted");
    CSpecificInstrument in = null;
    while ((in = subRsp.poll()) != null) {
      var info = subRsp.getRspInfo(in);
      if (info == null || info.ErrorID != ErrorCodes.NONE)
        throw new RuntimeException("sub md " + in.InstrumentID + " failed");
    }
  }

  private void checkAll() throws InterruptedException {
    // It takes quite a long time for server to send all history data,
    // so just wait a bit longer.
    TimeUnit.SECONDS.sleep(60);
    checkLogin();
    checkSubMd();
  }

  public void run(HeadlessTrader trader, InetSocketAddress serverAddress) {
    this.trader = trader;
    this.handler = trader;
    OP.scheduleOnce(new TimerTask() {
      @Override
      public void run() {
        try {
          initTrader(trader, serverAddress);
          checkAll();
        } catch (Throwable th) {
          th.printStackTrace();
          trader.getLogger().severe(th.getMessage());
        }
      }
    }, startDate);
  }

  public void run(FigureTrader trader, InetSocketAddress serverAddress) {
    this.trader = trader;
    this.handler = trader;
    OP.scheduleOnce(new TimerTask() {
      @Override
      public void run() {
        try {
          initTrader(trader, serverAddress);
          checkAll();
        } catch (Throwable th) {
          th.printStackTrace();
          trader.getLogger().severe(th.getMessage());
        }
      }
    }, startDate);
  }

  public void noExit() {
    while (true) {
      try {
        new CountDownLatch(1).await();
      } catch (InterruptedException e) {
        trader.getLogger().warning(e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public void exitAt(LocalDateTime dateTime) {
    if (dateTime == null) {
      trader.getLogger().severe(
          "client exits immediately because exit time has passed");
      return;
    }
    while (LocalDateTime.now().isBefore(dateTime)) {
      try {
        TimeUnit.SECONDS.sleep(1);
      } catch (InterruptedException ignored) {
      }
    }
    // Stop.
    try {
      handler.onStop();
    } catch (Throwable th) {
      th.printStackTrace();
      trader.getLogger().warning(th.getMessage());
    } finally {
      super.stop();
      trader.stop();
    }
  }
}
