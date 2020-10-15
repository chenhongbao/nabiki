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

import com.nabiki.client.sdk.ResponseConsumer;
import com.nabiki.commons.ctpobj.*;
import com.nabiki.commons.utils.Utils;

import java.util.Collection;
import java.util.LinkedList;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TimerPositionSupervisor extends TimerTask implements PositionSupervisor {
  private final Trader trader;
  private final SuggestionListener listener;

  private final int DEFAULT_TO = 3;
  private final TimeUnit DEFAULT_TO_UNIT = TimeUnit.SECONDS;

  private Suggestion su;
  private long lastQryTimeStamp = 0;

  public TimerPositionSupervisor(Trader t) {
    trader = t;
    listener = s -> {
    };
    Utils.schedule(this, TimeUnit.SECONDS.toMillis(1));
  }

  public TimerPositionSupervisor(Trader t, SuggestionListener sl) {
    trader = t;
    listener = sl;
    Utils.schedule(this, TimeUnit.SECONDS.toMillis(1));
  }

  @Override
  public void suggestPosition(String instrumentID, String exchangeID, char direction, int position, double priceHigh, double priceLow) {
    if (su != null && su.getState() != SuggestionState.Completed) {
      throw new RuntimeException("last suggestion not completed");
    } else {
      su = new Suggestion(listener);
      su.setInstrumentID(instrumentID);
      su.setExchangeID(exchangeID);
      su.setDirection(direction);
      su.setPosition(Math.abs(position));
      su.setPosDiff(0);
      su.setPriceHigh(priceHigh);
      su.setPriceLow(priceLow);
      su.setState(SuggestionState.QryAccount);
    }
  }

  @Override
  public int getPosition(String instrumentID) {
    return getPosition(su.getPositions());
  }

  @Override
  public void run() {
    try {
      if (su != null) {
        state(su);
      }
    } catch (Exception e) {
      e.printStackTrace();
      trader.getLogger().severe("process suggestion failed: " + e.getMessage());
    }
  }

  private void state(Suggestion su) throws Exception {
    switch (su.getState()) {
      case QryAccount:
        queryAccount(su);
        break;
      case QryPosition:
      case ConfirmCut:
      case Confirm:
        queryPosition(su);
        break;
      case OpenLong:
      case OpenShort:
        open(su);
        su.setState(SuggestionState.Confirm);
        break;
      case CloseLong:
      case CloseShort:
        close(su);
        su.setState(SuggestionState.Confirm);
        break;
      case CutCloseLong:
      case CutCloseShort:
        close(su);
        su.setState(SuggestionState.ConfirmCut);
        break;
      default:
        break;
    }
  }

  private void open(Suggestion su) throws Exception {
    char direction;
    double price;
    switch (su.getState()) {
      case OpenLong:
        direction = DirectionType.DIRECTION_BUY;
        price = su.getPriceHigh();
        break;
      case OpenShort:
        direction = DirectionType.DIRECTION_SELL;
        price = su.getPriceLow();
        break;
      default:
        trader.getLogger().severe("wrong state: " + su.getState());
        return;
    }
    trader.orderInsert(
        su.getInstrumentID(),
        su.getExchangeID(),
        price,
        su.getPosDiff(),
        direction,
        CombOffsetFlagType.OFFSET_OPEN);
  }

  private void close(Suggestion su) throws Exception {
    char direction;
    double price;
    switch (su.getState()) {
      case CloseLong:
      case CutCloseLong:
        direction = DirectionType.DIRECTION_SELL;
        price = su.getPriceLow();
        break;
      case CloseShort:
      case CutCloseShort:
        direction = DirectionType.DIRECTION_BUY;
        price = su.getPriceHigh();
        break;
      default:
        trader.getLogger().severe("wrong state: " + su.getState());
        return;
    }
    trader.orderInsert(
        su.getInstrumentID(),
        su.getExchangeID(),
        price,
        su.getPosDiff(),
        direction,
        CombOffsetFlagType.OFFSET_CLOSE);
  }

  private boolean checkRspTimeout() {
    return System.currentTimeMillis() - lastQryTimeStamp >= DEFAULT_TO_UNIT.toMillis(DEFAULT_TO);
  }

  private void queryPosition(Suggestion su) throws Exception {
    if (!su.isQueryingPosition() || checkRspTimeout()) {
      lastQryTimeStamp = System.currentTimeMillis();
      su.setQueryingPosition(true);
      trader.getPosition(su.getInstrumentID(), "").consume(new QryPositionConsumer(su));
    }
  }

  private void queryAccount(Suggestion su) throws Exception {
    if (!su.isQueryingAccount() || checkRspTimeout()) {
      lastQryTimeStamp = System.currentTimeMillis();
      su.setQueryingAccount(true);
      trader.getAccount().consume(new QryAccountConsumer(su));
    }
  }

  private int getPosition(Collection<CInvestorPosition> positions) {
    if (positions == null || positions.isEmpty()) {
      return 0;
    }
    int l = 0, s = 0;
    for (var p : positions) {
      if (p.PosiDirection == PosiDirectionType.LONG) {
        l = p.Position;
      } else if (p.PosiDirection == PosiDirectionType.SHORT) {
        s = p.Position;
      }
    }
    return (l - s);
  }

  class QryAccountConsumer implements ResponseConsumer<CTradingAccount> {
    private final Suggestion su;

    QryAccountConsumer(Suggestion s) {
      this.su = s;
    }

    @Override
    public void accept(CTradingAccount object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        trader.getLogger().severe(String.format(
            "fail qry account[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        su.setAccount(object);
        // Complete query, reset.
        su.setQueryingAccount(false);
        su.setState(SuggestionState.QryPosition);
      }
    }
  }

  class QryPositionConsumer implements ResponseConsumer<CInvestorPosition> {
    private final Suggestion su;
    private final Collection<CInvestorPosition> positions = new LinkedList<>();

    QryPositionConsumer(Suggestion s) {
      this.su = s;
    }

    private void judgePosition() {
      var p = getPosition(positions);
      // Clear all position.
      if (su.getPosition() == 0) {
        if (p > 0) {
          su.setPosDiff(p);
          su.setState(SuggestionState.CloseLong);
        } else if (p < 0) {
          su.setPosDiff(-p);
          su.setState(SuggestionState.CloseShort);
        } else {
          su.setPosDiff(0);
          su.setState(SuggestionState.Completed);
        }
      } else {
        // No position, so just open it.
        if (p == 0) {
          su.setPosDiff(su.getPosition());
          if (su.getDirection() == PosiDirectionType.LONG) {
            su.setState(SuggestionState.OpenLong);
          } else if (su.getDirection() == PosiDirectionType.SHORT) {
            su.setState(SuggestionState.OpenShort);
          }
        } else if (p > 0) {
          if (su.getDirection() == PosiDirectionType.LONG) {
            // Current position is less than required, open more.
            if (p < su.getPosition()) {
              su.setPosDiff(su.getPosition() - p);
              su.setState(SuggestionState.OpenLong);
            } else if (p > su.getPosition()) {
              // Current position is more than required, close some.
              su.setPosDiff(p - su.getPosition());
              su.setState(SuggestionState.CloseLong);
            } else {
              su.setPosDiff(0);
              su.setState(SuggestionState.Completed);
            }
          } else if (su.getDirection() == PosiDirectionType.SHORT) {
            // Current position is not required, close all.
            su.setPosDiff(p);
            su.setState(SuggestionState.CutCloseLong);
          }
        } else {
          var xp = -p;
          if (su.getDirection() == PosiDirectionType.SHORT) {
            // Current position is less than required, open more.
            if (xp < su.getPosition()) {
              su.setPosDiff(su.getPosition() - xp);
              su.setState(SuggestionState.OpenShort);
            } else if (xp > su.getPosition()) {
              // Current position is more than required, close some.
              su.setPosDiff(xp - su.getPosition());
              su.setState(SuggestionState.CloseShort);
            } else {
              su.setPosDiff(0);
              su.setState(SuggestionState.Completed);
            }
          } else if (su.getDirection() == PosiDirectionType.LONG) {
            // Current position is not required, close all.
            su.setPosDiff(xp);
            su.setState(SuggestionState.CutCloseShort);
          }
        }
      }
      if (su.getState() == SuggestionState.Completed) {
        su.setPositions(positions);
      }
    }

    private void judgeCut() {
      if (0 == getPosition(positions)) {
        switch (su.getDirection()) {
          case PosiDirectionType.LONG:
            su.setPosDiff(su.getPosition());
            su.setState(SuggestionState.OpenLong);
            break;
          case PosiDirectionType.SHORT:
            su.setPosDiff(su.getPosition());
            su.setState(SuggestionState.OpenShort);
            break;
          default:
            trader.getLogger().severe("wrong direction: " + su.getState());
            break;
        }
        // No need set position in suggestion because it is ZERO position.
      }
    }

    private void judgeCompleted() {
      var p = getPosition(positions);
      var c0 = su.getDirection() == PosiDirectionType.LONG && p == su.getPosition();
      var c1 = su.getDirection() == PosiDirectionType.SHORT && -p == su.getPosition();
      if (c0 || c1) {
        su.setState(SuggestionState.Completed);
        su.setPositions(positions);
      }
    }

    @Override
    public void accept(CInvestorPosition object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        trader.getLogger().severe(String.format(
            "fail qry position[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        positions.add(object);
      }
      if (positions.size() == totalCount) {
        if (su.getState() == SuggestionState.Confirm) {
          judgeCompleted();
        } else if (su.getState() == SuggestionState.ConfirmCut) {
          judgeCut();
        } else if (su.getState() == SuggestionState.QryPosition) {
          judgePosition();
        }
        // Complete query, reset.
        su.setQueryingPosition(false);
      }
    }
  }
}
