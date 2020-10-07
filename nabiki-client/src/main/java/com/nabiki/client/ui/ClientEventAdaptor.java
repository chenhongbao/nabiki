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

import com.nabiki.client.sdk.TradeClientListener;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ClientEventAdaptor implements TradeClientListener {
  private final Lock lock = new ReentrantLock();
  private final Condition cond = lock.newCondition();

  private boolean open = false;

  @Override
  public void onError(Throwable th) {
    th.printStackTrace();
  }

  @Override
  public void onClose() {
    open = false;
    signal();
  }

  @Override
  public void onOpen() {
    open = true;
    signal();
  }

  private void signal() {
    lock.lock();
    try {
      cond.signal();
    } catch (Throwable th) {
      th.printStackTrace();
    } finally {
      lock.unlock();
    }
  }

  boolean waitOpen(long millis) {
    var toWait = millis;
    while (!open && toWait > 0) {
      var beg = System.currentTimeMillis();
      lock.lock();
      try {
        cond.await(toWait, TimeUnit.MILLISECONDS);
      } catch (Throwable th) {
        th.printStackTrace();
      } finally {
        lock.unlock();
        toWait -= (System.currentTimeMillis() - beg);
      }
    }
    return open;
  }

  boolean waitClose(long millis) {
    var toWait = millis;
    while (open && toWait > 0) {
      var beg = System.currentTimeMillis();
      lock.lock();
      try {
        cond.await(toWait, TimeUnit.MILLISECONDS);
      } catch (Throwable th) {
        th.printStackTrace();
      } finally {
        lock.unlock();
        toWait -= (System.currentTimeMillis() - beg);
      }
    }
    return !open;
  }
}
