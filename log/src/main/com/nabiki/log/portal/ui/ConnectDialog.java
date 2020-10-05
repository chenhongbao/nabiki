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

import com.nabiki.log.portal.core.LogSource;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;

public class ConnectDialog extends JDialog {
	private final JPanel contentPanel = new JPanel();
	private final JTextField addressField;
	private final JButton connBtn;

	private final LogSource source;

	private void locateSelf() {
		var pp = getParent().getLocation();
		var pSize = getParent().getSize();
		var size = getSize();
		var x = pp.x + (pSize.width - size.width) / 2;
		var y = pp.y + (pSize.height - size.height) / 2;
		setLocation(new Point(x, y));
	}

	public void display() {
		locateSelf();
		if (!isVisible()) {
			setVisible(true);
		}
	}

	public String getAddress() {
		return addressField.getText();
	}

	/**
	 * Create the dialog.
	 */
	public ConnectDialog(JFrame parent, LogSource source) {
		super(parent);

		this.source = source;

		setModal(true);
		setTitle("\u8FDE\u63A5\u670D\u52A1\u5668");
		setBounds(100, 100, 550, 100);
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		SpringLayout sl_contentPanel = new SpringLayout();
		contentPanel.setLayout(sl_contentPanel);
		
		JLabel addressLabel = new JLabel("\u670D\u52A1\u5668\u5730\u5740");
		sl_contentPanel.putConstraint(SpringLayout.NORTH, addressLabel, 20, SpringLayout.NORTH, contentPanel);
		sl_contentPanel.putConstraint(SpringLayout.WEST, addressLabel, 10, SpringLayout.WEST, contentPanel);
		contentPanel.add(addressLabel);
		
		addressField = new JTextField();
		addressField.setHorizontalAlignment(SwingConstants.CENTER);
		sl_contentPanel.putConstraint(SpringLayout.NORTH, addressField, -5, SpringLayout.NORTH, addressLabel);
		sl_contentPanel.putConstraint(SpringLayout.WEST, addressField, 10, SpringLayout.EAST, addressLabel);
		sl_contentPanel.putConstraint(SpringLayout.SOUTH, addressField, 5, SpringLayout.SOUTH, addressLabel);
		sl_contentPanel.putConstraint(SpringLayout.EAST, addressField, -80, SpringLayout.EAST, contentPanel);
		contentPanel.add(addressField);
		addressField.setColumns(10);
		
		connBtn = new JButton("\u8FDE\u63A5");
		sl_contentPanel.putConstraint(SpringLayout.NORTH, connBtn, 0, SpringLayout.NORTH, addressField);
		sl_contentPanel.putConstraint(SpringLayout.WEST, connBtn, 10, SpringLayout.EAST, addressField);
		sl_contentPanel.putConstraint(SpringLayout.SOUTH, connBtn, 0, SpringLayout.SOUTH, addressField);
		sl_contentPanel.putConstraint(SpringLayout.EAST, connBtn, -10, SpringLayout.EAST, contentPanel);
		contentPanel.add(connBtn);
		connBtn.addActionListener(e -> {
			onConnectBtn();
		});
	}

	private void onConnectBtn() {
		try {
			var address = getAddress().trim();
			if (address == null || address.length() == 0) {
				JOptionPane.showMessageDialog(this,
						"\u6CA1\u6709\u65E5\u5FD7\u670D\u52A1\u5668\u5730\u5740\uFF0C\u4F8B\u5982127.0.0.1:9039");
				return;
			}
			if (connBtn.getText().equals("\u8FDE\u63A5")) {
				source.open(address);
				connBtn.setText("\u65Ad\u5F00");
				addressField.setEnabled(false);
				setVisible(false);
			} else {
				source.close();
				connBtn.setText("\u8FDE\u63A5");
				setVisible(true);
			}
		} catch (IOException io) {
			JOptionPane.showMessageDialog(this, io.getMessage());
		}
	}
}
