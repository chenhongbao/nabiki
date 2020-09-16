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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class AccountPanel extends JPanel {
    final JTable accountTable;
    final JTextField insertInstrumentField;
    final JTextField insertPriceField;
    final JTextField insertVolumeField;
    final JTable insertedOrderTable;
    final JLabel userNameText;
    final JLabel loginStateText;
    final JButton buyOpenBtn, buyCloseBtn, sellOpenBtn, sellCloseBtn;

    private final AccountUpdater accountUpdater;
    private final OrderInserter orderInserter;

    AccountPanel(TradeClient client) {
        super.setBorder(null);
        SpringLayout sl_accountPanel = new SpringLayout();
        super.setLayout(sl_accountPanel);

        // Initialize account table and its daemon.
        accountTable = new JTable();
        accountUpdater = new AccountUpdater(accountTable, client);
        accountUpdater.start();

        JScrollPane accountScrollPane = new JScrollPane(accountTable);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, accountScrollPane, 50, SpringLayout.NORTH, this);
        sl_accountPanel.putConstraint(SpringLayout.WEST, accountScrollPane, 10, SpringLayout.WEST, this);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, accountScrollPane, -220, SpringLayout.SOUTH, this);
        sl_accountPanel.putConstraint(SpringLayout.EAST, accountScrollPane, -10, SpringLayout.EAST, this);
        accountScrollPane.setViewportBorder(null);
        super.add(accountScrollPane);

        JButton refreshBtn = new JButton("\u5237\u65B0");
        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                accountUpdater.update(userNameText.getText().trim(), e.getSource());
            }
        });
        sl_accountPanel.putConstraint(SpringLayout.EAST, refreshBtn, -10, SpringLayout.EAST, this);
        super.add(refreshBtn);

        JLabel userNameLabel = new JLabel("\u8D26\u53F7:");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, userNameLabel, 10, SpringLayout.NORTH, this);
        sl_accountPanel.putConstraint(SpringLayout.WEST, userNameLabel, 10, SpringLayout.WEST, this);
        super.add(userNameLabel);

        userNameText = new JLabel("\u65E0");
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, refreshBtn, 0, SpringLayout.SOUTH, userNameText);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, userNameText, 10, SpringLayout.NORTH, this);
        sl_accountPanel.putConstraint(SpringLayout.WEST, userNameText, 10, SpringLayout.EAST, userNameLabel);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, userNameText, 0, SpringLayout.SOUTH, userNameLabel);
        sl_accountPanel.putConstraint(SpringLayout.EAST, userNameText, 100, SpringLayout.EAST, userNameLabel);
        super.add(userNameText);

        JLabel loginStateLabel = new JLabel("\u72B6\u6001:");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, loginStateLabel, 10, SpringLayout.NORTH, this);
        sl_accountPanel.putConstraint(SpringLayout.WEST, loginStateLabel, 10, SpringLayout.EAST, userNameText);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, loginStateLabel, 0, SpringLayout.SOUTH, userNameLabel);
        super.add(loginStateLabel);

        loginStateText = new JLabel("\u672A\u767B\u5F55");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, refreshBtn, 0, SpringLayout.NORTH, loginStateText);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, loginStateText, 10, SpringLayout.NORTH, this);
        sl_accountPanel.putConstraint(SpringLayout.WEST, loginStateText, 10, SpringLayout.EAST, loginStateLabel);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, loginStateText, 0, SpringLayout.SOUTH, userNameLabel);
        sl_accountPanel.putConstraint(SpringLayout.EAST, loginStateText, 100, SpringLayout.EAST, loginStateLabel);
        super.add(loginStateText);

        JLabel insertInstrumentLabel = new JLabel("\u62A5\u5355\u5408\u7EA6:");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertInstrumentLabel, 20, SpringLayout.SOUTH, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertInstrumentLabel, 0, SpringLayout.WEST, accountScrollPane);
        super.add(insertInstrumentLabel);

        insertInstrumentField = new JTextField();
        insertInstrumentField.setHorizontalAlignment(SwingConstants.CENTER);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertInstrumentField, 16, SpringLayout.SOUTH, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertInstrumentField, 5, SpringLayout.EAST, insertInstrumentLabel);
        sl_accountPanel.putConstraint(SpringLayout.EAST, insertInstrumentField, 200, SpringLayout.EAST, insertInstrumentLabel);
        super.add(insertInstrumentField);
        insertInstrumentField.setColumns(10);

        JLabel insertPriceLabel = new JLabel("\u9650\u4EF7:");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertPriceLabel, 20, SpringLayout.SOUTH, insertInstrumentLabel);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertPriceLabel, 0, SpringLayout.WEST, accountScrollPane);
        super.add(insertPriceLabel);

        insertPriceField = new JTextField();
        insertPriceField.setHorizontalAlignment(SwingConstants.CENTER);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertPriceField, 15, SpringLayout.SOUTH, insertInstrumentField);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertPriceField, 0, SpringLayout.WEST, insertInstrumentField);
        sl_accountPanel.putConstraint(SpringLayout.EAST, insertPriceField, 0, SpringLayout.EAST, insertInstrumentField);
        super.add(insertPriceField);
        insertPriceField.setColumns(10);

        JLabel insertVolumeLabel = new JLabel("\u6570\u91CF:");
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertVolumeLabel, 20, SpringLayout.SOUTH, insertPriceLabel);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertVolumeLabel, 0, SpringLayout.WEST, accountScrollPane);
        super.add(insertVolumeLabel);

        insertVolumeField = new JTextField();
        insertVolumeField.setHorizontalAlignment(SwingConstants.CENTER);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertVolumeField, 15, SpringLayout.SOUTH, insertPriceField);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertVolumeField, 0, SpringLayout.WEST, insertPriceField);
        sl_accountPanel.putConstraint(SpringLayout.EAST, insertVolumeField, 0, SpringLayout.EAST, insertPriceField);
        super.add(insertVolumeField);
        insertVolumeField.setColumns(10);

        buyOpenBtn = new JButton("\u4E70\u5F00");
        buyOpenBtn.setForeground(Color.WHITE);
        buyOpenBtn.setBackground(Color.RED);
        sl_accountPanel.putConstraint(SpringLayout.NORTH, buyOpenBtn, 22, SpringLayout.SOUTH, insertVolumeField);
        sl_accountPanel.putConstraint(SpringLayout.WEST, buyOpenBtn, 0, SpringLayout.WEST, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.EAST, buyOpenBtn, 100, SpringLayout.WEST, this);
        super.add(buyOpenBtn);

        buyOpenBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                orderInserter.insert(
                        OrderInserter.OrderType.BUY_OPEN,
                        buyOpenBtn,
                        buyCloseBtn,
                        sellOpenBtn,
                        sellCloseBtn);
            }
        });

        buyCloseBtn = new JButton("\u4E70\u5E73");
        buyCloseBtn.setBackground(Color.RED);
        buyCloseBtn.setForeground(Color.LIGHT_GRAY);
        sl_accountPanel.putConstraint(SpringLayout.WEST, buyCloseBtn, -90, SpringLayout.EAST, insertVolumeField);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, buyCloseBtn, 0, SpringLayout.SOUTH, buyOpenBtn);
        sl_accountPanel.putConstraint(SpringLayout.EAST, buyCloseBtn, 0, SpringLayout.EAST, insertInstrumentField);
        super.add(buyCloseBtn);

        buyCloseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                orderInserter.insert(
                        OrderInserter.OrderType.BUY_CLOSE,
                        buyOpenBtn,
                        buyCloseBtn,
                        sellOpenBtn,
                        sellCloseBtn);
            }
        });

        sellOpenBtn = new JButton("\u5356\u5F00");
        sellOpenBtn.setForeground(Color.WHITE);
        sellOpenBtn.setBackground(new Color(0, 128, 0));
        sl_accountPanel.putConstraint(SpringLayout.WEST, sellOpenBtn, 0, SpringLayout.WEST, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, sellOpenBtn, -10, SpringLayout.SOUTH, this);
        sl_accountPanel.putConstraint(SpringLayout.EAST, sellOpenBtn, 100, SpringLayout.WEST, this);
        super.add(sellOpenBtn);

        sellOpenBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                orderInserter.insert(
                        OrderInserter.OrderType.SELL_OPEN,
                        buyOpenBtn,
                        buyCloseBtn,
                        sellOpenBtn,
                        sellCloseBtn);
            }
        });

        sellCloseBtn = new JButton("\u5356\u5E73");
        sellCloseBtn.setForeground(Color.LIGHT_GRAY);
        sellCloseBtn.setBackground(new Color(0, 128, 0));
        sl_accountPanel.putConstraint(SpringLayout.WEST, sellCloseBtn, -90, SpringLayout.EAST, insertVolumeField);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, sellCloseBtn, 0, SpringLayout.SOUTH, sellOpenBtn);
        sl_accountPanel.putConstraint(SpringLayout.EAST, sellCloseBtn, 0, SpringLayout.EAST, insertInstrumentField);
        super.add(sellCloseBtn);

        sellCloseBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                orderInserter.insert(
                        OrderInserter.OrderType.SELL_CLOSE,
                        buyOpenBtn,
                        buyCloseBtn,
                        sellOpenBtn,
                        sellCloseBtn);
            }
        });

        JScrollPane insertedOrderScrollPanel = new JScrollPane();
        sl_accountPanel.putConstraint(SpringLayout.NORTH, insertedOrderScrollPanel, 16, SpringLayout.SOUTH, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.WEST, insertedOrderScrollPanel, 300, SpringLayout.WEST, accountScrollPane);
        sl_accountPanel.putConstraint(SpringLayout.SOUTH, insertedOrderScrollPanel, -10, SpringLayout.SOUTH, this);
        sl_accountPanel.putConstraint(SpringLayout.EAST, insertedOrderScrollPanel, 0, SpringLayout.EAST, accountScrollPane);
        super.add(insertedOrderScrollPanel);

        insertedOrderTable = new JTable();
        // Initialize order insert daemon.
        orderInserter = new OrderInserter(
                client,
                insertInstrumentField,
                insertPriceField,
                insertVolumeField,
                insertedOrderTable);
        orderInserter.start();

        insertedOrderScrollPanel.setViewportView(insertedOrderTable);
    }
}
