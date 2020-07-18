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

package com.nabiki.centre.md;

import com.nabiki.centre.utils.Config;
import com.nabiki.centre.utils.Utils;
import com.nabiki.ctp4j.jni.struct.CThostFtdcCandleField;
import com.nabiki.ctp4j.jni.struct.CThostFtdcDepthMarketDataField;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CandleWriter implements MarketDataReceiver {
    private final Path candleDir;
    private final Config config;
    private final Map<String, Map<Integer, File>> files = new ConcurrentHashMap<>();

    public CandleWriter(Config cfg) {
        this.config = cfg;
        this.candleDir = getPath(this.config);
    }

    private Path getPath(Config cfg) {
        var dirs = cfg.getRootDirectory().recursiveGet("dir.cdl");
        if (dirs.size() > 0)
            return dirs.iterator().next().path();
        else
            return Path.of("");
    }

    private File getFile(String instrumentID, int minute) throws IOException {
        var instrFiles = this.files.get(instrumentID);
        if (instrFiles == null) {
            instrFiles = new ConcurrentHashMap<>();
            this.files.put(instrumentID, instrFiles);
        }
        File file = instrFiles.get(minute);
        if (file == null) {
            var path = Path.of(this.candleDir.toString(),
                    instrumentID + "_" + minute + ".csv");
            Utils.createFile(path, false);
            file = path.toFile();
            instrFiles.put(minute, file);
        }
        return file;
    }

    @Override
    public void depthReceived(CThostFtdcDepthMarketDataField depth) {

    }

    @Override
    public void candleReceived(CThostFtdcCandleField candle) {
        try {
            CandleAccess.write(getFile(candle.InstrumentID, candle.Minute), candle);
        } catch (IOException e) {
            config.getLogger().warning(
                    "fail writing candle " + candle.InstrumentID
                            + "(" + candle.Minute + ")");
        }
    }
}