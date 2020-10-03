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

package com.nabiki.chart.control;

import com.nabiki.chart.buffer.*;
import com.nabiki.chart.custom.CustomType;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ChartPanel extends ImagePanel {
  private final XAxis x = new XAxis();
  private final YAxis y = new YAxis();

  private CustomChart chart;
  private GridXY world;

  public ChartPanel() {}

  public ChartPanel(GridXY world) {
    this.world = world;
    this.chart =  new CustomChart(world);
    prepare();
  }

  protected void setWorld(GridXY world) {
    this.world = world;
    this.chart = new CustomChart(world);
    prepare();
  }

  public GridXY getWorld() {
    if (world == null) {
      throw new NullPointerException("world not set");
    }
    return world;
  }

  public CustomChart getChart() {
    if (chart == null) {
      throw new NullPointerException("chart not set");
    }
    return chart;
  }

  public XAxis getXAxis() {
    return x;
  }

  public YAxis getYAxis() {
    return y;
  }

  public void showLegend(boolean shown) {
    getChart().showLegend(shown);
  }

  public void setCustomData(String name, CustomType type, Double[] vars) {
    getChart().addCustomData(name, type, vars);
  }

  public Image getBuffer() {
    return super.getImage();
  }

  @Override
  protected void onResize(Dimension newSize) {
    synchronized (getChart()) {
      updateChart(newSize);
    }
  }

  @Override
  protected void onShown(Dimension newSize) {
    synchronized (getChart()) {
      updateChart(newSize);
    }
  }

  @Override
  protected void onHidden(Dimension newSize) {
    synchronized (getChart()) {
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
    setBackground(getWorld().getBackground());
  }

  private Dimension getProperChartSize(Dimension total) {
    var r = new Dimension();
    r.width = total.width - DefaultStyles.AXIS_Y_WIDTH - DefaultStyles.CHART_OFFSET;
    r.height = total.height - DefaultStyles.AXIS_X_HEIGHT - DefaultStyles.CHART_OFFSET;
    return r;
  }

  private void setupChart(BufferedImage image, Dimension newSize) {
    getChart().setImage(image);
    getXAxis().setImage(image);
    getYAxis().setImage(image);
    // Set chart size.
    var size = getProperChartSize(newSize);
    getWorld().setSize(size.width, size.height);
    // Paint.
    getChart().paint();
    getYAxis().paint();
    getYAxis().paint();
  }

  private void prepare() {
    getWorld().setOffset(
        DefaultStyles.CHART_OFFSET,
        DefaultStyles.CHART_OFFSET);
    getWorld().setMargin(
        DefaultStyles.CHART_MARGIN,
        DefaultStyles.CHART_MARGIN,
        DefaultStyles.CHART_MARGIN,
        DefaultStyles.CHART_MARGIN);
    // Axis.
    getXAxis().bindXY(getWorld());
    getXAxis().bindCanvas(getWorld());
    getYAxis().bindXY(getWorld());
    getYAxis().bindCanvas(getWorld());
  }
}
