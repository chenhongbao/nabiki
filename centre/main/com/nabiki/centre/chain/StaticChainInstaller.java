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

import com.nabiki.centre.active.ActiveUserManager;
import com.nabiki.centre.md.CandleRW;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.utils.Config;
import com.nabiki.iop.IOPServer;

import java.util.Objects;

public class StaticChainInstaller {
    public static void install(
            IOPServer server,
            UserAuthManager auth,
            ActiveUserManager user,
            MarketDataRouter router,
            Config cfg) {
        Objects.requireNonNull(server, "iop server null");
        Objects.requireNonNull(auth, "user auth manager null");
        Objects.requireNonNull(user, "active user manager null");
        Objects.requireNonNull(router, "md router null");
        Objects.requireNonNull(router, "md router null");
        // Install candle writer.
        var rw = new CandleRW(cfg);
        router.addReceiver(rw);
        // Install login manager.
        server.setLoginManager(new UserLoginManager(auth, user));
        // Install session adaptor.
        server.setSessionAdaptor(new SessionAdaptor(router, cfg));
        // Install adaptors.
        var chain = server.getAdaptorChain();
        chain.addAdaptor(new RequestValidator());
        chain.addAdaptor(new RequestExecutor());
        chain.addAdaptor(new SubscriptionAdaptor(router, rw, cfg));
        chain.addAdaptor(new QueryAdaptor(cfg));
    }
}
