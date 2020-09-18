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

package com.nabiki.iop;

import com.nabiki.objects.*;

public abstract class ServerMessageAdaptor {
  public void doSubDepthMarketData(
      ServerSession session,
      CSubMarketData request,
      String requestID,
      int current,
      int total) {
  }

  public void doUnsubDepthMarketData(
      ServerSession session,
      CUnsubMarketData request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqAuthenticate(
      ServerSession session,
      CReqAuthenticate request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqLogin(
      ServerSession session,
      CReqUserLogin request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqLogout(
      ServerSession session,
      CUserLogout request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqSettlementConfirm(
      ServerSession session,
      CSettlementInfoConfirm request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqOrderInsert(
      ServerSession session,
      CInputOrder request,
      String requestID,
      int current,
      int total) {
  }

  public void doReqOrderAction(
      ServerSession session,
      CInputOrderAction request,
      String requestID,
      int current,
      int total) {
  }

  public void doQryDepthMarketData(
      ServerSession session,
      CQryDepthMarketData query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryAccount(
      ServerSession session,
      CQryTradingAccount query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryOrder(
      ServerSession session,
      CQryOrder query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryPosition(
      ServerSession session,
      CQryInvestorPosition query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryPositionDetail(
      ServerSession session,
      CQryInvestorPositionDetail query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryInstrument(
      ServerSession session,
      CQryInstrument query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryCommission(
      ServerSession session,
      CQryInstrumentCommissionRate query,
      String requestID,
      int current,
      int total) {
  }

  public void doQryMargin(
      ServerSession session,
      CQryInstrumentMarginRate query,
      String requestID,
      int current,
      int total) {
  }
}
