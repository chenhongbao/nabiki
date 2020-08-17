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

package com.nabiki.centre.utils.plain;

import com.nabiki.centre.utils.Utils;
import com.nabiki.iop.x.OP;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedList;

public class TradingHourConfigTest {
    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

    TradingHourConfig.SingleTradingHour hour(String from, String to) {
        var h = new TradingHourConfig.SingleTradingHour();
        h.From = LocalTime.parse(from, formatter);
        h.To = LocalTime.parse(to, formatter);
        return h;
    }

    private void setName(TradingHourConfig cfg, String n) {
        cfg.Name = n;
    }

    private void setProducts(TradingHourConfig cfg, String... p) {
        cfg.ProductID = new LinkedList<>();
        cfg.ProductID.addAll(Arrays.asList(p));
    }

    private void setHours(
            TradingHourConfig cfg,
            TradingHourConfig.SingleTradingHour hour) {
        if (cfg.TradingHour == null)
            cfg.TradingHour = new LinkedList<>();
        cfg.TradingHour.add(hour);
    }

    private void write(Path dir, TradingHourConfig cfg) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            Files.createDirectories(dir);
        var f = new File(dir.toString() + "/hour." + cfg.Name + ".json");
        Utils.writeText(OP.toJson(cfg), f, StandardCharsets.UTF_8, false);
    }

    @Test
    public void basic() {
        var dir = Paths.get("C:\\Users\\chenh\\Desktop\\.root\\.cfg\\.hour");

        var cfg = new TradingHourConfig();
        setName(cfg, "p0");
        setProducts(cfg,
                "eb", "j", "jm", "i", "eg",
                "l", "v", "pp", "c", "cs",
                "a", "b", "p", "y", "m",
                "rb", "hc", "fu", "bu", "ru",
                "sp", "rr", "nr", "FG", "ZC",
                "TA", "MA", "SA", "RM", "OI",
                "CF", "SR", "CY");
        setHours(cfg, hour("21:00", "23:00"));
        setHours(cfg, hour("09:00", "10:15"));
        setHours(cfg, hour("10:30", "11:30"));
        setHours(cfg, hour("13:30", "15:00"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        cfg = new TradingHourConfig();
        setName(cfg, "p1");
        setProducts(cfg,
                "ss", "pb", "ni", "sn", "cu", "al", "zn");
        setHours(cfg, hour("21:00", "01:00"));
        setHours(cfg, hour("09:00", "10:15"));
        setHours(cfg, hour("10:30", "11:30"));
        setHours(cfg, hour("13:30", "15:00"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        cfg = new TradingHourConfig();
        setName(cfg, "p2");
        setProducts(cfg,
                "au", "ag", "sc");
        setHours(cfg, hour("21:00", "02:30"));
        setHours(cfg, hour("09:00", "10:15"));
        setHours(cfg, hour("10:30", "11:30"));
        setHours(cfg, hour("13:30", "15:00"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        cfg = new TradingHourConfig();
        setName(cfg, "p3");
        setProducts(cfg,
                "pg", "wr", "jd", "bb", "fb",
                "WH", "RI", "LR", "JR", "RS",
                "SF", "SM", "AP", "PM", "CJ",
                "UR", "lu");
        setHours(cfg, hour("09:00", "10:15"));
        setHours(cfg, hour("10:30", "11:30"));
        setHours(cfg, hour("13:30", "15:00"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        cfg = new TradingHourConfig();
        setName(cfg, "p4");
        setProducts(cfg,
                "IC", "IH", "IF");
        setHours(cfg, hour("09:30", "11:30"));
        setHours(cfg, hour("13:00", "15:00"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }

        cfg = new TradingHourConfig();
        setName(cfg, "p5");
        setProducts(cfg,
                "T", "TF", "TS");
        setHours(cfg, hour("09:30", "11:30"));
        setHours(cfg, hour("13:00", "15:15"));

        try {
            write(dir, cfg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}