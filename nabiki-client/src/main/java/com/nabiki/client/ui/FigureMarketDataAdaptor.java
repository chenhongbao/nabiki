/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

import com.nabiki.commons.ctpobj.CCandle;
import com.nabiki.commons.ctpobj.CDepthMarketData;

public class FigureMarketDataAdaptor extends HeadlessMarketDataAdaptor {
  private final AbstractFigure figure;

  FigureMarketDataAdaptor(MarketDataHandler handler, AbstractFigure figure) {
    super(handler, figure);
    this.figure = figure;
  }

  @Override
  public void onDepthMarketData(CDepthMarketData depth) {
    try {
      super.onDepthMarketData(depth);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }

  @Override
  public void onCandle(CCandle candle) {
    try {
      for (var fid : figure.getStickFigureIDs()) {
        var id = figure.getBoundInstrumentID(fid);
        var m = figure.getBoundMinute(fid);
        if (id.compareTo(candle.InstrumentID) == 0 && m == candle.Minute) {
          figure.getStickController(fid).appendStick(
              candle.OpenPrice,
              candle.HighestPrice,
              candle.LowestPrice,
              candle.ClosePrice,
              candle.EndTime);
          figure.setUpdated(true);
        }
      }
      super.onCandle(candle);
    } catch (Throwable th) {
      th.printStackTrace();
    }
  }
}
