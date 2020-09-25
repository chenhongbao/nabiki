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

import com.nabiki.centre.chain.*;
import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.ctp.TickProvider;
import com.nabiki.centre.md.CandleEngine;
import com.nabiki.centre.md.CandleRW;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.GlobalConfig;
import com.nabiki.centre.utils.Utils;
import com.nabiki.iop.IOP;
import com.nabiki.iop.x.SystemStream;

import java.io.IOException;
import java.net.InetSocketAddress;
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
  private Global global;
  private OrderProvider orderProvider;
  private TickProvider tickProvider;
  private UserAuthManager authMgr;
  private ActiveUserManager userMgr;
  private ParkedRequestManager parkedReqMgr;

  private final MarketDataRouter router;
  private final Timer timerPlat;
  private final static long MILLIS = TimeUnit.MINUTES.toMillis(1);

  Platform() {
    this.timerPlat = new Timer();
    this.router = new MarketDataRouter();
  }

  OrderProvider getOrder() {
    return orderProvider;
  }

  TickProvider getTick() {
    return tickProvider;
  }

  UserAuthManager getAuth() {
    return authMgr;
  }

  ActiveUserManager getUsers() {
    return userMgr;
  }

  private void loadDlls() {
    try {
      System.loadLibrary("thostmduserapi_se");
      System.loadLibrary("thosttraderapi_se");
      System.loadLibrary("thostctpapi_se-6.3.19-P1");
    } catch (Throwable th) {
      th.printStackTrace();
      this.global.getLogger().severe(th.getMessage());
    }
  }

  private void providers() {
    // Prepare candle engine.
    var candleEngine = new CandleEngine(
        this.router,
        this.global);
    // Set order provider.
    this.orderProvider = new OrderProvider(
        candleEngine,
        this.global);
    // Set tick provider.
    this.tickProvider = new TickProvider(
        this.router,
        candleEngine,
        this.global);
  }

  private void server() throws IOException {
    String host = this.global.getArgument("--host");
    int port = Integer.parseInt(this.global.getArgument("--port"));
    // Server.
    var server = IOP.createServer();
    // Install candle writer.
    var rw = new CandleRW(global);
    router.addReceiver(rw);
    // Install login manager.
    server.setLoginManager(new UserLoginManager(authMgr, userMgr, global));
    // Install session adaptor.
    server.setSessionAdaptor(new SessionAdaptor(router, global));
    // Install adaptors.
    var chain = server.getAdaptorChain();
    chain.addAdaptor(new RequestValidator(parkedReqMgr, global));
    chain.addAdaptor(new RequestExecutor(global));
    chain.addAdaptor(new SubscriptionAdaptor(router, rw, global));
    chain.addAdaptor(new QueryAdaptor(global));
    // Install msg writer.
    // Create msg in/out writer.
    var msgWriter = new MsgInOutWriter(global);
    server.setMessageHandlerIn(new InputFromClientLogger(msgWriter));
    server.setMessageHandlerOut(new OutputToClientLogger(msgWriter));
    if (host == null || host.trim().length() == 0)
      server.bind(new InetSocketAddress(port));
    else
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
    this.parkedReqMgr = new ParkedRequestManager(
        orderProvider,
        userMgr,
        global);
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
    loadDlls();
  }

  private void setArguments(String[] args) {
    var prefix = new String[]{
        Global.CMD_ROOT_PREFIX,
        Global.CMD_HOST_PREFIX,
        Global.CMD_PORT_PREFIX,
        Global.CMD_START_NOW_PREFIX
    };
    for (var pre : prefix) {
      String arg = Utils.getArgument(args, pre);
      if (arg == null)
        arg = "";
      GlobalConfig.setArgument(pre, arg);
    }
  }

  private void initConfig(String root) throws IOException {
    if (root == null || root.trim().length() == 0)
      GlobalConfig.ROOT_PATH = "./";
    else
      GlobalConfig.ROOT_PATH = root;
    this.global = GlobalConfig.config();
  }

  public void start(String[] args) throws IOException {
    setArguments(args);
    initConfig(Utils.getArgument(args, Global.CMD_ROOT_PREFIX));
    system();
    providers();
    managers();
    server();
  }

  public void task() {
    this.timerPlat.scheduleAtFixedRate(
        new PlatformTask(this, this.global),
        MILLIS - System.currentTimeMillis() % MILLIS,
        MILLIS);
  }

  private static boolean needHelp(String[] args) {
    for (var arg : args)
      if (arg.compareTo("--help") == 0) {
        printHelp();
        return true;
      }
    return false;
  }

  private static void printHelp() {
    System.out.println("javaw[java] -Djava.library.path=<path-to-DLL> -jar <path-to-jar> --port <port> " +
        "[--root <root-dir>] [--host <address-to-listen>] [--start-now <true-or-false>]");
    System.out.println();
    System.out.println("<path-to-DLL>    Path to dynamic linked library required by the app.");
    System.out.println("                 It can be absolute path or relative path. Quoted with");
    System.out.println("                 (\") if necessary.");
    System.out.println("<path-to-jar>    Path to this app's jar.");
    System.out.println("<port>           Port to listen on.");
    System.out.println("<root-dir>       Path taken as root directory for all configuration");
    System.out.println("                 and files. Quoted with (\") if necessary. If this option");
    System.out.println("                 is undefined, it is the CWD of java application.");
    System.out.println("<address-to-listen>");
    System.out.println("                 The address that this server is bound to. If this");
    System.out.println("                 option is undefined, server is bound to any local");
    System.out.println("                 address, which usually includes both IPv4 and IPv6.");
    System.out.println("<true-or-false>  true for server to initiate resources right after it ");
    System.out.println("                 starts, and false for server to wait until market opens.");
    System.out.println("                 If this option is undefined, it is like false.");
  }

  public static void main(String[] args) {
    if (needHelp(args))
      return;
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
