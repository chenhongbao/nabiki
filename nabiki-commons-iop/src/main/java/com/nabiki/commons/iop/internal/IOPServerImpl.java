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

import com.nabiki.commons.iop.*;
import com.nabiki.commons.utils.frame.FrameParser;
import org.apache.mina.core.service.IoAcceptor;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;

public class IOPServerImpl implements IOPServer {
  private static final int DEFAULT_IDLE_SEC = 60;

  private final IoAcceptor acceptor = new NioSocketAcceptor();
  private final ServerFrameHandler frameHandler = new ServerFrameHandler();

  public IOPServerImpl() {
  }

  private void io(InetSocketAddress address) throws IOException {
    var chain = this.acceptor.getFilterChain();
    // Too many logs, don't use the logging filter.
    // chain.addLast(UUID.randomUUID().toString(), new LoggingFilter());
    chain.addLast(UUID.randomUUID().toString(), new ProtocolCodecFilter(
        new FrameCodecFactory()));
    // Frame handler.
    this.acceptor.setHandler(frameHandler);
    // Configure the session.
    var config = this.acceptor.getSessionConfig();
    config.setReadBufferSize(FrameParser.DEFAULT_BUFFER_SIZE * 2);
    config.setIdleTime(IdleStatus.BOTH_IDLE, DEFAULT_IDLE_SEC);
    // Bind address.
    this.acceptor.bind(address);
  }

  @Override
  public void bind(InetSocketAddress address) throws IOException {
    io(address);
  }

  @Override
  public void setSessionAdaptor(ServerSessionAdaptor adaptor) {
    this.frameHandler.setSessionAdaptor(adaptor);
  }

  @Override
  public void setMessageHandlerOut(ServerMessageHandler handler) {
    this.frameHandler.setHandlerOut(handler);
  }

  @Override
  public void setMessageHandlerIn(ServerMessageHandler handler) {
    this.frameHandler.setHandlerIn(handler);
  }

  @Override
  public void setLoginManager(LoginManager manager) {
    this.frameHandler.setLoginManager(manager);
  }

  @Override
  public AdaptorChain getAdaptorChain() {
    return this.frameHandler.getAdaptorChain();
  }
}
