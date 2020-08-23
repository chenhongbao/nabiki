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
import com.nabiki.objects.*;

public class JNI {
    public static CRspInfo toLocal(CThostFtdcRspInfoField jni) {
        return new CRspInfo();
    }

    public static CRspAuthenticate toLocal(CThostFtdcRspAuthenticateField jni) {
        return new CRspAuthenticate();
    }

    public static CRspUserLogin toLocal(CThostFtdcRspUserLoginField jni) {
        return new CRspUserLogin();
    }

    public static CUserLogout toLocal(CThostFtdcUserLogoutField pUserLogout) {
        return new CUserLogout();
    }

    public static CInputOrder toLocal(CThostFtdcInputOrderField pInputOrder) {
        return new CInputOrder();
    }

    public static CInputOrderAction toLocal(CThostFtdcInputOrderActionField pInputOrderAction) {
        return new CInputOrderAction();
    }

    public static CSettlementInfoConfirm toLocal(CThostFtdcSettlementInfoConfirmField pSettlementInfoConfirm) {
        return new CSettlementInfoConfirm();
    }

    public static CInstrumentMarginRate toLocal(CThostFtdcInstrumentMarginRateField pInstrumentMarginRate) {
        return new CInstrumentMarginRate();
    }

    public static CInstrumentCommissionRate toLocal(CThostFtdcInstrumentCommissionRateField pInstrumentCommissionRate) {
        return new CInstrumentCommissionRate();
    }

    public static CInstrument toLocal(CThostFtdcInstrumentField pInstrument) {
        return new CInstrument();
    }

    public static COrder toLocal(CThostFtdcOrderField pOrder) {
        return new COrder();
    }

    public static CTrade toLocal(CThostFtdcTradeField pTrade) {
        return new CTrade();
    }

    public static COrderAction toLocal(CThostFtdcOrderActionField pOrderAction) {
        return new COrderAction();
    }

    public static CSpecificInstrument toLocal(CThostFtdcSpecificInstrumentField pSpecificInstrument) {
        return new CSpecificInstrument();
    }

    public static CDepthMarketData toLocal(CThostFtdcDepthMarketDataField pDepthMarketData) {
        return new CDepthMarketData();
    }

    public static CThostFtdcReqUserLoginField toJni(CReqUserLogin req) {
        return new CThostFtdcReqUserLoginField();
    }

    public static CThostFtdcUserLogoutField toJni(CUserLogout local) {
        return new CThostFtdcUserLogoutField();
    }

    public static CThostFtdcSettlementInfoConfirmField toJni(CSettlementInfoConfirm req) {
        return new CThostFtdcSettlementInfoConfirmField();
    }

    public static CThostFtdcReqAuthenticateField toJni(CReqAuthenticate req) {
        return new CThostFtdcReqAuthenticateField();
    }

    public static CThostFtdcQryInstrumentField toJni(CQryInstrument req) {
        return new CThostFtdcQryInstrumentField();
    }

    public static CThostFtdcInputOrderField toJni(CInputOrder input) {
        return new CThostFtdcInputOrderField();
    }

    public static CThostFtdcInputOrderActionField toJni(CInputOrderAction action) {
        return new CThostFtdcInputOrderActionField();
    }

    public static CThostFtdcQryInstrumentMarginRateField toJni(CQryInstrumentMarginRate req) {
        return new CThostFtdcQryInstrumentMarginRateField();
    }

    public static CThostFtdcQryInstrumentCommissionRateField toJni(CQryInstrumentCommissionRate req0) {
        return new CThostFtdcQryInstrumentCommissionRateField();
    }
}
