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

public class PositionPanel extends JPanel {
    final PositionUpdater positionUpdater;
    final JTextField instrumentField;
    final JCheckBox positionCatCheck;
    final JTable positionTable;
    final JComboBox positionTypeComb;

    PositionPanel(TradeClient client) {
        
        SpringLayout sl_positionPanel = new SpringLayout();
        this.setLayout(sl_positionPanel);

        positionCatCheck = new JCheckBox("\u6240\u6709\u6301\u4ED3");
        positionCatCheck.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                var check = (JCheckBox)e.getSource();
                instrumentField.setEnabled(!check.isSelected());
            }
        });
        sl_positionPanel.putConstraint(SpringLayout.NORTH, positionCatCheck, 10, SpringLayout.NORTH, this);
        sl_positionPanel.putConstraint(SpringLayout.WEST, positionCatCheck, 10, SpringLayout.WEST, this);
        this.add(positionCatCheck);

        JLabel instrumentLabel = new JLabel("\u5408\u7EA6:");
        sl_positionPanel.putConstraint(SpringLayout.NORTH, instrumentLabel, 14, SpringLayout.NORTH, this);
        sl_positionPanel.putConstraint(SpringLayout.WEST, instrumentLabel, 80, SpringLayout.WEST, positionCatCheck);
        this.add(instrumentLabel);

        instrumentField = new JTextField();
        sl_positionPanel.putConstraint(SpringLayout.WEST, instrumentField, 5, SpringLayout.EAST, instrumentLabel);
        sl_positionPanel.putConstraint(SpringLayout.EAST, instrumentField, 100, SpringLayout.EAST, instrumentLabel);
        instrumentLabel.setLabelFor(instrumentField);
        instrumentField.setHorizontalAlignment(SwingConstants.CENTER);
        sl_positionPanel.putConstraint(SpringLayout.NORTH, instrumentField, 10, SpringLayout.NORTH, this);
        this.add(instrumentField);
        instrumentField.setColumns(10);

        JButton qryPositionBtn = new JButton("\u67E5\u8BE2");
        qryPositionBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String instrument = "";
                PositionUpdater.PositionType type;
                if (!positionCatCheck.isSelected())
                    instrument = instrumentField.getText().trim();
                if (positionTypeComb.getSelectedIndex() == 0)
                    type = PositionUpdater.PositionType.SUMMARY;
                else
                    type = PositionUpdater.PositionType.DETAIL;
                positionUpdater.query(instrument, type);
            }
        });
        sl_positionPanel.putConstraint(SpringLayout.NORTH, qryPositionBtn, 0, SpringLayout.NORTH, instrumentField);
        sl_positionPanel.putConstraint(SpringLayout.SOUTH, qryPositionBtn, 0, SpringLayout.SOUTH, instrumentField);
        this.add(qryPositionBtn);

        JScrollPane positionScrollPane = new JScrollPane();
        sl_positionPanel.putConstraint(SpringLayout.NORTH, positionScrollPane, 50, SpringLayout.NORTH, this);
        sl_positionPanel.putConstraint(SpringLayout.WEST, positionScrollPane, 10, SpringLayout.WEST, this);
        sl_positionPanel.putConstraint(SpringLayout.SOUTH, positionScrollPane, -10, SpringLayout.SOUTH, this);
        sl_positionPanel.putConstraint(SpringLayout.EAST, positionScrollPane, -10, SpringLayout.EAST, this);
        this.add(positionScrollPane);

        positionTable = new JTable();
        positionUpdater = new PositionUpdater(positionTable, client);
        positionUpdater.start();

        positionScrollPane.setViewportView(positionTable);

        positionTypeComb = new JComboBox();
        sl_positionPanel.putConstraint(SpringLayout.WEST, qryPositionBtn, 10, SpringLayout.EAST, positionTypeComb);
        sl_positionPanel.putConstraint(SpringLayout.NORTH, positionTypeComb, 0, SpringLayout.NORTH, instrumentField);
        sl_positionPanel.putConstraint(SpringLayout.WEST, positionTypeComb, 110, SpringLayout.WEST, instrumentField);
        sl_positionPanel.putConstraint(SpringLayout.SOUTH, positionTypeComb, 0, SpringLayout.SOUTH, instrumentLabel);
        sl_positionPanel.putConstraint(SpringLayout.EAST, positionTypeComb, 80, SpringLayout.EAST, instrumentField);
        positionTypeComb.setModel(new DefaultComboBoxModel(new String[] {"\u6C47\u603B", "\u660E\u7EC6"}));
        this.add(positionTypeComb);
    }
}
