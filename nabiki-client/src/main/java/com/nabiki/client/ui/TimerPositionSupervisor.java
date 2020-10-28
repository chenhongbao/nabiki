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
  private final PositionListener listener;

  private final int DEFAULT_TO = 3;
  private final TimeUnit DEFAULT_TO_UNIT = TimeUnit.SECONDS;

  private PositionExecution su;
  private long lastQryTimeStamp = 0;

  TimerPositionSupervisor(Trader t) {
    trader = t;
    listener = s -> {
    };
    Utils.schedule(this, TimeUnit.SECONDS.toMillis(1));
  }

  TimerPositionSupervisor(Trader t, PositionListener sl) {
    trader = t;
    listener = sl;
    Utils.schedule(this, TimeUnit.SECONDS.toMillis(1));
  }

  void tellMarketClose() {
    if (!isCompleted()) {
      su.setState(PositionExecState.Canceled);
      // If market closes but previous execution not completed, set null to force it
      // complete. Its order state in server will be cleared in settlement.
      su = null;
    }
  }

  @Override
  public void executePosition(
      String instrumentID,
      String exchangeID,
      char posiDirection,
      int position,
      double priceHigh,
      double priceLow) throws Exception {
    if (!isCompleted()) {
      throw new Exception("last execution not completed");
    } else {
      su = new PositionExecution(listener);
      su.setInstrumentID(instrumentID);
      su.setExchangeID(exchangeID);
      su.setPosiDirection(posiDirection);
      su.setPosition(Math.abs(position));
      su.setPosiDiff(0);
      su.setPriceHigh(priceHigh);
      su.setPriceLow(priceLow);
      su.setState(PositionExecState.QryAccount);
    }
  }

  @Override
  public int getPosition() {
    return getPosition(su.getPositions());
  }

  @Override
  public int getPosition(char posiDirection) {
    if (!isCompleted()) {
      throw new IllegalStateException("position not ready");
    }
    return getPosition(posiDirection, su.getPositions());
  }

  @Override
  public boolean isCompleted() {
    return su == null ||
        su.getState() == PositionExecState.Completed || su.getState() == PositionExecState.Canceled;
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

  private void state(PositionExecution su) throws Exception {
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
        su.setState(PositionExecState.Confirm);
        break;
      case CloseLong:
      case CloseShort:
        close(su);
        su.setState(PositionExecState.Confirm);
        break;
      case CutCloseLong:
      case CutCloseShort:
        close(su);
        su.setState(PositionExecState.ConfirmCut);
        break;
      default:
        break;
    }
  }

  private void open(PositionExecution su) throws Exception {
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
        su.getPosiDiff(),
        direction,
        CombOffsetFlagType.OFFSET_OPEN);
  }

  private void close(PositionExecution su) throws Exception {
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
        su.getPosiDiff(),
        direction,
        CombOffsetFlagType.OFFSET_CLOSE);
  }

  // If error occurs during a qry. the data won't be all ready, so qry won't be
  // completed. Then rsp timeout, and re-send qry.
  private boolean checkRspTimeout() {
    return System.currentTimeMillis() - lastQryTimeStamp >= DEFAULT_TO_UNIT.toMillis(DEFAULT_TO);
  }

  private void queryPosition(PositionExecution su) throws Exception {
    if (!su.isQueryingPosition() || checkRspTimeout()) {
      lastQryTimeStamp = System.currentTimeMillis();
      su.setQueryingPosition(true);
      trader.getPosition(su.getInstrumentID(), "").consume(new QryPositionConsumer(su));
    }
  }

  private void queryAccount(PositionExecution su) throws Exception {
    if (!su.isQueryingAccount() || checkRspTimeout()) {
      lastQryTimeStamp = System.currentTimeMillis();
      su.setQueryingAccount(true);
      trader.getAccount().consume(new QryAccountConsumer(su));
    }
  }

  private int getPosition(char posiDirection, Collection<CInvestorPosition> positions) {
    if (positions == null || positions.isEmpty()) {
      return 0;
    }
    int l = 0, s = 0;
    for (var p : positions) {
      if (p.PosiDirection == PosiDirectionType.LONG) {
        l += p.Position;
      } else if (p.PosiDirection == PosiDirectionType.SHORT) {
        s += p.Position;
      }
    }
    if (PosiDirectionType.NET == (byte) posiDirection) {
      return (l - s);
    } else if (PosiDirectionType.LONG == (byte) posiDirection) {
      return l;
    } else if (PosiDirectionType.SHORT == (byte) posiDirection) {
      return s;
    } else {
      throw new IllegalArgumentException("unknown posi-direction: " + posiDirection);
    }
  }

  private int getPosition(Collection<CInvestorPosition> positions) {
    return getPosition(PosiDirectionType.NET, positions);
  }

  class QryAccountConsumer implements ResponseConsumer<CTradingAccount> {
    private final PositionExecution su;

    QryAccountConsumer(PositionExecution s) {
      this.su = s;
    }

    @Override
    public void accept(CTradingAccount object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        // Error, the qry won't be completed any more, trigger timeout and re-qry.
        trader.getLogger().severe(String.format(
            "fail qry account[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        su.setAccount(object);
        // Complete query, reset.
        su.setQueryingAccount(false);
        su.setState(PositionExecState.QryPosition);
      }
    }
  }

  class QryPositionConsumer implements ResponseConsumer<CInvestorPosition> {
    private final PositionExecution su;
    private final Collection<CInvestorPosition> positions = new LinkedList<>();

    QryPositionConsumer(PositionExecution s) {
      this.su = s;
    }

    private void judgePosition() {
      var p = getPosition(positions);
      // Clear all position.
      if (su.getPosition() == 0) {
        if (p > 0) {
          su.setPosiDiff(p);
          su.setState(PositionExecState.CloseLong);
        } else if (p < 0) {
          su.setPosiDiff(-p);
          su.setState(PositionExecState.CloseShort);
        } else {
          su.setPosiDiff(0);
          su.setState(PositionExecState.Completed);
        }
      } else {
        // No position, so just open it.
        if (p == 0) {
          su.setPosiDiff(su.getPosition());
          if (su.getPosiDirection() == PosiDirectionType.LONG) {
            su.setState(PositionExecState.OpenLong);
          } else if (su.getPosiDirection() == PosiDirectionType.SHORT) {
            su.setState(PositionExecState.OpenShort);
          }
        } else if (p > 0) {
          if (su.getPosiDirection() == PosiDirectionType.LONG) {
            // Current position is less than required, open more.
            if (p < su.getPosition()) {
              su.setPosiDiff(su.getPosition() - p);
              su.setState(PositionExecState.OpenLong);
            } else if (p > su.getPosition()) {
              // Current position is more than required, close some.
              su.setPosiDiff(p - su.getPosition());
              su.setState(PositionExecState.CloseLong);
            } else {
              su.setPosiDiff(0);
              su.setState(PositionExecState.Completed);
            }
          } else if (su.getPosiDirection() == PosiDirectionType.SHORT) {
            // Current position is not required, close all.
            su.setPosiDiff(p);
            su.setState(PositionExecState.CutCloseLong);
          }
        } else {
          var xp = -p;
          if (su.getPosiDirection() == PosiDirectionType.SHORT) {
            // Current position is less than required, open more.
            if (xp < su.getPosition()) {
              su.setPosiDiff(su.getPosition() - xp);
              su.setState(PositionExecState.OpenShort);
            } else if (xp > su.getPosition()) {
              // Current position is more than required, close some.
              su.setPosiDiff(xp - su.getPosition());
              su.setState(PositionExecState.CloseShort);
            } else {
              su.setPosiDiff(0);
              su.setState(PositionExecState.Completed);
            }
          } else if (su.getPosiDirection() == PosiDirectionType.LONG) {
            // Current position is not required, close all.
            su.setPosiDiff(xp);
            su.setState(PositionExecState.CutCloseShort);
          }
        }
      }
      if (su.getState() == PositionExecState.Completed) {
        su.setPositions(positions);
      }
    }

    private void judgeCut() {
      if (0 == getPosition(positions)) {
        switch (su.getPosiDirection()) {
          case PosiDirectionType.LONG:
            su.setPosiDiff(su.getPosition());
            su.setState(PositionExecState.OpenLong);
            break;
          case PosiDirectionType.SHORT:
            su.setPosiDiff(su.getPosition());
            su.setState(PositionExecState.OpenShort);
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
      var c0 = su.getPosiDirection() == PosiDirectionType.LONG && p == su.getPosition();
      var c1 = su.getPosiDirection() == PosiDirectionType.SHORT && -p == su.getPosition();
      if (c0 || c1) {
        su.setState(PositionExecState.Completed);
        su.setPositions(positions);
      }
    }

    @Override
    public void accept(CInvestorPosition object, CRspInfo rspInfo, int currentCount, int totalCount) {
      if (rspInfo.ErrorID != ErrorCodes.NONE) {
        // Error, the qry won't be completed any more, trigger timeout and re-qry.
        trader.getLogger().severe(String.format(
            "fail qry position[%d]: %s", rspInfo.ErrorID, rspInfo.ErrorMsg));
      } else {
        positions.add(object);
      }
      if (positions.size() == totalCount) {
        if (su.getState() == PositionExecState.Confirm) {
          judgeCompleted();
        } else if (su.getState() == PositionExecState.ConfirmCut) {
          judgeCut();
        } else if (su.getState() == PositionExecState.QryPosition) {
          judgePosition();
        }
        // Complete query, reset.
        su.setQueryingPosition(false);
      }
    }
  }
}
