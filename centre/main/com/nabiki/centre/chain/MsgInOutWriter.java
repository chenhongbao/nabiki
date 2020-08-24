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

package com.nabiki.centre.chain;

import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.iop.IOPSession;
import com.nabiki.iop.Message;
import com.nabiki.iop.x.OP;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MsgInOutWriter {
    private final Config config;
    private final Path inDir, outDir;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    MsgInOutWriter(Config cfg) {
        this.config = cfg;
        this.inDir = getPath(cfg, "dir.flow.client_in");
        this.outDir = getPath(cfg, "dir.flow.client_out");
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

    private Path getClientDir(Path root, IOPSession session) {
        return Path.of(root.toString(), getUserID(session));
    }

    private String getUserID(IOPSession session) {
        var id = session.getAttribute(UserLoginManager.FRONT_USERID_KEY);
        return id == null ? "null" : (String)id;
    }

    void writeOut(Message out, IOPSession session) {
        write(OP.toJson(out), ensureFile(getClientDir(this.outDir, session),
                out.Type + "." + LocalDateTime.now().format(this.formatter) + "." + out.RequestID + ".json"));
    }

    void writeIn(Message in, IOPSession session) {
        write(OP.toJson(in), ensureFile(getClientDir(this.inDir, session),
                in.Type + "." + LocalDateTime.now().format(this.formatter) + "." + in.RequestID + ".json"));
    }
}
