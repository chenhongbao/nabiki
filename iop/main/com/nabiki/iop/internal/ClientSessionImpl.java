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

import com.nabiki.iop.ClientSession;
import com.nabiki.iop.Message;
import com.nabiki.iop.MessageType;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.frame.FrameType;
import com.nabiki.iop.x.OP;
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
            return (String)id;
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
        message.Type = MessageType.REQ_LOGIN;
        super.send(toBody(message), FrameType.LOGIN);
    }

    private Body toBody(Message message) {
        var body = new Body();
        body.Type = message.Type;
        body.RequestID = message.RequestID;
        body.ResponseID = message.ResponseID;
        body.CurrentCount = message.CurrentCount;
        body.TotalCount = message.TotalCount;
        if (message.Body != null)
            body.Body = OP.toJson(message.Body);
        if (message.RspInfo != null)
            body.RspInfo = OP.toJson(message.RspInfo);
        return body;
    }

    @Override
    public void sendRequest(Message message) {
        // Send message and set response state.
        // Need to ensure the request has been sent before return.
        try {
            super.send(toBody(message), FrameType.REQUEST).await();
        } catch (InterruptedException ignored) {
        }
    }

    @Override
    public void sendHeartbeat(String heartbeatID) {
        var body = new Body();
        body.RequestID = heartbeatID;
        body.Type = MessageType.HEARTBEAT;
        super.send(body, FrameType.HEARTBEAT);
        // Take down latest heartbeat ID.
        setAttribute(IOP_HEARTBEAT_ID_KEY, heartbeatID);
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
}
