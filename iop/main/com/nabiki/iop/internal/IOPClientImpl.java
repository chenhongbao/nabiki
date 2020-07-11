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

import com.nabiki.iop.ClientMessageAdaptor;
import com.nabiki.iop.ClientSession;
import com.nabiki.iop.ClientSessionAdaptor;
import com.nabiki.iop.IOPClient;
import com.nabiki.iop.frame.FrameParser;
import com.nabiki.iop.x.OP;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.logging.LoggingFilter;
import org.apache.mina.transport.socket.nio.NioSocketConnector;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class IOPClientImpl implements IOPClient {
    private static final int DEFAULT_IDLE_SEC = 60;
    private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;

    private final NioSocketConnector connector = new NioSocketConnector();
    private final ClientFrameHandler frameHnd = new ClientFrameHandler();
    private ClientSessionImpl session;

    public IOPClientImpl() {
    }

    private IoSession io(InetSocketAddress connectAddress) throws IOException {
        System.out.println("client connect()");
        this.connector.setConnectTimeoutMillis(DEFAULT_CONNECT_TIMEOUT_MILLIS);
        // Set filters.
        var chain = this.connector.getFilterChain();
        chain.addLast(OP.randomString(), new LoggingFilter());
        chain.addLast(OP.randomString(), new ProtocolCodecFilter(
                new FrameCodecFactory()));
        // Set handler.
        this.connector.setHandler(this.frameHnd);
        // Configure the session.
        var config = this.connector.getSessionConfig();
        config.setReadBufferSize(FrameParser.DEFAULT_BUFFER_SIZE * 2);
        config.setIdleTime(IdleStatus.BOTH_IDLE, DEFAULT_IDLE_SEC);
        // Connect and construct session.
        ConnectFuture future = connector.connect(connectAddress);
        try {
            if (!future.await(DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    TimeUnit.MILLISECONDS))
                throw new IOException("connect timeout");
        } catch (InterruptedException e) {
            throw new IOException("wait interrupted");
        }
        return future.getSession();
    }

    @Override
    public void connect(InetSocketAddress address) throws IOException {
        // Construct session.
        this.session = ClientSessionImpl.from(io(address));
    }

    @Override
    public void disconnect() {
        this.session.close();
        this.connector.dispose();
    }

    @Override
    public void setSessionAdaptor(ClientSessionAdaptor adaptor) {
        this.frameHnd.setClientSessionAdaptor(adaptor);
    }

    @Override
    public void setMessageAdaptor(ClientMessageAdaptor adaptor) {
        this.frameHnd.setMessageAdaptor(adaptor);
    }

    @Override
    public ClientSession getSession() {
        return this.session;
    }
}
