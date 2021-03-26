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

import com.nabiki.commons.ctpobj.CCandle;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;

/**
 * Load candles from CSV.
 *
 * @author Hongbao Chen
 * @since 1.0
 */
public class CandleLoader extends LinkedList<CCandle> {

    private static final long serialVersionUID = 27639190272L;

    private String ec = "UTF-8";

    public CandleLoader(InputStream stream,
                        String encoding)
    throws
    IOException {
        if (stream == null) {
            throw new IOException("Can't read stream with a null pointer.");
        }
        if (encoding == null || encoding.isBlank()) {
            ec = "UTF-8";
        } else {
            ec = encoding;
        }
        CSVFormat.RFC4180
                .withHeader()
                .parse(new InputStreamReader(stream, ec))
                .forEach(record -> {
                    add(parse(record));
                });

    }

    @Override
    public Object clone() {
        return super.clone();
    }

    private CCandle parse(CSVRecord record) {
        var candle = new CCandle();
        candle.TradingDay = record.get(0);
        candle.OpenPrice = parsePrice(record.get(1));
        candle.HighestPrice = parsePrice(record.get(2));
        candle.LowestPrice = parsePrice(record.get(3));
        candle.ClosePrice = parsePrice(record.get(4));
        return candle;
    }

    private double parsePrice(String price) {
        return Double.parseDouble(price);
    }

}
