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

package com.nabiki.client.portal;

import com.nabiki.client.sdk.TradeClient;
import com.nabiki.objects.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OrderUpdater extends Updater implements Runnable {
  private final JTable table;
  private final TradeClient client;

  private Thread daemon;
  private String orderID;
  private JButton src;

  private final String[] headers = new String[]{
      "\u62A5\u5355\u5F15\u7528",
      "\u5408\u7EA6",
      "\u9650\u4EF7",
      "\u4E70\u5356",
      "\u5F00\u5E73",
      "\u603B\u91CF",
      "\u5DF2\u6210",
      "\u6392\u961F",
      "\u63D0\u4EA4",
      "\u6210\u4EA4",
      "\u66F4\u65B0",
      "\u65E5\u671F"
  };

  OrderUpdater(JTable table, TradeClient client) {
    this.table = table;
    this.client = client;
    setupTable();
  }

  public void query(String orderID, Object src) {
    if (orderID == null || orderID.length() == 0) {
      showMsg("\u9700\u8981\u62a5\u5355\u7f16\u53f7");
      return;
    }
    this.src = (JButton) src;
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
        queryOrder();
      } catch (Throwable th) {
        showMsg(th.getMessage());
      }
    }
  }

  private void queryOrder() throws Exception {
    var req = new CQryOrder();
    req.OrderSysID = orderID;
    var rsp = client.queryOrder(
        req, UUID.randomUUID().toString());
    src.setEnabled(false);
    sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
    src.setEnabled(true);
    if (!rsp.hasResponse())
      showMsg("\u67E5\u8BE2\u4E0D\u5230\u62A5\u5355\u4FE1\u606F");
    else {
      CRspInfo error = null;
      COrder o = null;
      var orders = new HashSet<COrder>();
      while ((o = rsp.poll()) != null) {
        orders.add(o);
        var info = rsp.getRspInfo(o);
        if (info.ErrorID != ErrorCodes.NONE) {
          error = info;
          break;
        }
      }
      if (error != null)
        showMsg(String.format("[%d]%s", error.ErrorID, error.ErrorMsg));
      else
        updateTable(orders);
    }
  }

  private String getDirection(byte direction) {
    switch (direction) {
      case DirectionType.DIRECTION_BUY:
        return "\u4E70";
      case DirectionType.DIRECTION_SELL:
        return "\u5356";

    }
    return "\u672A\u77E5";
  }

  private String getOffset(byte offset) {
    switch (offset) {
      case CombOffsetFlagType.OFFSET_OPEN:
        return "\u5F00";
      case CombOffsetFlagType.OFFSET_CLOSE:
        return "\u81EA\u52A8\u5E73";
      case CombOffsetFlagType.OFFSET_CLOSE_TODAY:
        return "\u5E73\u4ECA";
      case CombOffsetFlagType.OFFSET_CLOSE_YESTERDAY:
        return "\u5E73\u6628";
      case CombOffsetFlagType.OFFSET_LOCAL_FORCE_CLOSE:
        return "\u5f3a\u5e73";
      case CombOffsetFlagType.OFFSET_FORCE_OFF:
        return "\u5F3A\u51CF";
    }
    return "\u672A\u77E5";
  }

  private String getSubmitStatus(byte submitStatus) {
    switch (submitStatus) {
      case OrderSubmitStatusType.ACCEPTED:
        return "\u63A5\u53D7";
      case OrderSubmitStatusType.INSERT_REJECTED:
        return "\u62D2\u7EDD\u62A5\u5165";
      case OrderSubmitStatusType.INSERT_SUBMITTED:
        return "\u62A5\u5165\u63D0\u4EA4";
      case OrderSubmitStatusType.CANCEL_REJECTED:
        return "\u62D2\u7EDD\u64A4\u5355";
      case OrderSubmitStatusType.CANCEL_SUBMITTED:
        return "\u64A4\u5355\u63D0\u4EA4";
      case OrderSubmitStatusType.MODIFY_REJECTED:
        return "\u62D2\u7EDD\u6539\u5355";
      case OrderSubmitStatusType.MODIFY_SUBMITTED:
        return "\u6539\u5355\u63D0\u4EA4";
    }
    return "\u672A\u77E5";
  }

  private String getOrderStatus(byte status) {
    switch (status) {
      case OrderStatusType.ALL_TRADED:
        return "\u5168\u6210";
      case OrderStatusType.CANCELED:
        return "\u5DF2\u64A4\u5355";
      case OrderStatusType.NO_TRADE_NOT_QUEUEING:
        return "\u672A\u6210\u4EA4\u4E0D\u5728\u961F";
      case OrderStatusType.NO_TRADE_QUEUEING:
        return "\u672A\u6210\u4EA4\u6392\u961F";
      case OrderStatusType.NOT_TOUCHED:
        return "\u672A\u5904\u7406";
      case OrderStatusType.TOUCHED:
        return "\u5DF2\u5904\u7406";
      case OrderStatusType.PART_TRADED_NOT_QUEUEING:
        return "\u90E8\u5206\u6210\u4EA4\u4E0D\u5728\u961F";
      case OrderStatusType.PART_TRADED_QUEUEING:
        return "\u90E8\u5206\u6210\u4EA4\u6392\u961F";
      case OrderStatusType.UNKNOWN:
        return "\u672A\u77E5";
    }
    return "\u672A\u77E5";
  }

  private void updateTable(Collection<COrder> orders) {
    var objects = new Object[orders.size()][];
    int idx = 0;
    for (var o : orders) {
      var row = new Object[]{
          o.OrderRef,
          o.InstrumentID,
          o.LimitPrice,
          getDirection(o.Direction),
          getOffset(o.CombOffsetFlag),
          o.VolumeTotalOriginal,
          o.VolumeTraded,
          o.VolumeTotal,
          getSubmitStatus(o.OrderSubmitStatus),
          getOrderStatus(o.OrderStatus),
          o.UpdateTime,
          o.InsertDate
      };
      objects[idx++] = row;
    }
    var model = (DefaultTableModel) table.getModel();
    model.setDataVector(objects, headers);
    model.fireTableDataChanged();
  }

  private void setupTable() {
    table.setModel(new DefaultTableModel(headers, 0) {
      final Class[] columnTypes = new Class[]{
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class,
          String.class
      };

      public Class getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
      }

      final boolean[] columnEditables = new boolean[]{
          false, false, false, false, false, false, false, false, false, false, false, false
      };

      public boolean isCellEditable(int row, int column) {
        return columnEditables[column];
      }
    });
  }

  private void showMsg(String msg) {
    JOptionPane.showMessageDialog(table, msg);
  }
}
