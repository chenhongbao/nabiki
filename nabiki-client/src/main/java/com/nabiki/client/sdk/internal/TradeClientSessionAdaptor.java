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

package com.nabiki.client.sdk.internal;

import com.nabiki.client.sdk.TradeClientListener;
import com.nabiki.commons.iop.ClientSession;
import com.nabiki.commons.iop.ClientSessionAdaptor;
import com.nabiki.commons.iop.SessionEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

class TradeClientSessionAdaptor extends ClientSessionAdaptor {
  private class DefaultClientListener implements TradeClientListener {
    @Override
    public void onError(Throwable th) {
    }

    @Override
    public void onClose() {
    }

    @Override
    public void onOpen() {
    }
  }

  private final AtomicBoolean closed = new AtomicBoolean(true);
  private final AtomicReference<TradeClientListener> listener
      = new AtomicReference<>(new DefaultClientListener());

  TradeClientSessionAdaptor() {
  }

  void setListener(TradeClientListener listener) {
    if (listener != null)
      this.listener.set(listener);
  }

  @Override
  public void doEvent(ClientSession session, SessionEvent event,
                      Object eventObject) {
    switch (event) {
      case OPENED:
        this.closed.set(false);
        try {
          this.listener.get().onOpen();
        } catch (Throwable th) {
          th.printStackTrace();
        }
        break;
      case CLOSED:
        this.closed.set(true);
        try {
          this.listener.get().onClose();
        } catch (Throwable th) {
          th.printStackTrace();
        }
        break;
      case ERROR:
        try {
          this.listener.get().onError((Throwable) eventObject);
        } catch (Throwable th) {
          th.printStackTrace();
        }
      default:
        break;
    }
  }
}
