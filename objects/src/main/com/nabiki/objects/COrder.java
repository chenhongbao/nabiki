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

public class COrder implements Serializable {
  public String BrokerID;
  public String InvestorID;
  public String InstrumentID;
  public String OrderRef;
  public String UserID;
  public byte OrderPriceType;
  public byte Direction;
  public byte CombOffsetFlag;
  public byte CombHedgeFlag;
  public double LimitPrice;
  public int VolumeTotalOriginal;
  public byte TimeCondition;
  public String GTDDate;
  public byte VolumeCondition;
  public int MinVolume;
  public byte ContingentCondition;
  public double StopPrice;
  public byte ForceCloseReason;
  public int IsAutoSuspend;
  public String BusinessUnit;
  public int RequestID;
  public String OrderLocalID;
  public String ExchangeID;
  public String ParticipantID;
  public String ClientID;
  public String ExchangeInstID;
  public String TraderID;
  public int InstallID;
  public byte OrderSubmitStatus;
  public int NotifySequence;
  public String TradingDay;
  public int SettlementID;
  public String OrderSysID;
  public byte OrderSource;
  public byte OrderStatus;
  public byte OrderType;
  public int VolumeTraded;
  public int VolumeTotal;
  public String InsertDate;
  public String InsertTime;
  public String ActiveTime;
  public String SuspendTime;
  public String UpdateTime;
  public String CancelTime;
  public String ActiveTraderID;
  public String ClearingPartID;
  public int SequenceNo;
  public int FrontID;
  public int SessionID;
  public String UserProductInfo;
  public String StatusMsg;
  public int UserForceClose;
  public String ActiveUserID;
  public int BrokerOrderSeq;
  public String RelativeOrderSysID;
  public int ZCETotalTradedVolume;
  public int IsSwapOrder;
  public String BranchID;
  public String InvestUnitID;
  public String AccountID;
  public String CurrencyID;
  public String IPAddress;
  public String MacAddress;

  public COrder() {
  }
}
