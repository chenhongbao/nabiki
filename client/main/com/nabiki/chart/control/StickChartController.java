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

public class StickChartController implements ViewController {
    class CustomStick {
        double open, high, low, close;
        String label = null;
        Map<String, Double> customs = new HashMap<>();
    }

    private final Map<String, CustomType> types = new HashMap<>();
    private final List<CustomStick> sticks = new LinkedList<>();
    private final StickChartPanel chart;

    private boolean resetCursor = true;
    private int cursor;
    private int windowSize = DefaultStyles.VIEW_DEFAULT_WINSIZE;

    // Currently shown data.
    private double[] open, high, low, close, chartY;
    private final Map<String, Double[]> customs = new HashMap<>();
    Map<Double, String> mapLabels = new HashMap<>();

    public StickChartController(StickChartPanel chart) {
        this.chart = chart;
    }

    public void createLine(String name, Color color) {
        var type = new CustomType(color, CustomType.LINE);
        this.types.put(name, type);
    }

    public void createDot(String name, Color color) {
        var type = new CustomType(color, CustomType.DOT);
        this.types.put(name, type);
    }

    public void append(double open, double high, double low, double close, String xLabel) {
        var stick = new CustomStick();
        stick.open = open;
        stick.high = high;
        stick.low = low;
        stick.close = close;
        stick.label = xLabel;
        this.sticks.add(stick);
        // Set cursor for new stick.
        setCursor();
    }

    public void append(String name, Double value) {
        if (this.sticks.size() == 0)
            throw new NoStickException("no stick to append to");
        this.sticks.get(this.sticks.size() - 1).customs.put(name, value);
    }

    @Override
    public void forward(int count) {
        setResetCursor(false);
        cursor = Math.min(cursor + count, this.sticks.size() - 1);
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
        windowSize *= 2;
        update();
    }

    @Override
    public int getDataCount() {
        return sticks.size();
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
            cursor = sticks.size() - 1;
    }

    private void setResetCursor(boolean reset) {
        resetCursor = reset;
    }

    private int getProperWindowSize() {
        return Math.min(this.sticks.size(), windowSize);
    }

    private void updateChart() {
        extractCurrentData();
        paintCurrentData();
    }

    private void extractCurrentData() {
        double maxY = -Double.MAX_VALUE, minY = Double.MAX_VALUE;
        var size = getProperWindowSize();
        open = new double[size];
        high = new double[size];
        low = new double[size];
        close = new double[size];
        chartY = new double[size];
        customs.clear();
        mapLabels.clear();
        for (var key : types.keySet())
            customs.put(key, new Double[size]);
        int tmpIdx = size - 1;
        for (int index = cursor;
             0 <= index && 0 <= tmpIdx;
             --index, -- tmpIdx) {
            open[tmpIdx] = sticks.get(index).open;
            high[tmpIdx] = sticks.get(index).high;
            low[tmpIdx] = sticks.get(index).low;
            close[tmpIdx] = sticks.get(index).close;
            mapLabels.put((double) tmpIdx, sticks.get(index).label);
            // Set customs' data.
            for (var entry : customs.entrySet()) {
                var key = entry.getKey();
                var val = sticks.get(index).customs.get(key);
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
        maxY = Charts.max(
                maxY,
                Charts.max(open),
                Charts.max(high),
                Charts.max(low),
                Charts.max(close));
        minY = Charts.min(
                minY,
                Charts.min(open),
                Charts.min(high),
                Charts.min(low),
                Charts.min(close));
        Arrays.fill(chartY, minY);
        chartY[0] = maxY;
    }

    private void paintCurrentData() {
        this.chart.setData(open, high, low, close);
        this.chart.setY(chartY);
        for (var entry : this.customs.entrySet())
            this.chart.setCustomData(
                    entry.getKey(),
                    types.get(entry.getKey()),
                    entry.getValue());
        this.chart.getXAxis().mapLabels(mapLabels);
        this.chart.getXAxis().paint();
        this.chart.getYAxis().paint();
        this.chart.getChart().paint();
        this.chart.updateUI();
    }
}
