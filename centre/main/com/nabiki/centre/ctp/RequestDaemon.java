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

import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.*;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

class RequestDaemon implements Runnable {
  private final OrderProvider orderProvider;
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
  private final Map<String, LocalDateTime> usedOrderRef = new ConcurrentHashMap<>();
  protected final int MAX_REQ_PER_SEC = 5;
  protected int sendCnt = 0;
  protected long threshold = TimeUnit.SECONDS.toMillis(1);
  protected long timeStamp = System.currentTimeMillis();

  public RequestDaemon(OrderProvider orderProvider) {
    this.orderProvider = orderProvider;
  }

  private boolean isOrderRefUsed(String orderRef) {
    if (orderRef == null || usedOrderRef.containsKey(orderRef))
      return true;
    else {
      usedOrderRef.put(orderRef, LocalDateTime.now());
      return false;
    }
  }

  private String getPrevOrderDateTime(String ref) {
    var time = usedOrderRef.get(ref);
    if (time == null)
      return "";
    else
      return time.format(formatter);
  }

  void clearOrderRef() {
    usedOrderRef.clear();
  }

  @Override
  public void run() {

    while (!Thread.interrupted()) {
      try {
        var pend = trySendRequest();
        // Pending request is not sent, enqueue the request for next
        // loop.
        if (pend != null) {
          Thread.sleep(threshold);
          orderProvider.pendingReqs.offer(pend);
        }
      } catch (InterruptedException e) {
        if (orderProvider.workingState == WorkingState.STOPPING
                || orderProvider.workingState == WorkingState.STOPPED)
          break;
        else
          orderProvider.global.getLogger().warning(
                  Utils.formatLog("order daemon interrupted",
                          null, e.getMessage(),
                          null));
      }
    }
  }

  private PendingRequest trySendRequest() throws InterruptedException {
    PendingRequest pend = null;
    while (pend == null)
      pend = orderProvider.pendingReqs.poll(1, TimeUnit.DAYS);
    // Await time out, or notified by new request.
    // Instrument not trading.
    if (!canTrade(getInstrID(pend))) {
      return pend;
    }
    int r = 0;
    // Send order or action.
    // Fill and send order at first place so its fields are filled.
    if (pend.action != null) {
      r = fillAndSendAction(pend.action);
      if (r == 0)
        orderProvider.msgWriter.writeReq(pend.action);
    } else if (pend.order != null) {
      var ref = pend.order.OrderRef;
      if (isOrderRefUsed(ref)) {
        orderProvider.global.getLogger().severe(String.format(
                "duplicated order[%s], previous order sent at %s",
                ref,
                getPrevOrderDateTime(ref)));
      } else {
        r = fillAndSendOrder(pend.order);
        if (r == 0)
          orderProvider.msgWriter.writeReq(pend.order);
      }
    }
    // Check send ret code.
    // If fail sending the request, add it back to queue and sleep
    // for some time.
    if (r != 0) {
      warn(r, pend);
      return pend;
    }
    // Flow control.
    long curTimeStamp = System.currentTimeMillis();
    long diffTimeStamp = threshold - (curTimeStamp - timeStamp);
    if (diffTimeStamp > 0) {
      ++sendCnt;
      if (sendCnt > MAX_REQ_PER_SEC) {
        Thread.sleep(diffTimeStamp);
        timeStamp = System.currentTimeMillis();
      }
    } else {
      sendCnt = 0;
      timeStamp = System.currentTimeMillis();
    }
    // Return null, indicates the request has been sent.
    // Otherwise, enqueue the request and wait.
    return null;
  }

  protected int fillAndSendOrder(CInputOrder input) {
    // Set correct users.
    input.BrokerID = orderProvider.rspLogin.BrokerID;
    input.UserID = orderProvider.rspLogin.UserID;
    input.InvestorID = orderProvider.rspLogin.UserID;
    // Adjust flags.
    input.CombHedgeFlag = CombHedgeFlagType.SPECULATION;
    input.ContingentCondition = ContingentConditionType.IMMEDIATELY;
    input.ForceCloseReason = ForceCloseReasonType.NOT_FORCE_CLOSE;
    input.IsAutoSuspend = 0;
    input.MinVolume = 1;
    input.OrderPriceType = OrderPriceTypeType.LIMIT_PRICE;
    input.StopPrice = 0;
    input.TimeCondition = TimeConditionType.GFD;
    input.VolumeCondition = VolumeConditionType.ANY_VOLUME;
    return orderProvider.api.ReqOrderInsert(
            JNI.toJni(input),
            Utils.getIncrementID());
  }

  protected int fillAndSendAction(CInputOrderAction action) {
    var instrInfo = orderProvider.global.getInstrInfo(action.InstrumentID);
    var rtn = orderProvider.mapper.getRtnOrder(action.OrderRef);
    // Use order ref + front ID + session ID by default.
    // Keep original order ref and instrument ID.
    action.FrontID = orderProvider.rspLogin.FrontID;
    action.SessionID = orderProvider.rspLogin.SessionID;
    // Set common fields.
    action.BrokerID = orderProvider.rspLogin.BrokerID;
    action.InvestorID = orderProvider.rspLogin.UserID;
    action.UserID = orderProvider.rspLogin.UserID;
    // Action delete.
    action.ActionFlag = ActionFlagType.DELETE;
    // Set order sys ID if possible.
    if (rtn != null) {
      // Try OrderSysID.
      action.OrderSysID = rtn.OrderSysID;
      // Adjust flags.
      // Must need exchange id.
      action.ExchangeID = rtn.ExchangeID;
    } else {
      action.ExchangeID = (instrInfo.Instrument != null)
              ? instrInfo.Instrument.ExchangeID : null;
    }
    return orderProvider.api.ReqOrderAction(
            JNI.toJni(action),
            Utils.getIncrementID());
  }

  protected boolean canTrade(String instrID) {
    var hour = orderProvider.global.getTradingHour(null, instrID);
    if (hour == null) {
      orderProvider.global.getLogger().warning(
              Utils.formatLog("trading hour global null", instrID,
                      null, null));
      return false;
    }
    LocalTime now;
    var ins = orderProvider.global.getInstrInfo(instrID);
    if (ins != null && ins.Instrument != null)
      now = orderProvider.timeAligner.getAlignTime(ins.Instrument.ExchangeID,
              LocalTime.now());
    else
      now = LocalTime.now();
    return orderProvider.isConfirmed && hour.contains(now.minusSeconds(1));
  }

  protected String getInstrID(PendingRequest pend) {
    if (pend.action != null)
      return pend.action.InstrumentID;
    else if (pend.order != null)
      return pend.order.InstrumentID;
    else
      return null;
  }

  protected void warn(int r, PendingRequest pend) {
    String ref, hint;
    if (pend.order != null) {
      ref = pend.order.OrderRef;
      hint = "failed sending order";
    } else if (pend.action != null) {
      ref = pend.action.OrderRef;
      hint = "failed sending action";
    } else
      return;
    orderProvider.global.getLogger().warning(
            Utils.formatLog(hint, ref, null, r));
  }
}
