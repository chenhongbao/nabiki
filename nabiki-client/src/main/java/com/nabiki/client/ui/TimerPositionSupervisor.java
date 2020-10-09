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
import com.nabiki.commons.iop.x.OP;

import java.util.HashSet;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class TimerPositionSupervisor extends TimerTask implements PositionSupervisor {
  private final Trader trader;
  private final SuggestionListener listener;
  private Suggestion su;

  public TimerPositionSupervisor(Trader t) {
    trader = t;
    listener = s -> {};
    OP.schedule(this, TimeUnit.SECONDS.toMillis(1));
  }

  public TimerPositionSupervisor(Trader t, SuggestionListener sl) {
    trader = t;
    listener = sl;
    OP.schedule(this, TimeUnit.SECONDS.toMillis(1));
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
    return getPosition(su);
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
        su.setInvestorPos(null);
        break;
      case CloseLong:
      case CloseShort:
        close(su);
        su.setState(SuggestionState.Confirm);
        su.setInvestorPos(null);
        break;
      case CutCloseLong:
      case CutCloseShort:
        close(su);
        su.setState(SuggestionState.ConfirmCut);
        su.setInvestorPos(null);
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

  private void queryPosition(Suggestion su) throws Exception {
    if (su.getInvestorPos() == null) {
      su.setInvestorPos(new HashSet<>());
      trader.getPosition(su.getInstrumentID(), "").consume(new QryPositionConsumer(su));
    }
  }

  private void queryAccount(Suggestion su) throws Exception {
    if (su.getAccount() == null) {
      su.setAccount(new CTradingAccount());
      trader.getAccount().consume(new QryAccountConsumer(su));
    }
  }

  private int getPosition(Suggestion suggestion) {
    if (suggestion == null || suggestion.getInvestorPos() == null) {
      return 0;
    }
    int l = 0, s = 0;
    for (var p : suggestion.getInvestorPos()) {
      if (p.PosiDirection == PosiDirectionType.LONG) {
        l = p.Position;
      } else if (p.PosiDirection == PosiDirectionType.SHORT) {
        s = p.Position;
      }
    }
    return (l - s);
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
        suggestion.setAccount(object);
        suggestion.setState(SuggestionState.QryPosition);
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
    }

    private void judgeCut() {
      var p = getPosition(su);
      if (p != 0) {
        return;
      }
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
    }

    private void judgeCompleted() {
      var p = getPosition(su);
      if (su.getDirection() == PosiDirectionType.LONG && p == su.getPosition()) {
        su.setState(SuggestionState.Completed);
      } else if (su.getDirection() == PosiDirectionType.SHORT && -p == su.getPosition()) {
        su.setState(SuggestionState.Completed);
      }
    }

    @Override
    public void accept(CInvestorPosition object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        trader.getLogger().severe(String.format(
            "fail qry position[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        su.getInvestorPos().add(object);
      }
      if (su.getInvestorPos().size() == totalCount) {
        if (su.getState() == SuggestionState.Confirm) {
          judgeCompleted();
        } else if (su.getState() == SuggestionState.ConfirmCut) {
          judgeCut();
        } else if (su.getState() == SuggestionState.QryPosition) {
          judgePosition();
        }
      }
    }
  }
}
