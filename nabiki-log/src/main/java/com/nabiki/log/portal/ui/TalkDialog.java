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

package com.nabiki.log.portal.ui;

import com.nabiki.log.portal.core.LogLevelType;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SocketHandler;

public class TalkDialog extends JDialog {
  private final JPanel contentPanel = new JPanel();

  private final JLabel msgLabel;
  private final JTextField loggerField;
  private final JTextField classField;
  private final JTextField methodField;
  private final JTextField serverField;
  private final JComboBox levelComb;
  private final JTextArea msgArea;
  private final JButton sendBtn;

  private String lastAddress;
  private SocketHandler lastHandler;

  private void locateSelf() {
    var pp = getParent().getLocation();
    var pSize = getParent().getSize();
    var x = pp.x + pSize.width + 10;
    var y = pp.y;
    setLocation(new Point(x, y));
  }

  private InetSocketAddress getAddress(String address) {
    InetSocketAddress inetAddress;
    if (address.contains(":")) {
      inetAddress = new InetSocketAddress(
          address.substring(0, address.indexOf(":")).trim(),
          Integer.parseInt(address.substring(address.indexOf(":") + 1)));
    } else {
      inetAddress = new InetSocketAddress(Integer.parseInt(address.trim()));
    }
    return inetAddress;
  }

  private boolean tryConnect() {
    try {
      var str = serverField.getText();
      if (str== null || str.trim().length() == 0) {
        if (lastHandler != null) {
          lastHandler.flush();
          lastHandler.close();
          lastHandler = null;
          lastAddress = null;
        }
        return false;
      }
      if (lastAddress != null && lastAddress.equals(str)) {
        return true;
      }
      var address = getAddress(str);
      lastHandler = new SocketHandler(address.getHostString(), address.getPort());
      lastHandler.setEncoding(StandardCharsets.UTF_8.name());
      lastAddress = str;
      return true;
    } catch (Throwable e) {
      JOptionPane.showMessageDialog(this, e.getMessage());
      return false;
    }
  }

  public void display() {
    locateSelf();
    if (!isVisible()) {
      setVisible(true);
    }
  }

  /**
   * Create the dialog.
   */
  public TalkDialog(JFrame parent) {
    super(parent);

    setTitle("\u5BF9\u8BDD");
    setBounds(100, 100, 450, 400);
    setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    getContentPane().add(contentPanel, BorderLayout.CENTER);
    SpringLayout sl_contentPanel = new SpringLayout();
    contentPanel.setLayout(sl_contentPanel);
    msgLabel = new JLabel("\u6D88\u606F");
    sl_contentPanel.putConstraint(SpringLayout.WEST, msgLabel, 10, SpringLayout.WEST, contentPanel);
    contentPanel.add(msgLabel);

    JScrollPane msgScrollPane = new JScrollPane();
    sl_contentPanel.putConstraint(SpringLayout.NORTH, msgScrollPane, 0, SpringLayout.NORTH, msgLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, msgScrollPane, 10, SpringLayout.EAST, msgLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, msgScrollPane, -110, SpringLayout.SOUTH, contentPanel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, msgScrollPane, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(msgScrollPane);

    msgArea = new JTextArea();
    msgArea.setMargin(new Insets(2, 4, 2, 2));
    msgScrollPane.setViewportView(msgArea);

    JLabel loggerLabel = new JLabel("\u540D\u79F0");
    sl_contentPanel.putConstraint(SpringLayout.NORTH, loggerLabel, 10, SpringLayout.SOUTH, msgScrollPane);
    sl_contentPanel.putConstraint(SpringLayout.WEST, loggerLabel, 0, SpringLayout.WEST, msgLabel);
    contentPanel.add(loggerLabel);

    loggerField = new JTextField();
    loggerField.setHorizontalAlignment(SwingConstants.CENTER);
    sl_contentPanel.putConstraint(SpringLayout.NORTH, loggerField, -2, SpringLayout.NORTH, loggerLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, loggerField, 10, SpringLayout.EAST, loggerLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, loggerField, 2, SpringLayout.SOUTH, loggerLabel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, loggerField, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(loggerField);
    loggerField.setColumns(10);

    JLabel classLabel = new JLabel("\u7C7B\u578B");
    sl_contentPanel.putConstraint(SpringLayout.NORTH, classLabel, 10, SpringLayout.SOUTH, loggerLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, classLabel, 0, SpringLayout.WEST, loggerLabel);
    contentPanel.add(classLabel);

    classField = new JTextField();
    classField.setHorizontalAlignment(SwingConstants.CENTER);
    sl_contentPanel.putConstraint(SpringLayout.NORTH, classField, -2, SpringLayout.NORTH, classLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, classField, 10, SpringLayout.EAST, classLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, classField, 2, SpringLayout.SOUTH, classLabel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, classField, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(classField);
    classField.setColumns(10);

    JLabel methodLabel = new JLabel("\u65B9\u6CD5");
    sl_contentPanel.putConstraint(SpringLayout.NORTH, methodLabel, 10, SpringLayout.SOUTH, classField);
    sl_contentPanel.putConstraint(SpringLayout.WEST, methodLabel, 0, SpringLayout.WEST, classLabel);
    contentPanel.add(methodLabel);

    methodField = new JTextField();
    methodField.setHorizontalAlignment(SwingConstants.CENTER);
    sl_contentPanel.putConstraint(SpringLayout.NORTH, methodField, -2, SpringLayout.NORTH, methodLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, methodField, 10, SpringLayout.EAST, methodLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, methodField, 2, SpringLayout.SOUTH, methodLabel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, methodField, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(methodField);
    methodField.setColumns(10);

    JLabel serverLabel = new JLabel("\u5730\u5740");
    sl_contentPanel.putConstraint(SpringLayout.NORTH, serverLabel, 10, SpringLayout.SOUTH, methodField);
    sl_contentPanel.putConstraint(SpringLayout.WEST, serverLabel, 0, SpringLayout.WEST, methodLabel);
    contentPanel.add(serverLabel);

    serverField = new JTextField();
    serverField.setHorizontalAlignment(SwingConstants.CENTER);
    sl_contentPanel.putConstraint(SpringLayout.NORTH, serverField, -2, SpringLayout.NORTH, serverLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, serverField, 10, SpringLayout.EAST, serverLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, serverField, 2, SpringLayout.SOUTH, serverLabel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, serverField, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(serverField);
    serverField.setColumns(10);

    JLabel levelLabel = new JLabel("\u7EA7\u522B");
    sl_contentPanel.putConstraint(SpringLayout.NORTH, msgLabel, 10, SpringLayout.SOUTH, levelLabel);
    sl_contentPanel.putConstraint(SpringLayout.NORTH, levelLabel, 15, SpringLayout.NORTH, contentPanel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, levelLabel, 10, SpringLayout.WEST, contentPanel);
    contentPanel.add(levelLabel);

    levelComb = new JComboBox();
    levelComb.setAutoscrolls(true);
    levelComb.setModel(new DefaultComboBoxModel(LogLevelType.values()));
    sl_contentPanel.putConstraint(SpringLayout.NORTH, levelComb, -2, SpringLayout.NORTH, levelLabel);
    sl_contentPanel.putConstraint(SpringLayout.WEST, levelComb, 10, SpringLayout.EAST, levelLabel);
    sl_contentPanel.putConstraint(SpringLayout.SOUTH, levelComb, 2, SpringLayout.SOUTH, levelLabel);
    sl_contentPanel.putConstraint(SpringLayout.EAST, levelComb, -10, SpringLayout.EAST, contentPanel);
    contentPanel.add(levelComb);

    JPanel buttonPane = new JPanel();
    FlowLayout fl_buttonPane = new FlowLayout(FlowLayout.CENTER);
    fl_buttonPane.setHgap(10);
    fl_buttonPane.setVgap(10);
    buttonPane.setLayout(fl_buttonPane);
    getContentPane().add(buttonPane, BorderLayout.SOUTH);
    sendBtn = new JButton("\u53D1\u9001");
    sendBtn.setActionCommand("OK");
    buttonPane.add(sendBtn);
    getRootPane().setDefaultButton(sendBtn);
    sendBtn.addActionListener(e -> {
      onSend();
    });
  }

  private void onSend() {
    if (tryConnect()) {
      lastHandler.publish(getRecord());
      lastHandler.flush();
    } else {
      JOptionPane.showMessageDialog(this, "\u65e0\u6cd5\u8fde\u63a5" + serverField.getText());
    }
  }

  private LogRecord getRecord() {
    var record = new LogRecord(Level.ALL, "");
    record.setLevel(getLevel((LogLevelType) levelComb.getSelectedItem()));
    record.setMessage(msgArea.getText());
    record.setLoggerName(loggerField.getText());
    record.setSourceClassName(classField.getText());
    record.setSourceMethodName(methodField.getText());
    return record;
  }

  private Level getLevel(LogLevelType type) {
    switch (type) {
      case ALL:
        return Level.ALL;
      case OFF:
        return Level.OFF;
      case FINE:
        return Level.FINE;
      case FINER:
        return Level.FINER;
      case FINEST:
        return Level.FINEST;
      case INFO:
        return Level.INFO;
      case CONFIG:
        return Level.CONFIG;
      case WARNING:
        return Level.WARNING;
      case SEVERE:
        return Level.SEVERE;
    }
    return Level.INFO;
  }
}
