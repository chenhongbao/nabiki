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

public class DefaultStyles {
  public static int AXIS_DEFAULT_Y_LABEL_COUNT = 7;
  public static int AXIS_DEFAULT_X_LABEL_COUNT = 10;

  public static int TEXTVIEW_FONT_BLANK_WIDTH = 15;
  public static float TEXTVIEW_FONT_SIZE = 18;
  public static float TEXTVIEW_FONT_LINESEP = 1.5F;

  public static double X_NODE_BLANK_PORTION = 0.25;

  public static Stroke GRID_DASHLINE_STROKE = new BasicStroke(
      1,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_ROUND,
      1.0f,
      new float[]{1f, 4f},
      2f);

  public static Color CANVAS_BG_COLOR = Color.WHITE;

  public static Color GRID_DASHLINE_COLOR = Color.BLUE;
  public static Color GRID_LINE_COLOR = Color.BLUE;
  public static Color AXIS_LINE_COLOR = Color.BLUE;
  public static Color AXIS_LABEL_COLOR = Color.BLUE;
  public static int AXIS_TICK_LENGTH = 5;
  public static int AXIS_X_HEIGHT = 30;
  public static int AXIS_Y_WIDTH = 50;

  public static Color STICK_UP_COLOR = Color.RED;
  public static Color STICK_DOWN_COLOR = new Color(23, 145, 45);
  public static Color STICK_FAIR_COLOR = Color.BLUE;

  public static Color BAR_UP_COLOR = Color.RED;
  public static Color BAR_DOWN_COLOR = new Color(23, 145, 45);
  public static Color BAR_FAIR_COLOR = Color.BLUE;

  public static Color FONT_UP_COLOR = Color.RED;
  public static Color FONT_DOWN_COLOR = new Color(23, 145, 45);
  public static Color FONT_FAIR_COLOR = Color.BLUE;

  public static Stroke STICK_STROKE = new BasicStroke(1.0f);

  public static int CHART_MARGIN = 20;
  public static int CHART_OFFSET = 10;

  public static int LEGEND_SINGLE_HEIGHT = 20;
  public static int LEGEND_BLANK_WIDTH = 20;
  public static int LEGEND_SAMPLE_WIDTH = 20;
  public static int LEGEND_MARGIN = 10;

  public static int DOT_WIDTH = 5;

  public static int VIEW_DEFAULT_WINSIZE = 60;
  public static int VIEW_MIN_WINSIZE = 10;
}
