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

import com.nabiki.chart.exception.CanvasSizeOverflowException;

import java.awt.*;
import java.awt.image.BufferedImage;

public abstract class ImageCanvas implements Canvas {
    private BufferedImage image;
    private Graphics2D g2d;
    private final int[] size = new int[2];
    private final int[] offset = new int[2];
    private final int[] visibleSize = new int[2];
    private final int[] margin = new int[4];

    public ImageCanvas() {
    }

    public ImageCanvas(BufferedImage image) {
        setImage(image);
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        this.g2d = image.createGraphics();
        size[0] = image.getWidth();
        size[1] = image.getHeight();
        setBackground(DefaultStyles.CANVAS_BG_COLOR);
    }

    @Override
    public BufferedImage getBuffer() {
        return image;
    }

    @Override
    public boolean isVisible(int x, int y) {
        return 0 <= x && x <= getVisibleSize()[0]
                && 0 <= y && y <= getVisibleSize()[1];
    }

    @Override
    public void setSize(int width, int height) {
        if (image.getWidth() < width || image.getHeight() < height
                || width < 0 || height < 0)
            throw new CanvasSizeOverflowException(
                    String.format("canvas size(%d,%d) overflow", width, height));
        size[0] = width;
        size[1] = height;
        visibleSize[0] = size[0] - (getMargin()[1] + getMargin()[3]);
        visibleSize[1] = size[1] - (getMargin()[0] + getMargin()[2]);
    }

    @Override
    public int[] getSize() {
        return size;
    }

    @Override
    public int[] getVisibleSize() {
        return visibleSize;
    }

    @Override
    public void setMargin(int top, int right, int bottom, int left) {
        margin[0] = top;
        margin[1] = right;
        margin[2] = bottom;
        margin[3] = left;
        if (size[0] * size[1] > 0) {
            visibleSize[0] -= (getMargin()[1] + getMargin()[3]);
            visibleSize[1] -= (getMargin()[0] + getMargin()[2]);
        }
    }

    @Override
    public int[] getMargin() {
        return margin;
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        g2d.drawLine(getCanvasX(x1), getCanvasY(y1), getCanvasX(x2), getCanvasY(y2));
    }

    @Override
    public void drawString(String text, int x, int y) {
        g2d.drawString(text, getCanvasX(x), getCanvasY(y));
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        g2d.drawRect(getCanvasX(x), getCanvasY(y), width, height);
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        g2d.fillRect(getCanvasX(x), getCanvasY(y), width, height);
    }

    @Override
    public void drawVisibleLine(int x1, int y1, int x2, int y2) {
        g2d.drawLine(getVisibleX(x1), getVisibleY(y1), getVisibleX(x2), getVisibleY(y2));
    }

    @Override
    public void drawVisibleString(String text, int x, int y) {
        g2d.drawString(text, getVisibleX(x), getVisibleY(y));
    }

    @Override
    public void drawVisibleRect(int x, int y, int width, int height) {
        g2d.drawRect(getVisibleX(x), getVisibleY(y), width, height);
    }

    @Override
    public void fillVisibleRect(int x, int y, int width, int height) {
        g2d.fillRect(getVisibleX(x), getVisibleY(y), width, height);
    }

    @Override
    public void setStroke(Stroke stroke) {
        g2d.setStroke(stroke);
    }

    @Override
    public Stroke getStroke() {
        return g2d.getStroke();
    }

    @Override
    public void setColor(Color color) {
        g2d.setColor(color);
    }

    @Override
    public Color getColor() {
        return g2d.getColor();
    }

    @Override
    public void setOffset(int offsetX, int offsetY) {
        offset[0] = offsetX;
        offset[1] = offsetY;
    }

    @Override
    public int[] getOffset() {
        return offset;
    }

    @Override
    public void clear() {
        g2d.clearRect(getCanvasX(0), getCanvasY(0), getSize()[0], getSize()[1]);
    }

    @Override
    public void dispose() {
        g2d.dispose();
        image = null;
        g2d = null;
    }

    @Override
    public void setBackground(Color c) {
        g2d.setBackground(c);
    }

    @Override
    public Color getBackground() {
        return g2d.getBackground();
    }

    @Override
    public void setFont(Font font) {
        g2d.setFont(font);
    }

    @Override
    public Font getFont() {
        return g2d.getFont();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return g2d.getFontMetrics(getFont());
    }

    @Override
    public int getStringWidth(String text) {
        var metrics = getFontMetrics(getFont());
        int width = 0;
        for (int i = 0; i < text.length(); i++)
            width += metrics.charWidth(text.charAt(i));
        return width;
    }

    @Override
    public void showBox(boolean show) {
        Color color;
        if (show)
            color = DefaultStyles.GRID_LINE_COLOR;
        else
            color = getBackground();
        paintBox(color);
    }

    private void paintBox(Color color) {
        var size = getSize();
        var oldColor = getColor();
        setColor(color);
        drawLine(0, 0, size[0], 0);
        drawLine(size[0], 0, size[0], size[1]);
        drawLine(0, size[1], size[0], size[1]);
        drawLine(0, 0, 0, size[1]);
        setColor(oldColor);
    }

    protected int getCanvasX(int x) {
        return getOffset()[0] + x;
    }

    protected int getCanvasY(int y) {
        return getOffset()[1] + y;
    }

    protected int getVisibleX(int x) {
        return getCanvasX(x) + margin[3];
    }

    protected int getVisibleY(int y) {
        return getCanvasY(y) + margin[0];
    }
}
