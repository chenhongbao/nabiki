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

public interface Canvas {
    boolean isVisible(int x, int y);

    void setSize(int width, int height);

    int[] getSize();

    int[] getVisibleSize();

    void setMargin(int top, int right, int bottom, int left);

    int[] getMargin();

    void drawLine(int x1, int y1, int x2, int y2);

    void drawString(String text, int x, int y);

    void drawRect(int x, int y, int width, int height);

    void fillRect(int x, int y, int width, int height);

    void drawVisibleLine(int x1, int y1, int x2, int y2);

    void drawVisibleString(String text, int x, int y);

    void drawVisibleRect(int x, int y, int width, int height);

    void fillVisibleRect(int x, int y, int width, int height);

    void setStroke(Stroke stroke);

    Stroke getStroke();

    void showBox(boolean show);

    void setColor(Color color);

    Color getColor();

    void setOffset(int offsetX, int offsetY);

    int[] getOffset();

    void paint();

    void clear();

    void dispose();

    void setBackground(Color c);

    Color getBackground();

    void setFont(Font font);

    Font getFont();

    FontMetrics getFontMetrics(Font font);

    int getStringWidth(String text);
}
