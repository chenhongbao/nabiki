/*
 * Copyright (c) 2020-2021. Hongbao Chen <chenhongbao@outlook.com>
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

package com.nabiki.centre;

import com.nabiki.centre.config.Global;
import com.nabiki.centre.config.GlobalConfig;
import com.nabiki.centre.ctp.WorkingState;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.commons.utils.Utils;

import java.io.IOException;
import java.time.LocalTime;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

class PlatformTask extends TimerTask {
  public static final LocalTime[] whenStart = new LocalTime[]{
          LocalTime.of(8, 5), LocalTime.of(8, 10), LocalTime.of(8, 15),
          LocalTime.of(8, 20), LocalTime.of(8, 25), LocalTime.of(8, 30),
          LocalTime.of(8, 35), LocalTime.of(8, 40), LocalTime.of(8, 45),
          LocalTime.of(8, 50), LocalTime.of(8, 55),
          LocalTime.of(20, 5), LocalTime.of(20, 10), LocalTime.of(20, 15),
          LocalTime.of(20, 20), LocalTime.of(20, 25), LocalTime.of(20, 30),
          LocalTime.of(20, 35), LocalTime.of(20, 40), LocalTime.of(20, 45),
          LocalTime.of(20, 50), LocalTime.of(20, 55)
  };
  public static final LocalTime[] whenStop = new LocalTime[]{
          // Remote server disconnects at around 2:33 am., so logout before
          // remote server becomes unavailable.
          // Stop at 31 minutes to avoid the small chance that prevents
          // generating candles for 2:30.
          LocalTime.of(2, 31),
          // Wait enough time for settlement ticks sent by server.
          LocalTime.of(15, 45)
  };
  private final Platform main;
  private final Global global;
  private WorkingState workingState = WorkingState.STOPPED;
  private UserState userState = UserState.SETTLED;

  PlatformTask(Platform main, Global global) {
    this.main = main;
    this.global = global;
  }

  private UserState getUserState() {
    return userState;
  }

  private void setUserState(UserState state) {
    if (userState != state) {
      global.getLogger().info("user state: " + state);
    }
    userState = state;
  }

  private WorkingState getWorkingState() {
    return workingState;
  }

  private void setWorkingState(WorkingState state) {
    if (workingState != state) {
      global.getLogger().info("platform state: " + state);
    }
    workingState = state;
  }

  private LocalTime now() {
    var now = LocalTime.now();
    return LocalTime.of(now.getHour(), now.getMinute());
  }

  private boolean needStart() {
    if (getWorkingState() == WorkingState.STARTED) {
      return false;
    }
    var startNow = global.getArgument(Global.CMD_START_NOW_PREFIX);
    if (startNow != null && startNow.compareToIgnoreCase("true") == 0) {
      // So it won't start again right after stopped.
      GlobalConfig.setArgument(Global.CMD_START_NOW_PREFIX, "false");
      return true;
    } else {
      return Utils.isWorkday() && Utils.equalsAny(now(), whenStart);
    }
  }

  private boolean needStop() {
    // No need to check work day because it depends on whether the platform
    // starts prior to being stopped.
    return Utils.equalsAny(now(), whenStop)
            && getWorkingState() != WorkingState.STOPPED;
  }

  private void renew() {
    try {
      main.getAuth().load();
      main.getUsers().renew();
      setUserState(UserState.RENEW);
    } catch (Throwable th) {
      th.printStackTrace();
      global.getLogger().severe("load failed, " + th.getMessage());
    }
  }

  private void settle() {
    try {
      main.getAuth().flush();
      main.getUsers().settle();
      main.getOrder().settle();
      setUserState(UserState.SETTLED);
    } catch (Throwable th) {
      th.printStackTrace();
      global.getLogger().severe("settlement failed, " + th.getMessage());
    }
  }

  private void startTrader() {
    try {
      boolean r = true;
      if (!main.getOrder().isInit()) {
        main.getOrder().init();
        r = main.getOrder().waitConnected(TimeUnit.SECONDS.toMillis(5));
      }
      if (!r) {
        main.getOrder().release();
        global.getLogger().warning("trader connect failed");
        return;
      }
      main.getOrder().login();
      // Wait query instruments completed.
      if (!main.getOrder()
               .waitLastInstrument(TimeUnit.MINUTES.toMillis(1))) {
        global.getLogger().info("query instrument timeout");
      } else {
        // Update subscription so at next reconnect it will subscribe the
        // new instruments.
        // Don't update when it fails completing qry instruments and just use those
        // from previous day. Except is the init day when no instruments qry yet.
        main.getTick().setSubscription(main.getOrder().getInstrumentIDs());
      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    if (main.getOrder().getWorkingState() != WorkingState.STARTED) {
      global.getLogger().severe("trader didn't start up");
    } else if (getUserState() == UserState.SETTLED) {
      // Renew user information.
      renew();
    }
  }

  private void startMd() {
    try {
      boolean r = true;
      if (!main.getTick().isInit()) {
        main.getTick().init();
        r = main.getTick().waitConnected(TimeUnit.SECONDS.toMillis(5));
      }
      if (!r) {
        main.getTick().release();
        global.getLogger().warning("md connect failed");
        return;
      }
      main.getTick().login();
      r = main.getTick().waitWorkingState(
              WorkingState.STARTED,
              TimeUnit.MINUTES.toMillis(1));
      if (!r) {
        global.getLogger().info("wait md login timeout");
      }
      // No need to call TickProvider.subscribe() here because it is called
      // internally by tick provider after it logins.
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  // Try until providers all start.
  // Hence, need to improve internal working state management in
  // providers for more accurate report of the login/out ops,
  // especially on login/out failure.
  private void start() {
    setWorkingState(WorkingState.STARTING);
    // GC memory before start.
    System.gc();
    // Re-load config before starting platform for a new day.
    try {
      GlobalConfig.config();
    } catch (IOException e) {
      e.printStackTrace();
      global.getLogger().warning(e.getMessage());
    }
    // Start order provider first because it qry available instruments used
    // in subscription in tick provider.
    if (main.getOrder().getWorkingState() != WorkingState.STARTED)
      startTrader();
    if (main.getTick().getWorkingState() != WorkingState.STARTED)
      startMd();
    // Check state.
    if (main.getOrder().getWorkingState() == WorkingState.STARTED
            && main.getTick().getWorkingState() == WorkingState.STARTED) {
      setWorkingState(WorkingState.STARTED);
    } else {
      global.getLogger().warning("platform doesn't start");
    }
  }

  private void stopTrader() {
    try {
      // The CTP docs says it is not encouraged to release api.
      // Users should wait reconnect and then login to reuse the api. So here
      // just logout, don't disconnect.
      main.getOrder().logout();
      // Wait for logout.
      var r = main.getOrder().waitWorkingState(
              WorkingState.STOPPED,
              TimeUnit.MINUTES.toMillis(1));
      if (!r) {
        global.getLogger().severe("trader logout timeout");
      }
    } catch (Throwable th) {
      th.printStackTrace();
    } finally {
      //Settle user information at the end of a trading day.
      // Settlement is a must no matter whether trader logout successfully.
      var hour = LocalTime.now().getHour();
      if (14 < hour && hour < 21) {
        if (getUserState() == UserState.RENEW) {
          settle();
          checkPerformance();
          // Fix date: 2021-02-14
          // Clear products at every settlement so the outdated instruments are
          // cleared up in the candle engine.
          main.getCandleEngine().clearProducts();
        }
      }
    }
  }

  private void stop() {
    setWorkingState(WorkingState.STOPPING);
    if (main.getOrder().getWorkingState() != WorkingState.STOPPED) {
      stopTrader();
    }
    // Don't stop md because its logout return CTP error(77).
    // Change state.
    if (main.getOrder().getWorkingState() != WorkingState.STOPPED) {
      global.getLogger()
              .warning("platform doesn't stop, wait for disconnect");
    }
    // Front is disconnected automatically after remote shutdown, so no
    // need to force all logout here.
    setWorkingState(WorkingState.STOPPED);
  }

  private void checkPerformance() {
    var m = this.global.getPerformance().getAllMeasures();
    for (var entry : m.entrySet()) {
      var ms = entry.getValue().toMillis();
      global.getLogger().info(
              "performance, " + entry.getKey() + ": " + ms + "ms");
    }
  }

  @Override
  public void run() {
    try {
      if (needStart()) {
        start();
      }
      if (needStop()) {
        stop();
      }
    } catch (Throwable th) {
      th.printStackTrace();
      global.getLogger().severe(th.getMessage());
    }
  }
}
