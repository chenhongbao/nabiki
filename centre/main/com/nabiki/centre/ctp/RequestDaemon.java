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
import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

class RequestDaemon implements Runnable {
  private final OrderProvider provider;
  private final Global global;
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd HH:mm:ss");
  private final Map<String, LocalDateTime> usedOrderRef = new ConcurrentHashMap<>();
  protected final int MAX_REQ_PER_SEC = 5;
  protected int sendCnt = 0;
  protected long threshold = TimeUnit.SECONDS.toMillis(1);
  protected long timeStamp = System.currentTimeMillis();

  private String lastOrderRef = "";
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition cond = lock.newCondition();

  public RequestDaemon(OrderProvider provider, Global global) {
    this.provider = provider;
    this.global = global;
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

  void signalOrderRef(String ref) {
    if (lastOrderRef != null && ref.compareTo(lastOrderRef) == 0) {
      lock.lock();
      try {
        cond.signal();
      } finally {
        lock.unlock();
      }
    }
  }

  private boolean waitOrderRsp(int value, TimeUnit unit) {
    if (lastOrderRef == null || lastOrderRef.length() == 0) {
      return true;
    }
    lock.lock();
    try {
      return cond.await(value, unit);
    } catch (InterruptedException e) {
      e.printStackTrace();
      return false;
    } finally {
      lock.unlock();
      // Reset last order ref after getting signaled.
      lastOrderRef = null;
    }
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
          // Don't change the order of requests in queue. The request is polled from
          // the front of the queue, then reset it to the first.
          provider.getPendingRequests().offerFirst(pend);
        } else {
          // Wait order rsp because async inserting order causes refs no auto-inc.
          // For example, ref(14) arrives, and then ref(13) arrives.
          if (!waitOrderRsp(15, TimeUnit.SECONDS)) {
            global.getLogger().warning("order rsp timeout[" + lastOrderRef + "]");
          }
          // Control max number of requests sent per second.
          trafficControl();
        }
      } catch (InterruptedException e) {
        if (provider.getWorkingState() == WorkingState.STOPPING
            || provider.getWorkingState() == WorkingState.STOPPED) {
          break;
        } else {
          global.getLogger().warning(
              Utils.formatLog("order daemon interrupted",
                  null, e.getMessage(),
                  null));
        }
      }
    }
  }

  private void trafficControl() throws InterruptedException {
    long curTimeStamp = System.currentTimeMillis();
    long diffTimeStamp = threshold - (curTimeStamp - timeStamp);
    if (diffTimeStamp > 0) {
      ++sendCnt;
      // Sleep before next second.
      if (sendCnt > MAX_REQ_PER_SEC) {
        Thread.sleep(diffTimeStamp);
        // Reset send count for next second.
        sendCnt = 0;
        timeStamp = System.currentTimeMillis();
      }
    } else {
      sendCnt = 1;
      timeStamp = System.currentTimeMillis();
    }
  }

  private PendingRequest trySendRequest() throws InterruptedException {
    PendingRequest pend = null;
    while (pend == null)
      pend = provider.getPendingRequests().pollFirst(1, TimeUnit.DAYS);
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
      if (r == 0) {
        provider.getMsgWriter().writeReq(pend.action);
      }
    } else if (pend.order != null) {
      var ref = pend.order.OrderRef;
      if (isOrderRefUsed(ref)) {
        global.getLogger().severe(String.format(
            "duplicated order[%s], previous order sent at %s",
            ref,
            getPrevOrderDateTime(ref)));
      } else {
        r = fillAndSendOrder(pend.order);
        if (r == 0) {
          // Remember the last order ref, wait for rsp.
          lastOrderRef = ref;
          provider.getMsgWriter().writeReq(pend.order);
        }
      }
    }
    // Check send ret code.
    // If fail sending the request, add it back to queue and sleep
    // for some time.
    if (r != 0) {
      warn(r, pend);
      return pend;
    } else {
      // Return null, indicates the request has been sent.
      // Otherwise, enqueue the request and wait.
      return null;
    }
  }

  private int fillAndSendOrder(CInputOrder input) {
    // Set correct users.
    input.BrokerID = provider.getLoginRsp().BrokerID;
    input.UserID = provider.getLoginRsp().UserID;
    input.InvestorID = provider.getLoginRsp().UserID;
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
    return provider.getApi().ReqOrderInsert(
        JNI.toJni(input),
        Utils.getIncrementID());
  }

  private int fillAndSendAction(CInputOrderAction action) {
    var instrInfo = global.getInstrInfo(action.InstrumentID);
    var rtn = provider.getMapper().getRtnOrder(action.OrderRef);
    // Use order ref + front ID + session ID by default.
    // Keep original order ref and instrument ID.
    action.FrontID = provider.getLoginRsp().FrontID;
    action.SessionID = provider.getLoginRsp().SessionID;
    // Set common fields.
    action.BrokerID = provider.getLoginRsp().BrokerID;
    action.InvestorID = provider.getLoginRsp().UserID;
    action.UserID = provider.getLoginRsp().UserID;
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
    return provider.getApi().ReqOrderAction(
        JNI.toJni(action),
        Utils.getIncrementID());
  }

  private boolean canTrade(String instrID) {
    var hour = global.getTradingHour(null, instrID);
    if (hour == null) {
      global.getLogger().warning(
          Utils.formatLog("trading hour global null", instrID,
              null, null));
      return false;
    }
    LocalTime now;
    var ins = global.getInstrInfo(instrID);
    if (ins != null && ins.Instrument != null) {
      now = provider.getTimeAligner().getAlignTime(
          ins.Instrument.ExchangeID,
          LocalTime.now());
    } else {
      now = LocalTime.now();
    }
    // If remote counter opens for some while during weekend.
    // Yes, some hosts do open for some time and the if-clause stops wrong sending.
    var dayOfWeek = LocalDate.now().getDayOfWeek();
    if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
      return false;
    }
    return provider.isConfirmed() && hour.contains(now.minusSeconds(1));
  }

  private String getInstrID(PendingRequest pend) {
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
    } else {
      return;
    }
    global.getLogger().warning(Utils.formatLog(hint, ref, null, r));
  }
}
