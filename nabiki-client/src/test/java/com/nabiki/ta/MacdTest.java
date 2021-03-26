/*
 * Copyright (c) 2020-2021. Hongbao Chen <chenhongbao@outlook.com>
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
package com.nabiki.ta;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
public class MacdTest {

    private final Macd macd = new Macd();
    private CandleLoader candles;

    @Test
    @Order(0)
    public void loadCandles() {
        assertFalse(candles.isEmpty());
    }

    @Test
    @BeforeEach
    public void loadCandle() {
        Assertions.assertDoesNotThrow(() -> {
            var stream = this.getClass().getResourceAsStream("/corn-day.txt");
            candles = new CandleLoader(stream, "gb2312");
        });
    }

    @Test
    @Order(1)
    public void testMacd() {
        assertDoesNotThrow(() -> {
            candles.forEach(candle -> {
                macd.add(candle.ClosePrice);
            });
        });
        var last = macd.getTail();
        assertEquals(-22.54D, last.getDif(), 0.005D);
        assertEquals(-14.83D, last.getDea(), 0.005D);
        assertEquals(-15.40D, last.getMacd(), 0.005D);
    }
}
