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

package com.nabiki.centre;

import com.nabiki.centre.ctp.Connectable;
import com.nabiki.centre.ctp.WorkingState;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.centre.utils.Utils;

import java.time.LocalTime;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

class PlatformTask extends TimerTask {
    private final Platform P;
    private final Global global;

    private WorkingState workingState = WorkingState.STOPPED;
    private UserState userState = UserState.SETTLED;

    // Try several times when starting.
    public static final LocalTime[] whenStart = new LocalTime[]{
            LocalTime.of(8, 45),
            LocalTime.of(20, 45)
    };
    public static final LocalTime[] whenStop = new LocalTime[] {
            // Remote server disconnects at around 2:33 am., so logout before
            // remote server becomes unavailable.
            // Stop at 31 minutes to avoid the small chance that prevents
            // generating candles for 2:30.
            LocalTime.of(2, 31),
            // Wait enough time for settlement ticks sent by server.
            LocalTime.of(15, 45)
    };

     PlatformTask(Platform p, Global global) {
        this.P = p;
        this.global = global;
    }

    private LocalTime now() {
        var now = LocalTime.now();
        return LocalTime.of(now.getHour(), now.getMinute());
    }

    private boolean needStart() {
        if (this.workingState == WorkingState.STARTED)
            return false;
        var startNow = global.getArgument(Global.CMD_START_NOW_PREFIX);
        if ( startNow != null && startNow.compareToIgnoreCase("true") == 0) {
            // So it won't start again right after stopped.
            GlobalConfig.setArgument(Global.CMD_START_NOW_PREFIX, "false");
            return true;
        } else
            return Utils.isWorkday() && Utils.equalsAny(now(), whenStart);
    }

    private boolean needStop() {
        // No need to check work day because it depends on whether the platform
        // starts prior to being stopped.
        return Utils.equalsAny(now(), whenStop)
                && this.workingState != WorkingState.STOPPED;
    }

    private void renew() {
        try {
            this.global.getLogger().info("platform renewing");
            P.authMgr.load();
            P.userMgr.renew();
            GlobalConfig.config();
            this.userState = UserState.RENEW;
            this.global.getLogger().info("platform renewed");
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe("load failed");
        }
    }

    private void settle() {
        this.global.getLogger().info("platform settling");
        try {
            P.authMgr.flush();
            P.userMgr.settle();
            this.userState = UserState.SETTLED;
            this.global.getLogger().info("platform settled");
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe("settlement failed");
        }
    }

    private boolean connect(Connectable conn) {
        if (!conn.isConnected()) {
            conn.connect();
            return conn.waitConnected(TimeUnit.SECONDS.toMillis(5));
        }
        return true;
    }

    private void startTrader() {
        try {
            if (!connect(P.orderProvider)) {
                this.global.getLogger().warning("trader connect failed");
                return;
            }
            P.orderProvider.login();
            // Wait query instruments completed.
            if (!P.orderProvider.waitLastInstrument(TimeUnit.MINUTES.toMillis(1)))
                this.global.getLogger().info("query instrument timeout");
            else
                // Update subscription so at next reconnect it will subscribe the
                // new instruments.
                P.tickProvider.setSubscription(P.orderProvider.getInstruments());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (P.orderProvider.getWorkingState() != WorkingState.STARTED)
            this.global.getLogger().severe("trader didn't start up");
        else if (this.userState == UserState.SETTLED)
            // Renew user information.
            renew();
    }

    private void startMd() {
        try {
            if (!connect(P.tickProvider)) {
                this.global.getLogger().warning("md connect failed");
                return;
            }
            P.tickProvider.login();
            var r = P.tickProvider.waitWorkingState(
                    WorkingState.STARTED,
                    TimeUnit.MINUTES.toMillis(1));
            if (!r)
                this.global.getLogger().info("wait md login timeout");
            else
                // This code works on first startup.
                // For later reconnect, it will use the internal instruments set by
                // order provider after qry all instruments.
                P.tickProvider.subscribe();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    // Try until providers all start.
    // Hence, need to improve internal working state management in
    // providers for more accurate report of the login/out ops,
    // especially on login/out failure.
    private void start() {
        this.workingState = WorkingState.STARTING;
        this.global.getLogger().info("platform starting");
        // Start order provider first because it qry available instruments used
        // in subscription in tick provider.
        if (P.orderProvider.getWorkingState() != WorkingState.STARTED)
            startTrader();
        if (P.tickProvider.getWorkingState() != WorkingState.STARTED)
            startMd();
        // Check state.
        if (P.orderProvider.getWorkingState() == WorkingState.STARTED
                && P.tickProvider.getWorkingState() == WorkingState.STARTED) {
            this.workingState = WorkingState.STARTED;
            this.global.getLogger().info("platform started");
        } else {
            this.global.getLogger().warning("platform doesn't start");
        }
    }

    private void stopTrader() {
        try {
            P.orderProvider.logout();
            // Wait for logout.
            var r = P.orderProvider.waitWorkingState(
                    WorkingState.STOPPED,
                    TimeUnit.MINUTES.toMillis(1));
            if (!r)
                this.global.getLogger().severe("trader logout timeout");
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private void stop() {
        this.workingState = WorkingState.STOPPING;
        this.global.getLogger().info("platform stopping");
        if (P.orderProvider.getWorkingState() != WorkingState.STOPPED)
            stopTrader();
        // Don't stop md because its logout return CTP error(77).
        // Change state.
        if (P.orderProvider.getWorkingState() == WorkingState.STOPPED)
            this.global.getLogger().info("platform stopped");
        else
            this.global.getLogger()
                    .warning("platform doesn't stop, wait for disconnect");
        // Front is disconnected automatically after remote shutdown, so no
        // need to force all logout here.
        this.workingState = WorkingState.STOPPED;
        //Settle user information after platform stops at the end of a trading day.
        var hour = LocalTime.now().getHour();
        if (14 < hour && hour < 21) {
            settle();
            checkPerformance();
            P.tickProvider.checkSubscription();
        }
    }

    private void checkPerformance() {
        var m = this.global.getPerformanceMeasure().getAllMeasures();
        for (var entry : m.entrySet()) {
            var ms = entry.getValue().toMillis();
            this.global.getLogger().info(
                    "performance, " + entry.getKey() + ": " + ms + "ms");
        }
    }

    @Override
    public void run() {
        try {
            if (needStart())
                start();
            if (needStop())
                stop();
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe(th.getMessage());
        }
    }
}
