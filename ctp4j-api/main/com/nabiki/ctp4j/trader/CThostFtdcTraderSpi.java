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

package com.nabiki.ctp4j.trader;

import com.nabiki.ctp4j.jni.struct.*;

public abstract class CThostFtdcTraderSpi {
    public void OnFrontConnected() {}

    public void OnFrontDisconnected(int reason) {}

    public void OnErrRtnOrderAction(CThostFtdcOrderActionField orderAction,
                                    CThostFtdcRspInfoField rspInfo) {}

    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField inputOrder,
                                    CThostFtdcRspInfoField rspInfo) {}

    public void OnRspAuthenticate(
            CThostFtdcRspAuthenticateField rspAuthenticateField,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {
    }

    /**
     * Error response callback. The method is called whenever there is error from
     * native(JVM) internals, CTP response or
     * java codes.
     *
     * <p> The error code in {@link CThostFtdcRspInfoField} has 3 categories:
     * <ul>
     * <li>if code < 0, error is caused by native(JVM) or java codes
     * <li>if code = 0, no error
     * <li>if code > 0, error is caused by CTP
     * </ul>
     *
     * @param rspInfo   error response information
     * @param requestId identifier for the request that causes this error
     * @param isLast    {@code true} if the current response is the last piece from
     *                              this error, {@code false} otherwise.
     */
    public void OnRspError(CThostFtdcRspInfoField rspInfo, int requestId,
                           boolean isLast) {
    }

    public void OnRspOrderAction(CThostFtdcInputOrderActionField inputOrderAction,
                                 CThostFtdcRspInfoField rspInfo, int requestId,
                                 boolean isLast) {
    }

    public void OnRspOrderInsert(CThostFtdcInputOrderField inputOrder,
                                 CThostFtdcRspInfoField rspInfo, int requestId,
                                 boolean isLast) {}

    public void OnRspQryInstrument(CThostFtdcInstrumentField instrument,
                                   CThostFtdcRspInfoField rspInfo, int requestId,
                                   boolean isLast) {}

    public void OnRspQryInstrumentCommissionRate(
            CThostFtdcInstrumentCommissionRateField instrumentCommissionRate,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {}

    public void OnRspQryInstrumentMarginRate(
            CThostFtdcInstrumentMarginRateField instrumentMarginRate,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {}

    public void OnRspQryInvestorPositionDetail(
            CThostFtdcInvestorPositionDetailField investorPositionDetail,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {}

    public void OnRspQryTradingAccount(CThostFtdcTradingAccountField tradingAccount,
                                       CThostFtdcRspInfoField rspInfo, int requestId,
                                       boolean isLast) {}

    public void OnRspSettlementInfoConfirm(
            CThostFtdcSettlementInfoConfirmField settlementInfoConfirm,
            CThostFtdcRspInfoField rspInfo, int requestId, boolean isLast) {}

    public void OnRspUserLogin(CThostFtdcRspUserLoginField rspUserLogin,
                               CThostFtdcRspInfoField rspInfo, int requestId,
                               boolean isLast) {}

    public void OnRspUserLogout(CThostFtdcUserLogoutField userLogout,
                                CThostFtdcRspInfoField rspInfo, int requestId,
                                boolean isLast) {}

    public void OnRtnOrder(CThostFtdcOrderField order) {}

    public void OnRtnTrade(CThostFtdcTradeField trade) {}
}
