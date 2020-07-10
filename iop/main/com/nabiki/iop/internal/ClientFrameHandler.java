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

import com.nabiki.iop.*;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.frame.Frame;
import com.nabiki.iop.frame.FrameType;
import com.nabiki.iop.x.OP;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class ClientFrameHandler implements IoHandler {
    /*
    Default adaptors. The adaptors can be override by setter.
     */
    static class DefaultClientSessionAdaptor extends ClientSessionAdaptor {
    }

    static class DefaultClientMessageAdaptor extends ClientMessageAdaptor {
    }

    public static final String IOP_ISLOGIN_KEY = "iop.islogin";

    private ClientSessionAdaptor clientSessionAdaptor = new DefaultClientSessionAdaptor();
    private ClientMessageAdaptor clientAdaptor = new DefaultClientMessageAdaptor();

    void setMessageAdaptor(ClientMessageAdaptor adaptor) {
        this.clientAdaptor = adaptor;
    }

    void setClientSessionAdaptor(ClientSessionAdaptor adaptor) {
        this.clientSessionAdaptor = adaptor;
    }

    private void handleResponse(Message message) throws IOException {
        switch (message.Type) {
            case RSP_QRY_ORDER:
                this.clientAdaptor.doRspQryOrder(message);
                break;
            case RSP_QRY_POSITION:
                this.clientAdaptor.doRspQryPosition(message);
                break;
            case RSP_QRY_ACCOUNT:
                this.clientAdaptor.doRspQryAccount(message);
                break;
            case RSP_REQ_ORDER_ACTION:
                this.clientAdaptor.doRspReqOrderAction(message);
                break;
            case RSP_REQ_ORDER_INSERT:
                this.clientAdaptor.doRspReqOrderInsert(message);
                break;
            case RSP_SUB_MD:
                this.clientAdaptor.doRspSubscribeMarketData(message);
                break;
            case FLOW_DEPTH:
                this.clientAdaptor.doRspDepthMarketData(message);
                break;
            case FLOW_CANDLE:
                this.clientAdaptor.doRspCandle(message);
                break;
            default:
                throw new IllegalStateException(
                        "unknown message type " + message.Type);
        }
    }

    private void handleLogin(ClientSession session, Message message) {
        session.setAttribute(IOP_ISLOGIN_KEY,
                message.RspInfo != null && message.RspInfo.ErrorID == 0);
        this.clientAdaptor.doRspReqLogin(message);
    }

    private boolean isLogin(IoSession session) {
        var isLogin = session.getAttribute(IOP_ISLOGIN_KEY);
        if (isLogin == null)
            return false;
        else
            return (Boolean) isLogin;
    }

    private Message toMessage(Body body) throws IOException {
        try {
            return MessageImpl.toMessage(body);
        } catch (Throwable th) {
            throw new IOException("message not properly handled", th);
        }
    }

    private void handleHeartbeat(ClientSessionImpl session, Message message) {
        if (message.Type != MessageType.HEARTBEAT)
            return;
        if (!session.getHeartbeatID().equals(message.RequestID))
            this.clientSessionAdaptor.doEvent(session,
                    SessionEvent.MISS_HEARTBEAT, message.RequestID);
    }

    @Override
    public void sessionCreated(IoSession session) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.CREATED, null);
    }

    @Override
    public void sessionOpened(IoSession session) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.OPENED, null);
    }

    @Override
    public void sessionClosed(IoSession session) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.CLOSED, null);
    }

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.IDLE, status);
    }

    @Override
    public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.ERROR, cause);
    }

    @Override
    public void messageReceived(IoSession session, Object message) throws Exception {
        if (!(message instanceof Frame))
            throw new IllegalStateException("message is not frame");
        Body body = null;
        Message iopMessage = null;
        ClientSessionImpl iopSession = ClientSessionImpl.from(session);
        var frame = (Frame) message;
        try {
            body = OP.fromJson(new String(
                    frame.Body, StandardCharsets.UTF_8), Body.class);
            iopMessage = toMessage(body);
            switch (frame.Type) {
                case FrameType.RESPONSE:
                    if (isLogin(session))
                        handleResponse(toMessage(body));
                    break;
                case FrameType.LOGIN:
                    handleLogin(ClientSessionImpl.from(session), toMessage(body));
                    break;
                case FrameType.HEARTBEAT:
                    handleHeartbeat(ClientSessionImpl.from(session),
                            toMessage(body));
                    break;
                default:
                    throw new IllegalStateException("unknown frame type");
            }
        } catch (IOException e) {
            this.clientSessionAdaptor.doEvent(
                    iopSession, SessionEvent.BROKEN_BODY, body);
        }
    }

    @Override
    public void messageSent(IoSession session, Object message) throws Exception {
        // nothing.
    }

    @Override
    public void inputClosed(IoSession session) throws Exception {
        this.clientSessionAdaptor.doEvent(ClientSessionImpl.from(session),
                SessionEvent.INPUT_CLOSED, null);
    }

    @Override
    public void event(IoSession session, FilterEvent event) throws Exception {
        // nothing.
    }
}
