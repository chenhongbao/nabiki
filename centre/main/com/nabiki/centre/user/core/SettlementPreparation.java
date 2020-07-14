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

package com.nabiki.centre.user.core;

import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentCommissionRateField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcInstrumentMarginRateField;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SettlementPreparation {
    private final Map<String, CThostFtdcDepthMarketDataField> depths
            = new ConcurrentHashMap<>();
    private final Map<String, CThostFtdcInstrumentField> instruments
            = new ConcurrentHashMap<>();
    private final Map<String, CThostFtdcInstrumentMarginRateField> margins
            = new ConcurrentHashMap<>();
    private final Map<String, CThostFtdcInstrumentCommissionRateField> commissions
            = new ConcurrentHashMap<>();
    private String tradingDay;

    public SettlementPreparation() {
    }

    public String getTradingDay() {
        return this.tradingDay;
    }

    public CThostFtdcDepthMarketDataField getDepth(String instrID) {
        return this.depths.get(instrID);
    }

    public CThostFtdcInstrumentField getInstrument(String instrID) {
        return this.instruments.get(instrID);
    }

    public CThostFtdcInstrumentMarginRateField getMargin(String instrID) {
        return this.margins.get(instrID);
    }

    public CThostFtdcInstrumentCommissionRateField getCommission(String instrID) {
        return this.commissions.get(instrID);
    }

    public void prepare(String tradingDay) {
        this.tradingDay = tradingDay;
    }

    public void prepare(CThostFtdcDepthMarketDataField depth) {
        Objects.requireNonNull(depth, "depth null");
    }

    public void prepare(CThostFtdcInstrumentField instrument) {
        Objects.requireNonNull(instrument, "instrument null");
    }

    public void prepare(CThostFtdcInstrumentMarginRateField margin) {
        Objects.requireNonNull(margin, "margin null");
    }

    public void prepare(CThostFtdcInstrumentCommissionRateField commission) {
        Objects.requireNonNull(commission, "commission null");
    }
}
