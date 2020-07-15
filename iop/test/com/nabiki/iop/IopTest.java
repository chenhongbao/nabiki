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

import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.frame.Body;
import com.nabiki.iop.x.OP;
import com.nabiki.iop.x.SystemStream;
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
                CThostFtdcSpecificInstrumentField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_SUB_MD);
        }

        @Override
        public void doRspUnsubscribeMarketData(
                CThostFtdcSpecificInstrumentField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_UNSUB_MD);
        }

        @Override
        public void doRspDepthMarketData(
                CThostFtdcDepthMarketDataField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.FLOW_DEPTH);
        }

        @Override
        public void doRspCandle(
                CThostFtdcCandleField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.FLOW_CANDLE);
        }

        @Override
        public void doRspAuthenticate(
                CThostFtdcRspAuthenticateField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_AUTHENTICATE);
        }

        @Override
        public void doRspReqLogin(
                CThostFtdcRspUserLoginField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_LOGIN);
        }

        @Override
        public void doRspReqLogout(
                CThostFtdcUserLogoutField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_LOGOUT);
        }

        @Override
        public void doRspReqSettlementConfirm(
                CThostFtdcSettlementInfoConfirmField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_SETTLEMENT);
        }

        @Override
        public void doRspReqOrderInsert(
                CThostFtdcOrderField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_ORDER_INSERT);
        }

        @Override
        public void doRspReqOrderAction(
                CThostFtdcOrderActionField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_REQ_ORDER_ACTION);
        }

        @Override
        public void doRspQryAccount(
                CThostFtdcTradingAccountField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_ACCOUNT);
        }

        @Override
        public void doRspQryOrder(
                CThostFtdcOrderField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_ORDER);
        }

        @Override
        public void doRspQryPosition(
                CThostFtdcInvestorPositionField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_POSITION);
        }

        @Override
        public void doRspQryPositionDetail(
                CThostFtdcInvestorPositionDetailField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_POSI_DETAIL);
        }

        @Override
        public void doRspQryInstrument(
                CThostFtdcInstrumentField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_INSTRUMENT);
        }

        @Override
        public void doRspQryCommission(
                CThostFtdcInstrumentCommissionRateField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_COMMISSION);
        }

        @Override
        public void doRspQryMargin(
                CThostFtdcInstrumentMarginRateField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_QRY_MARGIN);
        }

        @Override
        public void doRtnOrder(
                CThostFtdcOrderField rtn,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RTN_ORDER);
        }

        @Override
        public void doRtnTrade(
                CThostFtdcTradeField rtn,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RTN_TRADE);
        }

        @Override
        public void doRtnOrderAction(
                CThostFtdcOrderActionField rtn,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RTN_ORDER_ACTION);
        }

        @Override
        public void doRtnOrderInsert(
                CThostFtdcInputOrderField rtn,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RTN_ORDER_INSERT);
        }

        @Override
        public void doRspOrderAction(
                CThostFtdcInputOrderActionField rsp,
                CThostFtdcRspInfoField info,
                String requestID,
                String responseID,
                int current,
                int total) {
            hit(MessageType.RSP_ORDER_ACTION);
        }

        @Override
        public void doRspOrderInsert(
                CThostFtdcInputOrderField rsp,
                CThostFtdcRspInfoField info,
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
                CThostFtdcSubMarketDataField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcSpecificInstrumentField(),
                    MessageType.RSP_SUB_MD, 1, 1);
            hit(MessageType.SUB_MD);
            // Send flow.
            send(session, new CThostFtdcDepthMarketDataField(),
                    MessageType.FLOW_DEPTH, 1, 1);
            send(session, new CThostFtdcCandleField(),
                    MessageType.FLOW_CANDLE, 1, 1);
        }

        @Override
        public void doUnsubDepthMarketData(
                ServerSession session,
                CThostFtdcUnsubMarketDataField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcSpecificInstrumentField(),
                    MessageType.RSP_UNSUB_MD, 1, 1);
            hit(MessageType.UNSUB_MD);
        }

        @Override
        public void doReqAuthenticate(
                ServerSession session,
                CThostFtdcReqAuthenticateField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcRspAuthenticateField(),
                    MessageType.RSP_REQ_AUTHENTICATE, 1, 1);
            hit(MessageType.REQ_AUTHENTICATE);
        }

        @Override
        public void doReqLogin(
                ServerSession session,
                CThostFtdcReqUserLoginField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcRspUserLoginField(),
                    MessageType.RSP_REQ_LOGIN, 1, 1);
            hit(MessageType.REQ_LOGIN);
        }

        @Override
        public void doReqLogout(
                ServerSession session,
                CThostFtdcUserLogoutField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcUserLogoutField(),
                    MessageType.RSP_REQ_LOGOUT, 1, 1);
            hit(MessageType.REQ_LOGOUT);
        }

        @Override
        public void doReqSettlementConfirm(
                ServerSession session,
                CThostFtdcSettlementInfoConfirmField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcSettlementInfoConfirmField(),
                    MessageType.RSP_REQ_SETTLEMENT, 1, 1);
            hit(MessageType.REQ_SETTLEMENT);
        }

        @Override
        public void doReqOrderInsert(
                ServerSession session,
                CThostFtdcInputOrderField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcOrderField(),
                    MessageType.RSP_REQ_ORDER_INSERT, 1, 2);
            send(session, new CThostFtdcOrderField(),
                    MessageType.RSP_REQ_ORDER_INSERT, 2, 2);
            hit(MessageType.REQ_ORDER_INSERT);
            // Send rtn.
            send(session, new CThostFtdcOrderField(),
                    MessageType.RTN_ORDER, 1, 1);
            send(session, new CThostFtdcTradeField(),
                    MessageType.RTN_TRADE, 1, 1);
            // Send error.
            send(session, new CThostFtdcInputOrderField(),
                    MessageType.RSP_ORDER_INSERT, 1, 1);
            send(session, new CThostFtdcInputOrderField(),
                    MessageType.RTN_ORDER_INSERT, 1, 1);
        }

        @Override
        public void doReqOrderAction(
                ServerSession session,
                CThostFtdcInputOrderActionField request,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcOrderActionField(),
                    MessageType.RSP_REQ_ORDER_ACTION, 1, 1);
            hit(MessageType.REQ_ORDER_ACTION);
            // Send error.
            send(session, new CThostFtdcInputOrderActionField(),
                    MessageType.RSP_ORDER_ACTION, 1, 1);
            send(session, new CThostFtdcOrderActionField(),
                    MessageType.RTN_ORDER_ACTION, 1, 1);
        }

        @Override
        public void doQryAccount(
                ServerSession session,
                CThostFtdcQryTradingAccountField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcTradingAccountField(),
                    MessageType.RSP_QRY_ACCOUNT, 1, 1);
            hit(MessageType.QRY_ACCOUNT);
        }

        @Override
        public void doQryOrder(
                ServerSession session,
                CThostFtdcQryOrderField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcOrderField(),
                    MessageType.RSP_QRY_ORDER, 1, 1);
            hit(MessageType.QRY_ORDER);
        }

        @Override
        public void doQryPosition(
                ServerSession session,
                CThostFtdcQryInvestorPositionField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcInvestorPositionField(),
                    MessageType.RSP_QRY_POSITION, 1, 1);
            hit(MessageType.QRY_POSITION);
        }

        @Override
        public void doQryPositionDetail(
                ServerSession session,
                CThostFtdcQryInvestorPositionDetailField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcInvestorPositionDetailField(),
                    MessageType.RSP_QRY_POSI_DETAIL, 1, 3);
            send(session, new CThostFtdcInvestorPositionDetailField(),
                    MessageType.RSP_QRY_POSI_DETAIL, 2, 3);
            send(session, new CThostFtdcInvestorPositionDetailField(),
                    MessageType.RSP_QRY_POSI_DETAIL, 3, 3);
            hit(MessageType.QRY_POSI_DETAIL);
        }

        @Override
        public void doQryInstrument(
                ServerSession session,
                CThostFtdcQryInstrumentField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcInstrumentField(),
                    MessageType.RSP_QRY_INSTRUMENT, 1, 1);
            hit(MessageType.QRY_INSTRUMENT);
        }

        @Override
        public void doQryCommission(
                ServerSession session,
                CThostFtdcQryInstrumentCommissionRateField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcInstrumentCommissionRateField(),
                    MessageType.RSP_QRY_COMMISSION, 1, 1);
            hit(MessageType.QRY_COMMISSION);
        }

        @Override
        public void doQryMargin(
                ServerSession session,
                CThostFtdcQryInstrumentMarginRateField query,
                String requestID,
                int current,
                int total) {
            send(session, new CThostFtdcInstrumentMarginRateField(),
                    MessageType.RSP_QRY_MARGIN, 1, 1);
            hit(MessageType.QRY_MARGIN);
        }
    }

    class TestLoginManager extends LoginManager {
        @Override
        public int doLogin(ServerSession session, Message message) {
            if (message.Body != null) {
                assertTrue(message.Body instanceof CThostFtdcReqUserLoginField);
                System.out.println(OP.toJson(message.Body));
            }
            if (message.RspInfo != null)
                System.out.println(OP.toJson(message.RspInfo));
            assertEquals(message.Type, MessageType.REQ_LOGIN);
            hit(message.Type);
            return TThostFtdcErrorCode.NONE;
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
            login(session, new CThostFtdcReqUserLoginField(), 1, 1);
            // Test logout.
            send(session, new CThostFtdcUserLogoutField(),
                    MessageType.REQ_LOGOUT, 1, 1);
            // Test order insert.
            send(session, new CThostFtdcInputOrderField(),
                    MessageType.REQ_ORDER_INSERT, 1, 1);
            // Test order action.
            send(session, new CThostFtdcInputOrderActionField(),
                    MessageType.REQ_ORDER_ACTION, 1, 1);
            // Test query account.
            send(session, new CThostFtdcQryTradingAccountField(),
                    MessageType.QRY_ACCOUNT, 1, 1);
            // Test query order.
            send(session, new CThostFtdcQryOrderField(),
                    MessageType.QRY_ORDER, 1, 1);
            // Test query position.
            send(session, new CThostFtdcQryInvestorPositionField(),
                    MessageType.QRY_POSITION, 1, 1);
            // Test subscribe/unsubscribe.
            send(session, new CThostFtdcSubMarketDataField(), MessageType.SUB_MD,
                    1, 1);
            send(session, new CThostFtdcUnsubMarketDataField(), MessageType.UNSUB_MD,
                    1, 1);
            // Test authenticate.
            send(session, new CThostFtdcReqAuthenticateField(), MessageType.REQ_AUTHENTICATE,
                    1, 1);
            // Test settlement.
            send(session, new CThostFtdcSettlementInfoConfirmField(), MessageType.REQ_SETTLEMENT,
                    1, 1);
            // Test query position detail.
            send(session, new CThostFtdcQryInvestorPositionDetailField(), MessageType.QRY_POSI_DETAIL,
                    1, 1);
            // Test query instrument/commission/margin.
            send(session, new CThostFtdcQryInstrumentField(), MessageType.QRY_INSTRUMENT,
                    1, 1);
            send(session, new CThostFtdcQryInstrumentCommissionRateField(), MessageType.QRY_COMMISSION,
                    1, 1);
            send(session, new CThostFtdcQryInstrumentMarginRateField(), MessageType.QRY_MARGIN,
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
