/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

import com.nabiki.log.portal.core.FilterConditionType;
import com.nabiki.log.portal.core.LogDisplay;
import com.nabiki.log.portal.core.LogFilter;
import com.nabiki.log.portal.core.LogSource;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LogMainWin {
	private ConnectDialog connDlg;
	private TalkDialog talkDlg;
	private JFrame frame;
	private JTextField loggerField;
	private JTextField levelField;
	private JTextField msgField;
	private JTextArea logArea;
	private JComboBox<FilterConditionType> loggerComb;
	private JComboBox<FilterConditionType> levelComb;
	private JComboBox<FilterConditionType> msgComb;

	private final LogFilter filter = new LogFilter();
	private LogDisplay display;
	private LogSource source;

	/**
	 * Launch the application.
	 */
	public static void work(int onClose) {
		EventQueue.invokeLater(() -> {
			try {
				LogMainWin window = new LogMainWin(onClose);
				window.frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * Create the application.
	 */
	public LogMainWin(int onClose) {
		initialize(onClose);
		core();
	}

	private void core() {
		display = new LogDisplay(logArea, filter);
		source = new LogSource(display);
		connDlg = new ConnectDialog(frame, source);
		talkDlg = new TalkDialog(frame);
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(int onClose) {
		frame = new JFrame();
		frame.setTitle("\u672A\u8FDE\u63A5");
		frame.setBounds(100, 100, 900, 600);
		frame.setDefaultCloseOperation(onClose);
		
		JToolBar toolBar = new JToolBar();
		toolBar.setFloatable(false);
		frame.getContentPane().add(toolBar, BorderLayout.NORTH);
		
		JButton connectBtn = new JButton("\u8FDE\u63A5");
		connectBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				connDlg.display();
			}
		});
		toolBar.add(connectBtn);
		
		JButton talkBtn = new JButton("\u5BF9\u8BDD");
		talkBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				talkDlg.display();
			}
		});
		toolBar.add(talkBtn);
		
		JPanel mainPanel = new JPanel();
		frame.getContentPane().add(mainPanel, BorderLayout.CENTER);
		SpringLayout sl_mainPanel = new SpringLayout();
		mainPanel.setLayout(sl_mainPanel);
		
		JLabel loggerLabel = new JLabel("\u540D\u79F0");
		sl_mainPanel.putConstraint(SpringLayout.NORTH, loggerLabel, 15, SpringLayout.NORTH, mainPanel);
		sl_mainPanel.putConstraint(SpringLayout.WEST, loggerLabel, 10, SpringLayout.WEST, mainPanel);
		mainPanel.add(loggerLabel);

		JLabel levelLabel = new JLabel("\u7EA7\u522B");
		sl_mainPanel.putConstraint(SpringLayout.NORTH, levelLabel, 10, SpringLayout.SOUTH, loggerLabel);
		sl_mainPanel.putConstraint(SpringLayout.WEST, levelLabel, 0, SpringLayout.WEST, loggerLabel);
		mainPanel.add(levelLabel);

		JLabel msgLabel = new JLabel("\u6D88\u606F");
		sl_mainPanel.putConstraint(SpringLayout.NORTH, msgLabel, 10, SpringLayout.SOUTH, levelLabel);
		sl_mainPanel.putConstraint(SpringLayout.WEST, msgLabel, 0, SpringLayout.WEST, loggerLabel);
		mainPanel.add(msgLabel);

		loggerComb = new JComboBox<>();
		sl_mainPanel.putConstraint(SpringLayout.NORTH, loggerComb, -2, SpringLayout.NORTH, loggerLabel);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, loggerComb, 2, SpringLayout.SOUTH, loggerLabel);
		sl_mainPanel.putConstraint(SpringLayout.EAST, loggerComb, 120, SpringLayout.EAST, loggerLabel);
		loggerComb.setModel(new DefaultComboBoxModel<>(FilterConditionType.values()));
		sl_mainPanel.putConstraint(SpringLayout.WEST, loggerComb, 20, SpringLayout.EAST, loggerLabel);
		mainPanel.add(loggerComb);

		levelComb = new JComboBox<>();
		levelComb.setModel(new DefaultComboBoxModel<>(FilterConditionType.values()));
		sl_mainPanel.putConstraint(SpringLayout.NORTH, levelComb, -2, SpringLayout.NORTH, levelLabel);
		sl_mainPanel.putConstraint(SpringLayout.WEST, levelComb, 20, SpringLayout.EAST, levelLabel);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, levelComb, 2, SpringLayout.SOUTH, levelLabel);
		sl_mainPanel.putConstraint(SpringLayout.EAST, levelComb, 120, SpringLayout.EAST, levelLabel);
		mainPanel.add(levelComb);

		msgComb = new JComboBox<>();
		msgComb.setModel(new DefaultComboBoxModel<>(FilterConditionType.values()));
		sl_mainPanel.putConstraint(SpringLayout.NORTH, msgComb, -2, SpringLayout.NORTH, msgLabel);
		sl_mainPanel.putConstraint(SpringLayout.WEST, msgComb, 20, SpringLayout.EAST, msgLabel);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, msgComb, 2, SpringLayout.SOUTH, msgLabel);
		sl_mainPanel.putConstraint(SpringLayout.EAST, msgComb, 120, SpringLayout.EAST, msgLabel);
		mainPanel.add(msgComb);

		loggerField = new JTextField();
		loggerField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_mainPanel.putConstraint(SpringLayout.NORTH, loggerField, 0, SpringLayout.NORTH, loggerComb);
		sl_mainPanel.putConstraint(SpringLayout.WEST, loggerField, 20, SpringLayout.EAST, loggerComb);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, loggerField, 0, SpringLayout.SOUTH, loggerComb);
		sl_mainPanel.putConstraint(SpringLayout.EAST, loggerField, -100, SpringLayout.EAST, mainPanel);
		mainPanel.add(loggerField);
		loggerField.setColumns(10);
		
		levelField = new JTextField();
		sl_mainPanel.putConstraint(SpringLayout.NORTH, levelField, 0, SpringLayout.NORTH, levelComb);
		sl_mainPanel.putConstraint(SpringLayout.WEST, levelField, 20, SpringLayout.EAST, levelComb);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, levelField, 0, SpringLayout.SOUTH, levelComb);
		sl_mainPanel.putConstraint(SpringLayout.EAST, levelField, -100, SpringLayout.EAST, mainPanel);
		levelField.setHorizontalAlignment(SwingConstants.CENTER);
		mainPanel.add(levelField);
		levelField.setColumns(10);
		
		msgField = new JTextField();
		msgField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_mainPanel.putConstraint(SpringLayout.NORTH, msgField, 0, SpringLayout.NORTH, msgComb);
		sl_mainPanel.putConstraint(SpringLayout.WEST, msgField, 20, SpringLayout.EAST, msgComb);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, msgField, 0, SpringLayout.SOUTH, msgComb);
		sl_mainPanel.putConstraint(SpringLayout.EAST, msgField, -100, SpringLayout.EAST, mainPanel);
		mainPanel.add(msgField);
		msgField.setColumns(10);
		
		JButton filterBtn = new JButton("\u7B5B\u9009");
		sl_mainPanel.putConstraint(SpringLayout.NORTH, filterBtn, 0, SpringLayout.NORTH, loggerField);
		sl_mainPanel.putConstraint(SpringLayout.WEST, filterBtn, 20, SpringLayout.EAST, loggerField);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, filterBtn, 10, SpringLayout.SOUTH, loggerField);
		sl_mainPanel.putConstraint(SpringLayout.EAST, filterBtn, -10, SpringLayout.EAST, mainPanel);
		mainPanel.add(filterBtn);
		filterBtn.addActionListener(e -> {
			if (filterBtn.getText().equals("\u7B5B\u9009")) {
				setFilter();
				enableFilterUI(false);
				filter.enable(true);
				display.reset();
				filterBtn.setText("\u505C\u6B62");
			} else {
				enableFilterUI(true);
				filter.enable(false);
				display.reset();
				filterBtn.setText("\u7B5B\u9009");
			}
		});

		JButton clearBtn = new JButton("\u6E05\u9664");
		sl_mainPanel.putConstraint(SpringLayout.NORTH, clearBtn, -10, SpringLayout.NORTH, msgField);
		sl_mainPanel.putConstraint(SpringLayout.WEST, clearBtn, 20, SpringLayout.EAST, loggerField);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, clearBtn, 0, SpringLayout.SOUTH, msgField);
		sl_mainPanel.putConstraint(SpringLayout.EAST, clearBtn, -10, SpringLayout.EAST, mainPanel);
		mainPanel.add(clearBtn);
		clearBtn.addActionListener(e -> {
			display.clear();
		});
		
		JScrollPane logScrollPane = new JScrollPane();
		sl_mainPanel.putConstraint(SpringLayout.NORTH, logScrollPane, 10, SpringLayout.SOUTH, msgComb);
		sl_mainPanel.putConstraint(SpringLayout.WEST, logScrollPane, 10, SpringLayout.WEST, mainPanel);
		sl_mainPanel.putConstraint(SpringLayout.SOUTH, logScrollPane, -10, SpringLayout.SOUTH, mainPanel);
		sl_mainPanel.putConstraint(SpringLayout.EAST, logScrollPane, -10, SpringLayout.EAST, mainPanel);
		mainPanel.add(logScrollPane);
		
		logArea = new JTextArea();
		logArea.setMargin(new Insets(2, 4, 2, 2));
		logArea.setEditable(false);
		logScrollPane.setViewportView(logArea);
	}

	private void setFilter() {
		filter.setLoggerCondition((FilterConditionType) loggerComb.getSelectedItem(), loggerField.getText());
		filter.setMsgCondition((FilterConditionType) msgComb.getSelectedItem(), msgField.getText());
		filter.setLevelCondition((FilterConditionType) levelComb.getSelectedItem(), levelField.getText());
	}

	private void enableFilterUI(boolean b) {
		loggerComb.setEnabled(b);
		loggerField.setEnabled(b);
		levelComb.setEnabled(b);
		levelField.setEnabled(b);
		msgComb.setEnabled(b);
		msgField.setEnabled(b);
	}
}
