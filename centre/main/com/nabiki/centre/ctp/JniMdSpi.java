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
import com.nabiki.objects.CDepthMarketData;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class JniMdSpi extends CThostFtdcMdSpi {
    private final TickProvider provider;
    private final BlockingQueue<CDepthMarketData> depths = new LinkedBlockingQueue<>();
    private Thread depthUpdateThread;

    JniMdSpi(TickProvider provider) {
        this.provider = provider;
        prepare();
    }

    private void prepare() {
        // The update may take long time, so do it in another thread.
        this.depthUpdateThread = new Thread(new DepthUpdateDaemon());
        this.depthUpdateThread.start();
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
    public void OnRspSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pSpecificInstrument, "rsp sub md null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspSubMarketData(JNI.toLocal(pSpecificInstrument), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRspUnSubMarketData(CThostFtdcSpecificInstrumentField pSpecificInstrument, CThostFtdcRspInfoField pRspInfo, int nRequestID, boolean bIsLast) {
        try {
            Objects.requireNonNull(pSpecificInstrument, "rsp unsub md null");
            if (pRspInfo == null)
                pRspInfo = new CThostFtdcRspInfoField();
            this.provider.whenRspUnSubMarketData(JNI.toLocal(pSpecificInstrument), JNI.toLocal(pRspInfo), nRequestID, bIsLast);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    @Override
    public void OnRtnDepthMarketData(CThostFtdcDepthMarketDataField pDepthMarketData) {
        try {
            Objects.requireNonNull(pDepthMarketData, "return md null");
            var depth = JNI.toLocal(pDepthMarketData);
            if (!this.depths.offer(depth))
                this.provider.config.getLogger()
                        .warning("can't offer depth: " + depth.InstrumentID);
        } catch (Throwable th) {
            th.printStackTrace();
            this.provider.config.getLogger().warning(th.getMessage());
        }
    }

    class DepthUpdateDaemon implements Runnable {
        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    provider.whenRtnDepthMarketData(depths.take());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
