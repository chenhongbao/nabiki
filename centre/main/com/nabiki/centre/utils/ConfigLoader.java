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

package com.nabiki.centre.utils;

import com.nabiki.centre.utils.plain.InstrumentInfo;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.centre.utils.plain.TradingHourConfig;
import com.nabiki.iop.x.OP;
import com.nabiki.objects.CDepthMarketData;
import com.nabiki.objects.CInstrument;
import com.nabiki.objects.CInstrumentCommissionRate;
import com.nabiki.objects.CInstrumentMarginRate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ConfigLoader {
    public static String rootPath;

    static AtomicBoolean configLoaded = new AtomicBoolean(false);
    final static Config config = new Config();

    /**
     * Get single {@link com.nabiki.centre.utils.Config} instance. If the instance
     * exists, the method first clears the internal data then initializes again.
     *
     * @return instance of {@link com.nabiki.centre.utils.Config}
     * @throws IOException fail to read or process configuration files, or content
     *                     in the configuration file is corrupted or invalid
     */
    public static Config config() throws IOException {
        synchronized (config) {
            // Clear old config.
            if (configLoaded.get())
                clearConfig();
            loadConfig();
        }
        return config;
    }

    public static void setDepthMarketData(CDepthMarketData md) {
        config.depths.put(md.InstrumentID, md);
    }

    public static void setTradingDay(String day) {
        config.tradingDay = day;
    }

    public static void setInstrConfig(CInstrument instr) {
        synchronized (config.instrInfo) {
            if (!config.instrInfo.containsKey(instr.InstrumentID))
                config.instrInfo.put(instr.InstrumentID, new InstrumentInfo());
            config.instrInfo.get(instr.InstrumentID).Instrument = instr;
        }
        // Set the product and its instruments.
        var pid = Utils.getProductID(instr.InstrumentID);
        synchronized (config.products) {
            if (!config.products.containsKey(pid))
                config.products.put(pid, new HashSet<>());
            config.products.get(pid).add(instr.InstrumentID);
        }
    }

    private static void setProductConfig(
            CInstrumentMarginRate margin,
            String productID) {
        var instruments = config.getProduct(productID);
        if (instruments == null)
            return;
        for (var instrument : instruments) {
            var m = Utils.deepCopy(margin);
            if (m != null) {
                m.InstrumentID = instrument;
                setSingleConfig(m);
            }
        }
    }

    public static void setSingleConfig(
            CInstrumentMarginRate margin) {
        synchronized (config.instrInfo) {
            if (!config.instrInfo.containsKey(margin.InstrumentID))
                config.instrInfo.put(margin.InstrumentID, new InstrumentInfo());
            config.instrInfo.get(margin.InstrumentID).Margin = margin;
        }
    }

    public static void setInstrConfig(
            CInstrumentMarginRate margin) {
        var pid = Utils.getProductID(margin.InstrumentID);
        if (pid != null) {
            if (pid.compareTo(margin.InstrumentID) == 0)
                setProductConfig(margin, pid);
            else
                setSingleConfig(margin);
        }
    }

    private static void setProductConfig(
            CInstrumentCommissionRate commission,
            String productID) {
        var instruments = config.getProduct(productID);
        if (instruments == null)
            return;
        for (var instrument : instruments) {
            // The commission has product ID as instrument, so I need to get all
            // instruments under the product, and set one by one.
            // They must not share the same commission instance.
            var c = Utils.deepCopy(commission);
            if (c != null) {
                c.InstrumentID = instrument;
                setSingleConfig(c);
            }
        }
    }

    private static void setSingleConfig(
            CInstrumentCommissionRate commission) {
        synchronized (config.instrInfo) {
            if (!config.instrInfo.containsKey(commission.InstrumentID))
                config.instrInfo.put(commission.InstrumentID, new InstrumentInfo());
            config.instrInfo.get(commission.InstrumentID).Commission = commission;
        }
    }

    public static void setInstrConfig(
            CInstrumentCommissionRate commission) {
        var pid = Utils.getProductID(commission.InstrumentID);
        if (pid != null) {
            if (pid.compareTo(commission.InstrumentID) == 0)
                setProductConfig(commission, pid);
            else
                setSingleConfig(commission);
        }
    }

    private static void setInstrConfig() {
        var dirs = config.getRootDirectory()
                .recursiveGet("dir.flow.info");
        if (dirs.size() == 0)
            return;
        // Instrument are all ready with correct instrument ID.
        for (var d : dirs) {
            d.path().toFile().listFiles(file -> {
                try {
                    if (file.getName().startsWith("instrument")) {
                        setInstrConfig(OP.fromJson(
                                Utils.readText(file, StandardCharsets.UTF_8),
                                CInstrument.class));
                    }
                } catch (IOException e) {
                    config.getLogger().warning(
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
                    if (file.getName().startsWith("commission")) {
                        setInstrConfig(OP.fromJson(
                                Utils.readText(file, StandardCharsets.UTF_8),
                                CInstrumentCommissionRate.class));
                    } else if (file.getName().startsWith("margin")) {
                        setInstrConfig(OP.fromJson(
                                Utils.readText(file, StandardCharsets.UTF_8),
                                CInstrumentMarginRate.class));
                    }
                } catch (IOException e) {
                    config.getLogger().warning(
                            Utils.formatLog("failed instr config",
                                    null, e.getMessage(), null));
                }
                return false;
            });
        }
    }

    /*
     Only clear the config-defined settings. For those updated in runtime, don't
     clear them.
     */
    private static void clearConfig() {
        config.tradingHour.clear();
        config.login.clear();
    }

    private static void loadConfig() throws IOException {
        // First create dirs, then logger.
        setDirectories();
        setLogger();
        // Config below uses logger to keep error info.
        setLoginConfig();
        setTradingHourConfig();
        setInstrConfig();
        // Set mark.
        configLoaded.set(true);
    }

    private static void setTradingHourConfig() throws IOException {
        var s = config.getRootDirectory().recursiveGet("dir.cfg.hour");
        if (s.size() == 0)
            throw new IOException("directory for trading hour configs not found");
        // Iterate over all dirs.
        for (var cfg : s) {
            cfg.file().listFiles(file -> {
                try {
                    if (!file.isFile() || file.length() == 0
                            || !file.getName().endsWith(".json"))
                        return false;
                    var c = OP.fromJson(
                            Utils.readText(file, StandardCharsets.UTF_8),
                            TradingHourConfig.class);
                    // Not null.
                    Objects.requireNonNull(c);
                    Objects.requireNonNull(c.TradingHour);
                    Objects.requireNonNull(c.ProductID);
                    // Empty config.
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
                    // Save mapping into config.
                    // All product IDs are lower case.
                    for (var p : c.ProductID)
                        config.tradingHour.put(p, h);
                } catch (IOException | NullPointerException e) {
                    config.getLogger().warning(
                            Utils.formatLog("failed trading hour config",
                                    null, e.getMessage(), null));
                }
                return false;
            });
        }
        // Write sample config.
        if (config.tradingHour.size() == 0) {
            var cfg = s.iterator().next();
            cfg.setFile("cfg.hour.sample", "hour.sample.json");
            Utils.writeText(OP.toJson(new TradingHourConfig()),
                    cfg.get("cfg.hour.sample").file(), StandardCharsets.UTF_8,
                    false);
        } else {
            // Set hour keepers.
            for (var keeper : config.tradingHour.values()) {
                for (var du : config.durations)
                    keeper.sample(du);
            }
        }
    }

    private static void setLoginConfig() throws IOException {
        var s = config.getRootDirectory().recursiveGet("dir.cfg.login");
        if (s.size() == 0)
            throw new IOException("directory for login configs not found");
        // Iterate over all files under dir.
        for (var cfg : s) {
            cfg.file().listFiles(file -> {
                try {
                    if (!file.isFile() || file.length() == 0
                            || !file.getName().endsWith(".json"))
                        return false;
                    var c = OP.fromJson(
                            Utils.readText(file, StandardCharsets.UTF_8),
                            LoginConfig.class);
                    config.login.put(c.Name, c);
                } catch (IOException e) {
                    config.getLogger().warning(
                            Utils.formatLog("failed login config",
                                    null, e.getMessage(), null));
                }
                return false;
            });
        }
        // Write a configuration sample.
        if (config.login.size() == 0) {
            var cfg = s.iterator().next();
            cfg.setFile("cfg.login.sample", "login.sample.json");
            Utils.writeText(OP.toJson(new LoginConfig()),
                    cfg.get("cfg.login.sample").file(), StandardCharsets.UTF_8,
                    false);
        }
    }

    private static void setDirectories() throws IOException {
        if (rootPath == null)
            rootPath = "";
        var root = new EasyFile(rootPath, false);
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
        flow.setDirectory("dir.flow.err", ".err");
        flow.setDirectory("dir.flow.info", ".info");

        // Set config.
        config.rootDirectory = root;
    }

    private static void setLogger() {
        if (Config.logger == null) {
            // Get logging directory.
            String fp;
            var iter = config.getRootDirectory().recursiveGet("dir.log")
                    .iterator();
            if (!iter.hasNext())
                fp = "system.log";
            else
                fp = Path.of(iter.next().path().toString(), "system.log")
                        .toAbsolutePath().toString();

            try {
                // File and format.
                var fh = new FileHandler(fp, true);
                fh.setFormatter(new SimpleFormatter());
                // Get logger with config's name.
                Config.logger = Logger.getLogger(Config.class.getCanonicalName());
                Config.logger.addHandler(fh);
                Config.logger.setUseParentHandlers(false);
            } catch (IOException e) {
                Config.logger = Logger.getGlobal();
            }
        }
    }
}
