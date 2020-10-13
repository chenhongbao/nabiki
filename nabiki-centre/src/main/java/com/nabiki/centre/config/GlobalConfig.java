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

package com.nabiki.centre.config;

import com.nabiki.centre.config.plain.InstrumentInfo;
import com.nabiki.centre.config.plain.LoginConfig;
import com.nabiki.centre.config.plain.TradingHourConfig;
import com.nabiki.commons.ctpobj.CDepthMarketData;
import com.nabiki.commons.ctpobj.CInstrument;
import com.nabiki.commons.ctpobj.CInstrumentCommissionRate;
import com.nabiki.commons.ctpobj.CInstrumentMarginRate;
import com.nabiki.commons.utils.EasyFile;
import com.nabiki.commons.utils.Performance;
import com.nabiki.commons.utils.SocketLoggingHandler;
import com.nabiki.commons.utils.Utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class GlobalConfig {
  public static String ROOT_PATH;

  static AtomicBoolean configLoaded = new AtomicBoolean(false);
  final static Global GLOBAL = new Global();

  /**
   * Get single {@link Global} instance. If the instance
   * exists, the method first clears the internal data then initializes again.
   *
   * @return instance of {@link Global}
   * @throws IOException fail to read or process configuration files, or content
   *                     in the configuration file is corrupted or invalid
   */
  public static Global config() throws IOException {
    synchronized (GLOBAL) {
      // Clear old GLOBAL.
      if (configLoaded.get())
        clearConfig();
      loadConfig();
    }
    return GLOBAL;
  }

  public static void setArgument(String prefix, String argument) {
    GLOBAL.args.put(prefix, argument);
  }

  public static void setDepthMarketData(CDepthMarketData md) {
    GLOBAL.depths.put(md.InstrumentID, md);
  }

  public static void setTradingDay(String day) {
    GLOBAL.tradingDay = day;
  }

  public static void resetInstrConfig(Collection<CInstrument> instruments) {
    if (instruments == null || instruments.size() == 0)
      return;
    // If it's first run, just set all instruments.
    // Otherwise, need to filter obsolete instruments.
    if (GLOBAL.getAllInstrInfo().size() == 0) {
      for (var i : instruments) {
        setInstrumentConfig(i);
      }
    } else {
      var m = new HashMap<String, InstrumentInfo>();
      for (var i : instruments) {
        var info = GLOBAL.instrInfo.get(i.InstrumentID);
        if (info == null) {
          info = new InstrumentInfo();
          info.Instrument = i;
        }
        m.put(i.InstrumentID, info);
      }
      synchronized (GLOBAL.instrInfo) {
        GLOBAL.instrInfo.clear();
        GLOBAL.instrInfo.putAll(m);
      }
    }
  }

  public static void setInstrumentConfig(CInstrument instr) {
    synchronized (GLOBAL.instrInfo) {
      if (!GLOBAL.instrInfo.containsKey(instr.InstrumentID)) {
        GLOBAL.instrInfo.put(instr.InstrumentID, new InstrumentInfo());
      }
      GLOBAL.instrInfo.get(instr.InstrumentID).Instrument = instr;
    }
    // Set the product and its instruments.
    synchronized (GLOBAL.products) {
      if (!GLOBAL.products.containsKey(instr.ProductID)) {
        GLOBAL.products.put(instr.ProductID, new HashSet<>());
      }
      GLOBAL.products.get(instr.ProductID).add(instr.InstrumentID);
    }
  }

  public static void setMarginConfig(
      CInstrumentMarginRate margin) {
    synchronized (GLOBAL.instrInfo) {
      if (!GLOBAL.instrInfo.containsKey(margin.InstrumentID))
        GLOBAL.instrInfo.put(margin.InstrumentID, new InstrumentInfo());
      GLOBAL.instrInfo.get(margin.InstrumentID).Margin = margin;
    }
  }

  public static void setCommissionConfig(
      CInstrumentCommissionRate commission) {
    synchronized (GLOBAL.instrInfo) {
      if (!GLOBAL.instrInfo.containsKey(commission.InstrumentID)) {
        GLOBAL.instrInfo.put(commission.InstrumentID, new InstrumentInfo());
      }
      GLOBAL.instrInfo.get(commission.InstrumentID).Commission = commission;
    }
  }

  private static void setInstrInfoConfig() {
    var dirs = GLOBAL.getRootDirectory()
        .recursiveGet("dir.flow.info");
    if (dirs.size() == 0)
      return;
    // Instrument are all ready with correct instrument ID.
    for (var d : dirs) {
      d.path().toFile().listFiles(file -> {
        try {
          if (file.getName().startsWith("Instrument.")) {
            setInstrumentConfig(Utils.fromJson(
                Utils.readText(file, StandardCharsets.UTF_8),
                CInstrument.class));
          }
        } catch (IOException e) {
          GLOBAL.getLogger().warning(
              Utils.formatLog("failed instr config",
                  null, e.getMessage(), null));
        }
        return false;
      });
    }
    // Some rate's info has product ID as instrument ID, and it needs to know
    // all instrument ID before setting the data.
    // So loading margin and commission after loading instruments.
    for (var d : dirs) {
      d.path().toFile().listFiles(file -> {
        try {
          if (file.getName().startsWith("Commission.")) {
            setCommissionConfig(Utils.fromJson(
                Utils.readText(file, StandardCharsets.UTF_8),
                CInstrumentCommissionRate.class));
          } else if (file.getName().startsWith("Margin.")) {
            setMarginConfig(Utils.fromJson(
                Utils.readText(file, StandardCharsets.UTF_8),
                CInstrumentMarginRate.class));
          }
        } catch (IOException e) {
          GLOBAL.getLogger().warning(
              Utils.formatLog("failed instr config",
                  null, e.getMessage(), null));
        }
        return false;
      });
    }
  }

  /*
   Only clear the GLOBAL-defined settings. For those updated in runtime, don't
   clear them.
   */
  private static void clearConfig() {
    GLOBAL.tradingHour.clear();
    GLOBAL.login.clear();
  }

  private static void loadConfig() throws IOException {
    // First create dirs, then logger.
    setDirectories();
    setLogger();
    setPerformanceMeasure();
    // Global below uses logger to keep error info.
    setLoginConfig();
    setTradingHourConfig();
    setInstrInfoConfig();
    // Set mark.
    configLoaded.set(true);
  }

  private static void setPerformanceMeasure() {
    GLOBAL.performance = new Performance();
  }

  private static void setTradingHourConfig() throws IOException {
    var s = GLOBAL.getRootDirectory().recursiveGet("dir.cfg.hour");
    if (s.size() == 0)
      throw new IOException("directory for trading hour configs not found");
    // Iterate over all dirs.
    for (var cfg : s) {
      cfg.file().listFiles(file -> {
        try {
          if (!file.isFile() || file.length() == 0
              || !file.getName().endsWith(".json"))
            return false;
          var c = Utils.fromJson(
              Utils.readText(file, StandardCharsets.UTF_8),
              TradingHourConfig.class);
          // Not null.
          Objects.requireNonNull(c);
          Objects.requireNonNull(c.TradingHour);
          Objects.requireNonNull(c.ProductID);
          // Empty GLOBAL.
          if (c.TradingHour.size() == 0 || c.ProductID.size() == 0)
            return false;
          // Prepare parameters to construct keeper.
          var index = 0;
          var hours = new TradingHourKeeper
              .TradingHour[c.TradingHour.size()];
          for (var hour : c.TradingHour)
            hours[index++] = new TradingHourKeeper
                .TradingHour(hour.From, hour.To);
          var h = new TradingHourKeeper(hours);
          // Save mapping into GLOBAL.
          // All product IDs are lower case.
          for (var p : c.ProductID)
            GLOBAL.tradingHour.put(p, h);
        } catch (IOException | NullPointerException e) {
          GLOBAL.getLogger().warning(
              Utils.formatLog("failed trading hour GLOBAL",
                  null, e.getMessage(), null));
        }
        return false;
      });
    }
    // Write sample GLOBAL.
    if (GLOBAL.tradingHour.size() == 0) {
      var cfg = s.iterator().next();
      var p = Path.of(cfg.file().getAbsolutePath(), "hour.sample.json");
      Utils.writeText(
          Utils.toJson(new TradingHourConfig()), p.toFile(), StandardCharsets.UTF_8, false);
    } else {
      // Set hour keepers.
      for (var keeper : GLOBAL.tradingHour.values()) {
        for (var du : GLOBAL.durations)
          keeper.sample(du);
      }
    }
  }

  private static void setLoginConfig() throws IOException {
    var s = GLOBAL.getRootDirectory().recursiveGet("dir.cfg.login");
    if (s.size() == 0)
      throw new IOException("directory for login configs not found");
    // Iterate over all files under dir.
    for (var cfg : s) {
      cfg.file().listFiles(file -> {
        try {
          if (!file.isFile() || file.length() == 0
              || !file.getName().endsWith(".json"))
            return false;
          var c = Utils.fromJson(
              Utils.readText(file, StandardCharsets.UTF_8),
              LoginConfig.class);
          GLOBAL.login.put(c.Name, c);
        } catch (IOException e) {
          GLOBAL.getLogger().warning(
              Utils.formatLog("failed login GLOBAL",
                  null, e.getMessage(), null));
        }
        return false;
      });
    }
    // Write a configuration sample.
    if (GLOBAL.login.size() == 0) {
      var cfg = s.iterator().next();
      var p = Path.of(cfg.file().getAbsolutePath(), "login.sample.json");
      Utils.writeText(
          Utils.toJson(new LoginConfig()), p.toFile(), StandardCharsets.UTF_8, false);
    }
  }

  private static void setDirectories() throws IOException {
    if (ROOT_PATH == null)
      ROOT_PATH = "";
    var root = new EasyFile(ROOT_PATH, false);
    root.setDirectory("dir.cfg", ".cfg");
    root.setDirectory("dir.flow", ".flow");
    root.setDirectory("dir.cdl", ".cdl");
    root.setDirectory("dir.log", ".log");
    root.setDirectory("dir.user", ".user");

    var cfg = root.get("dir.cfg");
    cfg.setDirectory("dir.cfg.login", ".login");
    cfg.setDirectory("dir.cfg.hour", ".hour");

    var flow = root.get("dir.flow");
    flow.setDirectory("dir.flow.ctp", ".ctp");
    flow.setDirectory("dir.flow.req", ".req");
    flow.setDirectory("dir.flow.rtn", ".rtn");
    flow.setDirectory("dir.flow.rsp", ".rsp");
    flow.setDirectory("dir.flow.client_in", ".client_in");
    flow.setDirectory("dir.flow.client_out", ".client_out");
    flow.setDirectory("dir.flow.err", ".err");
    flow.setDirectory("dir.flow.info", ".info");

    // Set GLOBAL.
    GLOBAL.rootDirectory = root;
  }

  private static void setLogger() {
    if (Global.logger == null) {
      // Get logging directory.
      String fp;
      var iter = GLOBAL.getRootDirectory().recursiveGet("dir.log").iterator();
      if (!iter.hasNext()) {
        fp = "system.log";
      } else {
        fp = Path.of(iter.next().path().toString(), "system.log")
            .toAbsolutePath().toString();
      }
      Global.logger = Logger.getLogger("com.nabiki.centre");
      Global.logger.setUseParentHandlers(false);
      try {
        // File handler and format.
        var fh = new FileHandler(fp, true);
        fh.setFormatter(new SimpleFormatter());
        Global.logger.addHandler(fh);
      } catch (Throwable th) {
        System.err.println("fail init file logging, " + th.getMessage());
      }
      try {
        // Socket handler.
        String listen = GLOBAL.getArgument(Global.CMD_LOGSVR_PREFIX);
        if (listen != null && listen.trim().length() > 0) {
          int idx = listen.indexOf(":");
          String host = null;
          int port;
          if (idx >= 0) {
            host = listen.substring(0, idx).trim();
            port = Integer.parseInt(listen.substring(listen.indexOf(":") + 1).trim());
          } else {
            port = Integer.parseInt(listen.trim());
          }
          Handler sh;
          if (host.length() > 0) {
            sh = new SocketLoggingHandler(host, port);
          } else {
            sh = new SocketLoggingHandler("localhost", port);
          }
          Global.logger.addHandler(sh);
        }
      } catch (Throwable th) {
        System.err.println("fail init socket logging, " + th.getMessage());
      }
    }
  }
}
