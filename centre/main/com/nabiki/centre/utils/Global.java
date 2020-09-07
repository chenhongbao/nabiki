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

package com.nabiki.centre.utils;

import com.nabiki.centre.utils.plain.InstrumentInfo;
import com.nabiki.centre.utils.plain.LoginConfig;
import com.nabiki.iop.x.PerformanceMeasure;
import com.nabiki.objects.CDepthMarketData;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class Global {
    // Global's name -> LoginConfig
    final Map<String, LoginConfig> login = new ConcurrentHashMap<>();
    // ProductID -> TradingHourKeeper
    final Map<String, TradingHourKeeper> tradingHour = new ConcurrentHashMap<>();
    // Instrument ID -> TradingHourKeeper
    final Map<String, TradingHourKeeper> instrTradingHour = new ConcurrentHashMap<>();
    // Instrument ID -> InstrumentInfo
    final Map<String, InstrumentInfo> instrInfo = new ConcurrentHashMap<>();
    // Instrument ID -> Depth market data
    final Map<String, CDepthMarketData> depths = new ConcurrentHashMap<>();
    // Product ID -> Instrument ID of the product
    final Map<String, Set<String>> products = new ConcurrentHashMap<>();
    // Commandline arguments: prefix -> argument
    final Map<String, String> args = new ConcurrentHashMap<>();

    final Duration[] durations = new Duration[] {
            Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15),
            Duration.ofMinutes(30), Duration.ofHours(1), Duration.ofHours(2),
            Duration.ofHours(24)
    };

    // Constants
    public static final String CMD_ROOT_PREFIX = "--root";
    public static final String CMD_HOST_PREFIX = "--host";
    public static final String CMD_PORT_PREFIX = "--port";
    public static final String CMD_START_NOW_PREFIX = "--start-now";

    static Logger logger;
    PerformanceMeasure performanceMeasure;
    String tradingDay;
    EasyFile rootDirectory;

    Global() {
    }

    /**
     * Get global performance measure.
     *
     * @return global performance measure.
     */
    public PerformanceMeasure getPerformanceMeasure() {
        return this.performanceMeasure;
    }

    /**
     * Get login configurations.
     *
     * @return {@link Map} of {@link LoginConfig#Name} and {@link LoginConfig}
     */
    public Map<String, LoginConfig> getLoginConfigs() {
        return this.login;
    }

    /**
     * Get trading hour configuration of the specified product ID and instrument ID.
     * The method first gets the configuration with the specified product ID and
     * returns the result. If the product ID is null, then the method tries with the
     * instrument ID.
     *
     * @param proID product ID, if {@code null} the method turns to instrument ID
     * @param instrID instrument ID
     * @return trading hour configuration
     */
    public TradingHourKeeper getTradingHour(String proID, String instrID) {
        TradingHourKeeper keeper = null;
        if (proID != null)
            keeper = this.tradingHour.get(proID);
        if (keeper == null && instrID != null) {
            keeper = this.instrTradingHour.get(instrID);
            if (keeper == null) {
                var pid = Utils.getProductID(instrID);
                keeper = this.tradingHour.get(pid);
                // Cache instrument id mapping to hour keeper, so it doesn't need to
                // extract product id at next call.
                // Just go instrument id mapping.
                if (keeper != null)
                    this.instrTradingHour.put(instrID, keeper);
            }
        }
        return keeper;
    }

    /**
     * Get all trading hours.
     *
     * @return mapping of all trading hours
     */
    public Map<String, TradingHourKeeper> getAllTradingHour() {
        return this.tradingHour;
    }

    /**
     * Get root directory object.
     *
     * @return root directory object
     */
    public EasyFile getRootDirectory() {
        return this.rootDirectory;
    }

    /**
     * Get the specified instrument's information containing instrument, commission
     * and margin. If the instrument ID doesn't exist, return {@code null}.
     *
     * @param instrID instrument ID
     * @return instrument's information
     */
    public InstrumentInfo getInstrInfo(String instrID) {
        return this.instrInfo.get(instrID);
    }

    /**
     * Get all instrument info.
     *
     * @return all instrument info.
     */
    public Collection<InstrumentInfo> getAllInstrInfo() {
        return this.instrInfo.values();
    }

    /**
     * Get today's trading day. If the value is not available, usually not login,
     * return {@code null}.
     *
     * @return trading day
     */
    public String getTradingDay() {
        return this.tradingDay;
    }

    /**
     * Get the shared logger in this application.
     *
     * @return {@link Logger} with default setting
     */
    public Logger getLogger() {
        return Global.logger;
    }

    /**
     * Get the latest depth market data.
     *
     * @param instr instrument ID
     * @return {@link CDepthMarketData} or {@code null} if not found
     */
    public CDepthMarketData getDepthMarketData(String instr) {
        return this.depths.get(instr);
    }

    /**
     * Get all instrument IDs under the specified product.
     *
     * @param productID product ID
     * @return instrument IDs of the specified product
     */
    public Set<String> getProduct(String productID) {
        return this.products.get(productID);
    }

    /**
     * Get candle durations.
     *
     * @return candle durations
     */
    public Duration[] getDurations() {
        return this.durations;
    }

    /**
     * Get command line argument with prefix.
     * @param prefix prefix of the argument
     * @return argument string
     */
    public String getArgument(String prefix) {
        return this.args.get(prefix);
    }
}
