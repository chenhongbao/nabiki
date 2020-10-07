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

package com.nabiki.centre.user.auth;

import com.nabiki.centre.utils.Utils;
import com.nabiki.commons.iop.x.OP;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class UserAuthManager {
  private final Map<String, UserAuthProfile> profiles = new ConcurrentHashMap<>();
  private final Path dataDir;

  public UserAuthManager(Path dataDir) {
    Objects.requireNonNull(dataDir, "user profile data root null");
    this.dataDir = dataDir;
  }

  private void init(Path dir) {
    if (!Files.exists(dir) || !Files.isDirectory(dir))
      throw new IllegalArgumentException("user profile data root not exist");
    dir.toFile().listFiles(file -> {
      for (var auth : readUser(file.toPath()))
        profiles.put(auth.UserID, auth);
      return false;
    });
    if (this.profiles.size() < 1)
      throw new IllegalStateException("no user auth");
  }

  private Collection<UserAuthProfile> readUser(Path userDir) {
    var r = new LinkedList<UserAuthProfile>();
    userDir.toFile().listFiles(file -> {
      var name = file.getName();
      if (name.startsWith("auth.") && name.endsWith(".json")) {
        try {
          var profile = OP.fromJson(
              Utils.readText(file, StandardCharsets.UTF_8),
              UserAuthProfile.class);
          if (profile != null && profile.UserID != null)
            r.add(profile);
          else
            throw new IOException("invalid auth json");
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      return false;
    });
    return r;
  }

  private void write(Path dir) throws IOException {
    for (var user : this.profiles.values()) {
      var userDir = Path.of(dir.toString(), user.UserID);
      try {
        writeUser(userDir, user);
      } catch (Throwable th) {
        th.printStackTrace();
      }
    }
  }

  private void checkWriteSuccess(Path p, String content) throws IOException {
    if (!p.toFile().exists())
      throw new IllegalStateException("fail creating auth file: " + p);
    var text = Utils.readText(p.toFile(), StandardCharsets.UTF_8);
    if (text.compareTo(content) != 0)
      throw new IllegalStateException("auth write wrong content");
  }

  private void writeUser(Path userDir, UserAuthProfile profile)
      throws IOException {
    var path = Path.of(userDir.toString(),
        "auth." + profile.UserID + ".json");
    Utils.createFile(userDir, true);
    var json = OP.toJson(profile);
    Utils.writeText(
        json,
        path.toFile(),
        StandardCharsets.UTF_8,
        false);
    checkWriteSuccess(path, json);
  }

  public UserAuthProfile getAuthProfile(String userID) {
    return this.profiles.get(userID);
  }

  public void load() throws Exception {
    this.profiles.clear();
    init(this.dataDir);
  }

  public void flush() throws Exception {
    write(this.dataDir);
  }
}
