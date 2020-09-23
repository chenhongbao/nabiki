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

package com.nabiki.chart.buffer;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class BarChartTest {
    private double[] sampleGridY(double[] begin, double[] end) {
        var sample = Arrays.copyOf(end, end.length);
        sample[0] = Charts.max(Charts.max(begin), Charts.max(end));
        sample[1] = Charts.min(Charts.min(begin), Charts.min(end));
        return sample;
    }

    @Test
    public void basic() {
        var image = new BufferedImage(
                500,
                600,
                BufferedImage.TYPE_INT_ARGB);

        var begin = new double[] {3.1, 5.0, 5.2, 4.2, 4.3, 5.0, 6.5, 8.5, 7.9};
        var end = new double[] {5.4, 6.0, 5.8, 4.5, 4.9, 6.5, 8.7, 5.5, 7.9};

        var sampleY = sampleGridY(begin, end);

        var chart = new BarChart(image);
        chart.setOffset(100, 100);
        chart.setMargin(20, 20, 20, 20);
        chart.setSize(300, 400);
        chart.setData(begin, end);
        chart.setY(sampleY);

        var x = new XAxis(image);
        x.bindXY(chart);
        x.bindCanvas(chart);

        var y = new YAxis(image);
        y.bindXY(chart);
        y.bindCanvas(chart);

        chart.paint();
        x.paint();
        y.paint();

        try {
            ImageIO.write(
                    image,
                    "png",
                    new File("C:\\Users\\chenh\\Desktop\\bar_chart_basic.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<Double, String> getLabels() {
        var m = new HashMap<Double, String>();
        m.put(0.0, "00:00");
        m.put(1.0, "01:00");
        m.put(2.0, "02:00");
        m.put(3.0, "03:00");
        m.put(4.0, "04:00");
        m.put(5.0, "05:00");
        m.put(6.0, "06:00");
        m.put(7.0, "07:00");
        m.put(8.0, "08:00");
        return m;
    }

    @Test
    public void zero() {
        var image = new BufferedImage(
                500,
                600,
                BufferedImage.TYPE_INT_ARGB);

        var end = new double[] {5.4, 6.0, -5.8, 4.5, 0, -6.5, 8.7, -5.5, 7.9};

        var sampleY = sampleGridY(end, end);

        var chart = new BarChart(image);
        chart.setOffset(100, 100);
        chart.setMargin(20, 20, 20, 20);
        chart.setSize(300, 400);
        chart.setData(end);
        chart.setY(sampleY);

        var x = new XAxis(image);
        x.bindXY(chart);
        x.bindCanvas(chart);
        x.mapLabels(getLabels());

        var y = new YAxis(image);
        y.bindXY(chart);
        y.bindCanvas(chart);

        chart.paint();
        x.paint();
        y.paint();

        try {
            ImageIO.write(
                    image,
                    "png",
                    new File("C:\\Users\\chenh\\Desktop\\bar_chart_zero.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}