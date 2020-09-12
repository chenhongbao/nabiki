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
	final JTable accountTable;
	final LoginDialog loginDlg;
	final JTextField instrumentField;
	final JTable positionTable;
	final JTable orderTable;
	final JTextField orderIDField;
	final JTextField insertInstrumentField;
	final JTextField insertPriceField;
	final JTextField insertVolumeField;
	final JTable insertedOrderTable;
	final JLabel userNameText;
	final JLabel loginStateText;

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
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
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
		
		JPanel accountPanel = new JPanel();
		accountPanel.setBorder(null);
		contentTabs.addTab("\u8D26\u6237", null, accountPanel, "\u8D26\u6237\u4FE1\u606F");
		SpringLayout sl_accountPanel = new SpringLayout();
		accountPanel.setLayout(sl_accountPanel);
		
		accountTable = new JTable();
		accountTable.setBackground(Color.WHITE);
		accountTable.setModel(new DefaultTableModel(
			new Object[][] {
				{"\u7ECF\u7EAA\u5546", null, null},
				{"\u8D26\u6237\u53F7", null, null},
				{"\u6743\u76CA", null, null},
				{"\u4FDD\u8BC1\u91D1", null, null},
				{"\u53EF\u7528\u8D44\u91D1", null, null},
				{"\u624B\u7EED\u8D39", null, null},
				{"\u51BB\u7ED3\u624B\u7EED\u8D39", null, null},
				{"\u51BB\u7ED3\u73B0\u91D1", null, null},
				{"\u51BB\u7ED3\u4FDD\u8BC1\u91D1", null, null},
				{"\u5E73\u4ED3\u5229\u6DA6", null, null},
				{"\u6301\u4ED3\u5229\u6DA6", null, null},
				{"\u4EA4\u6613\u65E5", null, null},
			},
			new String[] {
				"\u5C5E\u6027", "\u4ECA\u65E5\u503C", "\u6628\u65E5\u503C"
			}
		) {
			final Class[] columnTypes = new Class[] {
				String.class, String.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			final boolean[] columnEditables = new boolean[] {
				false, false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		
		JScrollPane accountScrollPane = new JScrollPane(accountTable);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, accountScrollPane, 50, SpringLayout.NORTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, accountScrollPane, 10, SpringLayout.WEST, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, accountScrollPane, -220, SpringLayout.SOUTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, accountScrollPane, -10, SpringLayout.EAST, accountPanel);
		accountScrollPane.setViewportBorder(null);
		accountPanel.add(accountScrollPane);
		
		JButton refreshBtn = new JButton("\u5237\u65B0");
		refreshBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_accountPanel.putConstraint(SpringLayout.EAST, refreshBtn, -10, SpringLayout.EAST, accountPanel);
		accountPanel.add(refreshBtn);
		
		JLabel userNameLabel = new JLabel("\u8D26\u53F7:");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, userNameLabel, 10, SpringLayout.NORTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, userNameLabel, 10, SpringLayout.WEST, accountPanel);
		accountPanel.add(userNameLabel);
		
		userNameText = new JLabel("\u65E0");
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, refreshBtn, 0, SpringLayout.SOUTH, userNameText);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, userNameText, 10, SpringLayout.NORTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, userNameText, 10, SpringLayout.EAST, userNameLabel);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, userNameText, 0, SpringLayout.SOUTH, userNameLabel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, userNameText, 100, SpringLayout.EAST, userNameLabel);
		accountPanel.add(userNameText);
		
		JLabel loginStateLabel = new JLabel("\u72B6\u6001:");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, loginStateLabel, 10, SpringLayout.NORTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, loginStateLabel, 10, SpringLayout.EAST, userNameText);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, loginStateLabel, 0, SpringLayout.SOUTH, userNameLabel);
		accountPanel.add(loginStateLabel);
		
		loginStateText = new JLabel("\u672A\u767B\u5F55");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, refreshBtn, 0, SpringLayout.NORTH, loginStateText);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, loginStateText, 10, SpringLayout.NORTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, loginStateText, 10, SpringLayout.EAST, loginStateLabel);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, loginStateText, 0, SpringLayout.SOUTH, userNameLabel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, loginStateText, 100, SpringLayout.EAST, loginStateLabel);
		accountPanel.add(loginStateText);
		
		JLabel insertInstrumentLabel = new JLabel("\u62A5\u5355\u5408\u7EA6:");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertInstrumentLabel, 20, SpringLayout.SOUTH, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertInstrumentLabel, 0, SpringLayout.WEST, accountScrollPane);
		accountPanel.add(insertInstrumentLabel);
		
		insertInstrumentField = new JTextField();
		insertInstrumentField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertInstrumentField, 16, SpringLayout.SOUTH, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertInstrumentField, 5, SpringLayout.EAST, insertInstrumentLabel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, insertInstrumentField, 200, SpringLayout.EAST, insertInstrumentLabel);
		accountPanel.add(insertInstrumentField);
		insertInstrumentField.setColumns(10);
		
		JLabel insertPriceLabel = new JLabel("\u9650\u4EF7:");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertPriceLabel, 20, SpringLayout.SOUTH, insertInstrumentLabel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertPriceLabel, 0, SpringLayout.WEST, accountScrollPane);
		accountPanel.add(insertPriceLabel);
		
		insertPriceField = new JTextField();
		insertPriceField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertPriceField, 15, SpringLayout.SOUTH, insertInstrumentField);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertPriceField, 0, SpringLayout.WEST, insertInstrumentField);
		sl_accountPanel.putConstraint(SpringLayout.EAST, insertPriceField, 0, SpringLayout.EAST, insertInstrumentField);
		accountPanel.add(insertPriceField);
		insertPriceField.setColumns(10);
		
		JLabel insertVolumeLabel = new JLabel("\u6570\u91CF:");
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertVolumeLabel, 20, SpringLayout.SOUTH, insertPriceLabel);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertVolumeLabel, 0, SpringLayout.WEST, accountScrollPane);
		accountPanel.add(insertVolumeLabel);
		
		insertVolumeField = new JTextField();
		insertVolumeField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertVolumeField, 15, SpringLayout.SOUTH, insertPriceField);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertVolumeField, 0, SpringLayout.WEST, insertPriceField);
		sl_accountPanel.putConstraint(SpringLayout.EAST, insertVolumeField, 0, SpringLayout.EAST, insertPriceField);
		accountPanel.add(insertVolumeField);
		insertVolumeField.setColumns(10);
		
		JButton buyOpenBtn = new JButton("\u4E70\u5F00");
		buyOpenBtn.setForeground(Color.WHITE);
		buyOpenBtn.setBackground(Color.RED);
		sl_accountPanel.putConstraint(SpringLayout.NORTH, buyOpenBtn, 22, SpringLayout.SOUTH, insertVolumeField);
		sl_accountPanel.putConstraint(SpringLayout.WEST, buyOpenBtn, 0, SpringLayout.WEST, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.EAST, buyOpenBtn, 100, SpringLayout.WEST, accountPanel);
		accountPanel.add(buyOpenBtn);
		
		JButton buyCloseBtn = new JButton("\u4E70\u5E73");
		buyCloseBtn.setBackground(Color.RED);
		buyCloseBtn.setForeground(Color.LIGHT_GRAY);
		sl_accountPanel.putConstraint(SpringLayout.WEST, buyCloseBtn, -90, SpringLayout.EAST, insertVolumeField);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, buyCloseBtn, 0, SpringLayout.SOUTH, buyOpenBtn);
		sl_accountPanel.putConstraint(SpringLayout.EAST, buyCloseBtn, 0, SpringLayout.EAST, insertInstrumentField);
		accountPanel.add(buyCloseBtn);
		
		JButton sellOpenBtn = new JButton("\u5356\u5F00");
		sellOpenBtn.setForeground(Color.WHITE);
		sellOpenBtn.setBackground(new Color(0, 128, 0));
		sl_accountPanel.putConstraint(SpringLayout.WEST, sellOpenBtn, 0, SpringLayout.WEST, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, sellOpenBtn, -10, SpringLayout.SOUTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, sellOpenBtn, 100, SpringLayout.WEST, accountPanel);
		accountPanel.add(sellOpenBtn);
		
		JButton sellCloseBtn = new JButton("\u5356\u5E73");
		sellCloseBtn.setForeground(Color.LIGHT_GRAY);
		sellCloseBtn.setBackground(new Color(0, 128, 0));
		sl_accountPanel.putConstraint(SpringLayout.WEST, sellCloseBtn, -90, SpringLayout.EAST, insertVolumeField);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, sellCloseBtn, 0, SpringLayout.SOUTH, sellOpenBtn);
		sl_accountPanel.putConstraint(SpringLayout.EAST, sellCloseBtn, 0, SpringLayout.EAST, insertInstrumentField);
		accountPanel.add(sellCloseBtn);
		
		JScrollPane insertedOrderScrollPanel = new JScrollPane();
		sl_accountPanel.putConstraint(SpringLayout.NORTH, insertedOrderScrollPanel, 16, SpringLayout.SOUTH, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.WEST, insertedOrderScrollPanel, 300, SpringLayout.WEST, accountScrollPane);
		sl_accountPanel.putConstraint(SpringLayout.SOUTH, insertedOrderScrollPanel, -10, SpringLayout.SOUTH, accountPanel);
		sl_accountPanel.putConstraint(SpringLayout.EAST, insertedOrderScrollPanel, 0, SpringLayout.EAST, accountScrollPane);
		accountPanel.add(insertedOrderScrollPanel);
		
		insertedOrderTable = new JTable();
		insertedOrderTable.setCellSelectionEnabled(true);
		insertedOrderTable.setModel(new DefaultTableModel(
			new Object[][] {
				{null, null},
			},
			new String[] {
				"\u65F6\u95F4", "\u62A5\u5355\u7F16\u53F7"
			}
		) {
			final Class[] columnTypes = new Class[] {
				String.class, String.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
			final boolean[] columnEditables = new boolean[] {
				true, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		insertedOrderTable.getColumnModel().getColumn(1).setPreferredWidth(250);
		insertedOrderTable.getColumnModel().getColumn(1).setMinWidth(250);
		insertedOrderScrollPanel.setViewportView(insertedOrderTable);
		
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
		
		JButton btnNewButton = new JButton("\u67E5\u8BE2");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		sl_positionPanel.putConstraint(SpringLayout.NORTH, btnNewButton, 0, SpringLayout.NORTH, instrumentField);
		sl_positionPanel.putConstraint(SpringLayout.SOUTH, btnNewButton, 0, SpringLayout.SOUTH, instrumentField);
		positionPanel.add(btnNewButton);
		
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
		sl_positionPanel.putConstraint(SpringLayout.WEST, btnNewButton, 10, SpringLayout.EAST, positionTypeComb);
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
	}
}
