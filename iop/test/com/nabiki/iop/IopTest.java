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

import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.x.OP;
import org.apache.mina.core.session.IdleStatus;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
                assertTrue(eventObject instanceof  Throwable);
                System.out.println(((Throwable)eventObject).getMessage());
                break;
            case IDLE:
                assertTrue(eventObject instanceof IdleStatus);
                System.out.println(eventObject);
                break;
            case MISS_HEARTBEAT:
                assertTrue(eventObject instanceof UUID);
                System.out.println(eventObject);
                break;
            case MESSAGE_NOT_DONE:
            case STRANGE_MESSAGE:
                assertTrue(eventObject instanceof  Message);
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
            m.RspInfo = new CThostFtdcRspInfoField();
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
            m.RspInfo = new CThostFtdcRspInfoField();
        session.sendRequest(m);
    }

    static void login(ClientSession session, Object object, int current, int total) {
        var m = new Message();
        m.CurrentCount = current;
        m.TotalCount = total;
        m.Body = object;
        if (new Random().nextDouble() > 0.5)
            m.RspInfo = new CThostFtdcRspInfoField();
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

    static Map<MessageType, Boolean> hit;
    static {
        hit = new ConcurrentHashMap<>();
        for (var type : MessageType.values())
            hit.put(type, false);
    }

    private void hit(MessageType type) {
        hit.put(type, true);
    }

    class TestClientMessageAdaptor extends ClientMessageAdaptor {
        @Override
        public void doRspReqLogin(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcRspUserLoginField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_REQ_LOGIN);
            hit(message.Type);
        }

        @Override
        public void doRspReqOrderInsert(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcInputOrderField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_REQ_ORDER_INSERT);hit(message.Type);
            hit(message.Type);
        }

        @Override
        public void doRspReqOrderAction(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcInputOrderActionField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_REQ_ORDER_ACTION);
            hit(message.Type);
        }

        @Override
        public void doRspQryAccount(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcTradingAccountField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_QRY_ACCOUNT);
            hit(message.Type);
        }

        @Override
        public void doRspQryOrder(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcOrderField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_QRY_ORDER);
            hit(message.Type);
        }

        @Override
        public void doRspQryPosition(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcInvestorPositionField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_QRY_POSITION);
            assertTrue(message.CurrentCount <= message.TotalCount);
            System.out.println(message.CurrentCount + "/" + message.TotalCount);
            hit(message.Type);
        }

        @Override
        public void doRspSubscribeMarketData(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcSpecificInstrumentField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.RSP_SUB_MD);
            hit(message.Type);
        }

        @Override
        public void doRspDepthMarketData(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcDepthMarketDataField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.FLOW_DEPTH);
            hit(message.Type);
        }

        @Override
        public void doRspCandle(Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcCandleField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.FLOW_CANDLE);
            hit(message.Type);
        }
    }

    class TestServerMessageAdaptor extends ServerMessageAdaptor {
        @Override
        public void doReqOrderInsert(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcInputOrderField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.REQ_ORDER_INSERT);
            // Send response.
            send(session, new CThostFtdcRspUserLoginField(),
                    MessageType.RSP_REQ_ORDER_INSERT, 1, 1);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }

        @Override
        public void doReqOrderAction(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcInputOrderActionField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.REQ_ORDER_ACTION);
            // Send response.
            send(session, message.Body, MessageType.RSP_REQ_ORDER_ACTION,
                    1, 1);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }

        @Override
        public void doQryAccount(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcQryTradingAccountField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.QRY_ACCOUNT);
            // Send response.
            send(session, new CThostFtdcTradingAccountField(),
                    MessageType.RSP_QRY_ACCOUNT, 1, 1);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }

        @Override
        public void doQryOrder(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcOrderUuidField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.QRY_ORDER);
            // Send response.
            send(session, new CThostFtdcOrderField(), MessageType.RSP_QRY_ORDER,
                    1, 1);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }

        @Override
        public void doQryPosition(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcQryInvestorPositionField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.QRY_POSITION);
            // Send response.
            send(session, new CThostFtdcInvestorPositionField(),
                    MessageType.RSP_QRY_POSITION, 1, 3);
            send(session, new CThostFtdcInvestorPositionField(),
                    MessageType.RSP_QRY_POSITION, 2, 3);
            send(session, new CThostFtdcInvestorPositionField(),
                    MessageType.RSP_QRY_POSITION, 3, 3);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }

        @Override
        public void doSubDepthMarketData(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcSubMarketDataField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.SUB_MD);
            // Send response.
            send(session, new CThostFtdcSpecificInstrumentField(),
                    MessageType.RSP_SUB_MD,1, 1);
            // Send one depth market data back.
            send(session, new CThostFtdcDepthMarketDataField(),
                    MessageType.FLOW_DEPTH, 1, 1);
            // Send one candle.
            send(session, new CThostFtdcCandleField(),
                    MessageType.FLOW_CANDLE, 1, 1);
            if (new Random().nextDouble() > 0.5)
                session.done(); // Have a chance the message is not done.
            hit(message.Type);
        }
    }

    class TestLoginManager extends LoginManager {
        @Override
        public boolean doLogin(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcReqUserLoginField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.REQ_LOGIN);
            hit(message.Type);
            return true;
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
            session.sendHeartbeat(UUID.randomUUID());
            // Test login request.
            login(session, new CThostFtdcReqUserLoginField(), 1, 1);
            // Test order insert.
            send(session, new CThostFtdcInputOrderField(),
                    MessageType.REQ_ORDER_INSERT,1, 1);
            // Test order action.
            send(session, new CThostFtdcInputOrderActionField(),
                    MessageType.REQ_ORDER_ACTION,1, 1);
            // Test query account.
            send(session, new CThostFtdcQryTradingAccountField(),
                    MessageType.QRY_ACCOUNT,1, 1);
            // Test query order.
            send(session, new CThostFtdcOrderUuidField(),
                    MessageType.QRY_ORDER, 1, 1);
            // Test query position.
            send(session, new CThostFtdcQryInvestorPositionField(),
                    MessageType.QRY_POSITION, 1, 1);
            // Test subscribe md
            send(session, new CThostFtdcSubMarketDataField(), MessageType.SUB_MD,
                    1, 1);
            //.........Sleep........
            Thread.sleep(TimeUnit.SECONDS.toMillis(5));
            // Check all message types are tested.
            for (var entry : hit.entrySet()) {
                if (entry.getKey() != MessageType.HEARTBEAT)
                    assertTrue(entry.getKey() + " missed",
                            entry.getValue());
            }
            System.out.println("EXIT");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
