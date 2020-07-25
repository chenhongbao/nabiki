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

import com.nabiki.centre.active.ActiveUserManager;
import com.nabiki.centre.chain.StaticChainInstaller;
import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.ctp.TickProvider;
import com.nabiki.centre.ctp.WorkingState;
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.ConfigLoader;
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

    private Config config;
    private OrderProvider orderProvider;
    private TickProvider tickProvider;
    private UserAuthManager authManager;
    private ActiveUserManager userManager;

    private final MarketDataRouter router;
    private final Timer timer;
    private final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

    Platform() {
        this.timer = new Timer();
        this.router = new MarketDataRouter();
    }

    private void providers() {
        // Set order provider.
        this.orderProvider = new OrderProvider(this.config);
        this.orderProvider.initialize();
        // Set tick provider.
        // Prepare candle engine.
        var candleEngine = new CandleEngine(this.config);
        candleEngine.registerRouter(this.router);
        this.tickProvider = new TickProvider(this.config);
        this.tickProvider.register(candleEngine);
        this.tickProvider.register(this.router);
        this.tickProvider.initialize();
    }

    private void server(String host, int port) throws IOException {
        // Server.
        var server = IOP.createServer();
        StaticChainInstaller.install(
                server, this.authManager, this.userManager, router, config);
        server.bind(new InetSocketAddress(host, port));
    }

    private void managers() {
        // Set auth/user manager.
        var userDir = config.getRootDirectory()
                .recursiveGet("dir.user")
                .iterator()
                .next()
                .path();
        this.authManager = new UserAuthManager(userDir);
        this.userManager = new ActiveUserManager(
                orderProvider,
                config,
                userDir);
    }

    private void system() throws IOException {
        // Set system's output and error output.
        var dir = this.config.getRootDirectory()
                .recursiveGet("dir.log")
                .iterator()
                .next();
        SystemStream.setErr(dir.setFile(
                "file.log.err", "system.err.log").file());
        SystemStream.setOut(dir.setFile
                ("file.log.out", "system.out.log").file());
    }

    public void start(String[] args) throws IOException {
        // Set config.
        ConfigLoader.rootPath = getArgument(args, "--root");
        this.config = ConfigLoader.config();
        system();
        providers();
        managers();
        server(getArgument(args, "--host"),
                getIntArgument(args, "--port"));
    }

    public void task() {
        this.timer.scheduleAtFixedRate(
                new PlatformTask(),
                MILLIS - System.currentTimeMillis() % MILLIS,
                MILLIS);
    }

    private class PlatformTask extends TimerTask {
        private final LocalTime start0 = LocalTime.of(20, 30),
                start1 = LocalTime.of(8, 30),
                stop0 = LocalTime.of(3, 0),
                stop1 = LocalTime.of(15, 45);
        private final LocalTime renewTime = LocalTime.of(20, 0),
                settleTime = LocalTime.of(16, 0);

        private WorkingState workingState = WorkingState.STOPPED;
        private UserState userState = UserState.SETTLED;

        PlatformTask() {
        }

        private boolean needRenew() {
            return this.workingState == WorkingState.STARTED
                    && this.userState == UserState.SETTLED;
        }

        private boolean needSettle() {
            return this.workingState == WorkingState.STOPPED
                    && this.userState == UserState.RENEW;
        }

        private boolean needStart() {
            var day = LocalDate.now().getDayOfWeek();
            var time = LocalTime.now();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY)
                return false;
            return ((time.isAfter(this.start0) || time.isBefore(this.stop0)) ||
                    (time.isAfter(this.start1) && time.isBefore(stop1)))
                    && this.workingState == WorkingState.STOPPED;
        }

        private boolean needStop() {
            var time = LocalTime.now();
            return ((time.isAfter(this.stop0) && time.isBefore(this.start1)) ||
                    (time.isAfter(this.stop1) && time.isBefore(this.start0)))
                    && this.workingState == WorkingState.STARTED;
        }

        private void renew() {
            try {
                authManager.renew();
                userManager.renew();
                ConfigLoader.config();
                this.userState = UserState.RENEW;
            } catch (Exception e) {
                config.getLogger().severe("renew failed");
            }
        }

        private void settle() {
            try {
                authManager.settle();
                userManager.settle();
                this.userState = UserState.SETTLED;
            } catch (Exception e) {
                config.getLogger().severe("settle failed");
            }
        }

        private void start() {
            this.workingState = WorkingState.STARTING;
            // Trader logins.
            orderProvider.login();
            int count = 0;
            while (!orderProvider.waitLastInstrument(
                    TimeUnit.MINUTES.toMillis(1)) && ++count <= 10) {
                config.getLogger()
                        .info("wait query instrument(" + count + ")");
            }
            if (orderProvider.getWorkingState() != WorkingState.STARTED) {
                config.getLogger().severe(
                        "trader didn't start up");
                return;
            }
            if (count > 10)
                config.getLogger().warning(
                        "trader didn't finish querying instruments");
            // Md logins.
            tickProvider.login();
            count = 0;
            while (!tickProvider.waitLogin(
                    TimeUnit.MINUTES.toMillis(1)) && count++ < 10) {
                config.getLogger().info(
                        "wait md login(" + count + ")");
            }
            if (tickProvider.getWorkingState() != WorkingState.STARTED) {
                config.getLogger().severe(
                        "market data didn't start up");
                return;
            }
            tickProvider.subscribe(orderProvider.getInstruments());
            this.workingState = WorkingState.STARTED;
        }

        private void stop() {
            this.workingState = WorkingState.STOPPING;
            orderProvider.logout();
            tickProvider.logout();
            this.workingState = WorkingState.STOPPED;
        }

        @Override
        public void run() {
            if (needStart())
                start();
            if (needStop())
                stop();
            if (needRenew())
                renew();
            if (needSettle())
                settle();
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
