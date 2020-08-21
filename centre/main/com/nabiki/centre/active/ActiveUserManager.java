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

package com.nabiki.centre.active;

import com.nabiki.centre.Renewable;
import com.nabiki.centre.ctp.OrderProvider;
import com.nabiki.centre.user.core.UserManager;
import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.utils.Config;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ActiveUserManager implements Renewable {
    private final OrderProvider provider;
    private final Config config;
    private final Path dataDir;
    private final UserManager userMgr;
    private final Map<String, ActiveUser> users = new ConcurrentHashMap<>();

    public ActiveUserManager(OrderProvider provider, Config cfg, Path dataDir) {
        this.provider = provider;
        this.config = cfg;
        this.dataDir = dataDir;
        this.userMgr = UserManager.create(dataDir);
    }

    private ActiveUser getOrCreate(String userID) {
        ActiveUser active = this.users.get(userID);
        if (active == null) {
            var usr = this.userMgr.getUser(userID);
            if (usr == null)
                return null;
            active = new ActiveUser(usr, this.provider, this.config);
            this.users.put(userID, active);
        }
        return active;
    }

    public ActiveUser getActiveUser(String userID) {
        return getOrCreate(userID);
    }

    private SettlementPreparation prepare() {
        SettlementPreparation prep = new SettlementPreparation();
        prep.prepare(this.config.getTradingDay());
        for (var i : this.config.getAllInstrInfo()) {
            // There may be some info missing, but it doesn't matter if we don't
            // have that position.
            prep.prepare(i.Instrument);
            prep.prepare(i.Commission);
            prep.prepare(i.Margin);
            prep.prepare(this.config.getDepthMarketData(i.Instrument.InstrumentID));
        }
        return prep;
    }

    @Override
    public void renew() throws Exception {
        this.users.clear();
        this.userMgr.renew();
    }

    @Override
    public void settle() throws Exception {
        this.users.clear();
        // Prepare info.
        this.userMgr.prepareSettlement(prepare());
        this.userMgr.settle();
    }
}
