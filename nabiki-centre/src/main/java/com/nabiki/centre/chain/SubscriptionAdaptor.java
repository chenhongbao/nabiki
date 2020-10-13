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

package com.nabiki.centre.chain;

import com.nabiki.centre.config.Global;
import com.nabiki.centre.md.CandleRW;
import com.nabiki.centre.md.MarketDataReceiver;
import com.nabiki.centre.md.MarketDataRouter;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.iop.ServerMessageAdaptor;
import com.nabiki.commons.iop.ServerSession;
import com.nabiki.commons.utils.Utils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SubscriptionAdaptor extends ServerMessageAdaptor {
  private class SessionMarketDataReceiver implements MarketDataReceiver {
    private final ServerSession session;
    private final Map<String, Boolean> map = new ConcurrentHashMap<>();

    SessionMarketDataReceiver(ServerSession session) {
      this.session = session;
    }

    void subscribe(String instrID) {
      this.map.put(instrID, true);
    }

    void unsubscribe(String instrID) {
      this.map.remove(instrID);
    }

    @Override
    public void depthReceived(CDepthMarketData depth) {
      try {
        if (!this.session.isClosed()
            && this.map.containsKey(depth.InstrumentID))
          this.session.sendResponse(toMessage(depth));
      } catch (Throwable th) {
        th.printStackTrace();
        global.getLogger().warning(th.getMessage());
      }
    }

    @Override
    public void candleReceived(CCandle candle) {
      try {
        if (!this.session.isClosed() &&
            this.map.containsKey(candle.InstrumentID))
          this.session.sendResponse(toMessage(candle));
      } catch (Throwable th) {
        th.printStackTrace();
        global.getLogger().warning(th.getMessage());
      }
    }
  }

  static String FRONT_MDRECEIVER_KEY = "front.mdrecv";
  private final MarketDataRouter router;
  private final CandleRW candlRW;
  private final Global global;

  public SubscriptionAdaptor(MarketDataRouter router, CandleRW rw, Global global) {
    this.router = router;
    this.candlRW = rw;
    this.global = global;
  }

  private static Message toMessage(CDepthMarketData depth) {
    var rsp = new Message();
    rsp.Type = MessageType.FLOW_DEPTH;
    rsp.TotalCount = rsp.CurrentCount = 0;
    rsp.ResponseID = rsp.RequestID = "";
    rsp.Body = depth;
    return rsp;
  }

  private static Message toMessage(CCandle candle) {
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
      this.router.addReceiver((MarketDataReceiver) recv);
      session.setAttribute(FRONT_MDRECEIVER_KEY, recv);
    }
    return (SessionMarketDataReceiver) recv;
  }

  private void sendHistoryCandles(ServerSession session, String instrumentID) {
    for (var c : this.candlRW.queryCandle(instrumentID))
      session.sendResponse(toMessage(c));
  }

  private void sendRsp(
      ServerSession session,
      MessageType type,
      String instrID,
      String requestID,
      int count,
      int total,
      int errorCode) {
    var r = new Message();
    r.Type = type;
    r.RequestID = requestID;
    r.ResponseID = UUID.randomUUID().toString();
    r.CurrentCount = count;
    r.TotalCount = total;
    // Set response body.
    var ins = new CSpecificInstrument();
    ins.InstrumentID = instrID;
    r.Body = ins;
    // Set rsp info.
    r.RspInfo = new CRspInfo();
    r.RspInfo.ErrorID = errorCode;
    r.RspInfo.ErrorMsg = Utils.getErrorMsg(r.RspInfo.ErrorID);
    session.sendResponse(r);
  }

  private List<String> getValidInstruments(String[] instrID) {
    var r = new LinkedList<String>();
    if (instrID == null)
      return r;
    for (var s : instrID) {
      if (this.global.getInstrInfo(s) != null)
        r.add(s);
      else
        this.global.getLogger().warning("unknown instrument: " + s);
    }
    return r;
  }

  @Override
  public void doSubDepthMarketData(
      ServerSession session,
      CSubMarketData request,
      String requestID,
      int current,
      int total) {
    var recv = getReceiver(session);
    var instr = getValidInstruments(request.InstrumentID);
    if (instr.size() == 0) {
      sendRsp(
          session,
          MessageType.RSP_SUB_MD,
          "",
          requestID,
          1,
          1,
          ErrorCodes.NONE);
    } else {
      int count = 0;
      for (var instrID : instr) {
        int errorCode = ErrorCodes.NONE;
        try {
          sendHistoryCandles(session, instrID);
          recv.subscribe(instrID);
        } catch (Throwable ignored) {
          errorCode = ErrorCodes.BAD_FIELD;
        } finally {
          sendRsp(
              session,
              MessageType.RSP_SUB_MD,
              instrID,
              requestID,
              ++count,
              instr.size(),
              errorCode);
        }
      }
    }
    session.done();
  }

  @Override
  public void doUnsubDepthMarketData(
      ServerSession session,
      CUnsubMarketData request,
      String requestID,
      int current,
      int total) {
    var recv = getReceiver(session);
    if (request.InstrumentID == null || request.InstrumentID.length == 0) {
      sendRsp(
          session,
          MessageType.RSP_UNSUB_MD,
          "",
          requestID,
          1,
          1,
          ErrorCodes.NONE);
    } else {
      int count = 0;
      for (var instrID : request.InstrumentID) {
        int errorCode = ErrorCodes.NONE;
        try {
          recv.unsubscribe(instrID);
        } catch (Throwable ignored) {
          errorCode = ErrorCodes.BAD_FIELD;
        } finally {
          sendRsp(
              session,
              MessageType.RSP_UNSUB_MD,
              instrID,
              requestID,
              ++count,
              request.InstrumentID.length,
              errorCode);
        }
      }
    }
    session.done();
  }
}
