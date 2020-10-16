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

import com.nabiki.commons.ctpobj.PosiDirectionType;

public interface PositionSupervisor {
  /**
   * Direct the position. This method triggers the timer task to query account and
   * position to decide if current position meets the specified setting. If it does,
   * the execution succeeds, or adjust position in accordance with new setting.
   *
   * @param instrumentID  instrument ID
   * @param exchangeID    exchange ID
   * @param posiDirection position direction, use {@link PosiDirectionType}
   * @param position      new position, positive number
   * @param priceHigh     upper bound price if it buys
   * @param priceLow      lower bound price if it sells
   * @throws Exception throws exception if previous execution is not completed.
   */
  void suggestPosition(String instrumentID, String exchangeID, char posiDirection,
                       int position, double priceHigh, double priceLow)
      throws Exception;

  /**
   * Get position from last execution. If a new execution is run, this method will
   * return new position of the new instrument specified by new execution.
   *
   * @return position
   */
  int getPosition();
}
