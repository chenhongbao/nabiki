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

package com.nabiki.chart4j.buffer;

import java.awt.image.BufferedImage;

public class GridXY extends ImageXY {
    public GridXY() {}

    public GridXY(BufferedImage image) {
        super(image);
    }

    @Override
    public void paint() {
        clear();
        showBox(true);
        paintDashGrid();
    }

    private void gridLH(double label, double axisMin, double axisMax) {
        var y = getVisiblePixelY(label, axisMin, axisMax) + getMargin()[0];
        drawLine(0, y, getSize()[0], y);
    }

    private void gridV(double label, double axisMin, double axisMax) {
        var x = getVisiblePixelX(label, axisMin, axisMax) + getMargin()[3];
        drawLine(x, 0, x, getSize()[1]);
    }

    private void paintGridH() {
        var labels = getShowLabelY();
        for (var label : labels)
            gridLH(label, labels[0], labels[labels.length - 1]);
    }

    private void paintGridV() {
        var labels = getShowLabelX();
        for (var label : labels)
            gridV(label, labels[0], labels[labels.length - 1]);
    }

    private void paintDashGrid() {
        var oldColor = getColor();
        var oldStroke = getStroke();
        setColor(DefaultStyles.GRID_DASHLINE_COLOR);
        setStroke(DefaultStyles.GRID_DASHLINE_STROKE);
        paintGridH();
        paintGridV();
        setColor(oldColor);
        setStroke(oldStroke);
    }
}
