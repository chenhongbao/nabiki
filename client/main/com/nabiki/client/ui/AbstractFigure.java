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

package com.nabiki.client.ui;

import com.nabiki.chart.control.BarChartController;
import com.nabiki.chart.control.BarChartPanel;
import com.nabiki.chart.control.StickChartController;
import com.nabiki.chart.control.StickChartPanel;

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public abstract class AbstractFigure extends AbstractTrader implements Figure {
  static class UILoggingHandler extends Handler {
    private final UIPrinter printer;

    UILoggingHandler(UIPrinter printer) {
      this.printer = printer;
    }

    @Override
    public void publish(LogRecord record) {
      printer.appendLog(record);
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
    }
  }

  private final Map<Integer, ChartMainFrame> stickFrames = new ConcurrentHashMap<>();
  private final Map<Integer, ChartMainFrame> barFrames = new ConcurrentHashMap<>();
  private final LogDialog logDlg = new LogDialog();
  private final AtomicBoolean updated = new AtomicBoolean(false);

  protected AbstractFigure() {
    prepareStd();
    prepareTimer();
  }

  private void prepareStd() {
    logger.addHandler(new UILoggingHandler(logDlg));
    System.setOut(new UIPrintStream(logDlg, true));
    System.setErr(new UIPrintStream(logDlg, false));
  }

  private void prepareTimer() {
    Timer figureTimer = new Timer();
    figureTimer.schedule(
        new TimerTask() {
          @Override
          public void run() {
            synchronized (updated) {
              if (updated.get()) {
                for (var fid : getFigureID()) {
                  update(fid);
                }
                updated.set(false);
              }
            }
          }
        }, 500, 500);
  }

  private ChartMainFrame getStickFrame(int figureID) {
    return stickFrames.get(figureID);
  }

  private ChartMainFrame getBarFrame(int figureID) {
    return barFrames.get(figureID);
  }

  private void checkZeroValue(int figureID, String name, double value) {
    if (value == 0.0D)
      System.err.printf(
          "figure %d, plot %s, append zero value, possibly error%n",
          figureID, name);
  }

  private void checkFigureID(int id) {
    if (stickFrames.containsKey(id) || barFrames.containsKey(id)) {
      throw new RuntimeException("figure(" + id + ") already exists");
    }
  }

  @Override
  public void setLine(int figureID, String name, Color color) {
    var frame = getStickFrame(figureID);
    if (frame != null) {
      ((StickChartController) frame.getChartController()).createLine(name, color);
      return;
    }
    frame = getBarFrame(figureID);
    if (frame != null) {
      ((BarChartController) frame.getChartController()).createLine(name, color);
      return;
    }
    throw new IllegalArgumentException("figure(" + figureID + ") not found");
  }

  @Override
  public void setDot(int figureID, String name, Color color) {
    var frame = getStickFrame(figureID);
    if (frame != null) {
      ((StickChartController) frame.getChartController()).createDot(name, color);
      return;
    }
    frame = getBarFrame(figureID);
    if (frame != null) {
      ((BarChartController) frame.getChartController()).createDot(name, color);
      return;
    }
    throw new IllegalArgumentException("figure(" + figureID + ") not found");
  }

  @Override
  public void stick(int figureID, double open, double high, double low, double close, String xLabel) {
    checkZeroValue(figureID, "stick", open * high * low * close);
    synchronized (updated) {
      var frame = getStickFrame(figureID);
      if (frame != null) {
        ((StickChartController) frame.getChartController()).append(open, high, low, close, xLabel);
        updated.set(true);
      }
      throw new IllegalArgumentException("figure(" + figureID + ") not found");
    }
  }

  @Override
  public void draw(int figureID, String name, Double value) {
    checkZeroValue(figureID, name, value);
    synchronized (updated) {
      var frame = getStickFrame(figureID);
      if (frame != null) {
        ((StickChartController) frame.getChartController()).append(name, value);
        updated.set(true);
        return;
      }
      frame = getBarFrame(figureID);
      if (frame != null) {
        ((BarChartController) frame.getChartController()).append(name, value);
        updated.set(true);
        return;
      }
      throw new IllegalArgumentException("figure(" + figureID + ") not found");
    }
  }

  @Override
  public String getBoundInstrumentID(int figureID) {
    return getStickFrame(figureID).getInstrumentID();
  }

  @Override
  public int getBoundMinute(int figureID) {
    return getStickFrame(figureID).getMinute();
  }

  @Override
  public void setTitle(int figureID, String title) {
    if (title != null && title.length() > 0)
      getStickFrame(figureID).setTitle(title);
    else
      getStickFrame(figureID).setTitle("\u975E\u6CD5\u6807\u9898\u8BF7\u8054\u7CFB\u6280\u672F\u652F\u6301");
  }

  @Override
  public void setFigure(int figureID, String instrumentID, int minute) {
    checkFigureID(figureID);
    var frame = new ChartMainFrame(logDlg, new StickChartPanel());
    frame.setInstrumentID(instrumentID);
    frame.setMinute(minute);
    frame.setTitle(instrumentID + " - " + minute + "m");
    frame.setVisible(true);
    stickFrames.put(figureID, frame);
  }

  @Override
  public void setBarFigure(int figureID, String instrumentID, int minute) {
    checkFigureID(figureID);
    var frame = new ChartMainFrame(logDlg, new BarChartPanel());
    frame.setInstrumentID(instrumentID);
    frame.setMinute(minute);
    frame.setTitle(instrumentID + " - " + minute + "m");
    frame.setVisible(true);
    barFrames.put(figureID, frame);
  }

  @Override
  public Logger getLogger() {
    return logger;
  }

  @Override
  public void update(int figureID) {
    var ctrl = getStickFrame(figureID).getChartController();
    if (ctrl.getDataCount() > 0)
      ctrl.update();
  }

  @Override
  public Set<Integer> getFigureID() {
    return stickFrames.keySet();
  }

  @Override
  public void dispose(int figureID) {
    var frame = getStickFrame(figureID);
    frame.setVisible(false);
    frame.dispose();
  }
}
