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

import com.nabiki.centre.user.core.plain.SettlementPreparation;
import com.nabiki.centre.user.core.plain.UserState;
import com.nabiki.ctp4j.jni.struct.CThostFtdcRspInfoField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcTradingAccountField;

import java.util.List;
import java.util.Map;

public class User {
    private final UserPosition userPosition;
    private final UserAccount userAccount;
    private final String userID;
    private final CThostFtdcRspInfoField panicReason = new CThostFtdcRspInfoField();

    private UserState state = UserState.RENEW;

    public User(CThostFtdcTradingAccountField rawAccount,
                Map<String, List<UserPositionDetail>> positions) {
        this.userID  = rawAccount.AccountID;
        this.userAccount = new UserAccount(rawAccount, this);
        this.userPosition = new UserPosition(positions, this);
    }

    public String getUserID() {
        return this.userID;
    }

    /**
     * Get current trading account<b>(not settled account)</b>. The method always
     * return account object, not {@code null}.
     *
     * @return current trading account.
     */
    public CThostFtdcTradingAccountField getFeaturedAccount() {
        var total = this.userAccount.copyRawAccount();
        // Calculate fields from account and position.
        var posFrzCash = this.userPosition.getPositionFrozenCash();
        var posSettle = this.userPosition.getTotalTradedCash();
        var accFrzCash = this.userAccount.getAccountFrozenCash();
        // Summarize.
        total.FrozenCash = accFrzCash.FrozenCash;
        total.FrozenCommission = accFrzCash.FrozenCommission
                + posFrzCash.FrozenCommission;
        total.FrozenMargin = posSettle.Margin;
        total.Balance = total.PreBalance + (total.Deposit - total.Withdraw)
                + posSettle.CloseProfitByDate - total.Commission;
        total.Available = total.Balance - total.FrozenMargin
                - total.FrozenCommission - total.FrozenCash;
        return total;
    }

    public void setPanic(int code, String msg) {
        this.state = UserState.PANIC;
        this.panicReason.ErrorID = code;
        this.panicReason.ErrorMsg = msg;
    }

    CThostFtdcRspInfoField getRspInfo() {
        return this.panicReason;
    }

    public UserState getState() {
        return this.state;
    }

    public UserPosition getUserPosition() {
        return this.userPosition;
    }

    public UserAccount getUserAccount() {
        return this.userAccount;
    }

    public void settle(SettlementPreparation prep) {
        // First position, then account.
        this.userPosition.settle(prep);
        this.userAccount.settle(this.userPosition.getTotalTradedCash(),
                prep.getTradingDay());
        this.state = UserState.SETTLED;
    }
}
