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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class StickChart extends GridXY {
    private double[] open = new double[0],
            high = new double[0],
            low = new double[0],
            close = new double[0];

    public StickChart() {}

    public StickChart(BufferedImage image) {
        super(image);
    }

    public void setData(double[] open, double[] high, double[] low, double[] close) {
        Charts.requireSame(open.length, high.length, low.length, close.length);
        this.open = Arrays.copyOf(open, open.length);
        this.high = Arrays.copyOf(high, high.length);
        this.low = Arrays.copyOf(low, low.length);
        this.close = Arrays.copyOf(close, close.length);
    }

    @Override
    public void paint() {
        super.paint();
        paintSticks();
    }

    @Override
    public void setY(double... labels) {
        super.setY(labels);
        super.setX(sampleGridX(labels));
    }

    @Override
    public void setX(double... labels) {
        throw new UnsupportedOperationException("setX method is forbidden");
    }

    private double[] sampleGridX(double[] vars) {
        var sample = Arrays.copyOf(vars, vars.length);
        for (int i = 0; i< sample.length; ++i)
            sample[i] = i;
        return sample;
    }

    private void paintSticks() {
        // No data at init.
        if (getY() == null || getY().length == 0)
            return;
        // Or it already has data.
        var xLabels = getShowLabelX();
        var yLabels = getShowLabelY();
        var xAxisMin = xLabels[0];
        var xAxisMax = xLabels[xLabels.length-1];
        var yAxisMin = yLabels[0];
        var yAxisMax = yLabels[yLabels.length-1];
        for (var x : getX()) {
            int i = (int)x;
            var pixelX = getVisiblePixelX(i, xAxisMin, xAxisMax);
            var pixelOpen = getVisiblePixelY(open[i], yAxisMin, yAxisMax);
            var pixelHigh = getVisiblePixelY(high[i], yAxisMin, yAxisMax);
            var pixelLow = getVisiblePixelY(low[i], yAxisMin, yAxisMax);
            var pixelClose = getVisiblePixelY(close[i], yAxisMin, yAxisMax);
            paintStick(pixelX, pixelOpen, pixelHigh, pixelLow, pixelClose);
        }

    }

    private int fitVisibleX(int pixel) {
        var margin = getMargin();
        var visible = getVisibleSize();
        if (pixel < -margin[0])
            return -margin[0];
        else if (pixel > margin[0] + visible[0])
            return margin[0] + visible[0];
        else
            return pixel;
    }

    private int fitVisibleY(int pixel) {
        var visible = getVisibleSize();
        if (pixel < 0)
            return 0;
        else if (pixel > visible[1])
            return visible[1];
        else
            return pixel;
    }

    private int getPaintWidth() {
        return (int)(getVisibleSize()[0] / (open.length + 1.0D)
                * (1 - DefaultStyles.X_NODE_BLANK_PORTION));
    }

    private void paintStick(
            int pixelX, int pixelOpen, int pixelHigh, int pixelLow, int pixelClose) {
        var oldColor = getColor();
        var stroke = getStroke();
        Color color;
        if (pixelOpen >  pixelClose)
            color = DefaultStyles.STICK_UP_COLOR;
        else if (pixelOpen == pixelClose)
            color = DefaultStyles.STICK_FAIR_COLOR;
        else
            color = DefaultStyles.STICK_DOWN_COLOR;
        setStroke(DefaultStyles.STICK_STROKE);
        setColor(color);
        // Paint sticks.
        var offsetW = getPaintWidth() / 2;
        drawVisibleLine(
                pixelX,
                fitVisibleY(pixelHigh),
                pixelX,
                fitVisibleY(pixelLow));
        drawVisibleLine(
                fitVisibleX(pixelX - offsetW),
                fitVisibleY(pixelOpen),
                pixelX,
                fitVisibleY(pixelOpen));
        drawVisibleLine(
                pixelX,
                fitVisibleY(pixelClose),
                fitVisibleX(pixelX + offsetW),
                fitVisibleY(pixelClose));
        setColor(oldColor);
        setStroke(stroke);
    }
}
