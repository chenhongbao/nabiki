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

import com.nabiki.client.sdk.TradeClient;
import com.nabiki.objects.CReqUserLogin;
import com.nabiki.objects.ErrorCodes;

import javax.swing.*;
import java.awt.*;
import java.net.InetSocketAddress;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class LoginOutWorker implements Runnable {
    private final Thread thd = new Thread(this);
    private final JDialog srcDialog;
    private final TradeClient client;
    private final JButton loginBtn, logoutBtn;
    private final JTextPane resultDisplay;
    private final JLabel accountUserDisplay, accountStateDisplay;
    private final JTextField addrField, userField, pwdField;
    private final boolean login;

    private java.util.Timer disconnectWatcher;

    LoginOutWorker(
            boolean login,
            JDialog srcDlg,
            TradeClient client,
            JButton loginBtn,
            JButton logoutBtn,
            JTextPane resultDisplay,
            JLabel accountUserDisplay,
            JLabel accountStateDisplay,
            JTextField addrField,
            JTextField userField,
            JTextField pwdField) {
        this.srcDialog = srcDlg;
        this.client = client;
        this.loginBtn = loginBtn;
        this.logoutBtn = logoutBtn;
        this.resultDisplay = resultDisplay;
        this.accountUserDisplay = accountUserDisplay;
        this.accountStateDisplay = accountStateDisplay;
        this.addrField = addrField;
        this.userField = userField;
        this.pwdField = pwdField;
        this.login = login;
    }

    @Override
    public void run() {
        if (login)
            loginProcess();
        else
            logoutProcess();
    }

    public void start() {
        this.thd.setDaemon(true);
        this.thd.start();
    }

    private String getAddress(String address) {
        return address.substring(0, address.indexOf(":"));
    }

    private int getPort(String address) {
        return Integer.parseInt(address.substring(address.indexOf(":") + 1));
    }

    private void showLoginResult(String text, Color c) {
        resultDisplay.setText(text);
    }

    private void sleep(int value, TimeUnit unit) {
        try {
            unit.sleep(value);
        } catch (InterruptedException ignored) {
        }
    }

    private void loginProcess() {
        try {
            loginBtn.setEnabled(false);
            var address = addrField.getText().trim();
            if (address.length() == 0)
                throw new RuntimeException("\u9700\u8981\u670D\u52A1\u5668\u5730\u5740");
            client.open(new InetSocketAddress(
                    getAddress(address), getPort(address)));
        } catch (Throwable th) {
            showLoginResult(th.getMessage(), Color.RED);
            loginBtn.setEnabled(true);
            return;
        }
        var req = new CReqUserLogin();
        req.UserID = userField.getText().trim();
        req.Password = pwdField.getText().trim();
        try {
            var rsp = client.login(
                    req, UUID.randomUUID().toString());
            sleep(Constants.GLOBAL_WAIT_SECONDS, TimeUnit.SECONDS);
            if (!rsp.hasResponse())
                throw new RuntimeException("\u65E0\u767B\u5F55\u54CD\u5E94");
            var r = rsp.getRspInfo(rsp.poll());
            if (r != null && r.ErrorID != ErrorCodes.NONE)
                throw new RuntimeException(r.ErrorMsg);
            else {
                changeComponents(false);
                // Hide dialog.
                srcDialog.setVisible(false);
            }
            startWatcher();
        } catch (Throwable th) {
            showLoginResult("\u767B\u5F55\u5931\u8D25" + th.getMessage(), Color.RED);
            loginBtn.setEnabled(true);
        }
    }

    private void logoutProcess() {
        try {
            client.close();
            changeComponents(true);
            srcDialog.setVisible(false);
        } catch (Throwable th) {
            showLoginResult("\u9000\u51FA\u5931\u8D25, " + th.getMessage(), Color.RED);
        }
    }

    private void changeComponents(boolean forLogin) {
        if (!forLogin) {
            // Disable fields.
            addrField.setEnabled(false);
            userField.setEnabled(false);
            pwdField.setEnabled(false);
            // Enable logout.
            loginBtn.setEnabled(false);
            logoutBtn.setEnabled(true);
            // Update login state on account panel.
            accountUserDisplay.setText(userField.getText().trim());
            accountStateDisplay.setText("\u767B\u5F55");
            showLoginResult("\u767B\u9646\u6210\u529F", Color.BLACK);
        } else {
            addrField.setEnabled(true);
            userField.setEnabled(true);
            pwdField.setEnabled(true);
            loginBtn.setEnabled(true);
            logoutBtn.setEnabled(false);
            // Update login state on account panel.
            accountUserDisplay.setText("\u65E0");
            accountStateDisplay.setText("\u767B\u51FA");
            showLoginResult("\u9000\u51FA\u6210\u529F", Color.BLACK);
        }
    }

    private void startWatcher() {
        if (disconnectWatcher != null)
            return;
        disconnectWatcher = new java.util.Timer();
        disconnectWatcher.schedule(new TimerTask() {
            @Override
            public void run() {
                if (client.isClosed())
                    changeComponents(true);
            }
        }, 1000, 1000);
    }
}
