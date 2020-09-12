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

import com.nabiki.client.sdk.ClientUtils;
import com.nabiki.objects.CReqUserLogin;
import com.nabiki.objects.ErrorCodes;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LoginDialog extends JDialog {
    private final Portal portal;
    private final JPanel contentPanel = new JPanel();
    private final JTextField addrField;
    private final JTextField userNameField;
    private final JTextField pwdField;
    private final JTextPane loginResultTxt;
    private final JButton loginBtn;
    private final JButton logoutBtn;
    private final JButton cancelBtn;

    private void locateSelf() {
        var point = super.getParent().getLocation();
        var size = super.getParent().getSize();
        var selfSize = super.getSize();
        var x = point.x + (size.width - selfSize.width) / 2;
        var y = point.y + (size.height - selfSize.height) / 2;
        super.setLocation(new Point(x, y));
    }

    public void display() {
        try {
            locateSelf();
            super.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            super.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create the dialog.
     */
    public LoginDialog(Frame owner, Portal portal) {
        super(owner, true);
        this.portal = portal;

        setTitle("\u767B\u5F55\u670D\u52A1\u5668");
        setBounds(100, 100, 450, 300);
        getContentPane().setLayout(new BorderLayout());
        contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        getContentPane().add(contentPanel, BorderLayout.CENTER);
        contentPanel.setLayout(new BorderLayout(0, 0));

        JPanel loginPanel = new JPanel();
        contentPanel.add(loginPanel, BorderLayout.CENTER);
        SpringLayout sl_loginPanel = new SpringLayout();
        loginPanel.setLayout(sl_loginPanel);

        addrField = new JTextField();
        sl_loginPanel.putConstraint(SpringLayout.WEST, addrField, 100, SpringLayout.WEST, loginPanel);
        sl_loginPanel.putConstraint(SpringLayout.EAST, addrField, -10, SpringLayout.EAST, loginPanel);
        addrField.setHorizontalAlignment(SwingConstants.CENTER);
        addrField.setColumns(10);
        loginPanel.add(addrField);

        userNameField = new JTextField();
        sl_loginPanel.putConstraint(SpringLayout.NORTH, userNameField, 6, SpringLayout.SOUTH, addrField);
        sl_loginPanel.putConstraint(SpringLayout.WEST, userNameField, 100, SpringLayout.WEST, loginPanel);
        sl_loginPanel.putConstraint(SpringLayout.EAST, userNameField, -10, SpringLayout.EAST, loginPanel);
        userNameField.setHorizontalAlignment(SwingConstants.CENTER);
        userNameField.setColumns(10);
        loginPanel.add(userNameField);

        pwdField = new JTextField();
        pwdField.setHorizontalAlignment(SwingConstants.CENTER);
        sl_loginPanel.putConstraint(SpringLayout.NORTH, pwdField, 6, SpringLayout.SOUTH, userNameField);
        sl_loginPanel.putConstraint(SpringLayout.WEST, pwdField, 100, SpringLayout.WEST, loginPanel);
        sl_loginPanel.putConstraint(SpringLayout.EAST, pwdField, -10, SpringLayout.EAST, loginPanel);
        pwdField.setColumns(10);
        loginPanel.add(pwdField);

        JLabel addrLabel = new JLabel("\u670D\u52A1\u5668\u5730\u5740");
        sl_loginPanel.putConstraint(SpringLayout.NORTH, addrField, 0, SpringLayout.NORTH, addrLabel);
        sl_loginPanel.putConstraint(SpringLayout.NORTH, addrLabel, 10, SpringLayout.NORTH, loginPanel);
        sl_loginPanel.putConstraint(SpringLayout.WEST, addrLabel, 10, SpringLayout.WEST, loginPanel);
        addrLabel.setLabelFor(addrField);
        loginPanel.add(addrLabel);

        JLabel userNameLabel = new JLabel("\u8D26      \u53F7");
        sl_loginPanel.putConstraint(SpringLayout.NORTH, userNameLabel, 30, SpringLayout.NORTH, addrField);
        sl_loginPanel.putConstraint(SpringLayout.WEST, userNameLabel, 10, SpringLayout.WEST, loginPanel);
        userNameLabel.setLabelFor(userNameField);
        loginPanel.add(userNameLabel);

        JLabel pwdLabel = new JLabel("\u5BC6      \u7801");
        sl_loginPanel.putConstraint(SpringLayout.NORTH, pwdLabel, 30, SpringLayout.NORTH, userNameField);
        sl_loginPanel.putConstraint(SpringLayout.WEST, pwdLabel, 10, SpringLayout.WEST, loginPanel);
        pwdLabel.setLabelFor(pwdField);
        loginPanel.add(pwdLabel);

        JPanel titlePanel = new JPanel();
        contentPanel.add(titlePanel, BorderLayout.NORTH);

        JLabel bigBannerLabel = new JLabel("\u767B\u5F55\u670D\u52A1\u5668");
        bigBannerLabel.setFont(new Font("SansSerif", Font.PLAIN, 36));
        titlePanel.add(bigBannerLabel);

        loginResultTxt = new JTextPane();
        loginResultTxt.setText("\u672A\u767B\u5F55");
        loginResultTxt.setBackground(UIManager.getColor("Button.background"));
        contentPanel.add(loginResultTxt, BorderLayout.SOUTH);

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
        getContentPane().add(buttonPane, BorderLayout.SOUTH);
        loginBtn = new JButton("\u767B\u5F55");
        loginBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                loginProcess();
            }
        });
        buttonPane.add(loginBtn);
        getRootPane().setDefaultButton(loginBtn);

        logoutBtn = new JButton("\u9000\u51FA");
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                logoutProcess();
            }
        });
        logoutBtn.setEnabled(false);
        buttonPane.add(logoutBtn);

        cancelBtn = new JButton("\u53D6\u6D88");
        cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                setVisible(false);
            }
        });
        buttonPane.add(cancelBtn);
    }

    private String getAddress(String address) {
        return address.substring(0, address.indexOf(":"));
    }

    private int getPort(String address) {
        return Integer.parseInt(address.substring(address.indexOf(":") + 1));
    }

    private void showLoginResult(String text, Color c) {
        loginResultTxt.setText(text);
    }

    private void loginProcess() {
        try {
            loginBtn.setEnabled(false);
            var address = addrField.getText().trim();
            if (address.length() == 0)
                throw new RuntimeException("Need server address.");
            portal.getClient().open(new InetSocketAddress(
                    getAddress(address), getPort(address)));
        } catch (Throwable th) {
            showLoginResult(th.getMessage(), Color.RED);
            loginBtn.setEnabled(true);
            return;
        }
        var req = new CReqUserLogin();
        req.UserID = userNameField.getText().trim();
        req.Password = pwdField.getText().trim();
        try {
            var rsp = ClientUtils.get(
                    portal.getClient().login(req, UUID.randomUUID().toString()),
                    10,
                    TimeUnit.SECONDS);
            if (rsp.size() == 0)
                throw new RuntimeException("Empty login response.");
            var r = rsp.values().iterator().next();
            if (r.ErrorID != ErrorCodes.NONE)
                throw new RuntimeException(r.ErrorMsg);
            else {
                showLoginResult("Login is successful.", Color.BLACK);
                addrField.setEnabled(false);
                userNameField.setEnabled(false);
                pwdField.setEnabled(false);
                logoutBtn.setEnabled(true);
                super.setVisible(false);
            }
        } catch (Throwable th) {
            showLoginResult("Login failed, " + th.getMessage(), Color.RED);
            loginBtn.setEnabled(true);
        }
    }

    private void logoutProcess() {
        try {
            portal.getClient().close();
            addrField.setEnabled(true);
            userNameField.setEnabled(true);
            pwdField.setEnabled(true);
            loginBtn.setEnabled(true);
            logoutBtn.setEnabled(false);
            super.setVisible(false);
            showLoginResult("Logout is successful.", Color.BLACK);
        } catch (Throwable th) {
            showLoginResult("Logout failed, " + th.getMessage(), Color.RED);
        }
    }
}