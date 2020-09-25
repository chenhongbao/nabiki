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

package com.nabiki.centre.chain;

import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.CInputOrder;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ParkedRequestManager extends TimerTask {
  private final OrderProvider provider;
  private final ActiveUserManager actives;
  private final Global global;
  private final List<CInputOrder> inputs = new LinkedList<>();

  public ParkedRequestManager(OrderProvider provider, ActiveUserManager actives, Global global) {
    this.provider = provider;
    this.actives = actives;
    this.global = global;
    prepareTimer();
  }

  private void prepareTimer() {
    var parkedTimer = new Timer();
    parkedTimer.schedule(
        this,
        0,
        TimeUnit.SECONDS.toMillis(1));
  }

  private boolean canTrade(String instrID) {
    var hour = global.getTradingHour(null, instrID);
    if (hour == null) {
      global.getLogger().warning(
          Utils.formatLog(
              "trading hour global null",
              instrID,
              null,
              null));
      return false;
    }
    LocalTime now;
    var ins = global.getInstrInfo(instrID);
    if (ins != null && ins.Instrument != null) {
      now = provider.getTimeAligner().getAlignTime(
          ins.Instrument.ExchangeID,
          LocalTime.now());
    } else {
      now = LocalTime.now();
    }
    // If remote counter opens for some while during weekend.
    var dayOfWeek = LocalDate.now().getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return false;
    }
    return provider.isConfirmed() && hour.contains(now.minusSeconds(1));
  }

  void offer(CInputOrder input) {
    synchronized (inputs) {
      inputs.add(input);
    }
  }

  @Override
  public void run() {
    try {
      synchronized (inputs) {
        var iterator = inputs.iterator();
        while (iterator.hasNext()) {
          var in = iterator.next();
          if (!canTrade(in.InstrumentID)) {
            continue;
          }
          var user = actives.getActiveUser(in.UserID);
          if (user == null) {
            global.getLogger().warning(
                String.format("user[%s] not found", in.UserID));
          } else {
            try {
              user.insertOrder(in);
            } catch (Throwable th) {
              th.printStackTrace();
              global.getLogger().warning(th.getMessage());
            }
          }
          // Remove the processed parked order.
          iterator.remove();
        }
      }
    } catch (Throwable th) {
      th.printStackTrace();
      global.getLogger().warning(th.getMessage());
    }
  }
}
