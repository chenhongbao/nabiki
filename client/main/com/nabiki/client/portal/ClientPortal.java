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

import com.nabiki.iop.x.SystemStream;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

public class ClientPortal extends JFrame {

  final JPanel contentPane;
  final AccountPanel accountPanel;
  final PositionPanel positionPanel;
  final QueryOrderPanel queryOrderPanel;
  final LoginDialog loginDlg;

  // Client backend.
  final Portal portal = new Portal();

  /**
   * Launch the application.
   */
  public static void main(String[] args) {
    prepare();
    EventQueue.invokeLater(new Runnable() {
      public void run() {
        try {
          ClientPortal frame = new ClientPortal();
          locateWin(frame);
          frame.setVisible(true);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      private void locateWin(JFrame frame) {
        var screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        var frameSize = frame.getSize();
        var x = (screenSize.width - frameSize.width) / 2;
        var y = (screenSize.height - frameSize.height) / 2;
        frame.setLocation(new Point(x, y));
      }
    });
  }

  private static void prepare() {
    try {
      // Set default err/out.
      SystemStream.setErr("err.log");
      SystemStream.setOut("out.log");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private JToolBar initToolbar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    JButton userBtn = new JButton("\u7528\u6237");
    userBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        loginDlg.display();
      }
    });
    userBtn.setToolTipText("\u7528\u6237\u767B\u5F55");
    toolBar.add(userBtn);

    JButton helpBtn = new JButton("\u5E2E\u52A9");
    helpBtn.setToolTipText("\u5E2E\u52A9\u6587\u6863");
    toolBar.add(helpBtn);
    return toolBar;
  }

  private void setFrameSize() {
    var size = Toolkit.getDefaultToolkit().getScreenSize();
    var w = Math.min(1200, size.width);
    var h = Math.min(590, size.width);
    var x = (size.width - w) / 2;
    var y = (size.height - h) / 2;
    setBounds(x, y, w, h);
  }

  /**
   * Create the frame.
   */
  public ClientPortal() {
    setTitle("\u624B\u52A8\u64CD\u4F5C\u5165\u53E3");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setFrameSize();
    contentPane = new JPanel();
    contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
    contentPane.setLayout(new BorderLayout(0, 0));
    contentPane.add(initToolbar(), BorderLayout.NORTH);
    setContentPane(contentPane);

    loginDlg = new LoginDialog(this, portal);


    JTabbedPane contentTabs = new JTabbedPane(JTabbedPane.TOP);
    contentPane.add(contentTabs, BorderLayout.CENTER);

    accountPanel = new AccountPanel(portal.getClient());
    contentTabs.addTab("\u8D26\u6237", null, accountPanel, "\u8D26\u6237\u4FE1\u606F");

    positionPanel = new PositionPanel(portal.getClient());
    contentTabs.addTab("\u6301\u4ED3", null, positionPanel, "\u6301\u4ED3\u4FE1\u606F");

    queryOrderPanel = new QueryOrderPanel(portal.getClient());
    contentTabs.addTab("\u67E5\u8BE2\u62A5\u5355", null, queryOrderPanel, "\u67E5\u8BE2\u62A5\u5355");
  }
}
