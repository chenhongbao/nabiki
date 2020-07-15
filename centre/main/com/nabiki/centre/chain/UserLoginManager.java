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

package com.nabiki.centre.chain;

import com.nabiki.centre.active.ActiveUserManager;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.struct.CThostFtdcReqUserLoginField;
import com.nabiki.iop.LoginManager;
import com.nabiki.iop.Message;
import com.nabiki.iop.ServerSession;

import java.util.Objects;

public class UserLoginManager extends LoginManager {
    final static String FRONT_LOGINREQ_KEY = "chain.loginreq";
    final static String FRONT_USERAUTH_KEY = "chain.userauth";
    final static String FRONT_ACTIVEUSR_KEY = "chain.activeusr";

    private final UserAuthManager authMgr;
    private final ActiveUserManager userMgr;

    UserLoginManager(UserAuthManager auth, ActiveUserManager user) {
        this.authMgr = auth;
        this.userMgr = user;
    }

    private boolean isLogin(ServerSession session) {
        return session.getAttribute(FRONT_USERAUTH_KEY) != null;
    }

    @Override
    public int doLogin(ServerSession session, Message message) {
        if (isLogin(session))
            return TThostFtdcErrorCode.DUPLICATE_LOGIN;
        Objects.requireNonNull(message, "message null");
        Objects.requireNonNull(message.Body, "login request null");
        var req = (CThostFtdcReqUserLoginField)message.Body;
        Objects.requireNonNull(req.UserID, "user ID null");
        var auth = this.authMgr.getAuthProfile(req.UserID);
        if (auth == null)
            return TThostFtdcErrorCode.USER_NOT_FOUND;
        if (!auth.CanLogin)
            return TThostFtdcErrorCode.LOGIN_FORBIDDEN;
        if (auth.Password.compareTo(req.Password) != 0)
            return TThostFtdcErrorCode.INVALID_LOGIN;
        var user = this.userMgr.getActiveUser(req.UserID);
        if (user == null)
            return TThostFtdcErrorCode.NOT_INITED;
        else {
            session.setAttribute(FRONT_LOGINREQ_KEY, req);
            session.setAttribute(FRONT_USERAUTH_KEY, auth);
            session.setAttribute(FRONT_ACTIVEUSR_KEY, user);
            return TThostFtdcErrorCode.NONE;
        }
    }
}