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
import java.util.Arrays;

public abstract class ImageXY extends ImageCanvas implements XYCoordinate {
  private double[] x, y;
  private double maxX, maxY, minX, minY;
  private int preferredLabelCountX = DefaultStyles.AXIS_DEFAULT_X_LABEL_COUNT,
      preferredLabelCountY = DefaultStyles.AXIS_DEFAULT_Y_LABEL_COUNT;

  public ImageXY() {
  }

  public ImageXY(BufferedImage image) {
    super(image);
    x = y = new double[0];
  }

  @Override
  public void setX(double... labels) {
    if (labels.length > 0)
      x = Arrays.copyOf(labels, labels.length);
    maxX = -Double.MAX_VALUE;
    minX = Double.MAX_VALUE;
    for (var l : labels) {
      maxX = Math.max(maxX, l);
      minX = Math.min(minX, l);
    }
  }

  @Override
  public double[] getX() {
    return x;
  }

  @Override
  public void setY(double... labels) {
    if (labels.length > 0)
      y = Arrays.copyOf(labels, labels.length);
    maxY = -Double.MAX_VALUE;
    minY = Double.MAX_VALUE;
    for (var l : labels) {
      maxY = Math.max(maxY, l);
      minY = Math.min(minY, l);
    }
  }

  @Override
  public double[] getY() {
    return y;
  }

  @Override
  public void setPreferredLabelCountX(int count) {
    preferredLabelCountX = count;
  }

  @Override
  public int getPreferredLabelCountX() {
    return preferredLabelCountX;
  }

  @Override
  public void setPreferredLabelCountY(int count) {
    preferredLabelCountY = count;
  }

  @Override
  public int getPreferredLabelCountY() {
    return preferredLabelCountY;
  }

  @Override
  public double[] getShowLabelX() {
    var properMaxX = minX == maxX ? minX + 1 : maxX;
    return getPreferredLabels(minX, properMaxX, getPreferredLabelCountX());
  }

  @Override
  public double[] getShowLabelY() {
    var properMaxY = minY == maxY ? minY + 1 : maxY;
    return getPreferredLabels(minY, properMaxY, getPreferredLabelCountY());
  }

  @Override
  public int getVisiblePixelX(double x, double axisMin, double axisMax) {
    return (int) Math.round(x * getVisibleSize()[0] / (axisMax - axisMin));
  }

  @Override
  public int getVisiblePixelY(double y, double axisMin, double axisMax) {
    return (int) Math.round((axisMax - y) * getVisibleSize()[1] / (axisMax - axisMin));
  }

  protected double[] getPreferredLabels(double min, double max, int num) {
    double step = (max - min) / num;
    double temp = 0;
    // Normalize step into [0, 1).
    if (Math.pow(10, (int) Math.log10(step)) == step)
      temp = Math.pow(10, (int) (Math.log10(step)));
    else
      temp = Math.pow(10, (int) (Math.log10(step)) + 1);
    var tempStep = step / temp;
    var stdStep = 0.0D;
    if (0 <= tempStep && tempStep <= 0.1)
      stdStep = 0.1;
    else if (0.1 < tempStep && tempStep <= 0.2)
      stdStep = 0.2;
    else if (0.2 < tempStep && tempStep <= 0.25)
      stdStep = 0.25;
    else if (0.25 < tempStep && tempStep <= 0.5)
      stdStep = 0.5;
    else
      stdStep = 1.0;
    // Recover step from [0, 1).
    var finalStep = stdStep * temp;
    double beg, end;
    beg = Math.floor(min / finalStep);
    end = Math.ceil(max / finalStep);
    // Create return array.
    var array = new double[(int) (end - beg + 1)];
    for (int i = 0; i < array.length; ++i)
      array[i] = (beg + i) * finalStep;
    return array;
  }
}
