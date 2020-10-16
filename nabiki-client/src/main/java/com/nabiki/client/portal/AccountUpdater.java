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
import com.nabiki.commons.ctpobj.CQryTradingAccount;
import com.nabiki.commons.ctpobj.CRspInfo;
import com.nabiki.commons.ctpobj.CTradingAccount;
import com.nabiki.commons.ctpobj.ErrorCodes;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class AccountUpdater extends Updater implements Runnable {
  private final JTable table;
  private final TradeClient client;

  // Table data model.
  private final Object[][] model = new Object[][]{
      {"\u7ECF\u7EAA\u5546", null, null},
      {"\u8D26\u6237\u53F7", null, null},
      {"\u6743\u76CA", null, null},
      {"\u4FDD\u8BC1\u91D1", null, null},
      {"\u53EF\u7528\u8D44\u91D1", null, null},
      {"\u624B\u7EED\u8D39", null, null},
      {"\u51BB\u7ED3\u624B\u7EED\u8D39", null, null},
      {"\u51BB\u7ED3\u73B0\u91D1", null, null},
      {"\u51BB\u7ED3\u4FDD\u8BC1\u91D1", null, null},
      {"\u5E73\u4ED3\u5229\u6DA6", null, null},
      {"\u6301\u4ED3\u5229\u6DA6", null, null},
      {"\u4EA4\u6613\u65E5", null, null},
  };
  private final String[] headers = new String[]{
      "\u5C5E\u6027", "\u4ECA\u65E5\u503C", "\u6628\u65E5\u503C"
  };

  private Thread daemon;
  private String user;
  private JButton src;

  AccountUpdater(JTable table, TradeClient client) {
    this.table = table;
    this.client = client;
    setupTable();
  }

  public void update(String user, Object src) {
    this.user = user;
    this.src = (JButton) src;
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
        queryAccount();
      } catch (Throwable th) {
        showMsg(th.getMessage());
      }
    }
  }

  private void queryAccount() throws Exception {
    var req = new CQryTradingAccount();
    req.CurrencyID = "CNY";
    req.AccountID = req.InvestorID = user;
    var rsp = client.queryAccount(req);
    rsp.consume(new ResponseConsumer<CTradingAccount>() {
      @Override
      public void accept(CTradingAccount object, CRspInfo rspInfo, int currentCount,
                         int totalCount) {
        if (rspInfo != null && rspInfo.ErrorID != ErrorCodes.NONE) {
          showMsg(String.format("[%d]%s", rspInfo.ErrorID, rspInfo.ErrorMsg));
        } else {
          src.setEnabled(true);
          EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
              updateTable(object);
            }
          });
        }
      }
    });
    src.setEnabled(false);
    sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
    src.setEnabled(true);
    if (!rsp.hasResponse()) {
      showMsg("\u67E5\u8BE2\u4E0D\u5230\u8D26\u6237\u4fE1\u606F");
    }
  }

  private String format(double value) {
    return String.format("%.2f", value);
  }

  private void updateTable(CTradingAccount account) {
    // Can't be null, but guard it here.
    if (account == null)
      return;
    model[0][1] = account.BrokerID;
    model[1][1] = account.AccountID;
    model[2][1] = format(account.Balance);
    model[2][2] = format(account.PreBalance);
    model[3][1] = format(account.CurrMargin);
    model[3][2] = format(account.PreMargin);
    model[4][1] = format(account.Available);
    model[5][1] = format(account.Commission);
    model[6][1] = format(account.FrozenCommission);
    model[7][1] = format(account.FrozenCash);
    model[8][1] = format(account.FrozenMargin);
    model[9][1] = format(account.CloseProfit);
    model[10][1] = format(account.PositionProfit);
    model[11][1] = account.TradingDay;
    // Update table content.
    var model2 = (DefaultTableModel) table.getModel();
    model2.setDataVector(model, headers);
    model2.fireTableDataChanged();
  }

  private void showMsg(String msg) {
    JOptionPane.showMessageDialog(table, msg);
  }

  private void setupTable() {
    table.setBackground(Color.WHITE);
    table.setModel(new DefaultTableModel(model, headers) {
      final Class[] columnTypes = new Class[]{
          String.class, String.class, String.class
      };

      public Class getColumnClass(int columnIndex) {
        return columnTypes[columnIndex];
      }

      final boolean[] columnEditables = new boolean[]{
          false, false, false
      };

      public boolean isCellEditable(int row, int column) {
        return columnEditables[column];
      }
    });
  }
}
