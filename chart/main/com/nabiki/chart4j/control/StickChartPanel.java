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

import com.nabiki.chart4j.buffer.CustomStickChart;
import com.nabiki.chart4j.buffer.DefaultStyles;
import com.nabiki.chart4j.buffer.XAxis;
import com.nabiki.chart4j.buffer.YAxis;
import com.nabiki.chart4j.custom.CustomType;

import java.awt.*;
import java.awt.image.BufferedImage;

public class StickChartPanel extends ImagePanel {
    private final XAxis x = new XAxis();
    private final YAxis y = new YAxis();
    private final CustomStickChart chart = new CustomStickChart();

    public StickChartPanel() {
        super();
        prepare();
    }

    public CustomStickChart getChart() {
        return chart;
    }

    public XAxis getXAxis() {
        return x;
    }

    public YAxis getYAxis() {
        return y;
    }

    public void setData(double[] open, double[] high, double[] low, double[] close) {
        synchronized (chart) {
            chart.setData(open, high, low, close);
        }
    }

    public void setY(double... y) {
        synchronized (chart) {
            chart.setY(y);
        }
    }

    public void showLegend(boolean shown) {
        chart.showLegend(shown);
    }

    public void setCustomData(String name, CustomType type, Double[] vars) {
        chart.addCustomData(name, type, vars);
    }

    @Override
    protected void onResize(Dimension newSize) {
        synchronized (chart) {
            updateChart(newSize);
        }
    }

    @Override
    protected void onShown(Dimension newSize) {
        synchronized (chart) {
            updateChart(newSize);
        }
    }

    @Override
    protected void onHidden(Dimension newSize) {
        synchronized (chart) {
            // do nothing.
        }
    }

    private void updateChart(Dimension newSize) {
        var image = new BufferedImage(
                newSize.width,
                newSize.height,
                BufferedImage.TYPE_INT_ARGB);
        setupChart(image, newSize);
        setImage(image);
        setBackground(chart.getBackground());
    }

    private Dimension getProperChartSize(Dimension total) {
        var r = new Dimension();
        r.width = total.width - DefaultStyles.AXIS_Y_WIDTH - DefaultStyles.CHART_OFFSET;
        r.height = total.height - DefaultStyles.AXIS_X_HEIGHT - DefaultStyles.CHART_OFFSET;
        return r;
    }

    private void setupChart(BufferedImage image, Dimension newSize) {
        chart.setImage(image);
        x.setImage(image);
        y.setImage(image);
        // Set chart size.
        var size = getProperChartSize(newSize);
        chart.setSize(size.width, size.height);
        // Paint.
        chart.paint();
        x.paint();
        y.paint();
    }

    private void prepare() {
        chart.setOffset(
                DefaultStyles.CHART_OFFSET,
                DefaultStyles.CHART_OFFSET);
        chart.setMargin(
                DefaultStyles.CHART_MARGIN,
                DefaultStyles.CHART_MARGIN,
                DefaultStyles.CHART_MARGIN,
                DefaultStyles.CHART_MARGIN);
        // Axis.
        x.bindXY(chart);
        x.bindCanvas(chart);
        y.bindXY(chart);
        y.bindCanvas(chart);
    }
}
