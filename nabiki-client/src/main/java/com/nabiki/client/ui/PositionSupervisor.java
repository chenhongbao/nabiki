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
  void executePosition(String instrumentID, String exchangeID, char posiDirection,
                       int position, double priceHigh, double priceLow)
      throws Exception;

  /**
   * Get <b>NET</b> position from last execution. If a new execution is run, this
   * method will return position of the last instrument specified by new execution.
   * <br/>
   * For long net position, return non-negative value, and for short net position,
   * return non-positive value.
   *
   * @return position
   */
  int getPosition();

  /**
   * Get position of the specified posi-direction. The method always returns non
   * negative value.
   *
   * @param posiDirection position direction
   * @return position, non-negative value
   * @throws IllegalStateException if last execution is not completed or no exec yet.
   *                               Use {@link PositionSupervisor#isCompleted()} to check the state.
   */
  int getPosition(char posiDirection);

  /**
   * Check if last execution completes.
   *
   * @return {@code true} if last execution completes, {@code false} otherwise.
   */
  boolean isCompleted();
}
