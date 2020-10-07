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

import com.nabiki.chart.buffer.Charts;
import com.nabiki.chart.buffer.DefaultStyles;
import com.nabiki.chart.custom.CustomType;
import com.nabiki.chart.exception.NoStickException;

import java.awt.*;
import java.util.List;
import java.util.*;

public class BarChartController implements ViewController {
  static class CustomBar {
    double value;
    String label = null;
    Map<String, Double> customs = new HashMap<>();
  }

  private final Map<String, CustomType> types = new HashMap<>();
  private final List<CustomBar> bars = new LinkedList<>();
  private final BarChartPanel chart;

  private boolean resetCursor = true;
  private int cursor;
  private int windowSize = DefaultStyles.VIEW_DEFAULT_WINSIZE;

  // Currently shown data.
  private double[] values, chartY;
  private final Map<String, Double[]> customs = new HashMap<>();
  Map<Double, String> mapLabels = new HashMap<>();

  public BarChartController(BarChartPanel chart) {
    this.chart = chart;
  }

  @Override
  public void createLine(String name, Color color) {
    var type = new CustomType(color, CustomType.LINE);
    this.types.put(name, type);
  }

  @Override
  public void createDot(String name, Color color) {
    var type = new CustomType(color, CustomType.DOT);
    this.types.put(name, type);
  }

  public void appendBar(double value, String xLabel) {
    var bar = new CustomBar();
    bar.value = value;
    bar.label = xLabel;
    this.bars.add(bar);
    // Set cursor for new bar.
    setCursor();
  }

  @Override
  public void appendCustom(String name, Double value) {
    if (this.bars.size() == 0)
      throw new NoStickException("no stick to append to");
    this.bars.get(this.bars.size() - 1).customs.put(name, value);
  }

  @Override
  public void forward(int count) {
    setResetCursor(false);
    cursor = Math.min(cursor + count, this.bars.size() - 1);
    update();
  }

  @Override
  public void backward(int count) {
    setResetCursor(false);
    cursor = Math.max(cursor - count, getProperWindowSize() - 1);
    update();
  }

  @Override
  public void reset() {
    setResetCursor(true);
    setCursor();
    update();
  }

  @Override
  public void zoomIn() {
    windowSize = Math.max(windowSize / 2, DefaultStyles.VIEW_MIN_WINSIZE);
    update();
  }

  @Override
  public void zoomOut() {
    windowSize = Math.min(2 * windowSize, bars.size());
    update();
  }

  @Override
  public int getDataCount() {
    return bars.size();
  }

  @Override
  public int getShownSize() {
    return windowSize;
  }

  @Override
  public void update() {
    updateChart();
  }

  private void setCursor() {
    if (resetCursor)
      cursor = bars.size() - 1;
  }

  private void setResetCursor(boolean reset) {
    resetCursor = reset;
  }

  private int getProperWindowSize() {
    return Math.min(this.bars.size(), windowSize);
  }

  private void updateChart() {
    extractCurrentData();
    paintCurrentData();
  }

  private void extractCurrentData() {
    double maxY = -Double.MAX_VALUE, minY = Double.MAX_VALUE;
    var size = getProperWindowSize();
    values = new double[size];
    chartY = new double[size];
    customs.clear();
    mapLabels.clear();
    for (var key : types.keySet())
      customs.put(key, new Double[size]);
    int tmpIdx = size - 1;
    for (int index = cursor; 0 <= index && 0 <= tmpIdx; --index, --tmpIdx) {
      values[tmpIdx] = bars.get(index).value;
      mapLabels.put((double) tmpIdx, bars.get(index).label);
      // Set customs' data.
      for (var entry : customs.entrySet()) {
        var key = entry.getKey();
        var val = bars.get(index).customs.get(key);
        customs.get(key)[tmpIdx] = val;
        // Check min/max values of custom data.
        // Custom value could be null if it is not set.
        if (val != null) {
          maxY = Math.max(maxY, val);
          minY = Math.min(minY, val);
        }
      }
    }
    // Summarize overall min/max.
    maxY = Charts.max(maxY, Charts.max(values));
    minY = Charts.min(minY, Charts.min(values), 0.0D);
    Arrays.fill(chartY, minY);
    chartY[0] = maxY;
  }

  private void paintCurrentData() {
    this.chart.setData(values);
    this.chart.setY(chartY);
    for (var entry : this.customs.entrySet())
      this.chart.setCustomData(
          entry.getKey(),
          types.get(entry.getKey()),
          entry.getValue());
    this.chart.getXAxis().mapLabels(mapLabels);
    this.chart.paintAll();
    this.chart.updateUI();
  }
}
