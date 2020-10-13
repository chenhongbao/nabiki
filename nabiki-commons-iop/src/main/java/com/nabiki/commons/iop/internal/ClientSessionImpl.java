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

package com.nabiki.commons.iop.internal;

import com.nabiki.commons.iop.ClientSession;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.utils.frame.FrameType;
import org.apache.mina.core.session.IoSession;

import java.net.InetSocketAddress;

class ClientSessionImpl extends SessionImpl implements ClientSession {
  private static final String IOP_HEARTBEAT_ID_KEY = "iop.heartbeat_id";

  private ClientSessionImpl(IoSession ioSession) {
    super(ioSession);
  }

  /*
  Retrieve and return an iop session from the specified io session, or create
  a new one when no session nothing found.
  */
  static synchronized ClientSessionImpl from(IoSession ioSession) {
    var iop = findSelf(ioSession);
    if (iop == null)
      iop = new ClientSessionImpl(ioSession);
    return (ClientSessionImpl) iop;
  }

  String getHeartbeatID() {
    var id = getAttribute(IOP_HEARTBEAT_ID_KEY);
    if (id != null)
      return (String) id;
    else
      return null;
  }

  @Override
  public void close() {
    super.close();
  }

  @Override
  public boolean isClosed() {
    return super.isClosed();
  }

  @Override
  public void fix() {
    super.fix();
  }

  @Override
  public void sendLogin(Message message) {
    message.TimeStamp = System.currentTimeMillis();
    message.Type = MessageType.REQ_LOGIN;
    // Need to wait until login request is actually sent.
    try {
      super.send(toBody(message), FrameType.LOGIN).await();
    } catch (Throwable ignored) {
    }
  }

  @Override
  public void sendRequest(Message message) throws InterruptedException {
    // Send message and set response state.
    // Need to ensure the request has been sent before return.
    message.TimeStamp = System.currentTimeMillis();
    super.send(toBody(message), FrameType.REQUEST).await();
  }

  @Override
  public void sendHeartbeat(String heartbeatID) {
    // Take down latest heartbeat ID.
    setAttribute(IOP_HEARTBEAT_ID_KEY, heartbeatID);
    var body = new Body();
    body.RequestID = heartbeatID;
    body.Type = MessageType.HEARTBEAT;
    body.TimeStamp = System.currentTimeMillis();
    // NO NEED to wait heart beat sent.
    super.send(body, FrameType.HEARTBEAT);
  }

  @Override
  public void setAttribute(String key, Object attribute) {
    super.setAttribute(key, attribute);
  }

  @Override
  public void removeAttribute(String key) {
    super.removeAttribute(key);
  }

  @Override
  public Object getAttribute(String key) {
    return super.getAttribute(key);
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return super.getRemoteAddress();
  }

  @Override
  public long getLag() {
    return super.getLag();
  }
}
