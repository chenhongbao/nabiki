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

import com.nabiki.centre.chain.StaticChainInstaller;
import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.ctp.TickProvider;
import com.nabiki.centre.ctp.WorkingState;
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.iop.IOP;
import com.nabiki.iop.x.SystemStream;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Platform launcher. Start the program with following arguments:
 * <ul>
 * <li>--host {@code <host>}</li>
 * <li>--port {@code <port>}</li>
 * <li>--root {@code <data directory root>}</li>
 * </ul>
 */
public class Platform {
    static {
        System.loadLibrary("thostmduserapi_se");
        System.loadLibrary("thosttraderapi_se");
        System.loadLibrary("thostctpapi_se-6.3.19-P1");
    }

    public static String getArgument(String[] args, String prefix) {
        String value = null;
        for (var arg : args) {
            if (arg.trim().compareTo(prefix.trim()) == 0) {
                value = "";
                continue;
            }
            if (value != null)
                return arg.trim();
        }
        return value;
    }

    public static int getIntArgument(String[] args, String prefix) {
        return Integer.parseInt(getArgument(args, prefix));
    }

    private Global global;
    private CandleEngine candleEngine;
    private OrderProvider orderProvider;
    private TickProvider tickProvider;
    private UserAuthManager authManager;
    private ActiveUserManager userManager;

    private final MarketDataRouter router;
    private final Timer timerPlat;
    private final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

    Platform() {
        this.timerPlat = new Timer();
        this.router = new MarketDataRouter();
    }

    private void providers() {
        // Set order provider.
        this.orderProvider = new OrderProvider(this.global);
        this.orderProvider.initialize();
        // Prepare candle engine.
        this.candleEngine = new CandleEngine(this.global);
        this.candleEngine.registerRouter(this.router);
        // Set tick provider.
        this.tickProvider = new TickProvider(this.global);
        this.tickProvider.register(this.candleEngine);
        this.tickProvider.register(this.router);
        this.tickProvider.initialize();
    }

    private void server(String host, int port) throws IOException {
        // Server.
        var server = IOP.createServer();
        StaticChainInstaller.install(
                server, this.authManager, this.userManager, router, global);
        server.bind(new InetSocketAddress(host, port));
    }

    private void managers() {
        // Set auth/user manager.
        var userDir = global.getRootDirectory()
                .recursiveGet("dir.user")
                .iterator()
                .next()
                .path();
        this.authManager = new UserAuthManager(userDir);
        this.userManager = new ActiveUserManager(
                orderProvider,
                global,
                userDir);
    }

    private void system() throws IOException {
        // Set system's output and error output.
        var dir = this.global.getRootDirectory()
                .recursiveGet("dir.log")
                .iterator()
                .next();
        SystemStream.setErr(dir.setFile(
                "file.log.err", "system.err.log").file());
        SystemStream.setOut(dir.setFile
                ("file.log.out", "system.out.log").file());
    }

    public void start(String[] args) throws IOException {
        // Set global.
        GlobalConfig.rootPath = getArgument(args, "--root");
        this.global = GlobalConfig.config();
        system();
        providers();
        managers();
        server(getArgument(args, "--host"),
                getIntArgument(args, "--port"));
    }

    public void task() {
        this.timerPlat.scheduleAtFixedRate(
                new PlatformTask(),
                MILLIS - System.currentTimeMillis() % MILLIS,
                MILLIS);
    }

    private class PlatformTask extends TimerTask {
        private final LocalTime start0 = LocalTime.of(20, 30),
                start1 = LocalTime.of(8, 30),
                stop0 = LocalTime.of(2, 30),
                stop1 = LocalTime.of(15, 30);
        private final LocalTime renewTime = LocalTime.of(20, 25),
                settleTime = LocalTime.of(15, 35);

        private WorkingState workingState = WorkingState.STOPPED;
        private UserState userState = UserState.SETTLED;

        PlatformTask() {
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
                global.getLogger().info("platform renewing");
                authManager.load();
                userManager.renew();
                GlobalConfig.config();
                this.userState = UserState.RENEW;
                global.getLogger().info("platform renewed");
            } catch (Throwable th) {
                th.printStackTrace();
                global.getLogger().severe("load failed");
            }
        }

        private void settle() {
            global.getLogger().info("platform settling");
            try {
                authManager.flush();
                userManager.settle();
                this.userState = UserState.SETTLED;
                global.getLogger().info("platform settled");
            } catch (Throwable th) {
                th.printStackTrace();
                global.getLogger().severe("settlement failed");
            }
        }

        private void startTrader() {
            try {
                orderProvider.login();
                // Wait query instruments completed.
                if (!orderProvider.waitLastInstrument(
                        TimeUnit.MINUTES.toMillis(1)))
                    global.getLogger().info("query instrument timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (orderProvider.getWorkingState() != WorkingState.STARTED)
                global.getLogger().severe("trader didn't start up");
        }

        private void startMd() {
            try {
                tickProvider.login();
                if (WorkingState.STARTED != tickProvider.waitWorkingState(
                        TimeUnit.MINUTES.toMillis(1)))
                    global.getLogger().info("wait md login timeout");
                else
                    tickProvider.subscribe(orderProvider.getInstruments());
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
            global.getLogger().info("platform starting");
            if (orderProvider.getWorkingState() != WorkingState.STARTED)
                startTrader();
            if (tickProvider.getWorkingState() != WorkingState.STARTED)
                startMd();
            // Check state.
            if (orderProvider.getWorkingState() == WorkingState.STARTED
                    && tickProvider.getWorkingState() == WorkingState.STARTED) {
                this.workingState = WorkingState.STARTED;
                global.getLogger().info("platform started");
            } else {
                global.getLogger().warning("platform doesn't start");
            }
        }

        private void stopTrader() {
            try {
                orderProvider.logout();
                // Wait for logout.
                if (WorkingState.STOPPED != orderProvider.waitWorkingState(
                        TimeUnit.MINUTES.toMillis(1)))
                    global.getLogger().severe("trader logout timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void stopMd() {
            try {
                tickProvider.logout();
                // Wait for logout.
                if (WorkingState.STOPPED != tickProvider.waitWorkingState(
                        TimeUnit.MINUTES.toMillis(1)))
                    global.getLogger().severe("md logout timeout");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void stop() {
            this.workingState = WorkingState.STOPPING;
            global.getLogger().info("platform stopping");
            if (orderProvider.getWorkingState() != WorkingState.STOPPED)
                stopTrader();
            if (tickProvider.getWorkingState() != WorkingState.STOPPED)
                stopMd();
            // Change state.
            if (orderProvider.getWorkingState() == WorkingState.STOPPED
                    && tickProvider.getWorkingState() == WorkingState.STOPPED) {
                this.workingState = WorkingState.STOPPED;
                global.getLogger().info("platform stopped");
            } else {
                global.getLogger().warning("platform doesn't stop");
            }
        }

        private void checkPerformance() {
            var m = global.getPerformanceMeasure().getAllMeasures();
            for (var entry : m.entrySet()) {
                var ms = entry.getValue().toMillis();
                global.getLogger().info(
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
                global.getLogger().severe(th.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        try {
            var platform = new Platform();
            platform.start(args);
            platform.task();
            new CountDownLatch(1).await();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
