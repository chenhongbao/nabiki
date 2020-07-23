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

import com.nabiki.ctp4j.jni.struct.*;

public abstract class ClientMessageAdaptor {
    public void doRspSubscribeMarketData(
            CThostFtdcSpecificInstrumentField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspUnsubscribeMarketData(
            CThostFtdcSpecificInstrumentField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspDepthMarketData(
            CThostFtdcDepthMarketDataField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspCandle(
            CThostFtdcCandleField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspAuthenticate(
            CThostFtdcRspAuthenticateField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspReqLogin(
            CThostFtdcRspUserLoginField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspReqLogout(
            CThostFtdcUserLogoutField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspReqSettlementConfirm(
            CThostFtdcSettlementInfoConfirmField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspReqOrderInsert(
            CThostFtdcOrderField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspReqOrderAction(
            CThostFtdcOrderActionField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryAccount(
            CThostFtdcTradingAccountField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryOrder(
            CThostFtdcOrderField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryPosition(
            CThostFtdcInvestorPositionField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryPositionDetail(
            CThostFtdcInvestorPositionDetailField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryInstrument(
            CThostFtdcInstrumentField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryCommission(
            CThostFtdcInstrumentCommissionRateField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspQryMargin(
            CThostFtdcInstrumentMarginRateField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRtnOrder(
            CThostFtdcOrderField rtn,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRtnTrade(
            CThostFtdcTradeField rtn,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRtnOrderAction(
            CThostFtdcOrderActionField rtn,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRtnOrderInsert(
            CThostFtdcInputOrderField rtn,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspOrderAction(
            CThostFtdcInputOrderActionField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspOrderInsert(
            CThostFtdcInputOrderField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspError(
            CThostFtdcRspInfoField rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspConnect(
            CThostFtdcConnect rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }

    public void doRspDisconnect(
            CThostFtdcDisconnect rsp,
            CThostFtdcRspInfoField info,
            String requestID,
            String responseID,
            int current,
            int total) {
    }
}
