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

import com.nabiki.commons.ctpobj.CInvestorPosition;
import com.nabiki.commons.ctpobj.CTradingAccount;

import java.io.Serializable;
import java.util.Collection;

public class PositionExecution implements Serializable {
  private String instrumentID;
  private String exchangeID;
  private Character posiDirection;
  private Integer posi, posiDiff;
  private Double priceHigh, priceLow;
  private final PositionListener listener;
  private CTradingAccount account = null; /* init as nil so it knows qry isn't sent yet */
  private Collection<CInvestorPosition> positions = null;
  private boolean queryingAccount = false;
  private boolean queryingPosition = false;
  private PositionExecState state;

  public PositionExecution(PositionListener listener) {
    this.listener = listener;
  }

  public Collection<CInvestorPosition> getPositions() {
    return positions;
  }

  public void setPositions(Collection<CInvestorPosition> positions) {
    this.positions = positions;
  }

  public boolean isQueryingAccount() {
    return queryingAccount;
  }

  public void setQueryingAccount(boolean b) {
    this.queryingAccount = b;
  }

  public String getInstrumentID() {
    return instrumentID;
  }

  public void setInstrumentID(String instrumentID) {
    this.instrumentID = instrumentID;
  }

  public String getExchangeID() {
    return exchangeID;
  }

  public void setExchangeID(String exchangeID) {
    this.exchangeID = exchangeID;
  }

  public Character getPosiDirection() {
    return posiDirection;
  }

  public void setPosiDirection(Character posiDirection) {
    this.posiDirection = posiDirection;
  }

  public Integer getPosition() {
    return posi;
  }

  public void setPosition(Integer pos) {
    this.posi = pos;
  }

  public Integer getPosiDiff() {
    return posiDiff;
  }

  public void setPosiDiff(Integer posiDiff) {
    this.posiDiff = posiDiff;
  }

  public Double getPriceHigh() {
    return priceHigh;
  }

  public void setPriceHigh(Double priceHigh) {
    this.priceHigh = priceHigh;
  }

  public Double getPriceLow() {
    return priceLow;
  }

  public void setPriceLow(Double priceLow) {
    this.priceLow = priceLow;
  }

  public PositionExecState getState() {
    return state;
  }

  public void setState(PositionExecState state) {
    if (state != this.state) {
      this.state = state;
      try {
        listener.onStateChange(this);
      } catch (Throwable th) {
        th.printStackTrace();
      }
    }
  }

  public CTradingAccount getAccount() {
    return account;
  }

  public void setAccount(CTradingAccount account) {
    this.account = account;
  }

  public boolean isQueryingPosition() {
    return queryingPosition;
  }

  public void setQueryingPosition(boolean b) {
    this.queryingPosition = b;
  }
}
