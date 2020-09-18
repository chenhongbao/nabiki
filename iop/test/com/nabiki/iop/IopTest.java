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

package com.nabiki.iop;

import com.nabiki.iop.frame.Body;
import com.nabiki.iop.x.OP;
import com.nabiki.iop.x.SystemStream;
import com.nabiki.objects.*;
import org.apache.mina.core.session.IdleStatus;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class IopTest {
  static {
    try {
      SystemStream.setErr("C:/Users/chenh/Desktop/log/err.log");
      SystemStream.setOut("C:/Users/chenh/Desktop/log/out.log");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  static void checkSessionEvent(SessionEvent event, Object eventObject) {
    switch (event) {
      case ERROR:
        assertTrue(eventObject instanceof Throwable);
        System.out.println(((Throwable) eventObject).getMessage());
        break;
      case IDLE:
        assertTrue(eventObject instanceof IdleStatus);
        System.out.println(eventObject);
        break;
      case MISS_HEARTBEAT:
        assertTrue(eventObject instanceof String);
        System.out.println(eventObject);
        break;
      case MESSAGE_NOT_DONE:
      case STRANGE_MESSAGE:
        assertTrue(eventObject instanceof Message);
        System.out.println(OP.toJson(eventObject));
        break;
      case BROKEN_BODY:
        assertTrue(eventObject instanceof Body);
        System.out.println(OP.toJson(eventObject));
        break;
      default:
        assertNull(eventObject);
        System.out.println(event);
        break;
    }
  }

  static void send(ServerSession session, Object object, MessageType type,
                   int current, int total) {
    var m = new Message();
    m.CurrentCount = current;
    m.TotalCount = total;
    m.Type = type;
    m.Body = object;
    if (new Random().nextDouble() > 0.5)
      m.RspInfo = new CRspInfo();
    session.sendResponse(m);
  }

  static void send(ClientSession session, Object object, MessageType type,
                   int current, int total) {
    var m = new Message();
    m.CurrentCount = current;
    m.TotalCount = total;
    m.Type = type;
    m.Body = object;
    if (new Random().nextDouble() > 0.5)
      m.RspInfo = new CRspInfo();
    try {
      session.sendRequest(m);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  static void login(ClientSession session, Object object, int current, int total) {
    var m = new Message();
    m.CurrentCount = current;
    m.TotalCount = total;
    m.Body = object;
    if (new Random().nextDouble() > 0.5)
      m.RspInfo = new CRspInfo();
    session.sendLogin(m);
  }

  class TestServerSessionAdaptor extends ServerSessionAdaptor {
    @Override
    public void doEvent(ServerSession session, SessionEvent event,
                        Object eventObject) {
      checkSessionEvent(event, eventObject);
    }
  }

  class TestClientSessionAdaptor extends ClientSessionAdaptor {
    @Override
    public void doEvent(ClientSession session, SessionEvent event,
                        Object eventObject) {
      checkSessionEvent(event, eventObject);
    }
  }

  static Set<MessageType> hit;

  static {
    hit = new HashSet<>();
  }

  private void hit(MessageType type) {
    hit.add(type);
  }

  class TestClientMessageAdaptor extends ClientMessageAdaptor {
    @Override
    public void doRspSubscribeMarketData(
        CSpecificInstrument rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_SUB_MD);
    }

    @Override
    public void doRspUnsubscribeMarketData(
        CSpecificInstrument rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_UNSUB_MD);
    }

    @Override
    public void doRspDepthMarketData(
        CDepthMarketData rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.FLOW_DEPTH);
    }

    @Override
    public void doRspCandle(
        CCandle rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.FLOW_CANDLE);
    }

    @Override
    public void doRspAuthenticate(
        CRspAuthenticate rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_AUTHENTICATE);
    }

    @Override
    public void doRspReqLogin(
        CRspUserLogin rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_LOGIN);
    }

    @Override
    public void doRspReqLogout(
        CUserLogout rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_LOGOUT);
    }

    @Override
    public void doRspReqSettlementConfirm(
        CSettlementInfoConfirm rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_SETTLEMENT);
    }

    @Override
    public void doRspReqOrderInsert(
        COrder rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_ORDER_INSERT);
    }

    @Override
    public void doRspReqOrderAction(
        COrderAction rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_REQ_ORDER_ACTION);
    }

    @Override
    public void doRspQryAccount(
        CTradingAccount rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_ACCOUNT);
    }

    @Override
    public void doRspQryOrder(
        COrder rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_ORDER);
    }

    @Override
    public void doRspQryPosition(
        CInvestorPosition rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_POSITION);
    }

    @Override
    public void doRspQryPositionDetail(
        CInvestorPositionDetail rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_POSI_DETAIL);
    }

    @Override
    public void doRspQryInstrument(
        CInstrument rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_INSTRUMENT);
    }

    @Override
    public void doRspQryCommission(
        CInstrumentCommissionRate rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_COMMISSION);
    }

    @Override
    public void doRspQryMargin(
        CInstrumentMarginRate rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_QRY_MARGIN);
    }

    @Override
    public void doRtnOrder(
        COrder rtn,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RTN_ORDER);
    }

    @Override
    public void doRtnTrade(
        CTrade rtn,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RTN_TRADE);
    }

    @Override
    public void doRtnOrderAction(
        COrderAction rtn,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RTN_ORDER_ACTION);
    }

    @Override
    public void doRtnOrderInsert(
        CInputOrder rtn,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RTN_ORDER_INSERT);
    }

    @Override
    public void doRspOrderAction(
        CInputOrderAction rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_ORDER_ACTION);
    }

    @Override
    public void doRspOrderInsert(
        CInputOrder rsp,
        CRspInfo info,
        String requestID,
        String responseID,
        int current,
        int total) {
      hit(MessageType.RSP_ORDER_INSERT);
    }
  }

  class TestServerMessageAdaptor extends ServerMessageAdaptor {
    @Override
    public void doSubDepthMarketData(
        ServerSession session,
        CSubMarketData request,
        String requestID,
        int current,
        int total) {
      send(session, new CSpecificInstrument(),
          MessageType.RSP_SUB_MD, 1, 1);
      hit(MessageType.SUB_MD);
      // Send flow.
      send(session, new CDepthMarketData(),
          MessageType.FLOW_DEPTH, 1, 1);
      send(session, new CCandle(),
          MessageType.FLOW_CANDLE, 1, 1);
    }

    @Override
    public void doUnsubDepthMarketData(
        ServerSession session,
        CUnsubMarketData request,
        String requestID,
        int current,
        int total) {
      send(session, new CSpecificInstrument(),
          MessageType.RSP_UNSUB_MD, 1, 1);
      hit(MessageType.UNSUB_MD);
    }

    @Override
    public void doReqAuthenticate(
        ServerSession session,
        CReqAuthenticate request,
        String requestID,
        int current,
        int total) {
      send(session, new CRspAuthenticate(),
          MessageType.RSP_REQ_AUTHENTICATE, 1, 1);
      hit(MessageType.REQ_AUTHENTICATE);
    }

    @Override
    public void doReqLogin(
        ServerSession session,
        CReqUserLogin request,
        String requestID,
        int current,
        int total) {
      send(session, new CRspUserLogin(),
          MessageType.RSP_REQ_LOGIN, 1, 1);
      hit(MessageType.REQ_LOGIN);
    }

    @Override
    public void doReqLogout(
        ServerSession session,
        CUserLogout request,
        String requestID,
        int current,
        int total) {
      send(session, new CUserLogout(),
          MessageType.RSP_REQ_LOGOUT, 1, 1);
      hit(MessageType.REQ_LOGOUT);
    }

    @Override
    public void doReqSettlementConfirm(
        ServerSession session,
        CSettlementInfoConfirm request,
        String requestID,
        int current,
        int total) {
      send(session, new CSettlementInfoConfirm(),
          MessageType.RSP_REQ_SETTLEMENT, 1, 1);
      hit(MessageType.REQ_SETTLEMENT);
    }

    @Override
    public void doReqOrderInsert(
        ServerSession session,
        CInputOrder request,
        String requestID,
        int current,
        int total) {
      send(session, new COrder(),
          MessageType.RSP_REQ_ORDER_INSERT, 1, 2);
      send(session, new COrder(),
          MessageType.RSP_REQ_ORDER_INSERT, 2, 2);
      hit(MessageType.REQ_ORDER_INSERT);
      // Send rtn.
      send(session, new COrder(),
          MessageType.RTN_ORDER, 1, 1);
      send(session, new CTrade(),
          MessageType.RTN_TRADE, 1, 1);
      // Send error.
      send(session, new CInputOrder(),
          MessageType.RSP_ORDER_INSERT, 1, 1);
      send(session, new CInputOrder(),
          MessageType.RTN_ORDER_INSERT, 1, 1);
    }

    @Override
    public void doReqOrderAction(
        ServerSession session,
        CInputOrderAction request,
        String requestID,
        int current,
        int total) {
      send(session, new COrderAction(),
          MessageType.RSP_REQ_ORDER_ACTION, 1, 1);
      hit(MessageType.REQ_ORDER_ACTION);
      // Send error.
      send(session, new CInputOrderAction(),
          MessageType.RSP_ORDER_ACTION, 1, 1);
      send(session, new COrderAction(),
          MessageType.RTN_ORDER_ACTION, 1, 1);
    }

    @Override
    public void doQryAccount(
        ServerSession session,
        CQryTradingAccount query,
        String requestID,
        int current,
        int total) {
      send(session, new CTradingAccount(),
          MessageType.RSP_QRY_ACCOUNT, 1, 1);
      hit(MessageType.QRY_ACCOUNT);
    }

    @Override
    public void doQryOrder(
        ServerSession session,
        CQryOrder query,
        String requestID,
        int current,
        int total) {
      send(session, new COrder(),
          MessageType.RSP_QRY_ORDER, 1, 1);
      hit(MessageType.QRY_ORDER);
    }

    @Override
    public void doQryPosition(
        ServerSession session,
        CQryInvestorPosition query,
        String requestID,
        int current,
        int total) {
      send(session, new CInvestorPosition(),
          MessageType.RSP_QRY_POSITION, 1, 1);
      hit(MessageType.QRY_POSITION);
    }

    @Override
    public void doQryPositionDetail(
        ServerSession session,
        CQryInvestorPositionDetail query,
        String requestID,
        int current,
        int total) {
      send(session, new CInvestorPositionDetail(),
          MessageType.RSP_QRY_POSI_DETAIL, 1, 3);
      send(session, new CInvestorPositionDetail(),
          MessageType.RSP_QRY_POSI_DETAIL, 2, 3);
      send(session, new CInvestorPositionDetail(),
          MessageType.RSP_QRY_POSI_DETAIL, 3, 3);
      hit(MessageType.QRY_POSI_DETAIL);
    }

    @Override
    public void doQryInstrument(
        ServerSession session,
        CQryInstrument query,
        String requestID,
        int current,
        int total) {
      send(session, new CInstrument(),
          MessageType.RSP_QRY_INSTRUMENT, 1, 1);
      hit(MessageType.QRY_INSTRUMENT);
    }

    @Override
    public void doQryCommission(
        ServerSession session,
        CQryInstrumentCommissionRate query,
        String requestID,
        int current,
        int total) {
      send(session, new CInstrumentCommissionRate(),
          MessageType.RSP_QRY_COMMISSION, 1, 1);
      hit(MessageType.QRY_COMMISSION);
    }

    @Override
    public void doQryMargin(
        ServerSession session,
        CQryInstrumentMarginRate query,
        String requestID,
        int current,
        int total) {
      send(session, new CInstrumentMarginRate(),
          MessageType.RSP_QRY_MARGIN, 1, 1);
      hit(MessageType.QRY_MARGIN);
    }
  }

  class TestLoginManager extends LoginManager {
    @Override
    public int doLogin(ServerSession session, Message message) {
      if (message.Body != null) {
        assertTrue(message.Body instanceof CReqUserLogin);
        System.out.println(OP.toJson(message.Body));
      }
      if (message.RspInfo != null)
        System.out.println(OP.toJson(message.RspInfo));
      assertEquals(message.Type, MessageType.REQ_LOGIN);
      hit(message.Type);
      return ErrorCodes.NONE;
    }
  }

  @Test
  public void basic() {
    try {
      //..........Server.........
      var server = IOP.createServer();
      // Test message adaptor and response state.
      // Same adaptors.
      server.getAdaptorChain().addAdaptor(new TestServerMessageAdaptor());
      server.getAdaptorChain().addAdaptor(new TestServerMessageAdaptor());
      server.getAdaptorChain().addAdaptor(new TestServerMessageAdaptor());
      // Test login manager.
      server.setLoginManager(new TestLoginManager());
      // Test session adaptor.
      server.setSessionAdaptor(new TestServerSessionAdaptor());
      // Bind to address.
      server.bind(new InetSocketAddress(24501));
      //......... Client.........
      var client = IOP.createClient();
      // Test message adaptor.
      client.setMessageAdaptor(new TestClientMessageAdaptor());
      // Test session adaptor.
      client.setSessionAdaptor(new TestClientSessionAdaptor());
      // Connect to server.
      client.connect(new InetSocketAddress(24501));

      // Test client's session.
      var session = client.getSession();
      // Test heart beat.
      session.sendHeartbeat(UUID.randomUUID().toString());
      // Test login.
      login(session, new CReqUserLogin(), 1, 1);
      // Test logout.
      send(session, new CUserLogout(),
          MessageType.REQ_LOGOUT, 1, 1);
      // Test order insert.
      send(session, new CInputOrder(),
          MessageType.REQ_ORDER_INSERT, 1, 1);
      // Test order action.
      send(session, new CInputOrderAction(),
          MessageType.REQ_ORDER_ACTION, 1, 1);
      // Test query account.
      send(session, new CQryTradingAccount(),
          MessageType.QRY_ACCOUNT, 1, 1);
      // Test query order.
      send(session, new CQryOrder(),
          MessageType.QRY_ORDER, 1, 1);
      // Test query position.
      send(session, new CQryInvestorPosition(),
          MessageType.QRY_POSITION, 1, 1);
      // Test subscribe/unsubscribe.
      send(session, new CSubMarketData(), MessageType.SUB_MD,
          1, 1);
      send(session, new CUnsubMarketData(), MessageType.UNSUB_MD,
          1, 1);
      // Test authenticate.
      send(session, new CReqAuthenticate(), MessageType.REQ_AUTHENTICATE,
          1, 1);
      // Test settlement.
      send(session, new CSettlementInfoConfirm(), MessageType.REQ_SETTLEMENT,
          1, 1);
      // Test query position detail.
      send(session, new CQryInvestorPositionDetail(), MessageType.QRY_POSI_DETAIL,
          1, 1);
      // Test query instrument/commission/margin.
      send(session, new CQryInstrument(), MessageType.QRY_INSTRUMENT,
          1, 1);
      send(session, new CQryInstrumentCommissionRate(), MessageType.QRY_COMMISSION,
          1, 1);
      send(session, new CQryInstrumentMarginRate(), MessageType.QRY_MARGIN,
          1, 1);
      //.........Sleep........
      Thread.sleep(TimeUnit.SECONDS.toMillis(5));
      // Check all message types are tested.
      for (var type : MessageType.values())
        if (type != MessageType.HEARTBEAT)
          assertTrue("missing " + type, hit.contains(type));

      client.disconnect();
      System.out.println("EXIT");
    } catch (IOException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
