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

package com.nabiki.centre.ctp;

import com.nabiki.centre.user.core.ActiveRequest;
import com.nabiki.commons.ctpobj.CInputOrder;
import com.nabiki.commons.ctpobj.COrder;
import com.nabiki.commons.ctpobj.OrderStatusType;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class OrderMapper {
  private final Map<String, ActiveRequest>
      ref2req = new ConcurrentHashMap<>(); // ref -> active order
  private final Map<String, ActiveRequest>
      uid2req = new ConcurrentHashMap<>();   // UUID -> active order
  private final Map<String, Set<String>>
      uid2ref = new ConcurrentHashMap<>();     // UUID -> ref
  private final Map<String, COrder>
      detRef2Rtn = new ConcurrentHashMap<>();   // ref -> rtn order
  private final Map<String, CInputOrder>
      ref2input = new ConcurrentHashMap<>();   // ref -> input order

  public OrderMapper() {
  }

  public void settle() {
    // Change unfilled orders' states.
    for (var o : detRef2Rtn.values()) {
      if (o.OrderStatus != OrderStatusType.ALL_TRADED) {
        // Following return order won't update again.
        // Those orders may be sent from other clients with the same order ref.
        o.OrderStatus = OrderStatusType.CANCELED;
      }
    }
  }

  /**
   * Register the detailed order and active order, and create mappings.
   *
   * @param order  detailed order
   * @param active active order that issues the detailed order
   */
  public void register(CInputOrder order, ActiveRequest active) {
    this.ref2req.put(order.OrderRef, active);
    this.uid2req.put(active.getRequestUUID(), active);
    this.uid2ref.computeIfAbsent(active.getRequestUUID(), k -> new HashSet<>());
    this.uid2ref.get(active.getRequestUUID()).add(order.OrderRef);
    this.ref2input.put(order.OrderRef, order);
  }

  /**
   * Register return order and create mapping.
   *
   * @param rtn return order
   */
  public void register(COrder rtn) {
    this.detRef2Rtn.put(rtn.OrderRef, rtn);
  }

  /**
   * Get the specified return order of the detail ref. If no order has the UUID,
   * return {@code null}.
   *
   * @param detailRef ref of the order
   * @return last updated return order, or {@code null} if no order has the UUID
   */
  public COrder getRtnOrder(String detailRef) {
    return this.detRef2Rtn.get(detailRef);
  }

  /**
   * Get all detail order refs under the specified {@link UUID}. If no mapping
   * found, return an empty set.
   *
   * @param uuid UUID of the alive order that issues the detail orders
   * @return {@link Set} of detail order refs
   */
  public Set<String> getOrderRef(String uuid) {
    return this.uid2ref.get(uuid);
  }

  /**
   * Get detail order of the specified detail ref. If no mapping found, return
   * {@code null}.
   *
   * @param ref detail order reference
   * @return detail order, or {@code null} if no such ref
   */
  public CInputOrder getInputOrder(String ref) {
    return this.ref2input.get(ref);
  }

  /*
  Get alive order that issued the detail order with the specified detail order
  reference.
   */
  public ActiveRequest getActiveRequest(String detailRef) {
    return this.ref2req.get(detailRef);
  }
}
