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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

class PlatformTask extends TimerTask {
    private final Platform P;
    private final Global global;
    private final LocalTime start0 = LocalTime.of(20, 30),
            start1 = LocalTime.of(8, 30),
            stop0 = LocalTime.of(2, 30),
            stop1 = LocalTime.of(15, 30);
    private final LocalTime renewTime = LocalTime.of(20, 25),
            settleTime = LocalTime.of(15, 35);

    private WorkingState workingState = WorkingState.STOPPED;
    private UserState userState = UserState.SETTLED;

    public PlatformTask(Platform p, Global global) {
        this.P = p;
        this.global = global;
    }

    private boolean needRenew() {
        // Renew at some time of day, or at platform startup.
        return (LocalTime.now().isAfter(this.renewTime)
                || this.workingState == WorkingState.STARTED)
                && this.userState != UserState.RENEW;
    }

    private boolean needSettle() {
        var n = LocalTime.now();
        return n.isAfter(this.settleTime) && n.isBefore(this.renewTime)
                && this.userState != UserState.SETTLED;
    }

    private boolean needStart() {
        var day = LocalDate.now().getDayOfWeek();
        var time = LocalTime.now();
        if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
            return false;
        return ((time.isAfter(this.start0) || time.isBefore(this.stop0)) ||
                (time.isAfter(this.start1) && time.isBefore(stop1)))
                && this.workingState != WorkingState.STARTED;
    }

    private boolean needStop() {
        var time = LocalTime.now();
        return ((time.isAfter(this.stop0) && time.isBefore(this.start1)) ||
                (time.isAfter(this.stop1) && time.isBefore(this.start0)))
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

    private void connect(Connectable conn) {
        boolean init = false;
        while (!conn.isConnected()) {
            if (init)
                throw new IllegalStateException("connect failed");
            init = true;
            conn.connect();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startTrader() {
        try {
            connect(P.orderProvider);
            P.orderProvider.login();
            // Wait query instruments completed.
            if (!P.orderProvider.waitLastInstrument(
                    TimeUnit.MINUTES.toMillis(1)))
                this.global.getLogger().info("query instrument timeout");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (P.orderProvider.getWorkingState() != WorkingState.STARTED)
            this.global.getLogger().severe("trader didn't start up");
    }

    private void startMd() {
        try {
            connect(P.tickProvider);
            P.tickProvider.login();
            if (WorkingState.STARTED != P.tickProvider.waitWorkingState(
                    TimeUnit.MINUTES.toMillis(1)))
                this.global.getLogger().info("wait md login timeout");
            else
                P.tickProvider.subscribe(P.orderProvider.getInstruments());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Try until providers all start.
    // Hence, need to improve internal working state management in
    // providers for more accurate report of the login/out ops,
    // especially on login/out failure.
    private void start() {
        this.workingState = WorkingState.STARTING;
        this.global.getLogger().info("platform starting");
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
            if (WorkingState.STOPPED != P.orderProvider.waitWorkingState(
                    TimeUnit.MINUTES.toMillis(1)))
                this.global.getLogger().severe("trader logout timeout");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stopMd() {
        try {
            P.tickProvider.logout();
            // Wait for logout.
            if (WorkingState.STOPPED != P.tickProvider.waitWorkingState(
                    TimeUnit.MINUTES.toMillis(1)))
                this.global.getLogger().severe("md logout timeout");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void stop() {
        this.workingState = WorkingState.STOPPING;
        this.global.getLogger().info("platform stopping");
        if (P.orderProvider.getWorkingState() != WorkingState.STOPPED)
            stopTrader();
        if (P.tickProvider.getWorkingState() != WorkingState.STOPPED)
            stopMd();
        // Change state.
        if (P.orderProvider.getWorkingState() == WorkingState.STOPPED
                && P.tickProvider.getWorkingState() == WorkingState.STOPPED) {
            this.workingState = WorkingState.STOPPED;
            this.global.getLogger().info("platform stopped");
        } else {
            this.global.getLogger().warning("platform doesn't stop");
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
            if (needRenew())
                renew();
            if (needSettle()) {
                settle();
                checkPerformance();
            }
        } catch (Throwable th) {
            th.printStackTrace();
            this.global.getLogger().severe(th.getMessage());
        }
    }
}
