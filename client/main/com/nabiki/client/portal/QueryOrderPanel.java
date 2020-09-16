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

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class QueryOrderPanel extends JPanel {
    private final JTable orderTable;
    private final JTextField orderIDField;
    private final OrderUpdater orderUpdater;
    private final OrderActioner orderActioner;

    QueryOrderPanel(TradeClient client) {
        SpringLayout sl_queryOrderPanel = new SpringLayout();
        this.setLayout(sl_queryOrderPanel);

        JScrollPane orderScrollPane = new JScrollPane();
        sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderScrollPane, 50, SpringLayout.NORTH, this);
        sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderScrollPane, 10, SpringLayout.WEST, this);
        sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, orderScrollPane, -10, SpringLayout.SOUTH, this);
        sl_queryOrderPanel.putConstraint(SpringLayout.EAST, orderScrollPane, -10, SpringLayout.EAST, this);
        this.add(orderScrollPane);

        orderTable = new JTable();
        orderUpdater = new OrderUpdater(orderTable, client);
        orderUpdater.start();

        orderActioner = new OrderActioner(client, orderUpdater);
        orderActioner.start();

        orderScrollPane.setViewportView(orderTable);

        JLabel orderIDLabel = new JLabel("\u62A5\u5355\u7F16\u53F7:");
        sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderIDLabel, 14, SpringLayout.NORTH, this);
        sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderIDLabel, 10, SpringLayout.WEST, this);
        this.add(orderIDLabel);

        orderIDField = new JTextField();
        sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderIDField, 10, SpringLayout.NORTH, this);
        sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderIDField, 5, SpringLayout.EAST, orderIDLabel);
        sl_queryOrderPanel.putConstraint(SpringLayout.EAST, orderIDField, 250, SpringLayout.EAST, orderIDLabel);
        this.add(orderIDField);
        orderIDField.setColumns(10);

        JButton queryOrderBtn = new JButton("\u67E5\u8BE2");
        queryOrderBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                orderUpdater.query(orderIDField.getText().trim(), e.getSource());
            }
        });
        sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, queryOrderBtn, 0, SpringLayout.NORTH, orderIDField);
        sl_queryOrderPanel.putConstraint(SpringLayout.WEST, queryOrderBtn, 10, SpringLayout.EAST, orderIDField);
        sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, queryOrderBtn, 0, SpringLayout.SOUTH, orderIDField);
        this.add(queryOrderBtn);

        JButton cancelOrderBtn = new JButton("\u64A4\u5355");
        cancelOrderBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                orderActioner.cancel(orderIDField.getText().trim(), e.getSource());
            }
        });
        sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, cancelOrderBtn, 0, SpringLayout.NORTH, orderIDField);
        sl_queryOrderPanel.putConstraint(SpringLayout.WEST, cancelOrderBtn, 10, SpringLayout.EAST, queryOrderBtn);
        sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, cancelOrderBtn, 0, SpringLayout.SOUTH, orderIDField);
        this.add(cancelOrderBtn);
    }
}
