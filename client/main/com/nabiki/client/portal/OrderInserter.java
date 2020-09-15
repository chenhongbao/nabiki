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

import com.nabiki.client.sdk.ClientUtils;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.objects.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class OrderInserter extends Updater implements Runnable {
    private final TradeClient client;
    private final JTextField instrumentField, priceField, volumeField;
    private final JTable table;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    // Table headers.
    private final String[] columns = new String[] {
            "\u65F6\u95F4", "\u62A5\u5355\u7F16\u53F7"
    };

    private Thread daemon;
    private OrderType type;

    enum OrderType {
        BUY_OPEN, BUY_CLOSE, SELL_OPEN, SELL_CLOSE
    }

    OrderInserter(
            TradeClient client,
            JTextField instrumentField,
            JTextField priceField,
            JTextField volumeField,
            JTable table) {
        this.client = client;
        this.table = table;
        this.instrumentField = instrumentField;
        this.priceField = priceField;
        this.volumeField = volumeField;
        setupTable();
    }

    public void start() {
        daemon = new Thread(this);
        daemon.setDaemon(true);
        daemon.start();
    }

    public void insert(OrderType type) {
        this.type = type;
        super.fire();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                super.waitFire();
                insertOrder();
            } catch (Throwable th) {
                showMsg(th.getMessage());
            }
        }
    }

    private void insertOrder() {
        var req = new CInputOrder();

        try {
            req.InstrumentID = instrumentField.getText().trim();
            req.LimitPrice = Double.parseDouble(priceField.getText().trim());
            req.VolumeTotalOriginal = Integer.parseInt(volumeField.getText().trim());
        } catch (Throwable th) {
            throw new IllegalArgumentException("invalid parameters for order insert");
        }
        switch (type) {
            case BUY_OPEN:
                req.Direction = DirectionType.DIRECTION_BUY;
                req.CombOffsetFlag = CombOffsetFlagType.OFFSET_OPEN;
                break;
            case BUY_CLOSE:
                req.Direction = DirectionType.DIRECTION_BUY;
                req.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE;
                break;
            case SELL_OPEN:
                req.Direction = DirectionType.DIRECTION_SELL;
                req.CombOffsetFlag = CombOffsetFlagType.OFFSET_OPEN;
                break;
            case SELL_CLOSE:
                req.Direction = DirectionType.DIRECTION_SELL;
                req.CombOffsetFlag = CombOffsetFlagType.OFFSET_CLOSE;
                break;
            default:
                break;
        }
        var rsp = ClientUtils.get(
                client.orderInsert(req, UUID.randomUUID().toString()),
                5,
                TimeUnit.SECONDS);
        if (rsp.size() == 0)
            showMsg("\u65E0\u62A5\u5355\u5E94\u7B54");
        else {
            var order = rsp.keySet().iterator().next();
            var rspInfo = rsp.get(order);
            if (rspInfo != null && rspInfo.ErrorID != ErrorCodes.NONE)
                showMsg(String.format("[%d]%s", rspInfo.ErrorID, rspInfo.ErrorMsg));
            else if (order == null)
                showMsg("\u62A5\u5355\u54CD\u5E94\u8FD4\u56DE\u7A7A\u5F15\u7528");
            else
                updateInsertedTable(order);
        }
        clearFields();
    }

    private void setupTable() {
        table.setCellSelectionEnabled(true);
        table.setModel(new DefaultTableModel(columns, 0) {
            final Class[] columnTypes = new Class[] {
                    String.class, String.class
            };
            public Class getColumnClass(int columnIndex) {
                return columnTypes[columnIndex];
            }
            final boolean[] columnEditables = new boolean[] {
                    false, false
            };
            public boolean isCellEditable(int row, int column) {
                return columnEditables[column];
            }
        });
        table.getColumnModel().getColumn(1).setPreferredWidth(250);
        table.getColumnModel().getColumn(1).setMinWidth(250);
    }

    private void updateInsertedTable(COrder order) {
        var timeStamp = LocalDateTime.now().format(formatter);
        var model = (DefaultTableModel) table.getModel();
        model.addRow(new Object[]{
                timeStamp,
                order.OrderLocalID});
        model.fireTableDataChanged();
        writeOrderID(timeStamp, order.OrderLocalID);
    }

    private void writeOrderID(String timeStamp, String id) {
        try (PrintWriter pw = new PrintWriter(
                new FileWriter("order_id.log", true))) {
            pw.print(timeStamp);
            pw.print("\t");
            pw.println(id);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void clearFields() {
        instrumentField.setText("");
        priceField.setText("");
        volumeField.setText("");
    }

    private void showMsg(String msg) {
        MessageDialog.showDefault(msg);
    }
}
