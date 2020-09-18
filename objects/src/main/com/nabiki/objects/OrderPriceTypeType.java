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

public class OrderPriceTypeType {
  public static final char ANY_PRICE = '1';
  public static final char LIMIT_PRICE = '2';
  public static final char BEST_PRICE = '3';
  public static final char LAST_PRICE = '4';
  public static final char LAST_PRICE_PLUS_ONE_TICKS = '5';
  public static final char LAST_PRICE_PLUS_TWO_TICKS = '6';
  public static final char LAST_PRICE_PLUS_THREE_TICKS = '7';
  public static final char ASKPRICE_1 = '8';
  public static final char ASKPRICE_1_PLUS_ONE_TICKS = '9';
  public static final char ASKPRICE_1_PLUS_TWO_TICKS = 'A';
  public static final char ASKPRICE_2_PLUS_THREE_TICKS = 'B';
  public static final char BIDPRICE_1 = 'C';
  public static final char BIDPRICE_1_PLUS_ONE_TICKS = 'D';
  public static final char BIDPRICE_1_PLUS_TWO_TICKS = 'E';
  public static final char BIDPRICE_1_PLUS_THREE_TICKS = 'F';
  public static final char FIVE_LEVEL_PRICE = 'G';
}
