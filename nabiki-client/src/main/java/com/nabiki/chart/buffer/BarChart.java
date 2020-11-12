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

package com.nabiki.chart.buffer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Arrays;

public class BarChart extends GridXY {
  private double[] begin = new double[0], end = new double[0];

  public BarChart() {
  }

  public BarChart(BufferedImage image) {
    super(image);
  }

  public void setData(double[] begin, double[] end) {
    Charts.requireSame(begin.length, end.length);
    this.begin = Arrays.copyOf(begin, begin.length);
    this.end = Arrays.copyOf(end, end.length);
  }

  public void setData(double[] vars) {
    setData(new double[vars.length], Arrays.copyOf(vars, vars.length));
  }

  @Override
  public void paint() {
    super.paint();
    paintBars();
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
    for (int i = 0; i < sample.length; ++i)
      sample[i] = i;
    return sample;
  }

  private int getPaintWidth() {
    return (int) (getVisibleSize()[0] / (end.length + 1.0D)
        * (1 - DefaultStyles.X_NODE_BLANK_PORTION));
  }

  private void paintBars() {
    // No data at init.
    if (getY() == null || getY().length == 0)
      return;
    var xLabels = getShowLabelX();
    var yLabels = getShowLabelY();
    var xAxisMin = xLabels[0];
    var xAxisMax = xLabels[xLabels.length - 1];
    var yAxisMin = yLabels[0];
    var yAxisMax = yLabels[yLabels.length - 1];
    // Paint zero line.
    if (yLabels[0] * yLabels[yLabels.length - 1] <= 0)
      paintZeroLine(yLabels[0], yLabels[yLabels.length - 1]);
    for (var x : getX()) {
      int i = (int) x;
      int pixelX = getVisiblePixelX(i, xAxisMin, xAxisMax);
      int pixelBegin = getVisiblePixelY(begin[i], yAxisMin, yAxisMax);
      int pixelEnd = getVisiblePixelY(end[i], yAxisMin, yAxisMax);
      paintBar(pixelX, pixelBegin, pixelEnd);
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

  private void paintBar(int pixelX, int pixelBegin, int pixelEnd) {
    var w = getPaintWidth();
    var oldColor = getColor();
    Color color;
    if (pixelBegin > pixelEnd)
      color = DefaultStyles.BAR_UP_COLOR;
    else if (pixelBegin == pixelEnd)
      color = DefaultStyles.BAR_FAIR_COLOR;
    else
      color = DefaultStyles.BAR_DOWN_COLOR;
    setColor(color);
    var rx = fitVisibleX(pixelX - w / 2);
    fillVisibleRect(
        rx,
        Math.min(pixelBegin, pixelEnd),
        pixelX + w / 2 - rx,
        Math.abs(pixelBegin - pixelEnd));
    setColor(oldColor);
  }

  private void paintZeroLine(double axisMin, double axisMax) {
    var zero = getVisiblePixelY(0, axisMin, axisMax) + getMargin()[0];
    var oldColor = getColor();
    var oldStroke = getStroke();
    setColor(DefaultStyles.GRID_LINE_COLOR);
    setStroke(DefaultStyles.STICK_STROKE);
    drawLine(0, zero, getSize()[0], zero);
    setColor(oldColor);
    setStroke(oldStroke);
  }
}
