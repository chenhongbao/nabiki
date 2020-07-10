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
import com.nabiki.iop.MessageType;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.frame.FrameType;
import com.nabiki.iop.x.OP;
import org.apache.mina.core.session.IoSession;

import java.util.UUID;

public class ServerSessionImpl extends SessionImpl implements ServerSession {
    private SessionResponseState responseState;

    /*
    Construct an iop session from mina's io session and set this instance into
    the specified io session.
     */
    private ServerSessionImpl(IoSession ioSession) {
        super(ioSession);
    }

    /*
    Retrieve and return an iop session from the specified io session, or create
    a new one when no session nothing found.
     */
    static synchronized ServerSessionImpl from(IoSession ioSession) {
        var iop = findSelf(ioSession);
        if (iop == null)
            iop = new ServerSessionImpl(ioSession);
        return (ServerSessionImpl) iop;
    }

    void setResponseState(SessionResponseState state) {
        this.responseState = state;
    }

    SessionResponseState getResponseState() {
        return this.responseState;
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
    public void done() {
        setResponseState(SessionResponseState.DONE);
    }

    @Override
    public void fix() {
        super.fix();
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
    public void sendLogin(Message message) {
        super.send(toBody(message), FrameType.LOGIN);
    }

    @Override
    public void sendResponse(Message message) {
        // Send message and set response state.
        super.send(toBody(message), FrameType.RESPONSE);
        setResponseState(SessionResponseState.SENDING);
    }

    @Override
    public void sendHeartbeat(UUID heartbeatID) {
        var body = new Body();
        body.RequestID = heartbeatID;
        body.Type = MessageType.HEARTBEAT;
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
}
