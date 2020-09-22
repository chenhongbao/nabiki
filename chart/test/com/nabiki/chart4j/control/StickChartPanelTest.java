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

package com.nabiki.chart4j.control;

import com.nabiki.chart4j.buffer.Charts;
import com.nabiki.chart4j.custom.CustomType;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

public class StickChartPanelTest {
    private double[] sampleGridY(double[] open, double[] high, double[] low,
                                 double[] close) {
        var sample = Arrays.copyOf(open, open.length);
        sample[0] = Charts.max(
                Charts.max(open),
                Charts.max(high),
                Charts.max(low),
                Charts.max(close));
        sample[1] = Charts.min(
                Charts.min(open),
                Charts.min(high),
                Charts.min(low),
                Charts.min(close));
        return sample;
    }

    @Test
    public void basic() {

        var open = new double[] {3.1, 5.0, 5.2, 4.2, 4.3, 5.0, 6.5, 8.5, 7.9};
        var high = new double[] {5.4, 6.0, 5.2, 4.5, 4.9, 6.5, 8.7, 8.5, 7.9};
        var low = new double[] {3.0, 4.5, 4.1, 4.0, 4.1, 5.0, 6.0, 7.8, 6.5};
        var close = new double[] {5.1, 5.2, 4.2, 4.2, 4.9, 6.5, 8.6, 7.9, 6.9};

        Double[] line = new Double[] {3.1, 3.9, 3.7, 5.7, 3.0, 7.3, null, 5.4, 7.8};
        Double[] dot = new Double[] {3.0, 4.9, 6.0, 7.7, 5.0, 3.3, 5.1, null, 4.5};

        var sampleY = sampleGridY(open, high, low, close);

        var chart = new StickChartPanel();

        chart.setData(open, high, low, close);
        chart.setY(sampleY);

        chart.setCustomData("line", new CustomType(Color.BLACK, CustomType.LINE), line);
        chart.setCustomData("dot", new CustomType(Color.MAGENTA, CustomType.DOT), dot);

        var dialog = new JDialog();

        dialog.add(chart);
        dialog.setSize(new Dimension(500, 600));
        dialog.setVisible(true);

        try {
            new CountDownLatch(1).await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}