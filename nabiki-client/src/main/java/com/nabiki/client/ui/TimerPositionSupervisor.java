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

import com.nabiki.client.sdk.ResponseConsumer;
import com.nabiki.commons.ctpobj.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TimerPositionSupervisor extends TimerTask implements PositionSupervisor {
  private final Timer timer;
  private final Trader trader;
  private final Map<String, Suggestion> suggestions = new ConcurrentHashMap<>();

  public TimerPositionSupervisor(Trader t) {
    this.trader = t;
    this.timer = new Timer();
    this.timer.schedule(this, 0, TimeUnit.SECONDS.toMillis(1));
  }

  @Override
  public void suggestPosition(String instrumentID, String exchangeID, char direction, int position, double priceHigh, double priceLow) {
    var su = new Suggestion();
    su.instrumentID = instrumentID;
    su.exchangeID = exchangeID;
    su.direction = direction;
    su.position = position;
    su.posDiff = 0;
    su.priceHigh = priceHigh;
    su.priceLow = priceLow;
    su.state = SuggestionState.QryAccount;
    suggestions.put(instrumentID, su);
  }

  @Override
  public int getPosition(String instrumentID) {
    return getPosition(suggestions.get(instrumentID));
  }

  @Override
  public void run() {
    for (var s : suggestions.values()) {
      try {
        state(s);
      } catch (Exception e) {
        e.printStackTrace();
        trader.getLogger().severe("process suggestion failed: " + e.getMessage());
      }
    }
  }

  private void state(Suggestion su) throws Exception {
    switch (su.state) {
      case QryAccount:
        queryAccount(su);
        break;
      case QryPosition:
      case ConfirmCut:
        queryPosition(su);
        break;
      case OpenLong:
      case OpenShort:
        open(su);
        break;
      case CloseLong:
      case CloseShort:
        close(su);
        break;
      case CutCloseLong:
      case CutCloseShort:
        close(su);
        su.state = SuggestionState.ConfirmCut;
        break;
    }
  }

  private void open(Suggestion su) throws Exception {
    char direction;
    double price;
    switch (su.state) {
      case OpenLong:
        direction = DirectionType.DIRECTION_BUY;
        price = su.priceHigh;
        break;
      case OpenShort:
        direction = DirectionType.DIRECTION_SELL;
        price = su.priceLow;
        break;
      default:
        trader.getLogger().severe("wrong state: " + su.state);
        return;
    }
    trader.orderInsert(
        su.instrumentID,
        su.exchangeID,
        price,
        su.posDiff,
        direction,
        CombOffsetFlagType.OFFSET_OPEN);
  }

  private void close(Suggestion su) throws Exception {
    char direction;
    double price;
    switch (su.state) {
      case CloseLong:
      case CutCloseLong:
        direction = DirectionType.DIRECTION_SELL;
        price = su.priceLow;
        break;
      case CloseShort:
      case CutCloseShort:
        direction = DirectionType.DIRECTION_BUY;
        price = su.priceHigh;
        break;
      default:
        trader.getLogger().severe("wrong state: " + su.state);
        return;
    }
    trader.orderInsert(
        su.instrumentID,
        su.exchangeID,
        price,
        su.posDiff,
        direction,
        CombOffsetFlagType.OFFSET_CLOSE);
  }

  private void queryPosition(Suggestion su) throws Exception {
    trader.getPosition(su.instrumentID, "").consume(new QryPositionConsumer(su));
  }

  private void queryAccount(Suggestion su) throws Exception {
    trader.getAccount().consume(new QryAccountConsumer(su));
  }

  private int getPosition(Suggestion suggestion) {
    if (suggestion == null) {
      return 0;
    }
    int l = 0, s = 0;
    for (var p : suggestion.investorPos) {
      if (p.PosiDirection == PosiDirectionType.LONG) {
        l = p.Position;
      } else if (p.PosiDirection == PosiDirectionType.SHORT) {
        s = p.Position;
      }
    }
    return (l - s);
  }

  enum SuggestionState {
    QryAccount,
    QryPosition,
    CloseLong,
    CloseShort,
    CutCloseLong,
    CutCloseShort,
    ConfirmCut,
    OpenLong,
    OpenShort
  }

  static class Suggestion {
    String instrumentID, exchangeID;
    Character direction;
    Integer position, posDiff;
    Double priceHigh, priceLow;
    SuggestionState state;
    CTradingAccount account;
    Collection<CInvestorPosition> investorPos = new HashSet<>();
  }

  class QryAccountConsumer implements ResponseConsumer<CTradingAccount> {
    private final Suggestion suggestion;

    QryAccountConsumer(Suggestion s) {
      this.suggestion = s;
    }

    @Override
    public void accept(CTradingAccount object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        trader.getLogger().severe(String.format(
            "fail qry account[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        suggestion.account = object;
        suggestion.state = SuggestionState.QryPosition;
      }
    }
  }

  class QryPositionConsumer implements ResponseConsumer<CInvestorPosition> {
    private final Suggestion su;

    QryPositionConsumer(Suggestion s) {
      this.su = s;
    }

    private void judgePosition() {
      var p = getPosition(su);
      // Clear all position.
      if (su.position == 0) {
        if (p > 0) {
          su.posDiff = p;
          su.state = SuggestionState.CloseLong;
        } else if (p < 0) {
          su.posDiff = -p;
          su.state = SuggestionState.CloseShort;
        }
      } else {
        // No position, so just open it.
        if (p == 0) {
          su.posDiff = su.position;
          if (su.direction == PosiDirectionType.LONG) {
            su.state = SuggestionState.OpenLong;
          } else if (su.direction == PosiDirectionType.SHORT) {
            su.state = SuggestionState.OpenShort;
          }
        } else if (p > 0) {
          if (su.direction == PosiDirectionType.LONG) {
            // Current position is less than required, open more.
            if (p < su.position) {
              su.posDiff = su.position - p;
              su.state = SuggestionState.OpenLong;
            } else if (p > su.position) {
              // Current position is more than required, close some.
              su.posDiff = p - su.position;
              su.state = SuggestionState.CloseLong;
            }
          } else if (su.direction == PosiDirectionType.SHORT) {
            // Current position is not required, close all.
            su.posDiff = p;
            su.state = SuggestionState.CutCloseLong;
          }
        } else {
          var xp = -p;
          if (su.direction == PosiDirectionType.SHORT) {
            // Current position is less than required, open more.
            if (xp < su.position) {
              su.posDiff = su.position - xp;
              su.state = SuggestionState.OpenShort;
            } else if (xp > su.position) {
              // Current position is more than required, close some.
              su.posDiff = xp - su.position;
              su.state = SuggestionState.CloseShort;
            }
          } else if (su.direction == PosiDirectionType.LONG) {
            // Current position is not required, close all.
            su.posDiff = xp;
            su.state = SuggestionState.CutCloseShort;
          }
        }
      }
    }

    private void judgeCut() {
      var p = getPosition(su);
      if (p != 0) {
        return;
      }
      switch (su.state) {
        case CutCloseShort:
          su.posDiff = su.position;
          su.state = SuggestionState.OpenLong;
          break;
        case CutCloseLong:
          su.posDiff = su.position;
          su.state = SuggestionState.OpenShort;
          break;
        default:
          trader.getLogger().severe("wrong state: " + su.state);
          break;
      }
    }

    @Override
    public void accept(CInvestorPosition object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID == ErrorCodes.NONE) {
        trader.getLogger().severe(String.format(
            "fail qry position[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        su.investorPos.add(object);
      }
      if (su.investorPos.size() == totalCount) {
        if (su.state == SuggestionState.ConfirmCut) {
          judgeCut();
        } else if (su.state == SuggestionState.QryPosition) {
          judgePosition();
        }
      }
    }
  }
}
