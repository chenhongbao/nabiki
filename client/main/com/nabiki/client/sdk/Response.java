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

package com.nabiki.client.sdk;

import com.nabiki.objects.CRspInfo;

public interface Response<T> {
  /**
   * Retrieve the remove the first response in queue in order of FIFO. If there is
   * no response currently available in queue, return {@code null}.
   *
   * <p><b>If {@link ResponseConsumer} is registered, a response will be consumed
   * by the consumer and will not be retrieved by the method.
   * </b></p>
   *
   * @return the first element in queue, or {@code null} if no response in queue
   */
  T poll();

  /**
   * Get the corresponding {@link CRspInfo} of the specified
   * response. If no mapping for the specified response, return {@code null}.
   *
   * @param object response object
   * @return response information, or {@code null} if no mapping
   */
  CRspInfo getRspInfo(T object);

  /**
   * Set the {@link ResponseConsumer} to receive the upcoming responses.
   *
   * <p><b>The consumer is invoked at once when a response arrives and the
   * response will not be added to queue or retrieved by {@link Response#poll()}.
   * </b></p>
   *
   * @param consumer consumer to receive the upcoming response
   */
  void consume(ResponseConsumer<T> consumer);

  /**
   * Check if there was response arrived. Even all responses in queue are
   * retrieved by {@link Response#poll()} or consumed by {@link ResponseConsumer},
   * the mark is still {@code true}.
   *
   * @return {@code true} if there was response ever arrived, {@code false}
   * otherwise
   */
  boolean hasResponse();

  /**
   * Get the number of responses ever arrived. The response counted by the
   * method can be retrieved or consumed and is no long in queue.
   *
   * @return number of response ever arrived
   */
  int getArrivalCount();

  /**
   * Get the total number of responses for the corresponding request.
   *
   * @return total number of responses for the request
   */
  int getTotalCount();

  /**
   * Get the number of responses in queue that are available for
   * {@link Response#poll()}. If the consumer is set via
   * {@link Response#consume(ResponseConsumer)}, the response that just arrived
   * will be received by the consumer immediately and will not arrive in queue.
   *
   * @return number of responses available for {@code poll}
   */
  int availableCount();
}
