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

public class TextView extends ImageXY {
  public static final int HORIZONTAL = 0;
  public static final int VERTICAL = 1;

  private int orientation = VERTICAL;
  private TextItem[] data = new TextItem[0];

  public TextView(BufferedImage image) {
    super(image);
  }

  public void setData(TextItem[] data) {
    if (data != null)
      this.data = Arrays.copyOf(data, data.length);
  }

  public void setOrientation(int orientation) {
    this.orientation = orientation;
  }

  @Override
  public void paint() {
    for (int i = 0; i < data.length; ++i)
      paintItem(i, data[i]);
  }

  private void paintItem(int index, TextItem item) {
    var oldColor = getColor();
    var oldFont = getFont();
    setColor(item.color);
    setFont(getFont().deriveFont(DefaultStyles.TEXTVIEW_FONT_SIZE));
    if (orientation == VERTICAL)
      drawVisibleString(
          item.value.toString(),
          0,
          textY(index));
    else
      drawVisibleString(
          item.value.toString(),
          textX(index),
          getFont().getSize());
    setColor(oldColor);
    setFont(oldFont);
  }

  private int textY(int index) {
    var diffY = (int) (getFont().getSize()
        * DefaultStyles.TEXTVIEW_FONT_LINESEP);
    return getOffset()[1] + getMargin()[1] + diffY * index;
  }

  private int textX(int index) {
    int w = 0;
    if (index == 0) {
      return getStringWidth(data[0].value.toString());
    } else {
      for (int i = 0; i < index - 1; ++i)
        w += getStringWidth(data[i].value.toString())
            + DefaultStyles.TEXTVIEW_FONT_BLANK_WIDTH;
    }
    return w;
  }
}
