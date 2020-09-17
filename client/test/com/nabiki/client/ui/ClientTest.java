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

import com.nabiki.objects.CCandle;
import com.nabiki.objects.CDepthMarketData;
import com.nabiki.objects.CombOffsetFlagType;
import com.nabiki.objects.DirectionType;
import org.junit.Test;

import java.awt.*;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ClientTest {

    @Test
    public void basic() {
        var client = new Client();
        client.start(new FigureTrader() {
            CDepthMarketData depth;
            int currentPosition = 0;

            private final List<Double> close01 = new LinkedList<>(),
                    close05 = new LinkedList<>();

            private Double average(int m, List<Double> close) {
                var rm = Math.min(m, close.size());
                if (rm < 1)
                    return null;
                else {
                    var total = 0.0D;
                    var tail = close.size() - rm;
                    for (int idx = close.size() - 1; tail <= idx; --idx)
                        total += close.get(idx);
                    return total / rm;
                }
            }

            private void open(CDepthMarketData depth) throws Exception {
                if (Math.random() > 0.5) {
                    var r = orderInsert(
                            "c2101",
                            "DCE",
                            2450,
                            1,
                            DirectionType.DIRECTION_BUY,
                            CombOffsetFlagType.OFFSET_OPEN);
                    currentPosition += 1;
                    getLogger().info("buy open");
                } else {
                    var r = orderInsert(
                            "c2101",
                            "DCE",
                            2350,
                            1,
                            DirectionType.DIRECTION_SELL,
                            CombOffsetFlagType.OFFSET_OPEN);
                    currentPosition -= 1;
                    getLogger().info("sell open");
                }
            }

            private void close(CDepthMarketData depth) throws Exception {
                if (currentPosition > 0) {
                    var r = orderInsert(
                            "c2101",
                            "DCE",
                            2350,
                            currentPosition,
                            DirectionType.DIRECTION_SELL,
                            CombOffsetFlagType.OFFSET_CLOSE);
                    currentPosition = 0;
                    getLogger().info("sell close");
                } else {
                    var r = orderInsert(
                            "c2101",
                            "DCE",
                            2450,
                            -currentPosition,
                            DirectionType.DIRECTION_BUY,
                            CombOffsetFlagType.OFFSET_CLOSE);
                    currentPosition = 0;
                    getLogger().info("buy close");
                }
            }

            @Override
            public void onStart() {
                subscribe("c2101", 1);
                setFigure(1, "c2101", 1);
                setLine(1, "ma", Color.MAGENTA);

                subscribe("c2105", 1);
                setFigure(2, "c2105", 1);
                setLine(2, "ma", Color.MAGENTA);

                setUser("0001", "1234");
            }

            @Override
            public void onDepthMarketData(CDepthMarketData depthMarketData, boolean isTrading) {
                if (!isTrading || depthMarketData.InstrumentID.compareTo("c2101") != 0)
                    return;
                depth = depthMarketData;
            }

            @Override
            public void onCandle(CCandle candle, boolean isTrading) {
                if (candle.Minute != 1)
                    return;
                if (candle.InstrumentID.compareTo("c2101") == 0) {
                    close01.add(candle.ClosePrice);
                    draw(1, "ma", average(20, close01));
                } else if (candle.InstrumentID.compareTo("c2105") == 0) {
                    close05.add(candle.ClosePrice);
                    draw(2, "ma", average(20, close05));
                }
                if (candle.InstrumentID.compareTo("c2101") == 0 && depth != null) {
                    try {
                        if (currentPosition == 0)
                            open(depth);
                        else
                            close(depth);
                    } catch (Throwable th) {
                        th.printStackTrace();
                    }
                }
            }

            @Override
            public void onStop() {
                System.out.println("----------- END ----------");
            }
        }, new InetSocketAddress("localhost", 9038));
        client.exitAt(LocalDateTime.now().plusDays(1));
    }

    @Test
    public void only_trade() {
        var client = new Client();

        System.out.println("start trader");

        client.start(new HeadlessTrader() {
            private Thread daemon;

            @Override
            public void onStart() {
                setUser("0001", "1234");
                getLogger().info("set user login");

                daemon = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TimeUnit.SECONDS.sleep(5);
                            orderInsert(
                                    "c2101",
                                    "DCE",
                                    2450,
                                    1,
                                    DirectionType.DIRECTION_BUY,
                                    CombOffsetFlagType.OFFSET_OPEN);
                            getLogger().info("buy open");
                            orderInsert(
                                    "c2101",
                                    "DCE",
                                    2350,
                                    1,
                                    DirectionType.DIRECTION_SELL,
                                    CombOffsetFlagType.OFFSET_CLOSE);
                            getLogger().info("sell close");
                            orderInsert(
                                    "c2101",
                                    "DCE",
                                    2350,
                                    1,
                                    DirectionType.DIRECTION_SELL,
                                    CombOffsetFlagType.OFFSET_OPEN);
                            getLogger().info("sell open");
                            orderInsert(
                                    "c2101",
                                    "DCE",
                                    2450,
                                    1,
                                    DirectionType.DIRECTION_BUY,
                                    CombOffsetFlagType.OFFSET_CLOSE);
                            getLogger().info("buy close");
                        } catch (Throwable th) {
                            getLogger().warning("can't trade");
                            th.printStackTrace();
                        }
                    }
                });
                daemon.start();
            }

            @Override
            public void onDepthMarketData(CDepthMarketData depthMarketData, boolean isTrading) {

            }

            @Override
            public void onCandle(CCandle candle, boolean isTrading) {

            }

            @Override
            public void onStop() {

            }
        }, new InetSocketAddress("localhost", 9038));

        System.out.println("wait trader exits");
        client.exitAt(LocalDateTime.now().plusMinutes(1));
        System.out.println("only_trade exits");
    }
}