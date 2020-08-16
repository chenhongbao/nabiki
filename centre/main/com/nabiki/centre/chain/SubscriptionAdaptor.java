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

package com.nabiki.centre.chain;

import com.nabiki.centre.md.CandleRW;
import com.nabiki.centre.md.MarketDataReceiver;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.struct.*;
import com.nabiki.iop.Message;
import com.nabiki.iop.MessageType;
import com.nabiki.iop.ServerMessageAdaptor;
import com.nabiki.iop.ServerSession;
import com.nabiki.iop.x.OP;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionAdaptor extends ServerMessageAdaptor {
    private static class SessionMarketDataReceiver implements MarketDataReceiver {
        private final ServerSession session;
        private final Map<String, Boolean> map = new ConcurrentHashMap<>();

        SessionMarketDataReceiver(ServerSession session) {
            this.session = session;
        }

        void subscribe(String instrID) {
            this.map.put(instrID, true);
        }

        @Override
        public void depthReceived(CThostFtdcDepthMarketDataField depth) {
            if (this.map.containsKey(depth.InstrumentID))
                this.session.sendResponse(toMessage(depth));
        }

        @Override
        public void candleReceived(CThostFtdcCandleField candle) {
            if (this.map.containsKey(candle.InstrumentID))
                this.session.sendResponse(toMessage(candle));
        }
    }

    static String FRONT_MDRECEIVER_KEY = "front.mdrecv";
    private final MarketDataRouter router;
    private final CandleRW candlRW;

    SubscriptionAdaptor(MarketDataRouter router, CandleRW rw) {
        this.router = router;
        this.candlRW = rw;
    }

    private static Message toMessage(CThostFtdcDepthMarketDataField depth) {
        var rsp = new Message();
        rsp.Type = MessageType.FLOW_DEPTH;
        rsp.TotalCount = rsp.CurrentCount = 0;
        rsp.ResponseID = rsp.RequestID = "";
        rsp.Body = depth;
        return rsp;
    }

    private static Message toMessage(CThostFtdcCandleField candle) {
        var rsp = new Message();
        rsp.Type = MessageType.FLOW_CANDLE;
        rsp.TotalCount = rsp.CurrentCount = 0;
        rsp.ResponseID = rsp.RequestID = "";
        rsp.Body = candle;
        return rsp;
    }

    private SessionMarketDataReceiver getReceiver(ServerSession session) {
        var recv = session.getAttribute(FRONT_MDRECEIVER_KEY);
        if (recv == null) {
            recv = new SessionMarketDataReceiver(session);
            this.router.addReceiver((MarketDataReceiver)recv);
            session.setAttribute(FRONT_MDRECEIVER_KEY, recv);
        }
        return (SessionMarketDataReceiver)recv;
    }

    private void sendHistoryCandles(ServerSession session, String instrumentID) {
        for (var c : this.candlRW.queryCandle(instrumentID))
            session.sendResponse(toMessage(c));
    }

    private void sendRsp(ServerSession session, String instrID, String requestID) {
        var r = new Message();
        r.Type = MessageType.RSP_SUB_MD;
        r.RequestID = requestID;
        r.ResponseID = UUID.randomUUID().toString();
        r.CurrentCount = r.TotalCount = 1;
        // Set response body.
        var ins = new CThostFtdcSpecificInstrumentField();
        ins.InstrumentID = instrID;
        r.Body = ins;
        // Set rsp info.
        r.RspInfo = new CThostFtdcRspInfoField();
        r.RspInfo.ErrorID = TThostFtdcErrorCode.NONE;
        r.RspInfo.ErrorMsg = OP.getErrorMsg(r.RspInfo.ErrorID);
        session.sendResponse(r);
    }

    @Override
    public void doSubDepthMarketData(
            ServerSession session,
            CThostFtdcSubMarketDataField request,
            String requestID,
            int current,
            int total) {
        var recv = getReceiver(session);
        for (var instrID : request.InstrumentID) {
            sendHistoryCandles(session, instrID);
            recv.subscribe(instrID);
            sendRsp(session, instrID, requestID);
        }
        session.done();
    }
}
