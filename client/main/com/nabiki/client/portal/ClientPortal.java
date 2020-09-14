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

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ClientPortal extends JFrame {

	final JPanel contentPane;
	final AccountPanel accountPanel;
	final LoginDialog loginDlg;
	final JTextField instrumentField;
	final JTable positionTable;
	final JTable orderTable;
	final JTextField orderIDField;

	// Client backend.
	final Portal portal = new Portal();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
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

	/**
	 * Create the frame.
	 */
	public ClientPortal() {
		setTitle("\u624B\u52A8\u64CD\u4F5C\u5165\u53E3");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 800, 600);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		loginDlg = new LoginDialog(this, portal);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		contentPane.add(toolBar, BorderLayout.NORTH);
		
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
		
		JTabbedPane contentTabs = new JTabbedPane(JTabbedPane.TOP);
		contentPane.add(contentTabs, BorderLayout.CENTER);
		
		accountPanel = new AccountPanel(portal.getClient());
		contentTabs.addTab("\u8D26\u6237", null, accountPanel, "\u8D26\u6237\u4FE1\u606F");
		
		JPanel positionPanel = new JPanel();
		contentTabs.addTab("\u6301\u4ED3", null, positionPanel, "\u6301\u4ED3\u4FE1\u606F");
		SpringLayout sl_positionPanel = new SpringLayout();
		positionPanel.setLayout(sl_positionPanel);
		
		JCheckBox positionCatCheck = new JCheckBox("\u6240\u6709\u6301\u4ED3");
		positionCatCheck.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_positionPanel.putConstraint(SpringLayout.NORTH, positionCatCheck, 10, SpringLayout.NORTH, positionPanel);
		sl_positionPanel.putConstraint(SpringLayout.WEST, positionCatCheck, 10, SpringLayout.WEST, positionPanel);
		positionPanel.add(positionCatCheck);
		
		JLabel instrumentLabel = new JLabel("\u5408\u7EA6:");
		sl_positionPanel.putConstraint(SpringLayout.NORTH, instrumentLabel, 14, SpringLayout.NORTH, positionPanel);
		sl_positionPanel.putConstraint(SpringLayout.WEST, instrumentLabel, 80, SpringLayout.WEST, positionCatCheck);
		positionPanel.add(instrumentLabel);
		
		instrumentField = new JTextField();
		sl_positionPanel.putConstraint(SpringLayout.WEST, instrumentField, 5, SpringLayout.EAST, instrumentLabel);
		sl_positionPanel.putConstraint(SpringLayout.EAST, instrumentField, 100, SpringLayout.EAST, instrumentLabel);
		instrumentLabel.setLabelFor(instrumentField);
		instrumentField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_positionPanel.putConstraint(SpringLayout.NORTH, instrumentField, 10, SpringLayout.NORTH, positionPanel);
		positionPanel.add(instrumentField);
		instrumentField.setColumns(10);
		
		JButton qryPositionBtn = new JButton("\u67E5\u8BE2");
		qryPositionBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_positionPanel.putConstraint(SpringLayout.NORTH, qryPositionBtn, 0, SpringLayout.NORTH, instrumentField);
		sl_positionPanel.putConstraint(SpringLayout.SOUTH, qryPositionBtn, 0, SpringLayout.SOUTH, instrumentField);
		positionPanel.add(qryPositionBtn);
		
		JScrollPane positionScrollPane = new JScrollPane();
		sl_positionPanel.putConstraint(SpringLayout.NORTH, positionScrollPane, 50, SpringLayout.NORTH, positionPanel);
		sl_positionPanel.putConstraint(SpringLayout.WEST, positionScrollPane, 10, SpringLayout.WEST, positionPanel);
		sl_positionPanel.putConstraint(SpringLayout.SOUTH, positionScrollPane, -10, SpringLayout.SOUTH, positionPanel);
		sl_positionPanel.putConstraint(SpringLayout.EAST, positionScrollPane, -10, SpringLayout.EAST, positionPanel);
		positionPanel.add(positionScrollPane);
		
		positionTable = new JTable();
		positionTable.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null, null, null, null, "", null, null, null, null},
			},
			new String[] {
				"\u5408\u7EA6", "\u65B9\u5411", "\u603B\u8BA1", "\u6628\u4ED3", "\u4ECA\u4ED3", "\u5F00\u4ED3\u4EF7", "\u6628\u7ED3", "\u7ED3\u7B97\u4EF7", "\u9010\u7B14\u76C8\u4E8F", "\u9010\u65E5\u76C8\u4E8F"
			}
		) {
			final Class[] columnTypes = new Class[] {
				String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			final boolean[] columnEditables = new boolean[] {
				false, false, false, false, false, false, false, false, false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		positionScrollPane.setViewportView(positionTable);
		
		JComboBox positionTypeComb = new JComboBox();
		sl_positionPanel.putConstraint(SpringLayout.WEST, qryPositionBtn, 10, SpringLayout.EAST, positionTypeComb);
		sl_positionPanel.putConstraint(SpringLayout.NORTH, positionTypeComb, 0, SpringLayout.NORTH, instrumentField);
		sl_positionPanel.putConstraint(SpringLayout.WEST, positionTypeComb, 110, SpringLayout.WEST, instrumentField);
		sl_positionPanel.putConstraint(SpringLayout.SOUTH, positionTypeComb, 0, SpringLayout.SOUTH, instrumentLabel);
		sl_positionPanel.putConstraint(SpringLayout.EAST, positionTypeComb, 80, SpringLayout.EAST, instrumentField);
		positionTypeComb.setModel(new DefaultComboBoxModel(new String[] {"\u6C47\u603B", "\u660E\u7EC6"}));
		positionPanel.add(positionTypeComb);
		
		JPanel queryOrderPanel = new JPanel();
		contentTabs.addTab("\u67E5\u8BE2\u62A5\u5355", null, queryOrderPanel, "\u67E5\u8BE2\u62A5\u5355");
		SpringLayout sl_queryOrderPanel = new SpringLayout();
		queryOrderPanel.setLayout(sl_queryOrderPanel);
		
		JScrollPane orderScrollPane = new JScrollPane();
		sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderScrollPane, 50, SpringLayout.NORTH, queryOrderPanel);
		sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderScrollPane, 10, SpringLayout.WEST, queryOrderPanel);
		sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, orderScrollPane, -10, SpringLayout.SOUTH, queryOrderPanel);
		sl_queryOrderPanel.putConstraint(SpringLayout.EAST, orderScrollPane, -10, SpringLayout.EAST, queryOrderPanel);
		queryOrderPanel.add(orderScrollPane);
		
		orderTable = new JTable();
		orderTable.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null, null, null, null, null, null, null, null, null, null, null},
			},
			new String[] {
				"\u62A5\u5355\u5F15\u7528", "\u5408\u7EA6", "\u9650\u4EF7", "\u4E70\u5356", "\u5F00\u5E73", "\u603B\u91CF", "\u5DF2\u6210", "\u6392\u961F", "\u63D0\u4EA4", "\u6210\u4EA4", "\u66F4\u65B0", "\u65E5\u671F"
			}
		) {
			final Class[] columnTypes = new Class[] {
				String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			final boolean[] columnEditables = new boolean[] {
				false, false, false, false, true, true, true, true, false, false, false, true
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		orderScrollPane.setViewportView(orderTable);
		
		JLabel orderIDLabel = new JLabel("\u62A5\u5355\u7F16\u53F7:");
		sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderIDLabel, 14, SpringLayout.NORTH, queryOrderPanel);
		sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderIDLabel, 10, SpringLayout.WEST, queryOrderPanel);
		queryOrderPanel.add(orderIDLabel);
		
		orderIDField = new JTextField();
		sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, orderIDField, 10, SpringLayout.NORTH, queryOrderPanel);
		sl_queryOrderPanel.putConstraint(SpringLayout.WEST, orderIDField, 5, SpringLayout.EAST, orderIDLabel);
		sl_queryOrderPanel.putConstraint(SpringLayout.EAST, orderIDField, 250, SpringLayout.EAST, orderIDLabel);
		queryOrderPanel.add(orderIDField);
		orderIDField.setColumns(10);
		
		JButton queryOrderBtn = new JButton("\u67E5\u8BE2");
		queryOrderBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, queryOrderBtn, 0, SpringLayout.NORTH, orderIDField);
		sl_queryOrderPanel.putConstraint(SpringLayout.WEST, queryOrderBtn, 10, SpringLayout.EAST, orderIDField);
		sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, queryOrderBtn, 0, SpringLayout.SOUTH, orderIDField);
		queryOrderPanel.add(queryOrderBtn);

		JButton cancelOrderBtn = new JButton("\u64A4\u5355");
		cancelOrderBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_queryOrderPanel.putConstraint(SpringLayout.NORTH, cancelOrderBtn, 0, SpringLayout.NORTH, orderIDField);
		sl_queryOrderPanel.putConstraint(SpringLayout.WEST, cancelOrderBtn, 10, SpringLayout.EAST, queryOrderBtn);
		sl_queryOrderPanel.putConstraint(SpringLayout.SOUTH, cancelOrderBtn, 0, SpringLayout.SOUTH, orderIDField);
		queryOrderPanel.add(cancelOrderBtn);
	}
}
