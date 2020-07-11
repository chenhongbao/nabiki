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

package com.nabiki.client.internal;

import com.nabiki.client.TradeClientListener;
import com.nabiki.iop.ClientSession;
import com.nabiki.iop.ClientSessionAdaptor;
import com.nabiki.iop.SessionEvent;

import java.util.concurrent.atomic.AtomicBoolean;

class TradeClientSessionAdaptor extends ClientSessionAdaptor {
    private final AtomicBoolean closed = new AtomicBoolean(true);
    private final TradeClientListener listener;

    TradeClientSessionAdaptor(TradeClientListener listener) {
        this.listener = listener;
    }

    boolean isClosed() {
        return this.closed.get();
    }

    @Override
    public void doEvent(ClientSession session, SessionEvent event,
                        Object eventObject) {
        switch (event) {
            case OPENED:
                this.closed.set(false);
                try {
                    this.listener.onOpen();
                } catch (Throwable ignored) {
                }
                break;
            case CLOSED:
                this.closed.set(true);
                try {
                    this.listener.onClose();
                } catch (Throwable ignored) {
                }
                break;
            case ERROR:
                try {
                    this.listener.onError((Throwable) eventObject);
                } catch (Throwable ignored) {
                }
            default:
                break;
        }
    }
}
