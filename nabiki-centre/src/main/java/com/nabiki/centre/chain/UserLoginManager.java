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

package com.nabiki.centre.chain;

import com.nabiki.centre.config.Global;
import com.nabiki.centre.user.auth.UserAuthManager;
import com.nabiki.centre.user.core.ActiveUserManager;
import com.nabiki.commons.ctpobj.CReqUserLogin;
import com.nabiki.commons.ctpobj.CRspInfo;
import com.nabiki.commons.ctpobj.CRspUserLogin;
import com.nabiki.commons.ctpobj.ErrorCodes;
import com.nabiki.commons.iop.LoginManager;
import com.nabiki.commons.iop.Message;
import com.nabiki.commons.iop.MessageType;
import com.nabiki.commons.iop.ServerSession;
import com.nabiki.commons.utils.Utils;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class UserLoginManager extends LoginManager {
  final static String FRONT_USERID_KEY = "front.userid";
  private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final UserAuthManager authMgr;
  private final ActiveUserManager userMgr;
  private final Global global;

  public UserLoginManager(UserAuthManager auth, ActiveUserManager user, Global global) {
    this.authMgr = auth;
    this.userMgr = user;
    this.global = global;
  }

  private boolean isLogin(ServerSession session) {
    return session.getAttribute(FRONT_USERID_KEY) != null;
  }

  @Override
  public int doLogin(ServerSession session, Message message) {
    var req = (CReqUserLogin) message.Body;
    var code = checkLoginOK(session, req);
    sendLoginRsp(code, session, message);
    checkLoginCode(code, session, req);
    return code;
  }

  private void checkLoginCode(int code, ServerSession session, CReqUserLogin req) {
    if (code == ErrorCodes.NONE) {
      global.getLogger().info(String.format(
          "User %s login with %s from %s succeeds.",
          req.UserID,
          req.UserProductInfo,
          session.getRemoteAddress()));
    } else {
      global.getLogger().info(String.format(
          "User %s login with %s from %s fails because of %s[%d].",
          req.UserID,
          req.UserProductInfo,
          session.getRemoteAddress(),
          Utils.getErrorMsg(code),
          code));
    }
  }

  private int checkLoginOK(ServerSession session, CReqUserLogin req) {
    if (isLogin(session))
      return ErrorCodes.DUPLICATE_LOGIN;
    Objects.requireNonNull(req, "login request null");
    Objects.requireNonNull(req.UserID, "user ID null");
    var auth = this.authMgr.getAuthProfile(req.UserID);
    if (auth == null)
      return ErrorCodes.USER_NOT_FOUND;
    if (!auth.CanLogin)
      return ErrorCodes.LOGIN_FORBIDDEN;
    if (auth.Password.compareTo(req.Password) != 0)
      return ErrorCodes.INVALID_LOGIN;
    var user = this.userMgr.getActiveUser(req.UserID);
    if (user == null)
      return ErrorCodes.USER_NOT_FOUND;
    else {
      session.setAttribute(FRONT_USERID_KEY, req.UserID);
      return ErrorCodes.NONE;
    }
  }

  private void sendLoginRsp(int code, ServerSession session, Message message) {
    var r = new CRspUserLogin();
    r.LoginTime = LocalTime.now().format(timeFormatter);
    r.TradingDay = global.getTradingDay();
    // Construct message.
    Message rsp = new Message();
    rsp.Type = MessageType.RSP_REQ_LOGIN;
    rsp.CurrentCount = 1;
    rsp.TotalCount = 1;
    rsp.RequestID = message.RequestID;
    rsp.ResponseID = Utils.getUID();
    rsp.Body = r;
    rsp.RspInfo = new CRspInfo();
    rsp.RspInfo.ErrorID = code;
    rsp.RspInfo.ErrorMsg = Utils.getErrorMsg(code);
    session.sendLogin(rsp);
  }
}
