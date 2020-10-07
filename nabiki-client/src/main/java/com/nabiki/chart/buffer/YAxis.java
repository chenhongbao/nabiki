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

import java.awt.image.BufferedImage;

public class YAxis extends AbstractAxis {
  private ImageCanvas canvas;

  public YAxis() {
  }

  public YAxis(BufferedImage image) {
    super(image);
  }

  public void bindCanvas(ImageCanvas canvas) {
    this.canvas = canvas;
  }

  @Override
  protected void paintLabel(double label, double axisMin, double axisMax) {
    var oldColor = getColor();
    // Draw axis tick.
    setColor(DefaultStyles.AXIS_LINE_COLOR);
    int y = xy.getVisiblePixelY(label, axisMin, axisMax);
    drawVisibleLine(0, y, DefaultStyles.AXIS_TICK_LENGTH, y);
    // Draw axis label.
    setColor(DefaultStyles.AXIS_LABEL_COLOR);
    var str = getLabelString(label);
    int yOff = getFont().getSize() / 2;
    drawVisibleString(
        str,
        DefaultStyles.AXIS_TICK_LENGTH * 2,
        y + yOff);
    setColor(oldColor);
  }

  @Override
  public void paint() {
    setupAxis();
    clear();
    var labels = xy.getShowLabelY();
    for (var label : labels)
      paintLabel(label, labels[0], labels[labels.length - 1]);
  }

  private void setupAxis() {
    var size = canvas.getSize();
    var offset = canvas.getOffset();
    var margin = canvas.getMargin();
    setSize(DefaultStyles.AXIS_Y_WIDTH, size[1]);
    setOffset(offset[0] + size[0] + 1, offset[1]);
    setMargin(margin[0], 0, margin[2], 0);
  }
}
