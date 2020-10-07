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

package com.nabiki.commons.ctpobj;

import java.io.Serializable;

public class CTrade implements Serializable {
  public String BrokerID;
  public String InvestorID;
  public String InstrumentID;
  public String OrderRef;
  public String UserID;
  public String ExchangeID;
  public String TradeID;
  public byte Direction;
  public String OrderSysID;
  public String ParticipantID;
  public String ClientID;
  public byte TradingRole;
  public String ExchangeInstID;
  public byte OffsetFlag;
  public byte HedgeFlag;
  public double Price;
  public int Volume;
  public String TradeDate;
  public String TradeTime;
  public byte TradeType;
  public byte PriceSource;
  public String TraderID;
  public String OrderLocalID;
  public String ClearingPartID;
  public String BusinessUnit;
  public int SequenceNo;
  public String TradingDay;
  public int SettlementID;
  public int BrokerOrderSeq;
  public byte TradeSource;
  public String InvestUnitID;

  public CTrade() {
  }
}
