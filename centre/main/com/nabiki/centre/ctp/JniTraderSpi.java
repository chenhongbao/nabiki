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

package com.nabiki.centre.ctp;

import com.nabiki.ctp4j.*;

import java.util.Objects;

public class JniTraderSpi extends CThostFtdcTraderSpi {
    private final OrderProvider provider;

    JniTraderSpi(OrderProvider provider) {
        this.provider = provider;
    }

    @Override
    public void OnFrontConnected() {
        try {
            this.provider.whenFrontConnected();
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnFrontDisconnected(int nReason) {
        try {
            this.provider.whenFrontDisconnected(nReason);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspAuthenticate(CThostFtdcRspAuthenticateField pRspAuthenticateField, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pRspAuthenticateField, "rsp authenticate null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspAuthenticate(JNI.toLocal(pRspAuthenticateField), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pRspUserLogin, "rsp login null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspUserLogin(JNI.toLocal(pRspUserLogin), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pUserLogout, "rsp logout null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspUserLogout(JNI.toLocal(pUserLogout), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pInputOrder, "rsp input order null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspOrderInsert(JNI.toLocal(pInputOrder), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspOrderAction(CThostFtdcInputOrderActionField pInputOrderAction, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pInputOrderAction, "rsp input action null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspOrderAction(JNI.toLocal(pInputOrderAction), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspSettlementInfoConfirm(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pSettlementInfoConfirm, "rsp settlement null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspSettlementInfoConfirm(JNI.toLocal(pSettlementInfoConfirm), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspQryInstrumentMarginRate(CThostFtdcInstrumentMarginRateField pInstrumentMarginRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pInstrumentMarginRate, "rsp margin null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspQryInstrumentMarginRate(JNI.toLocal(pInstrumentMarginRate), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspQryInstrumentCommissionRate(CThostFtdcInstrumentCommissionRateField pInstrumentCommissionRate, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pInstrumentCommissionRate, "rsp commission null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspQryInstrumentCommissionRate(JNI.toLocal(pInstrumentCommissionRate), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspQryInstrument(CThostFtdcInstrumentField pInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pInstrument, "rsp instrument null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspQryInstrument(JNI.toLocal(pInstrument), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pRspInfo, "rsp error null");
            this.provider.whenRspError(JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRtnOrder(CThostFtdcOrderField pOrder) {
        try {
            Objects.requireNonNull(pOrder, "return order null");
            this.provider.whenRtnOrder(JNI.toLocal(pOrder));
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRtnTrade(CThostFtdcTradeField pTrade) {
        try {
            Objects.requireNonNull(pTrade, "return trade null");
            this.provider.whenRtnTrade(JNI.toLocal(pTrade));
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnErrRtnOrderInsert(CThostFtdcInputOrderField pInputOrder, CThostFtdcRspInfoField pRspInfo) {
        try {
            Objects.requireNonNull(pInputOrder, "err input order null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenErrRtnOrderInsert(JNI.toLocal(pInputOrder), JNI.toLocal(pRspInfo));
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnErrRtnOrderAction(CThostFtdcOrderActionField pOrderAction, CThostFtdcRspInfoField pRspInfo) {
        try {
            Objects.requireNonNull(pOrderAction, "err input action null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenErrRtnOrderAction(JNI.toLocal(pOrderAction), JNI.toLocal(pRspInfo));
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }
}
