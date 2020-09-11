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

import com.nabiki.iop.x.OP;
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
            private final List<Double> close = new LinkedList<>();

            private Double ma10, ma20;
            private int currentPosition = 0;
            private double limitPrice = 0.0D;

            private Double average(int m) {
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

            private void open(CDepthMarketData depth) {
                if (ma10 > ma20 && depth.LastPrice <= ma20) {
                    try {
                        orderInsert(
                                "c2101",
                                "DCE",
                                depth.AskPrice1,
                                1,
                                DirectionType.DIRECTION_BUY,
                                CombOffsetFlagType.OFFSET_OPEN);
                        currentPosition = 1;
                        limitPrice = depth.AskPrice1;
                    } catch (Throwable th) {
                        getLogger().warning("fail buy open, " + th.getMessage());
                        getLogger().warning(OP.toJson(depth));
                    }
                } else if (ma10 < ma20 && depth.LastPrice >= ma20) {
                    try {
                        orderInsert(
                                "c2101",
                                "DCE",
                                depth.BidPrice1,
                                1,
                                DirectionType.DIRECTION_SELL,
                                CombOffsetFlagType.OFFSET_OPEN);
                        currentPosition = -1;
                        limitPrice = depth.BidPrice1;
                    } catch (Throwable th) {
                        getLogger().warning("fail sell open, " + th.getMessage());
                        getLogger().warning(OP.toJson(depth));
                    }
                }
            }

            private void close(CDepthMarketData depth) {
                if (currentPosition > 0 && depth.BidPrice1 > limitPrice) {
                    try {
                        orderInsert(
                                "c2101",
                                "DCE",
                                depth.BidPrice1,
                                currentPosition,
                                DirectionType.DIRECTION_SELL,
                                CombOffsetFlagType.OFFSET_CLOSE);
                        currentPosition = 0;
                        limitPrice = 0;
                    } catch (Throwable th) {
                        getLogger().warning("fail sell close, " + th.getMessage());
                        getLogger().warning(OP.toJson(depth));
                    }
                } else if (currentPosition < 0 && depth.AskPrice1 < limitPrice) {
                    try {
                        orderInsert(
                                "c2101",
                                "DCE",
                                depth.AskPrice1,
                                currentPosition,
                                DirectionType.DIRECTION_BUY,
                                CombOffsetFlagType.OFFSET_CLOSE);
                        currentPosition = 0;
                        limitPrice = 0;
                    } catch (Throwable th) {
                        getLogger().warning("fail buy close, " + th.getMessage());
                        getLogger().warning(OP.toJson(depth));
                    }
                }
            }

            @Override
            public void onStart() {
                subscribe("c2101", "c2105");
                setFigure(1, "c2101", 1);
                setLine(1, "ma-10", Color.PINK);
                setLine(1, "ma-20", Color.MAGENTA);
                setUser("0001", "1234");
            }

            @Override
            public void onDepthMarketData(CDepthMarketData depthMarketData, boolean isTrading) {
                if (!isTrading)
                    return;
                try {
                    if (currentPosition == 0)
                        open(depthMarketData);
                    else
                        close(depthMarketData);
                } catch (Throwable th) {
                    th.printStackTrace();
                    getLogger().severe(th.getMessage());
                }
            }

            @Override
            public void onCandle(CCandle candle, boolean isTrading) {
                if (candle.Minute != 1)
                    return;
                close.add(candle.ClosePrice);
                ma10 = average(10);
                ma20 = average(20);
                draw(1, "ma-10", ma10);
                draw(1, "ma-20", ma20);
                System.out.println(String.format("ma-10: %.2f, ma-20: %.2f", ma10, ma20));
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
        client.start(new HeadlessTrader() {
            private Thread daemon;

            @Override
            public void onStart() {
                setUser("0001", "1234");
                daemon = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            TimeUnit.SECONDS.sleep(5);
                            orderInsert(
                                    "c2101",
                                    "DCE",
                                    2350,
                                    1,
                                    DirectionType.DIRECTION_SELL,
                                    CombOffsetFlagType.OFFSET_CLOSE);
                            getLogger().info("all traded");
                        } catch (Throwable th) {
                            getLogger().warning("not traded");
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
        client.exitAt(LocalDateTime.now().plusDays(1));
    }
}