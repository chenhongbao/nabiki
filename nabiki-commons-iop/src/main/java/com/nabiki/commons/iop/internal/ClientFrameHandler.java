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
import com.nabiki.commons.utils.Utils;
import com.nabiki.commons.utils.frame.Frame;
import com.nabiki.commons.utils.frame.FrameType;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.FilterEvent;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

class ClientFrameHandler implements IoHandler {
  /*
  Default adaptors. The adaptors can be override by setter.
   */
  static class EmptyClientSessionAdaptor extends ClientSessionAdaptor {
  }

  static class EmptyClientMessageHandler implements ClientMessageHandler {
    @Override
    public void onMessage(ClientSession session, Message message) {
    }
  }

  static class ResponseBag {
    final IoSession session;
    final Message message;
    final int frameType;

    ResponseBag(IoSession session, Message msg, int type) {
      this.session = session;
      this.message = msg;
      this.frameType = type;
    }
  }

  public static final String IOP_ISLOGIN_KEY = "iop.islogin";
  private final DefaultClientMessageHandler defaultMsgHandler
      = new DefaultClientMessageHandler();

  private ClientSessionAdaptor sessionAdaptor = new EmptyClientSessionAdaptor();
  private ClientMessageHandler msgHandlerIn = new EmptyClientMessageHandler(),
      msgHandlerOut = new EmptyClientMessageHandler();

  private Thread daemon;
  private final BlockingQueue<ResponseBag> responses = new LinkedBlockingQueue<>();

  ClientFrameHandler() {
    setupDaemon();
  }

  private void setupDaemon() {
    daemon = new Thread(new Runnable() {
      @Override
      public void run() {
        while (!Thread.currentThread().isInterrupted()) {
          try {
            var bag = responses.take();
            messageProc(bag.session, bag.message, bag.frameType);
          } catch (Throwable th) {
            // Print error information so it can be fixed.
            th.printStackTrace();
          }
        }
      }
    });
    daemon.setDaemon(true);
    daemon.start();
  }

  void setMessageAdaptor(ClientMessageAdaptor adaptor) {
    this.defaultMsgHandler.setAdaptor(adaptor);
  }

  void setSessionAdaptor(ClientSessionAdaptor adaptor) {
    this.sessionAdaptor = adaptor;
  }

  void setMessageHandlerIn(ClientMessageHandler handler) {
    this.msgHandlerIn = handler;
  }

  void setMessageHandlerOut(ClientMessageHandler handler) {
    this.msgHandlerOut = handler;
  }

  private void handleLogin(ClientSession session, Message message) {
    session.setAttribute(IOP_ISLOGIN_KEY,
        message.RspInfo != null && message.RspInfo.ErrorID == 0);
    this.defaultMsgHandler.onMessage(session, message);
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

  private void checkLag(SessionImpl session, Message msg) {
    if (session != null && msg != null) {
      if (msg.TimeStamp > 0) {
        session.setLag(System.currentTimeMillis() - msg.TimeStamp);
      } else {
        // Backward compatible
        session.setLag(0);
      }
    }
  }

  private void handleHeartbeat(ClientSessionImpl session, Message message) {
    if (message.Type != MessageType.HEARTBEAT)
      return;
    if (!session.getHeartbeatID().equals(message.RequestID))
      this.sessionAdaptor.doEvent(session,
          SessionEvent.MISS_HEARTBEAT, message.RequestID);
  }

  private void messageProc(IoSession session, Message message, int type) {
    try {
      ClientSessionImpl iopSession = ClientSessionImpl.from(session);
      // Take down lag from server to client.
      checkLag(iopSession, message);
      // First call message handler.
      try {
        this.msgHandlerIn.onMessage(iopSession, message);
      } catch (Throwable th) {
        exceptionCaught(session, th);
      }
      // Then call default message handler that wraps adaptor.
      switch (type) {
        case FrameType.RESPONSE:
          if (isLogin(session))
            this.defaultMsgHandler.onMessage(iopSession, message);
          break;
        case FrameType.LOGIN:
          handleLogin(iopSession, message);
          break;
        case FrameType.HEARTBEAT:
          handleHeartbeat(iopSession, message);
          break;
        default:
          throw new IllegalStateException(String.format("unknown frame type %X", type));
      }
    } catch (Throwable th) {
      try {
        exceptionCaught(session, th);
      } catch (Exception ignored) {
      }
    }
  }

  private void offer(IoSession session, Message msg, int type) {
    try {
      var bag = new ResponseBag(session, msg, type);
      if (!responses.offer(bag))
        throw new IllegalStateException("can't offer response to queue");
    } catch (Throwable th) {
      try {
        exceptionCaught(session, th);
      } catch (Throwable ignored) {
      }
    }
  }

  @Override
  public void sessionCreated(IoSession session) throws Exception {
    this.sessionAdaptor.doEvent(ClientSessionImpl.from(session),
        SessionEvent.CREATED, null);
  }

  @Override
  public void sessionOpened(IoSession session) throws Exception {
    this.sessionAdaptor.doEvent(ClientSessionImpl.from(session),
        SessionEvent.OPENED, null);
  }

  @Override
  public void sessionClosed(IoSession session) throws Exception {
    this.sessionAdaptor.doEvent(ClientSessionImpl.from(session),
        SessionEvent.CLOSED, null);
  }

  @Override
  public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
    var clientSession = ClientSessionImpl.from(session);
    this.sessionAdaptor.doEvent(clientSession, SessionEvent.IDLE, status);
    try {
      // If client detects idle, send heartbeat.
      clientSession.sendHeartbeat(UUID.randomUUID().toString());
    } catch (Throwable th) {
      exceptionCaught(session, th);
    }
  }

  @Override
  public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
    this.sessionAdaptor.doEvent(ClientSessionImpl.from(session),
        SessionEvent.ERROR, cause);
  }

  @Override
  public void messageReceived(IoSession session, Object message) throws Exception {
    if (!(message instanceof Frame))
      throw new IllegalStateException("message is not frame");
    try {
      var frame = (Frame) message;
      var body = Utils.fromJson(new String(
          frame.Body, StandardCharsets.UTF_8), Body.class);
      offer(session, toMessage(body), frame.Type);
    } catch (IOException e) {
      exceptionCaught(session, e);
    }
  }

  @Override
  public void messageSent(IoSession session, Object message) throws Exception {
    if (!(message instanceof Frame))
      throw new IllegalStateException("message is not frame");
    Body body = null;
    Message iopMessage;
    ClientSessionImpl iopSession = ClientSessionImpl.from(session);
    var frame = (Frame) message;
    try {
      body = Utils.fromJson(new String(
          frame.Body, StandardCharsets.UTF_8), Body.class);
      iopMessage = toMessage(body);
      try {
        this.msgHandlerOut.onMessage(iopSession, iopMessage);
      } catch (Throwable th) {
        th.printStackTrace();
      }
    } catch (IOException e) {
      this.sessionAdaptor.doEvent(
          iopSession, SessionEvent.BROKEN_BODY, body);
    }
  }

  @Override
  public void inputClosed(IoSession session) throws Exception {
    this.sessionAdaptor.doEvent(ClientSessionImpl.from(session),
        SessionEvent.INPUT_CLOSED, null);
  }

  @Override
  public void event(IoSession session, FilterEvent event) throws Exception {
    // nothing.
  }
}
