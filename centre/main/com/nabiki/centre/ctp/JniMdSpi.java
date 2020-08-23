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

public class JniMdSpi extends CThostFtdcMdSpi {
    private final TickProvider provider;

    JniMdSpi(TickProvider provider) {
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
    public void OnRspUserLogin(CThostFtdcRspUserLoginField pRspUserLogin, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            this.provider.whenRspUserLogin(JNI.toLocal(pRspUserLogin), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspUserLogout(CThostFtdcUserLogoutField pUserLogout, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            this.provider.whenRspUserLogout(JNI.toLocal(pUserLogout), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspError(CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            this.provider.whenRspError(JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            this.provider.whenRspSubMarketData(JNI.toLocal(pSpecificInstrument), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            this.provider.whenRspUnSubMarketData(JNI.toLocal(pSpecificInstrument), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
        try {
            this.provider.whenRtnDepthMarketData(JNI.toLocal(pDepthMarketData));
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }
}
