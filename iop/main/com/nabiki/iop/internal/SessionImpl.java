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

package com.nabiki.iop.internal;

import com.nabiki.iop.Message;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.frame.Frame;
import com.nabiki.iop.x.OP;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.session.IoSession;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

class SessionImpl {
  protected static final String IOP_SESSION_KEY = "iop.session";
  protected static final String IOP_LAG_KEY = "iop.lag.ms";

  private final IoSession session;
  static AtomicInteger countX = new AtomicInteger(0);

  protected SessionImpl(IoSession ioSession) {
    if (ioSession == null)
      throw new NullPointerException("io session null");
    if (findSelf(ioSession) != null)
      throw new IllegalStateException(
          "reused io session could cause inconsistency");
    ioSession.setAttribute(IOP_SESSION_KEY, this);
    this.session = ioSession;
  }

  void setLag(long lag) {
    session.setAttribute(IOP_LAG_KEY, lag);
  }

  protected long getLag() {
    var r = session.getAttribute(IOP_LAG_KEY);
    if (r instanceof Long) {
      return (long)r;
    } else {
      return Long.MAX_VALUE;
    }
  }

  protected static Object findSelf(IoSession session) {
    if (session == null)
      throw new NullPointerException("io session null");
    return session.getAttribute(IOP_SESSION_KEY);
  }

  protected Body toBody(Message message) {
    var body = new Body();
    body.Type = message.Type;
    body.RequestID = message.RequestID;
    body.ResponseID = message.ResponseID;
    body.CurrentCount = message.CurrentCount;
    body.TotalCount = message.TotalCount;
    body.timeStamp = System.currentTimeMillis();
    if (message.Body != null) {
      body.Body = OP.toJson(message.Body);
    }
    if (message.RspInfo != null) {
      body.RspInfo = OP.toJson(message.RspInfo);
    }
    return body;
  }

  protected void close() {
    synchronized (this) {
      if (!isClosed()) {
        // Just close the session ans don't wait for the CloseFuture to
        // return because it may continue checking input while waiting,
        // causing dead lock.
        session.closeNow();
        // Clear all attributes.
        for (var attr : session.getAttributeKeys()) {
          session.removeAttribute(attr);
        }
      }
    }
  }

  protected boolean isClosed() {
    return this.session == null || this.session.isClosing()
        || !this.session.isConnected();
  }

  protected void fix() {
    if (isClosed())
      throw new IllegalStateException("can't fix a closed session");
    if (this.session.isWriteSuspended())
      this.session.resumeWrite();
    if (this.session.isReadSuspended())
      this.session.resumeRead();
  }

  protected WriteFuture send(Body message, int type) {
    if (message == null)
      throw new NullPointerException("message null");
    synchronized (this) {
      if (isClosed())
        throw new IllegalStateException("session closed");
      // Get body bytes.
      var bytes = OP.toJson(message).getBytes(StandardCharsets.UTF_8);
      // Construct frame.
      var req = new Frame();
      req.Type = type;
      req.Length = bytes.length;
      req.Body = bytes;
      // Send frame.
      return this.session.write(req);
    }
  }

  protected void setAttribute(String key, Object attribute) {
    if (!isClosed())
      this.session.setAttribute(key, attribute);
  }

  protected void removeAttribute(String key) {
    if (!isClosed())
      this.session.setAttribute(key, null);
  }

  protected Object getAttribute(String key) {
    if (!isClosed())
      return this.session.getAttribute(key);
    else
      return null;
  }

  protected InetSocketAddress getRemoteAddress() {
    return (InetSocketAddress) this.session.getRemoteAddress();
  }
}
