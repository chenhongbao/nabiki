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

import com.nabiki.centre.md.MarketDataReceiver;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.centre.utils.Config;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.ServerSessionAdaptor;
import com.nabiki.iop.SessionEvent;
import com.nabiki.iop.x.OP;

public class SessionAdaptor extends ServerSessionAdaptor {
    private final MarketDataRouter router;
    private final Config config;

    SessionAdaptor(MarketDataRouter router, Config cfg) {
        this.router = router;
        this.config = cfg;
    }

    @Override
    public void doEvent(
            ServerSession session,
            SessionEvent event,
            Object eventObject) {
        switch (event) {
            case ERROR:
                this.config.getLogger().warning(
                        ((Throwable) eventObject).getMessage());
                break;
            case IDLE:
            case MISS_HEARTBEAT:
                this.config.getLogger().warning(
                        "miss heartbeat(" + eventObject + ")");
                break;
            case MESSAGE_NOT_DONE:
            case STRANGE_MESSAGE:
            case BROKEN_BODY:
                this.config.getLogger().warning("fail parsing message");
                this.config.getLogger().warning(OP.toJson(eventObject));
                break;
            case CLOSED:
            case INPUT_CLOSED:
                var recv = session.getAttribute(
                        SubscriptionAdaptor.FRONT_MDRECEIVER_KEY);
                if (recv != null)
                    this.router.removeReceiver((MarketDataReceiver) recv);
                this.config.getLogger().info(
                        "session closed(" + session.getRemoteAddress() + ")");
                break;
            case CREATED:
            case OPENED:
                this.config.getLogger().info(
                        "session created(" + session.getRemoteAddress() + ")");
                break;
            default:
                break;
        }
    }
}
