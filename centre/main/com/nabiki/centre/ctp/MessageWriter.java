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

package com.nabiki.centre.ctp;

import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.x.OP;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MessageWriter {
    private final Config config;
    private final Path reqDir, rtnDir, rspDir, errDir;
    private final DateTimeFormatter formatter
            = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSSSSS");

    public MessageWriter(Config cfg) {
        this.config = cfg;
        this.reqDir = getPath(cfg, "dir.flow.req");
        this.rtnDir = getPath(cfg, "dir.flow.rtn");
        this.rspDir = getPath(cfg, "dir.flow.rsp");
        this.errDir = getPath(cfg, "dir.flow.err");
    }

    private Path getPath(Config cfg, String key) {
        var dirs = cfg.getRootDirectory().recursiveGet(key);
        if (dirs.size() > 0)
            return dirs.iterator().next().path();
        else
            return Path.of("");
    }

    private void write(String text, File file) {
        try {
            Utils.writeText(text, file, StandardCharsets.UTF_8, false);
        } catch (IOException e) {
            this.config.getLogger()
                    .warning(file.toString() + ". " + e.getMessage());
        }
    }

    private File ensureFile(Path root, String fn) {
        try {
            var r = Path.of(root.toAbsolutePath().toString(), fn);
            Utils.createFile(root, true);
            Utils.createFile(r, false);
            return r.toFile();
        } catch (IOException e) {
            this.config.getLogger().warning(root.toString() + "/" + fn + ". "
                    + e.getMessage());
            return new File(".failover");
        }
    }

    private String getTimeStamp() {
        return LocalDateTime.now().format(this.formatter);
    }

    public void writeRtn(CThostFtdcOrderField rtn) {
        write(OP.toJson(rtn),
                ensureFile(this.rtnDir,
                        "order." + getTimeStamp() + ".json"));
    }

    public void writeRtn(CThostFtdcTradeField rtn) {
        write(OP.toJson(rtn),
                ensureFile(this.rtnDir,
                        "trade." + getTimeStamp() + ".json"));
    }

    public void writeReq(CThostFtdcInputOrderField req) {
        write(OP.toJson(req),
                ensureFile(this.reqDir,
                        "inputorder." + getTimeStamp() + ".json"));
    }

    public void writeReq(CThostFtdcInputOrderActionField req) {
        write(OP.toJson(req),
                ensureFile(this.reqDir,
                        "action." + getTimeStamp() + ".json"));
    }

    public void writeRsp(CThostFtdcInstrumentMarginRateField rsp) {
        write(OP.toJson(rsp),
                ensureFile(this.rspDir,
                        "margin." + getTimeStamp() + ".json"));
    }

    public void writeRsp(CThostFtdcInstrumentCommissionRateField rsp) {
        write(OP.toJson(rsp),
                ensureFile(this.rspDir,
                        "commission." + getTimeStamp() + ".json"));
    }

    public void writeRsp(CThostFtdcInstrumentField rsp) {
        write(OP.toJson(rsp),
                ensureFile(this.rspDir,
                        "instrument." + getTimeStamp() + ".json"));
    }

    public void writeErr(CThostFtdcOrderActionField err) {
        write(OP.toJson(err),
                ensureFile(this.errDir,
                        "orderaction." + getTimeStamp() + ".json"));
    }

    public void writeErr(CThostFtdcInputOrderActionField err) {
        write(OP.toJson(err),
                ensureFile(this.errDir,
                        "action." + getTimeStamp() + ".json"));
    }

    public void writeErr(CThostFtdcInputOrderField err) {
        write(OP.toJson(err),
                ensureFile(this.errDir,
                        "inputorder." + getTimeStamp() + ".json"));
    }

    public void writeErr(CThostFtdcRspInfoField err) {
        write(OP.toJson(err),
                ensureFile(this.errDir,
                        "info." + getTimeStamp() + ".json"));
    }
}
