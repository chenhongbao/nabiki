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

package com.nabiki.centre.md;

import com.nabiki.objects.CCandle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class CandleAccess {
    private static final String header = "InstrumentID,OpenPrice,HighestPrice," +
            "LowestPrice,ClosePrice,AveragePrice,OpenInterest,Volume,Minute," +
            "TradingDay,ActionDay,UpdateTime" + System.lineSeparator();

    protected void write(File file, CCandle candle) {
        Objects.requireNonNull(file, "file null");
        Objects.requireNonNull(candle, "candle null");
        var value = String.format("%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.0f,%d,%d,%s,%s,%s%n",
                candle.InstrumentID,
                candle.OpenPrice,
                candle.HighestPrice,
                candle.LowestPrice,
                candle.ClosePrice,
                candle.AveragePrice,
                candle.OpenInterest,
                candle.Volume,
                candle.Minute,
                candle.TradingDay,
                candle.ActionDay,
                candle.UpdateTime);
        try (FileWriter fw = new FileWriter(file, true)) {
            if (file.length() == 0)
                fw.write(header);
            fw.write(value);
            fw.flush();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    private CCandle parse(String line) {
        String[] vars = line.split(",");
        if (vars.length != 12)
            return null;
        CCandle candle = new CCandle();
        candle.InstrumentID = vars[0];
        candle.OpenPrice = Double.parseDouble(vars[1]);
        candle.HighestPrice = Double.parseDouble(vars[2]);
        candle.LowestPrice = Double.parseDouble(vars[3]);
        candle.ClosePrice = Double.parseDouble(vars[4]);
        candle.AveragePrice = Double.parseDouble(vars[5]);
        candle.OpenInterest = Double.parseDouble(vars[6]);
        candle.Volume = Integer.parseInt(vars[7]);
        candle.Minute = Integer.parseInt(vars[8]);
        candle.TradingDay = vars[9];
        candle.ActionDay = vars[10];
        candle.UpdateTime = vars[11];
        return candle;
    }

    protected List<CCandle> read(File file) {
        var list = new LinkedList<CCandle>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            // Skip the first header line.
            br.readLine();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0)
                    continue;
                try {
                    var candle = parse(line);
                    if (candle != null)
                        list.add(candle);
                    else {
                        System.err.println("wrong candle csv line");
                        System.err.println(line);
                    }
                } catch (Throwable th) {
                    th.printStackTrace();
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
        return list;
    }
}
