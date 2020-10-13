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

package com.nabiki.centre.user.core;

import com.nabiki.centre.config.Global;
import com.nabiki.centre.ctp.OrderProvider;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUserManager {
  private final OrderProvider provider;
  private final Global global;
  private final UserManager userMgr;
  private final Map<String, ActiveUser> users = new ConcurrentHashMap<>();

  public ActiveUserManager(OrderProvider provider, Global cfg, Path dataDir) {
    this.provider = provider;
    this.global = cfg;
    this.userMgr = UserManager.create(dataDir);
  }

  private void createActive() {
    for (var userID : this.userMgr.getAllUserID()) {
      var usr = this.userMgr.getUser(userID);
      if (usr != null)
        this.users.put(userID,
            new ActiveUser(usr, this.provider, this.global));
      else
        this.global.getLogger().warning("null user");
    }
  }

  public ActiveUser getActiveUser(String userID) {
    return this.users.get(userID);
  }

  public void renew() throws Exception {
    this.users.clear();
    this.userMgr.load();
    createActive();
  }

  public void settle() throws Exception {
    for (var active : this.users.values())
      active.settle();
    this.userMgr.flush();
  }
}
