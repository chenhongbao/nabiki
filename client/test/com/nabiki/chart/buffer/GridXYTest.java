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

public class GridXYTest {
  @Test
  public void basic() {
    var image = new BufferedImage(
        500,
        600,
        BufferedImage.TYPE_INT_ARGB);

    var grid = new GridXY(image);
    grid.setOffset(100, 100);
    grid.setMargin(20, 20, 20, 20);
    grid.setSize(300, 400);
    grid.setX(1, 1.5, 2.8, 5, 6, 7.3, 9, 10, 13, 14.5, 17);
    grid.setY(3, 1.5, 4.8, 5.1, 4.6, 10.3, 9.4, 8.7, 11.5, 16.7, 11.3);

    var x = new XAxis(image);
    x.bindXY(grid);
    x.bindCanvas(grid);

    var y = new YAxis(image);
    y.bindXY(grid);
    y.bindCanvas(grid);

    grid.paint();
    x.paint();
    y.paint();

    try {
      ImageIO.write(
          image,
          "png",
          new File("C:\\Users\\chenh\\Desktop\\grid_xy.png"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}