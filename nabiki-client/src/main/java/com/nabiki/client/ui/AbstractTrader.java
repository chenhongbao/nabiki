/*
 * Copyright (c) 2020-2020. Hongbao Chen <chenhongbao@outlook.com>
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

import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.TradeClient;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.utils.SocketLoggingHandler;
import com.nabiki.commons.utils.SystemStream;
import com.nabiki.commons.utils.Utils;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public abstract class AbstractTrader implements Trader {
  private TradeClient client;
  private String userID, password;
  private final Collection<String> subscribes = new LinkedList<>();
  private final Map<String, CInstrument> instruments = new ConcurrentHashMap<>();
  private final Map<String, CInstrumentMarginRate> margins = new ConcurrentHashMap<>();
  private final Map<String, CInstrumentCommissionRate> commissions = new ConcurrentHashMap<>();

  protected Logger logger;
  private MarketDataTraderAdaptor traderAdaptor;

  protected AbstractTrader() {
    prepare();
  }

  private void prepare() {
    try {
      setLogger();
      setStream();
      setReCache();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void setLogger() throws IOException {
    logger = Logger.getLogger(getAlgoName());
    logger.addHandler(new SimpleFileHandler());
    logger.setUseParentHandlers(false);
  }

  private void setStream() throws IOException {
    SystemStream.setErr("err.log");
    SystemStream.setOut("out.log");
  }

  private void setReCache() {
    var calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 20);
    calendar.set(Calendar.MINUTE, 59);
    calendar.set(Calendar.SECOND, 0);
    Utils.schedule(new TimerTask() {
      @Override
      public void run() {
        reCacheInfo();
      }
    }, calendar.getTime(), TimeUnit.DAYS.toMillis(1));
    calendar.set(Calendar.HOUR_OF_DAY, 8);
    Utils.schedule(new TimerTask() {
      @Override
      public void run() {
        reCacheInfo();
      }
    }, calendar.getTime(), TimeUnit.DAYS.toMillis(1));
  }

  private String getRequestID() {
    return UUID.randomUUID().toString();
  }

  protected void setDefaultAdaptor(MarketDataTraderAdaptor adaptor) {
    this.traderAdaptor = adaptor;
  }

  // Clear instrument information so they are re-queried.
  void reCacheInfo() {
    for (var i : instruments.keySet()) {
      sendQryInstrument(i);
    }
    for (var i : margins.keySet()) {
      sendQryMargin(i);
    }
    for (var i : commissions.keySet()) {
      sendQryCommission(i);
    }
  }

  private void sendQryInstrument(String i) {
    var qry = new CQryInstrument();
    qry.InstrumentID = i;
    try {
      client.queryInstrument(qry, UUID.randomUUID().toString()).consume(
          (object, rspInfo, currentCount, totalCount) -> {
            if (rspInfo.ErrorID == ErrorCodes.NONE) {
              if (object != null && object.InstrumentID != null) {
                instruments.put(object.InstrumentID, object);
              } else {
                getLogger().warning("null instrument");
              }
            } else {
              getLogger().warning(String.format(
                  "query instrument fail[%d]: %s",
                  rspInfo.ErrorID,
                  rspInfo.ErrorMsg));
            }
          });
    } catch (Exception e) {
      getLogger().warning("query instrument fail: " + e.getMessage());
    }
  }

  private void sendQryMargin(String i) {
    var qry = new CQryInstrumentMarginRate();
    qry.InstrumentID = i;
    try {
      client.queryMargin(qry, UUID.randomUUID().toString()).consume(
          (object, rspInfo, currentCount, totalCount) -> {
            if (rspInfo.ErrorID == ErrorCodes.NONE) {
              if (object != null && object.InstrumentID != null) {
                margins.put(object.InstrumentID, object);
              } else {
                getLogger().warning("null margin");
              }
            } else {
              getLogger().warning(String.format(
                  "query margin fail[%d]: %s",
                  rspInfo.ErrorID,
                  rspInfo.ErrorMsg));
            }
          });
    } catch (Exception e) {
      getLogger().warning("query margin fail: " + e.getMessage());
    }
  }

  private void sendQryCommission(String i) {
    var qry = new CQryInstrumentCommissionRate();
    qry.InstrumentID = i;
    try {
      client.queryCommission(qry, UUID.randomUUID().toString()).consume(
          (object, rspInfo, currentCount, totalCount) -> {
            if (rspInfo.ErrorID == ErrorCodes.NONE) {
              if (object != null && object.InstrumentID != null) {
                commissions.put(object.InstrumentID, object);
              } else {
                getLogger().warning("null commission");
              }
            } else {
              getLogger().warning(String.format(
                  "query commission fail[%d]: %s",
                  rspInfo.ErrorID,
                  rspInfo.ErrorMsg));
            }
          });
    } catch (Exception e) {
      getLogger().warning("query commission fail: " + e.getMessage());
    }
  }

  @Override
  public void setLoggingServer(String host, int port) {
    try {
      logger.addHandler(new SocketLoggingHandler(host, port));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  MarketDataTraderAdaptor getDefaultAdaptor() {
    return traderAdaptor;
  }

  @Override
  public void setUser(String userID, String password) {
    this.userID = userID;
    this.password = password;
  }

  @Override
  public String getUserID() {
    return userID;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public void subscribe(String instrument, int... minutes) {
    this.subscribes.add(instrument);
    traderAdaptor.setSubscribeMinute(instrument, minutes);
  }

  @Override
  public Collection<String> getSubscribe() {
    return subscribes;
  }

  @Override
  public Response<COrder> orderInsert(
      String instrumentID, String exchangeID, double price, int volume,
      char direction, char offset) throws Exception {
    var req = new CInputOrder();
    req.InstrumentID = instrumentID;
    req.ExchangeID = exchangeID;
    req.LimitPrice = price;
    req.VolumeTotalOriginal = volume;
    req.Direction = (byte) direction;
    req.CombOffsetFlag = (byte) offset;
    return this.client.orderInsert(req, getRequestID());
  }

  @Override
  public Response<CInvestorPosition> getPosition() throws Exception {
    return getPosition("", "");
  }

  @Override
  public Response<CInvestorPosition> getPosition(
      String instrumentID, String exchangeID) throws Exception {
    var qry = new CQryInvestorPosition();
    qry.ExchangeID = exchangeID;
    qry.InstrumentID = instrumentID;
    return this.client.queryPosition(qry, getRequestID());
  }

  @Override
  public Response<CTradingAccount> getAccount() throws Exception {
    return this.client.queryAccount(new CQryTradingAccount(), getRequestID());
  }

  @Override
  public Logger getLogger() {
    return this.logger;
  }

  @Override
  public TradeClient getClient() {
    return this.client;
  }

  @Override
  public void setClient(TradeClient client) {
    this.client = client;
  }

  @Override
  public CInstrument getInstrument(String instrumentID) {
    if (instruments.containsKey(instrumentID)) {
      return instruments.get(instrumentID);
    } else {
      sendQryInstrument(instrumentID);
      return null;
    }
  }

  @Override
  public CInstrumentMarginRate getMargin(String instrumentID) {
    if (margins.containsKey(instrumentID)) {
      return margins.get(instrumentID);
    } else {
      sendQryMargin(instrumentID);
      return null;
    }
  }

  @Override
  public CInstrumentCommissionRate getCommission(String instrumentID) {
    if (commissions.containsKey(instrumentID)) {
      return commissions.get(instrumentID);
    } else {
      sendQryCommission(instrumentID);
      return null;
    }
  }

  static class SimpleFileHandler extends FileHandler {
    public SimpleFileHandler() throws IOException, SecurityException {
      super("default.log", true);
      super.setFormatter(new SimpleFormatter());
    }
  }
}
