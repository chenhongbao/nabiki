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
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.iop.IOP;
import com.nabiki.iop.x.SystemStream;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
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
        try {
            System.loadLibrary("thostmduserapi_se");
            System.loadLibrary("thosttraderapi_se");
            System.loadLibrary("thostctpapi_se-6.3.19-P1");
        } catch (Throwable th) {
            writeLine(th.getMessage());
            var cause = th.getCause();
            while (cause != null) {
                writeLine(cause.getMessage());
                cause = cause.getCause();
            }
        }
    }

    private static void writeLine(String msg) {
        try (FileWriter fw = new FileWriter(
                new File("urgent.log"), true)) {
            fw.write(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            fw.write("\t");
            fw.write(msg);
            fw.write(System.lineSeparator());
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    Global global;
    CandleEngine candleEngine;
    OrderProvider orderProvider;
    TickProvider tickProvider;
    UserAuthManager authMgr;
    ActiveUserManager userMgr;

    final MarketDataRouter router;
    final Timer timerPlat;
    final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

    Platform() {
        this.timerPlat = new Timer();
        this.router = new MarketDataRouter();
    }

    private void providers() {
        // Set order provider.
        this.orderProvider = new OrderProvider(this.global);
        // Prepare candle engine.
        this.candleEngine = new CandleEngine(this.global);
        this.candleEngine.registerRouter(this.router);
        // Set tick provider.
        this.tickProvider = new TickProvider(this.global);
        this.tickProvider.register(this.candleEngine);
        this.tickProvider.register(this.router);
    }

    private void server(String host, int port) throws IOException {
        // Server.
        var server = IOP.createServer();
        StaticChainInstaller.install(
                server, this.authMgr, this.userMgr, router, global);
        server.bind(new InetSocketAddress(host, port));
    }

    private void managers() {
        // Set auth/user manager.
        var userDir = global.getRootDirectory()
                .recursiveGet("dir.user")
                .iterator()
                .next()
                .path();
        this.authMgr = new UserAuthManager(userDir);
        this.userMgr = new ActiveUserManager(
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
                new PlatformTask(this, this.global),
                MILLIS - System.currentTimeMillis() % MILLIS,
                MILLIS);
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
