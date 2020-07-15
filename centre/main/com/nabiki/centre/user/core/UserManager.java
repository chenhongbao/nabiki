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

package com.nabiki.centre.user.core;

import com.nabiki.centre.Renewable;
import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInvestorPositionDetailField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcTradingAccountField;
import com.nabiki.iop.x.OP;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager implements Renewable {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Path dataDir;

    // Default value.
    private SettlementPreparation prep = new SettlementPreparation();

    UserManager(Path dataDir) {
        Objects.requireNonNull(dataDir, "user data directory null");
        this.dataDir = dataDir;
    }

    private void init(Path dir) {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            throw new IllegalArgumentException("user account root not exist");
        dir.toFile().listFiles(file -> {
            if (file.isDirectory()) {
                var user = readUser(file.toPath());
                users.put(user.getUserID(), user);
            }
            return false;
        });
    }

    private File findLatestDir(Path userDir) {
        final File[] r = {null};
        userDir.toFile().listFiles(file -> {
            if (r[0] == null)
                r[0] = file;
            else if (file.getName().compareTo(r[0].getName()) > 0)
                r[0] = file;
            return false;
        });
        return r[0];
    }

    private User readUser(Path userDir) {
        final var account = new CThostFtdcTradingAccountField[1];
        var positions = new ConcurrentHashMap<String, List<UserPositionDetail>>();
        findLatestDir(userDir).listFiles(file -> {
            var name = file.getName();
            try {
                if (name.startsWith("account.") && name.endsWith(".json")) {
                    if (account[0] != null)
                        throw new IOException("ambiguous account");
                    account[0] = OP.fromJson(
                            Utils.readText(file, StandardCharsets.UTF_8),
                            CThostFtdcTradingAccountField.class);
                }
                if (name.startsWith("position.") && name.endsWith(".json")) {
                    var pos = OP.fromJson(Utils.readText(
                            file, StandardCharsets.UTF_8),
                            CThostFtdcInvestorPositionDetailField.class);
                    var instrPos = positions.get(pos.InstrumentID);
                    if (instrPos == null) {
                        instrPos = new LinkedList<>();
                        positions.put(pos.InstrumentID, instrPos);
                    }
                    instrPos.add(new UserPositionDetail((pos)));
                }
            } catch (IOException e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
            return false;
        });
        return new User(account[0], positions);
    }

    private void write(Path dir) throws IOException {
        for (var user : this.users.values()) {
            var userDir = Path.of(dir.toString(), user.getUserID());
            writeUser(userDir, user);
        }
    }

    private void writeUser(Path userDir, User user) throws IOException {
        var todayDir = Path.of(userDir.toString(),
                Utils.getDay(LocalDate.now(), null));
        Utils.createFile(todayDir, true);
        // Write trading account.
        var account = user.getUserAccount().copyRawAccount();
        var path = Path.of(todayDir.toString(),
                "account." + account.AccountID + ".json");
        Utils.createFile(path, false);
        Utils.writeText(OP.toJson(account),
                Utils.createFile(path, false),
                StandardCharsets.UTF_8, false);
        // Write positions.
        int count = 0;
        for (var positions : user.getUserPosition().getPositionMap().values()) {
            for (var pos : positions) {
                if (pos.getAvailableVolume() > 0) {
                    path = Path.of(todayDir.toString(),
                            "position." + (++count) + ".json");
                    Utils.writeText(OP.toJson(
                            pos.copyRawPosition()),
                            Utils.createFile(path, false),
                            StandardCharsets.UTF_8,
                            false);
                }
            }
        }
    }

    public User getUser(String userID) {
        return this.users.get(userID);
    }

    public void prepareSettlement(SettlementPreparation prep) {
        this.prep = prep;
    }

    @Override
    public void renew() throws Exception {
        this.users.clear();
        init(this.dataDir);
    }

    @Override
    public void settle() throws Exception {
        for (var user : this.users.values())
            user.settle(this.prep);
        write(this.dataDir);
    }
}
