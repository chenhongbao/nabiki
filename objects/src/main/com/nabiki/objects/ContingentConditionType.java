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

public class ContingentConditionType {
  public static final char IMMEDIATELY = '1';
  public static final char TOUCH = '2';
  public static final char TOUCH_PROFIT = '3';
  public static final char PACRKED_ORDER = '4';
  public static final char LASTPRICE_GREATER_THAN_STOPPRICE = '5';
  public static final char LASTPRICE_GREATER_EQUAL_STOPPRICE = '6';
  public static final char LASTPRICE_LESSER_THAN_STOPPRICE = '7';
  public static final char LASTPRICE_LESSER_EQUAL_STOPPRICE = '8';
  public static final char ASKPRICE_GREATER_THAN_STOPPRICE = '9';
  public static final char ASKPRICE_GREATER_EQUAL_STOPPRICE = 'A';
  public static final char ASKPRICE_LESSER_THAN_STOPPRICE = 'B';
  public static final char ASKPRICE_LESSER_EQUAL_STOPPRICE = 'C';
  public static final char BIDPRICE_GREATER_THAN_STOPPRICE = 'D';
  public static final char BIDPRICE_GREATER_EQUAL_STOPPRICE = 'E';
  public static final char BIDPRICE_LESSER_THAN_STOPPRICE = 'F';
  public static final char BIDPRICE_LESSER_EQUAL_STOPPRICE = 'H';
}
