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

import java.awt.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
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

    private final Map<Integer, ChartMainFrame> mains = new ConcurrentHashMap<>();
    private final LogDialog logDlg = new LogDialog();

    protected AbstractFigure() {
        logger.addHandler(new UILoggingHandler(logDlg));
        System.setOut(new UIPrintStream(logDlg, true));
        System.setErr(new UIPrintStream(logDlg, false));
    }

    private ChartMainFrame getFrame(int figureID) {
        var frame = mains.get(figureID);
        if (frame == null)
            throw new RuntimeException("no such figure ID: " + figureID);
        else
            return frame;
    }

    private void display(ChartMainFrame frame) {
        EventQueue.invokeLater(() -> {
            try {
                frame.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void setLine(int figureID, String name, Color color) {
        getFrame(figureID).getChartController().createLine(name, color);
    }

    @Override
    public void setDot(int figureID, String name, Color color) {
        getFrame(figureID).getChartController().createDot(name, color);
    }

    @Override
    public void stick(int figureID, double open, double high, double low, double close, String xLabel) {
        getFrame(figureID).getChartController().append(open, high, low, close, xLabel);
    }

    @Override
    public void draw(int figureID, String name, Double value) {
        getFrame(figureID).getChartController().append(name, value);
    }

    @Override
    public String getBoundInstrumentID(int figureID) {
        return getFrame(figureID).getInstrumentID();
    }

    @Override
    public int getBoundMinute(int figureID) {
        return getFrame(figureID).getMinute();
    }

    @Override
    public void setTitle(int figureID, String title) {
        if (title != null && title.length() > 0)
            getFrame(figureID).setTitle(title);
        else
            getFrame(figureID).setTitle("\u975E\u6CD5\u6807\u9898\u8BF7\u8054\u7CFB\u6280\u672F\u652F\u6301");
    }

    @Override
    public void setFigure(int figureID, String instrumentID, int minute) {
        if (mains.containsKey(figureID))
            throw new RuntimeException("figure(" + figureID + ") already exists");
        var frame = new ChartMainFrame(logDlg);
        frame.setInstrumentID(instrumentID);
        frame.setMinute(minute);
        display(frame);
        mains.put(figureID, frame);
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void update(int figureID) {
        getFrame(figureID).getChartController().update();
    }

    @Override
    public Set<Integer> getFigureID() {
        return mains.keySet();
    }

    @Override
    public void dispose(int figureID) {
        var frame = getFrame(figureID);
        frame.setVisible(false);
        frame.dispose();
    }
}
