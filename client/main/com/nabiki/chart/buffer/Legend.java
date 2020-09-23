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

public class Legend extends ImageCanvas {
    private final Map<String, CustomData> data = new HashMap<>();

    public Legend() {
    }

    public Legend(BufferedImage image) {
        super(image);
    }

    public void addCustomData(CustomData custom) {
        synchronized (data) {
            data.put(custom.getName(), custom);
        }
    }

    @Override
    public void paint() {
        resizeLegend();
        clear();
        showBox(true);
        paintLegend();
    }

    private void paintLegend() {
        int index = 0;
        for (var custom : data.values())
            paintSingleLegend(index++, custom);
    }

    private void paintSingleLegend(int index, CustomData custom) {
        paintLegendText(index, custom.getName());
        paintLegendSample(index, custom.getType());
    }

    private void paintLegendText(int index, String text) {
        int y = index * DefaultStyles.LEGEND_SINGLE_HEIGHT + getFont().getSize();
        var oldColor = getColor();
        setColor(DefaultStyles.AXIS_LINE_COLOR);
        drawVisibleString(text, 0, y);
        setColor(oldColor);
    }

    private void paintLegendSample(int index, CustomType type) {
        int y = index * DefaultStyles.LEGEND_SINGLE_HEIGHT + getFont().getSize() / 2;
        int x = getVisibleSize()[0] - DefaultStyles.LEGEND_SAMPLE_WIDTH;
        var oldColor = getColor();
        setColor(type.getColor());
        switch (type.getType()) {
            case CustomType.DOT:
                drawVisibleLine(x, y, x + DefaultStyles.DOT_WIDTH, y);
                break;
            case CustomType.LINE:
                drawVisibleLine(x, y, x + DefaultStyles.LEGEND_SAMPLE_WIDTH, y);
                break;
            default:
                break;
        }
        setColor(oldColor);
    }

    private void resizeLegend() {
        int maxNameWidth = 0;
        for (var custom : data.values())
            maxNameWidth = Math.max(getStringWidth(custom.getName()), maxNameWidth);
        var width = maxNameWidth
                + DefaultStyles.LEGEND_BLANK_WIDTH
                + DefaultStyles.LEGEND_SAMPLE_WIDTH
                + 2 * DefaultStyles.LEGEND_MARGIN;
        var height = DefaultStyles.LEGEND_SINGLE_HEIGHT * data.size()
                + 2 * DefaultStyles.LEGEND_MARGIN;
        setSize(width, height);
    }
}
