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

import com.nabiki.chart.custom.CustomData;
import com.nabiki.chart.custom.CustomType;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CustomChart {
  private final GridXY world;
  private final Map<String, CustomData> data = new HashMap<>();
  private final Legend legend = new Legend();
  private boolean legendShown = false;

  public CustomChart(GridXY parent) {
    world = parent;
    if (world.getBuffer() != null) {
      legend.setImage(world.getBuffer());
    }
    legend.setMargin(
        DefaultStyles.LEGEND_MARGIN,
        DefaultStyles.LEGEND_MARGIN,
        DefaultStyles.LEGEND_MARGIN,
        DefaultStyles.LEGEND_MARGIN);
  }

  public void addCustomData(String name, CustomType type, Double[] vars) {
    synchronized (data) {
      // Only when there is custom data, show legend.
      legendShown = true;
      data.put(name, new CustomData(name, type, vars));
    }
  }

  public void showLegend(boolean shown) {
    legendShown = shown;
  }

  public void paint() {
    world.paint();
    paintCustomData();
    paintLegend();
  }

  public void setImage(BufferedImage image) {
    world.setImage(image);
    legend.setImage(image);
  }

  private void paintSingleCustomData(CustomData data) {
    switch (data.getType().getType()) {
      case CustomType.DOT:
        paintCustomDot(data);
        break;
      case CustomType.LINE:
        paintCustomLine(data);
        break;
      default:
        break;
    }
  }

  private void paintCustomLine(CustomData line) {
    var xLabels = world.getShowLabelX();
    var yLabels = world.getShowLabelY();
    var xAxisMin = xLabels[0];
    var xAxisMax = xLabels[xLabels.length - 1];
    var yAxisMin = yLabels[0];
    var yAxisMax = yLabels[yLabels.length - 1];
    // Preserve color.
    var oldColor = world.getColor();
    world.setColor(line.getType().getColor());
    int revIdx = 0;
    // Previous start of line.
    int pixelX0 = -1, pixelY0 = -1;
    for (int i = line.getValue().length - 1;
         0 <= i && revIdx < world.getX().length;
         --i, ++revIdx) {
      var value = line.getValue()[i];
      if (value == null) {
        continue;
      }
      var idx = world.getX().length - 1 - revIdx;
      var pixelX = world.getVisiblePixelX(idx, xAxisMin, xAxisMax);
      var pixelY = world.getVisiblePixelY(value, yAxisMin, yAxisMax);
      if (pixelX0 >= 0 && pixelY0 >= 0) {
        world.drawVisibleLine(pixelX, pixelY, pixelX0, pixelY0);
      }
      // Move to next point.
      pixelX0 = pixelX;
      pixelY0 = pixelY;
    }
    world.setColor(oldColor);
  }

  private void paintCustomDot(CustomData dot) {
    var xLabels = world.getShowLabelX();
    var yLabels = world.getShowLabelY();
    var xAxisMin = xLabels[0];
    var xAxisMax = xLabels[xLabels.length - 1];
    var yAxisMin = yLabels[0];
    var yAxisMax = yLabels[yLabels.length - 1];
    // Preserve color.
    var oldColor = world.getColor();
    world.setColor(dot.getType().getColor());
    int revIdx = 0;
    for (int i = dot.getValue().length - 1;
         0 <= i && revIdx < world.getX().length;
         --i, ++revIdx) {
      var value = dot.getValue()[i];
      if (value == null) {
        continue;
      }
      var idx = world.getX().length - 1 - revIdx;
      var pixelX = world.getVisiblePixelX(idx, xAxisMin, xAxisMax);
      var pixelY = world.getVisiblePixelY(value, yAxisMin, yAxisMax);
      var fromPixelX = pixelX - DefaultStyles.DOT_WIDTH / 2;
      var toPixelX = fromPixelX + DefaultStyles.DOT_WIDTH;
      world.drawVisibleLine(fromPixelX, pixelY, toPixelX, pixelY);
    }
    world.setColor(oldColor);
  }

  private void paintCustomData() {
    for (var custom : data.values())
      paintSingleCustomData(custom);
  }

  private void paintLegend() {
    if (!legendShown)
      return;
    setLegendPosition();
    for (var custom : data.values()) {
      legend.addCustomData(custom);
    }
    legend.paint();
  }

  private void setLegendPosition() {
    var offsetX = world.getOffset()[0] + world.getMargin()[0];
    var offsetY = world.getOffset()[1] + world.getMargin()[1];
    legend.setOffset(offsetX, offsetY);
  }
}
