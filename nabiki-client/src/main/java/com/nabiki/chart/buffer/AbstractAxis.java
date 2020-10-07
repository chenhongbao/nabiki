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
import java.util.HashMap;
import java.util.Map;

public abstract class AbstractAxis extends ImageCanvas implements Axis {
  protected final Map<Double, String> map = new HashMap<>();
  protected String name;
  protected XYCoordinate xy;

  public AbstractAxis() {
  }

  public AbstractAxis(BufferedImage image) {
    super(image);
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void bindXY(XYCoordinate xy) {
    this.xy = xy;
  }

  @Override
  public void mapLabels(Map<Double, String> m) {
    map.clear();
    if (m != null)
      map.putAll(m);
  }

  protected abstract void paintLabel(double label, double axisMin, double axisMax);

  protected String getLabelString(double label) {
    if (map.isEmpty())
      return String.format("%.1f", label);
    else {
      var str = map.get(label);
      if (str == null)
        return "";
      else
        return str;
    }
  }
}
