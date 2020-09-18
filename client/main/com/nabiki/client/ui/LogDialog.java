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

public class LogDialog extends JDialog implements UIPrinter {
  private final JTextArea logArea;
  private final JTextArea outArea;
  private final JTextArea errArea;
  private final JScrollPane logScrollPane;
  private final JScrollPane outScrollPane;
  private final JScrollPane errScrollPane;
  private final JCheckBox atuoScrollCheck;

  private static final SimpleFormatter formatter = new SimpleFormatter();

  /**
   * Create the dialog.
   */
  public LogDialog() {
    setTitle("\u65E5\u5FD7\u76D1\u89C6");
    setBounds(100, 100, 400, 600);

    JTabbedPane tabPane = new JTabbedPane(JTabbedPane.TOP);
    tabPane.setBorder(null);
    getContentPane().add(tabPane, BorderLayout.CENTER);

    logScrollPane = new JScrollPane();
    logScrollPane.setViewportBorder(null);
    tabPane.addTab("\u65E5\u5FD7", null, logScrollPane, "Logger\u8F93\u51FA");

    logArea = new JTextArea();
    logArea.setTabSize(2);
    logArea.setEditable(false);
    logScrollPane.setViewportView(logArea);

    outScrollPane = new JScrollPane();
    outScrollPane.setViewportBorder(null);
    tabPane.addTab("\u8F93\u51FA", null, outScrollPane, "System.out");
    tabPane.setForegroundAt(1, Color.BLUE);

    outArea = new JTextArea();
    outArea.setForeground(new Color(0, 0, 255));
    outArea.setTabSize(2);
    outArea.setEditable(false);
    outScrollPane.setViewportView(outArea);

    errScrollPane = new JScrollPane();
    errScrollPane.setViewportBorder(null);
    tabPane.addTab("\u9519\u8BEF", null, errScrollPane, "System.err");
    tabPane.setForegroundAt(2, Color.RED);

    errArea = new JTextArea();
    errArea.setForeground(Color.RED);
    errArea.setTabSize(2);
    errArea.setEditable(false);
    errScrollPane.setViewportView(errArea);

    JPanel ctrlPane = new JPanel();
    FlowLayout fl_ctrlPane = (FlowLayout) ctrlPane.getLayout();
    fl_ctrlPane.setAlignment(FlowLayout.TRAILING);
    getContentPane().add(ctrlPane, BorderLayout.SOUTH);

    atuoScrollCheck = new JCheckBox("\u5411\u4E0B\u6EDA\u52A8");
    atuoScrollCheck.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        AbstractButton abstractButton = (AbstractButton) e.getSource();
        if (abstractButton.getModel().isSelected()) {
          scrollBottom(logArea);
          scrollBottom(outArea);
          scrollBottom(errArea);
        }
      }
    });
    atuoScrollCheck.setSelected(true);
    ctrlPane.add(atuoScrollCheck);

    setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
  }

  @Override
  public void appendOut(String msg) {
    appendSystem(msg, outArea);
  }

  @Override
  public void appendErr(String msg) {
    appendSystem(msg, errArea);
  }

  @Override
  public void appendLog(LogRecord log) {
    logArea.append(formatter.format(log));
    if (atuoScrollCheck.isSelected())
      scrollBottom(logArea);
  }

  private void appendSystem(String msg, JTextArea area) {
    area.append(msg);
    if (atuoScrollCheck.isSelected())
      scrollBottom(area);
  }

  private void scrollBottom(JTextArea area) {
    area.setCaretPosition(area.getDocument().getLength());
  }
}
