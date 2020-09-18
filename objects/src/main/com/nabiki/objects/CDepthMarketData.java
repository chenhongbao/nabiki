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

package com.nabiki.objects;

import java.io.Serializable;

public class CDepthMarketData implements Serializable {
  public String TradingDay;
  public String InstrumentID;
  public String ExchangeID;
  public String ExchangeInstID;
  public double LastPrice;
  public double PreSettlementPrice;
  public double PreClosePrice;
  public double PreOpenInterest;
  public double OpenPrice;
  public double HighestPrice;
  public double LowestPrice;
  public int Volume;
  public double Turnover;
  public double OpenInterest;
  public double ClosePrice;
  public double SettlementPrice;
  public double UpperLimitPrice;
  public double LowerLimitPrice;
  public double PreDelta;
  public double CurrDelta;
  public String UpdateTime;
  public int UpdateMillisec;
  public double BidPrice1;
  public int BidVolume1;
  public double AskPrice1;
  public int AskVolume1;
  public double BidPrice2;
  public int BidVolume2;
  public double AskPrice2;
  public int AskVolume2;
  public double BidPrice3;
  public int BidVolume3;
  public double AskPrice3;
  public int AskVolume3;
  public double BidPrice4;
  public int BidVolume4;
  public double AskPrice4;
  public int AskVolume4;
  public double BidPrice5;
  public int BidVolume5;
  public double AskPrice5;
  public int AskVolume5;
  public double AveragePrice;
  public String ActionDay;

  public CDepthMarketData() {
  }
}
