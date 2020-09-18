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

public abstract class ClientMessageAdaptor {
  public void doRspSubscribeMarketData(
      CSpecificInstrument rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspUnsubscribeMarketData(
      CSpecificInstrument rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspDepthMarketData(
      CDepthMarketData rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspCandle(
      CCandle rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspAuthenticate(
      CRspAuthenticate rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspReqLogin(
      CRspUserLogin rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspReqLogout(
      CUserLogout rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspReqSettlementConfirm(
      CSettlementInfoConfirm rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspReqOrderInsert(
      COrder rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspReqOrderAction(
      COrderAction rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryDepthMarketData(
      CDepthMarketData rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryAccount(
      CTradingAccount rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryOrder(
      COrder rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryPosition(
      CInvestorPosition rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryPositionDetail(
      CInvestorPositionDetail rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryInstrument(
      CInstrument rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryCommission(
      CInstrumentCommissionRate rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspQryMargin(
      CInstrumentMarginRate rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRtnOrder(
      COrder rtn,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRtnTrade(
      CTrade rtn,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRtnOrderAction(
      COrderAction rtn,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRtnOrderInsert(
      CInputOrder rtn,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspOrderAction(
      CInputOrderAction rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspOrderInsert(
      CInputOrder rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspError(
      CRspInfo rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspConnect(
      CConnect rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }

  public void doRspDisconnect(
      CDisconnect rsp,
      CRspInfo info,
      String requestID,
      String responseID,
      int current,
      int total) {
  }
}
