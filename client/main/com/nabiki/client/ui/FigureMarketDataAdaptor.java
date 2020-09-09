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

package com.nabiki.client.ui;

import com.nabiki.objects.CCandle;
import com.nabiki.objects.CDepthMarketData;

import java.util.concurrent.TimeUnit;

public class FigureMarketDataAdaptor extends HeadlessMarketDataAdaptor {
    private final AbstractFigure figure;

    FigureMarketDataAdaptor(MarketDataHandler handler, AbstractFigure figure) {
        super(handler);
        this.figure = figure;
    }

    @Override
    public void onDepthMarketData(CDepthMarketData depth) {
        super.onDepthMarketData(depth);
    }

    @Override
    public void onCandle(CCandle candle) {
        for (var fid : figure.getFigureID()) {
            var id = figure.getBoundInstrumentID(fid);
            var minute = figure.getBoundMinute(fid);
            if (id.compareToIgnoreCase(candle.InstrumentID) == 0
                    && minute == candle.Minute) {
                var unit = (minute == TimeUnit.DAYS.toMinutes(1))
                        ? "\u65E5" : (minute + "\u5206\u949F");
                figure.stick(fid,
                        candle.OpenPrice,
                        candle.HighestPrice,
                        candle.LowestPrice,
                        candle.ClosePrice,
                        candle.EndTime);
                super.onCandle(candle);
                figure.setTitle(fid, id + " -- " + unit);
                figure.update(fid);
            }
        }
    }
}
