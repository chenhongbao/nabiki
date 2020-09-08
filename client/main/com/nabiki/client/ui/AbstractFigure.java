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
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public abstract class AbstractFigure extends AbstractTrade implements Figure {
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

    private final ChartMainFrame main;
    private String instrumentID;
    private int minute;

    protected AbstractFigure() {
        main = new ChartMainFrame();
        main.display();
        logger.addHandler(new UILoggingHandler(main.getUIPrinter()));
        System.setOut(new UIPrintStream(main.getUIPrinter(), true));
        System.setErr(new UIPrintStream(main.getUIPrinter(), false));
    }

    @Override
    public void setLine(String name, Color color) {
        main.getChartController().createLine(name, color);
    }

    @Override
    public void setDot(String name, Color color) {
        main.getChartController().createDot(name, color);
    }

    @Override
    public void stick(double open, double high, double low, double close, String xLabel) {
        main.getChartController().append(open, high, low, close, xLabel);
    }

    @Override
    public void draw(String name, Double value) {
        main.getChartController().append(name, value);
    }

    @Override
    public String getBoundInstrumentID() {
        return instrumentID;
    }

    @Override
    public int getBoundMinute() {
        return minute;
    }

    @Override
    public void setTitle(String title) {
        if (title != null && title.length() > 0)
            main.setTitle(title);
        else
            main.setTitle("\u975E\u6CD5\u6807\u9898\u8BF7\u8054\u7CFB\u6280\u672F\u652F\u6301");
    }

    @Override
    public void bind(String instrumentID, int minute) {
        this.instrumentID = instrumentID;
        this.minute = minute;
    }

    @Override
    public Logger getLogger() {
        return logger;
    }

    @Override
    public void update() {
        this.main.getChart().getChart().paint();
        this.main.getChart().updateUI();
    }
}
