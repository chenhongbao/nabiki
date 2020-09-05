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

package com.nabiki.client.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class LoggingConsole extends JDialog {
    JTextArea displayArea;
    JPanel clientCtrl;
    JCheckBox autoScroll;
    static final SimpleFormatter formatter = new SimpleFormatter();

    LoggingConsole() {
        addComponentsToPane();
    }

    void append(LogRecord log) {
        displayArea.append(formatter.format(log));
        if (autoScroll.isSelected())
            scrollBottom();
    }

    private void scrollBottom() {
        displayArea.setCaretPosition(displayArea.getDocument().getLength());
    }

    private void addComponentsToPane() {
        autoScroll = new JCheckBox("Auto-scroll", true);
        autoScroll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                AbstractButton abstractButton = (AbstractButton) e.getSource();
                if (abstractButton.getModel().isSelected())
                    scrollBottom();
            }
        });

        clientCtrl = new JPanel();
        clientCtrl.setLayout(new FlowLayout(FlowLayout.RIGHT));
        clientCtrl.add(autoScroll);

        displayArea = new JTextArea();
        displayArea.setEditable(false);

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        JScrollPane scrollPane = new JScrollPane(displayArea);
        scrollPane.setPreferredSize(new Dimension(600, screenSize.height * 2 / 3));

        getContentPane().add(scrollPane, BorderLayout.CENTER);
        getContentPane().add(clientCtrl, BorderLayout.PAGE_END);

        super.setTitle("日志监视");
    }
}
