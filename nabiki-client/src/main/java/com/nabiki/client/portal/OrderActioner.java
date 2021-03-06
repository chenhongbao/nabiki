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

package com.nabiki.client.portal;

import com.nabiki.client.sdk.ResponseConsumer;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.commons.ctpobj.CInputOrderAction;
import com.nabiki.commons.ctpobj.COrderAction;
import com.nabiki.commons.ctpobj.CRspInfo;
import com.nabiki.commons.ctpobj.ErrorCodes;

import javax.swing.*;
import java.util.concurrent.TimeUnit;

public class OrderActioner extends Updater implements Runnable {
  private final TradeClient client;
  private final OrderUpdater updater;
  private final JComponent owner;
  private Thread daemon;
  private String orderID;
  private JButton src, rel;

  OrderActioner(JComponent owner, TradeClient client, OrderUpdater updater) {
    this.owner = owner;
    this.client = client;
    this.updater = updater;
  }

  public void cancel(String orderID, Object src, Object rel) {
    this.src = (JButton) src;
    this.rel = (JButton) rel;
    this.orderID = orderID;
    super.fire();
  }

  public void start() {
    daemon = new Thread(this);
    daemon.setDaemon(true);
    daemon.start();
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      try {
        super.waitFire();
        actionOrder();
      } catch (Throwable th) {
        showMsg(th.getMessage());
      }
    }
  }

  private void actionOrder() throws Exception {
    var req = new CInputOrderAction();
    req.OrderSysID = orderID;
    var rsp = client.orderAction(req);
    rsp.consume(new ResponseConsumer<COrderAction>() {
      @Override
      public void accept(COrderAction object, CRspInfo rspInfo, int currentCount,
                         int totalCount) {
        if (rspInfo != null && rspInfo.ErrorID != ErrorCodes.NONE) {
          showMsg(String.format("[%d]%s", rspInfo.ErrorID, rspInfo.ErrorMsg));
        } else {
          updater.query(orderID, rel);
          src.setEnabled(true);
        }
      }
    });
    src.setEnabled(false);
    sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
    src.setEnabled(true);
    if (!rsp.hasResponse()) {
      showMsg("\u65E0\u6301\u4ED3");
    }
  }

  private void showMsg(String msg) {
    JOptionPane.showMessageDialog(owner, msg);
  }
}
