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

package com.nabiki.centre.ctp;

import com.nabiki.centre.utils.Global;
import com.nabiki.centre.utils.Signal;
import com.nabiki.centre.utils.Utils;
import com.nabiki.commons.ctpobj.CQryInstrumentCommissionRate;
import com.nabiki.commons.ctpobj.CQryInstrumentMarginRate;
import com.nabiki.commons.ctpobj.CombHedgeFlagType;

import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class QueryTask implements Runnable {
  private final OrderProvider provider;
  private final Global global;

  // Wait last request return.
  protected final long qryWaitMillis = TimeUnit.SECONDS.toMillis(10);
  protected final Signal lastRtn = new Signal();
  protected final AtomicInteger lastID = new AtomicInteger(0);

  QueryTask(OrderProvider provider, Global global) {
    this.provider = provider;
    this.global = global;
  }

  void signalRequest(int requestID) {
    if (this.lastID.get() == requestID)
      this.lastRtn.signal();
  }

  private boolean waitRequestRsp(
      long millis, int requestID) throws InterruptedException {
    this.lastID.set(requestID);
    return this.lastRtn.waitSignal(millis);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        if (!provider.isQryInstrLast() || !provider.isConfirmed()) {
          continue;
        }
        var ids = new LinkedList<String>(provider.getInstrumentIDs());
        for (var id : ids) {
          var info = global.getInstrInfo(id);
          // Instrument should be return by default.
          if (info == null || info.Instrument == null) {
            continue;
          }
          if (info.Margin == null) {
            queryMargin(id);
            sleep(1, TimeUnit.SECONDS);
          } else if (info.Commission == null) {
            queryCommission(id);
            sleep(1, TimeUnit.SECONDS);
          }
        }
        // Sleep for next round.
      } catch (Throwable th) {
        th.printStackTrace();
        global.getLogger().warning(th.getMessage());
      } finally {
        sleep(1, TimeUnit.HOURS);
      }
    }
  }

  private void sleep(int value, TimeUnit unit) {
    try {
      unit.sleep(value);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private void queryMargin(String ins) {
    var req = new CQryInstrumentMarginRate();
    req.BrokerID = provider.getLoginCfg().BrokerID;
    req.InvestorID = provider.getLoginCfg().UserID;
    req.HedgeFlag = CombHedgeFlagType.SPECULATION;
    req.InstrumentID = ins;
    var reqID = Utils.getIncrementID();
    int r = provider.getApi().ReqQryInstrumentMarginRate(
        JNI.toJni(req),
        reqID);
    if (r != 0) {
      global.getLogger().warning(Utils.formatLog(
          "failed query margin", null, ins, r));
    } else {
      // Sleep up tp some seconds.
      try {
        if (!waitRequestRsp(qryWaitMillis, reqID))
          global.getLogger().warning("query margin timeout: " + ins);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }

  private void queryCommission(String ins) {
    var req0 = new CQryInstrumentCommissionRate();
    req0.BrokerID = provider.getLoginCfg().BrokerID;
    req0.InvestorID = provider.getLoginCfg().UserID;
    req0.InstrumentID = ins;
    var reqID = Utils.getIncrementID();
    var r = provider.getApi().ReqQryInstrumentCommissionRate(
        JNI.toJni(req0),
        reqID);
    if (r != 0) {
      global.getLogger().warning(Utils.formatLog(
          "failed query commission", null, ins, r));
    } else {
      // Sleep up tp some seconds.
      try {
        if (!waitRequestRsp(qryWaitMillis, reqID))
          global.getLogger().warning("query margin timeout: " + ins);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
}
