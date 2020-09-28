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

package com.nabiki.centre.md;

import com.nabiki.centre.utils.Utils;
import com.nabiki.objects.CCandle;
import com.nabiki.objects.CDepthMarketData;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

public class CandleProgress {
  private final CCandle candle = new CCandle();
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");

  private int lastVolume = 0, lastVolumeUpdated = 0;
  private double lastClosePrice = 0.0D;
  private boolean popped = true;

  public CandleProgress(String instrID, int minute) {
    this.candle.InstrumentID = instrID;
    this.candle.Minute = minute;
  }

  public void update(CDepthMarketData md) {
    // If last volume is not set, set it to current volume.
    // If last volume is bigger than current volume, which means last volume is
    // probably total traded volume of previous trading day, set it.
    if (this.lastVolume == 0 || this.lastVolume > md.Volume) {
      this.lastVolume = md.Volume;
    }
    synchronized (this.candle) {
      if (this.popped) {
        this.candle.InstrumentID = md.InstrumentID;
        this.candle.ActionDay
            = Utils.getDay(LocalDate.now(), "yyyyMMdd");
        this.candle.TradingDay = md.TradingDay;
        this.candle.OpenPrice
            = this.candle.HighestPrice
            = this.candle.LowestPrice
            = md.LastPrice;
        this.popped = false;
      } else {

        this.candle.HighestPrice = Math.max(this.candle.HighestPrice,
            md.LastPrice);
        this.candle.LowestPrice = Math.min(this.candle.LowestPrice,
            md.LastPrice);
      }
      this.candle.Volume = md.Volume - this.lastVolume;
      this.candle.OpenInterest = md.OpenInterest;
      this.candle.ClosePrice = md.LastPrice;
      this.candle.UpdateTime = md.UpdateTime;
    }
    this.lastClosePrice = md.LastPrice;
    this.lastVolumeUpdated = md.Volume;
  }

  public CCandle peak(String tradingDay) {
    synchronized (this.candle) {
      if (this.popped) {
        // Not updated since last pop.
        this.candle.TradingDay = tradingDay;
        this.candle.ActionDay
            = Utils.getDay(LocalDate.now(), "yyyyMMdd");
        this.candle.UpdateTime
            = Utils.getTime(LocalTime.now(), "HH:mm:ss");
        this.candle.OpenPrice
            = this.candle.ClosePrice
            = this.candle.HighestPrice
            = this.candle.LowestPrice
            = this.lastClosePrice;
      }
      if (this.candle.EndTime == null)
        this.candle.EndTime = "";
      return Utils.deepCopy(this.candle);
    }
  }

  public CCandle pop(String tradingDay) {
    var r = peak(tradingDay);
    r.EndTime = getEndTime();
    this.lastVolume = this.lastVolumeUpdated;
    this.lastVolumeUpdated = 0;
    this.popped = true;
    return r;
  }

  private String getEndTime() {
    var nowSec = LocalTime.now().toSecondOfDay();
    var roundMinute = Math.round(
        1.0D * nowSec / TimeUnit.MINUTES.toSeconds(1));
    return LocalTime.ofSecondOfDay(
        roundMinute * TimeUnit.MINUTES.toSeconds(1))
        .format(formatter);
  }
}
