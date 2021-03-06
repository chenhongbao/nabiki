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

package com.nabiki.client.sdk.internal;

import com.nabiki.client.sdk.Response;
import com.nabiki.client.sdk.ResponseConsumer;
import com.nabiki.commons.ctpobj.CRspInfo;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ResponseImpl<T> implements Response<T> {
  private static class ArriveResponse<T> {
    final T Response;
    final CRspInfo RspInfo;
    final int CurrentCount, TotalCount;

    ArriveResponse(T response, CRspInfo rspInfo, int count,
                   int total) {
      this.Response = response;
      this.RspInfo = rspInfo;
      this.CurrentCount = count;
      this.TotalCount = total;
    }
  }

  private final AtomicReference<ResponseConsumer<T>> consumer;
  private final Map<Integer, CRspInfo> infos;
  private final Queue<ArriveResponse<T>> responses;
  private final AtomicInteger totalCount = new AtomicInteger(0),
      arriveCount = new AtomicInteger(0);
  private final AtomicBoolean hasRsp = new AtomicBoolean(false);

  public ResponseImpl() {
    consumer = new AtomicReference<>(null);
    infos = new ConcurrentHashMap<>();
    responses = new ConcurrentLinkedQueue<>();
  }

  void put(T response, CRspInfo rspInfo, int count, int total) {
    if (consumer.get() != null) {
      try {
        consumer.get().accept(response, rspInfo, count, total);
      } catch (Throwable th) {
        th.printStackTrace();
      }
    } else {
      responses.add(
          new ArriveResponse<>(response, rspInfo, count, total));
      infos.put(response.hashCode(), rspInfo);
    }
    hasRsp.set(true);
    // Set count.
    arriveCount.incrementAndGet();
    if (totalCount.get() == 0) {
      totalCount.set(total);
    }
  }

  @Override
  public T poll() {
    var r = this.responses.poll();
    if (r != null)
      return r.Response;
    else
      return null;
  }

  @Override
  public CRspInfo getRspInfo(T response) {
    if (response == null)
      return null;
    return this.infos.get(response.hashCode());
  }

  @Override
  public void consume(ResponseConsumer<T> consumer) {
    if (consumer != null)
      this.consumer.set(consumer);
  }

  @Override
  public boolean hasResponse() {
    return this.hasRsp.get();
  }

  @Override
  public int getArrivalCount() {
    return this.arriveCount.get();
  }

  @Override
  public int getTotalCount() {
    return this.totalCount.get();
  }

  @Override
  public int availableCount() {
    return this.responses.size();
  }
}
