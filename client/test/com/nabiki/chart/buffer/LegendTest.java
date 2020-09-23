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
import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class LegendTest {
    @Test
    public void basic() {
        var image = new BufferedImage(
                500,
                600,
                BufferedImage.TYPE_INT_RGB);

        var legend = new Legend();
        legend.setImage(image);
        legend.setOffset(100, 200);
        legend.setMargin(10, 10, 10, 10);

        Double[] line = new Double[] {1.0, 2.9, 3.0, 5.7, 3.0, 2.3, 1.1};
        Double[] dot = new Double[] {3.0, 4.9, 1.0, 7.7, 5.0, 0.3, 2.1};

        var d0 = new CustomData("line", new CustomType(Color.PINK, CustomType.LINE), line);
        var d1 = new CustomData("dot", new CustomType(Color.CYAN, CustomType.DOT), dot);

        legend.addCustomData(d0);
        legend.addCustomData(d1);

        legend.paint();

        try {
            ImageIO.write(image,
                    "png",
                    new File("C:\\Users\\chenh\\Desktop\\legend.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}