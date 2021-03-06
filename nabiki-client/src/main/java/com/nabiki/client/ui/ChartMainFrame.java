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

import com.nabiki.chart.control.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;

public class ChartMainFrame extends JFrame {
  private final JPanel contentPane;
  private final LogDialog logDialog;

  private ViewController ctrl;
  private ChartPanel chart;
  private String instrumentID;
  private int minute;

  /**
   * Launch the application.
   */

  public void setInstrumentID(String instrumentID) {
    this.instrumentID = instrumentID;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public String getInstrumentID() {
    return this.instrumentID;
  }

  public int getMinute() {
    return this.minute;
  }

  public ViewController getChartController() {
    return ctrl;
  }

  /**
   * Create the frame.
   */
  public ChartMainFrame(LogDialog logDlg, ChartPanel chart) {
    // Set default ops.
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    setBounds(100, 100, 900, 600);
    contentPane = getContentPanel();
    contentPane.add(getToolBar(), BorderLayout.NORTH);
    contentPane.add(chart, BorderLayout.CENTER);
    setController(chart);
    logDialog = logDlg;
  }

  private JToolBar getToolBar() {
    JToolBar toolBar = new JToolBar();
    toolBar.setFloatable(false);
    // Buttons on toolbar.
    // Print btn.
    JButton printBtn = new JButton("\u6253\u5370");
    printBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        printImage();
      }
    });
    printBtn.setToolTipText("\u5C06\u5F53\u524D\u884C\u60C5\u4FDD\u5B58\u81F3\u56FE\u7247");
    toolBar.add(printBtn);
    // Reset btn.
    JButton resetBtn = new JButton("\u91CD\u7F6E");
    resetBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ctrl.reset();
      }
    });
    resetBtn.setToolTipText("\u91CD\u7F6E\u884C\u60C5\u81F3\u6700\u53F3\u4FA7");
    toolBar.add(resetBtn);
    // Most-left btn.
    JButton mostLeftBtn = new JButton("\u6700\u5DE6");
    mostLeftBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int move = Math.max(ctrl.getDataCount() - ctrl.getShownSize(), 0);
        ctrl.backward(move);
      }
    });
    mostLeftBtn.setToolTipText("\u56DE\u6EAF\u5386\u53F2\u884C\u60C5\u81F3\u6700\u5DE6\u4FA7");
    toolBar.add(mostLeftBtn);
    // Left btn.
    JButton goLeftBtn = new JButton("\u5411\u5DE6");
    goLeftBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ctrl.backward(1);
      }
    });
    goLeftBtn.setToolTipText("\u5411\u5DE6\u6D4F\u89C8\u5386\u53F2\u884C\u60C5");
    toolBar.add(goLeftBtn);
    // Right btn.
    JButton goRightBtn = new JButton("\u5411\u53F3");
    goRightBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ctrl.forward(1);
      }
    });
    goRightBtn.setToolTipText("\u5411\u53F3\u6D4F\u89C8\u884C\u60C5");
    toolBar.add(goRightBtn);
    // Most-right btn.
    JButton mostRightBtn = new JButton("\u6700\u53F3");
    mostRightBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        ctrl.reset();
      }
    });
    mostRightBtn.setToolTipText("\u884C\u60C5\u6D4F\u89C8\u81F3\u6700\u53F3\u4FA7");
    toolBar.add(mostRightBtn);
    // Zoom in.
    JButton zoomInBtn = new JButton("\u653E\u5927");
    zoomInBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.zoomIn();
      }
    });
    zoomInBtn.setToolTipText("\u653E\u5927\u4E00\u500D");
    toolBar.add(zoomInBtn);
    // Zoom out.
    JButton zoomOutBtn = new JButton("\u7F29\u5C0F");
    zoomOutBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        ctrl.zoomOut();
      }
    });
    zoomOutBtn.setToolTipText("\u7F29\u5C0F\u4E00\u500D");
    toolBar.add(zoomOutBtn);
    // Separate blank.
    JSeparator separator = new JSeparator();
    toolBar.add(separator);
    // Log dialog btn.
    JButton logBtn = new JButton("\u65E5\u5FD7");
    logBtn.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        logDialog.setLocation(getLogDialogPosition());
        logDialog.setVisible(true);
      }
    });
    logBtn.setToolTipText("\u6253\u5F00\u65E5\u5FD7\u7A97\u53E3");
    toolBar.add(logBtn);
    return toolBar;
  }

  private JPanel getContentPanel() {
    var panel = new JPanel();
    panel.setBorder(null);
    setContentPane(panel);
    panel.setLayout(new BorderLayout(0, 0));
    return panel;
  }

  void setController(ChartPanel chart) {
    if (chart instanceof StickChartPanel) {
      ctrl = new StickChartController((StickChartPanel) chart);
    } else if (chart instanceof BarChartPanel) {
      ctrl = new BarChartController((BarChartPanel) chart);
    } else {
      throw new IllegalArgumentException("unknown instance " + chart.getClass().getName());
    }
  }

  private Point getLogDialogPosition() {
    var point = super.getLocationOnScreen();
    var size = super.getSize();
    return new Point(point.x + size.width, point.y);
  }

  private void printImage() {
    JFileChooser fc = new JFileChooser();
    fc.setApproveButtonText("\u6253\u5370");
    fc.setDialogTitle("\u628a\u56fe\u7247\u6253\u5370\u5230\u6587\u4ef6");
    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
    if (fc.showOpenDialog(contentPane) == JFileChooser.APPROVE_OPTION) {
      try {
        ImageIO.write(
            (BufferedImage) chart.getBuffer(),
            "png",
            fc.getSelectedFile());
      } catch (Throwable th) {
        JOptionPane.showMessageDialog(contentPane, th.getMessage());
      }
    }
  }
}
