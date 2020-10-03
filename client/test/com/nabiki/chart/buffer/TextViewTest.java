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

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TextViewTest {
  private TextItem[] getTextItems() {
    var items = new TextItem[5];
    items[0] = new TextItem("Hello");
    items[1] = new TextItem("你好", DefaultStyles.FONT_DOWN_COLOR);
    items[2] = new TextItem("你好，China");
    items[3] = new TextItem("ABC一二三四", DefaultStyles.FONT_UP_COLOR);
    items[4] = new TextItem("43483648");
    return items;
  }

  @Test
  public void basic() {
    var image = new BufferedImage(
        500,
        500,
        BufferedImage.TYPE_INT_ARGB);

    var view = new TextView(image);
    view.setOrientation(TextView.VERTICAL);
    view.setData(getTextItems());

    view.paint();

    try {
      ImageIO.write(
          image,
          "png",
          new File("C:\\Users\\chenh\\Desktop\\text_view.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}