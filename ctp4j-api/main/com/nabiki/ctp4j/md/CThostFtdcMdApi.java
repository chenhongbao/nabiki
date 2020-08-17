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

package com.nabiki.ctp4j.md;

import com.nabiki.ctp4j.jni.struct.CThostFtdcReqUserLoginField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcUserLogoutField;
import com.nabiki.ctp4j.trader.CThostFtdcTraderApi;

import java.lang.reflect.InvocationTargetException;

/**
 * Mechanism of the market data instance is like the {@link CThostFtdcTraderApi}.
 *
 * @see CThostFtdcTraderApi
 */
public abstract class CThostFtdcMdApi {
    protected CThostFtdcMdApi() {
    }

    /**
     * Create new md api instance. The flow path {@link String} must points to a
     * valid directory path, or the native code would fail.
     *
     * <p>The combination of udp and multicast parameters apply to different md
     * front:
     * </p>
     * <ul>
     * <li>TCP, udp: {@code false}, multicast: {@code false}
     * <li>UDP, udp: {@code true}, multicast: {@code false}
     * <li>Multicast, udp: {@code true}, multicast: {@code true}
     * </ul>
     *
     * @param flowPath  path to the directory where native code keeps flow data
     * @param isUsingUdp UDP or TCP
     * @param isMulticast multicast or unicast
     * @return new md api instance
     */
    public static CThostFtdcMdApi CreateFtdcMdApi(
            String flowPath, boolean isUsingUdp, boolean isMulticast) {
        try {
            var clz = Class.forName(
                    "com.nabiki.ctp4j.md.internal.CThostFtdcMdApiImpl");
            return (CThostFtdcMdApi)clz
                    .getConstructor(String.class, boolean.class, boolean.class)
                    .newInstance(flowPath, isUsingUdp, isMulticast);
        } catch (ClassNotFoundException
                | NoSuchMethodException
                | IllegalAccessException
                | InstantiationException
                | InvocationTargetException e) {
            return new com.nabiki.ctp4j.md.internal
                    .CThostFtdcMdApiImpl_0(flowPath, isUsingUdp, isMulticast);
        }
    }

    /**
     * Get API version.
     *
     * @return API version string
     */
    public abstract String GetApiVersion();

    /**
     * Get trading day in string. If the session is not logined, the method returns
     * null.
     *
     * @return trading day string
     */
    public abstract String GetTradingDay();

    /**
     * Initialize session to remote counter. It is not login yet after the method
     * returns.
     */
    public abstract void Init();

    /**
     * Wait until the session is released. The method returns after
     * {@link CThostFtdcTraderApi#Release()} is called.
     */
    public abstract void Join();

    /**
     * Register front address for the session. Client can have more than one front
     * addresses and the native client chooses randomly to connect the remote counter.
     *
     * @param frontAddress front address in the format {@code tcp://127.0.0.1:40010}
     */
    public abstract void RegisterFront(String frontAddress);

    /**
     * Register SPI for the trader session.
     *
     * @param spi callback SPI for responses
     */
    public abstract void RegisterSpi(CThostFtdcMdSpi spi);

    /**
     * Release the native resources for the trader session.
     */
    public abstract void Release();

    /**
     * Request client login for the specified session. {@code OnRspUserLogin} is
     * called on login response.
     *
     * <p>The method doesn't throw exception.
     *
     * @param reqUserLoginField login request
     * @param requestID         identifier for this request
     * @return returned value from native function
     */
    public abstract int ReqUserLogin(CThostFtdcReqUserLoginField reqUserLoginField,
                                     int requestID);

    /**
     * Request client logout for the specified session. {@code OnRspUserLogout} is
     * called on logout response.
     *
     * <p>The method doesn't throw exception.
     *
     * @param userLogout logout request
     * @param requestID  identifier for this request
     * @return returned value from native function
     */
    public abstract int ReqUserLogout(CThostFtdcUserLogoutField userLogout,
                                      int requestID);

    /**
     * Subscribe the specified instruments from ma front. The instrument count is
     * not trivial and it is required by native code.
     *
     * @param instrumentID array of instruments to subscribe
     * @param count number of instruments in the specified array
     * @return returned value from native function
     */
    public abstract int SubscribeMarketData(String[] instrumentID, int count);

    /**
     * Un-subscribe the specified instruments from ma front. The instrument count is
     * not trivial and it is required by native code.
     *
     * @param instrumentID array of instruments to subscribe
     * @param count number of instruments in the specified array
     * @return returned value from native function
     */
    public abstract int UnSubscribeMarketData(String[] instrumentID, int count);
}
