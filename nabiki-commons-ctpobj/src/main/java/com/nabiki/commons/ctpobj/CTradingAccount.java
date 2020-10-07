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

public class CTradingAccount implements Serializable {
  public String BrokerID;
  public String AccountID;
  public double PreMortgage;
  public double PreCredit;
  public double PreDeposit;
  public double PreBalance;
  public double PreMargin;
  public double InterestBase;
  public double Interest;
  public double Deposit;
  public double Withdraw;
  public double FrozenMargin;
  public double FrozenCash;
  public double FrozenCommission;
  public double CurrMargin;
  public double CashIn;
  public double Commission;
  public double CloseProfit;
  public double PositionProfit;
  public double Balance;
  public double Available;
  public double WithdrawQuota;
  public double Reserve;
  public String TradingDay;
  public int SettlementID;
  public double Credit;
  public double Mortgage;
  public double ExchangeMargin;
  public double DeliveryMargin;
  public double ExchangeDeliveryMargin;
  public double ReserveBalance;
  public String CurrencyID;
  public double PreFundMortgageIn;
  public double PreFundMortgageOut;
  public double FundMortgageIn;
  public double FundMortgageOut;
  public double FundMortgageAvailable;
  public double MortgageableFund;
  public double SpecProductMargin;
  public double SpecProductFrozenMargin;
  public double SpecProductCommission;
  public double SpecProductFrozenCommission;
  public double SpecProductPositionProfit;
  public double SpecProductCloseProfit;
  public double SpecProductPositionProfitByAlg;
  public double SpecProductExchangeMargin;
  public byte BizType;
  public double FrozenSwap;
  public double RemainSwap;

  public CTradingAccount() {
  }
}
