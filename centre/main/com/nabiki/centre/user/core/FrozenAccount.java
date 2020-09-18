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

import com.nabiki.centre.user.core.plain.AccountFrozenCash;
import com.nabiki.centre.user.core.plain.ProcessStage;
import com.nabiki.objects.CInstrument;
import com.nabiki.objects.CInstrumentCommissionRate;
import com.nabiki.objects.CTrade;

import java.util.concurrent.atomic.AtomicBoolean;

public class FrozenAccount {
  private final AccountFrozenCash singleFrzCash;
  private final long totalFrozenCount;
  private final UserAccount parent;

  private ProcessStage stage = ProcessStage.ONGOING;
  private final AtomicBoolean frozenSet = new AtomicBoolean(false);
  private long tradedCount = 0;

  FrozenAccount(UserAccount parent, AccountFrozenCash single, long frzCount) {
    this.parent = parent;
    this.singleFrzCash = single;
    this.totalFrozenCount = frzCount;
  }

  /**
   * Add this frozen account to its parent's frozen list. Then this account is
   * calculated as frozen.
   */
  void setFrozen() {
    if (!this.frozenSet.get()) {
      this.parent.addFrozenAccount(this);
      this.frozenSet.set(true);
    }
  }

  double getFrozenVolume() {
    if (this.stage == ProcessStage.CANCELED)
      return 0;
    else
      return this.totalFrozenCount - this.tradedCount;
  }

  AccountFrozenCash getSingleFrozenCash() {
    return this.singleFrzCash;
  }

  /**
   * Cancel an open order whose frozen account is also canceled.
   */
  void cancel() {
    this.stage = ProcessStage.CANCELED;
  }

  /**
   * An open order is traded(or partly) whose frozen account is also decreased.
   *
   * @param trade trade response
   * @param instr instrument
   * @param comm  commission
   */
  void applyOpenTrade(CTrade trade,
                      CInstrument instr,
                      CInstrumentCommissionRate comm) {
    if (trade.Volume < 0)
      throw new IllegalArgumentException("negative traded share count");
    if (this.stage == ProcessStage.CANCELED)
      throw new IllegalStateException("trade on canceled order");
    if (getFrozenVolume() < trade.Volume)
      throw new IllegalStateException("not enough frozen shares");
    this.tradedCount += trade.Volume;
    // Update parent.
    this.parent.applyTrade(trade, instr, comm);
  }
}
