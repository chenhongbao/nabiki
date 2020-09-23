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

package com.nabiki.ta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

public class Series<T> extends ArrayList<T> {
  private SeriesPoint<T> getEastValue(List<T> a, Comparator<T> c, int token) {
    int idx = a.size();
    int revIdx = 0;
    T v = a.get(--idx);
    while (idx > 0) {
      var v0 = a.get(--idx);
      var r = c.compare(v, v0) * token;
      if (r < 0) {
        v = v0;
        revIdx = a.size() - 1 - idx;
      }
    }
    return new SeriesPoint<>(v, revIdx);
  }

  /*
  To find a bigger value, set token positive, otherwise set it negative.
   */
  private SeriesPoint<T> getEstValue(int days, Comparator<T> c, int token) {
    int n = Math.min(days, size());
    if (n < 1)
      return null;
    else if (n == 1)
      return new SeriesPoint<>(get(0), 0);
    else
      return getEastValue(subList(size() - n, size()), c, token);
  }

  /**
   * Get highest value from the latest {@code window} elements and return the value
   * and its reversed index. The reversed index counts {@code 0} for the tail, and
   * increases from tail to head.
   *
   * @param days window to scan from tail to head.
   * @param c    comparator
   * @return {@link SeriesPoint} if the container is not empty, or {@code null}
   * otherwise.
   */
  public SeriesPoint<T> getHigh(int days, Comparator<T> c) {
    return getEstValue(days, c, 1);
  }

  /**
   * Get lowest value from the latest {@code window} elements and return the value
   * and its reversed index.  The reversed index counts {@code 0} for the tail, and
   * increases from tail to head.
   *
   * @param days window to scan from tail to head.
   * @param c    comparator
   * @return {@link SeriesPoint} if the container is not empty, or {@code null}
   * otherwise.
   */
  public SeriesPoint<T> getLow(int days, Comparator<T> c) {
    return getEstValue(days, c, -1);
  }

  /**
   * Get element from head to tail.
   *
   * @param index index
   * @return element
   */
  public T getHead(int index) {
    if (size() == 0)
      return null;
    else
      return get(0);
  }

  /**
   * Get the first element.
   *
   * @return T first element.
   */
  public T getHead() {
    return getHead(0);
  }

  /**
   * Get element indexed from tail to head.
   *
   * @param reversedIndex reversed index from tail to head.
   * @return element
   */
  public T getTail(int reversedIndex) {
    if (isEmpty())
      return null;
    else
      return get(size() - 1 - reversedIndex);
  }

  /**
   * Get the last element.
   *
   * @return T last element
   */
  public T getTail() {
    return getTail(0);
  }

  /**
   * @deprecated Moving average is strongly coherent with order of elements, no element should be
   * added except at the tail.
   */
  @Deprecated(since = "0.1")
  @Override
  public void add(int index, T element) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  public T remove(int index) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException();
  }


  /**
   * @deprecated Moving average is strongly coherent with order of elements, no element should be
   * added except at the tail.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean addAll(int index, Collection<? extends T> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  protected void removeRange(int fromIndex, int toIndex) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated Removal of any element in a moving average sequence affects the following values
   * from that index. Don't do that.
   */
  @Deprecated(since = "0.1")
  @Override
  public boolean removeIf(Predicate<? super T> filter) {
    throw new UnsupportedOperationException();
  }

  /**
   * @deprecated The order of elements in a moving average sequence affects its all the values.
   * Don't change the order.
   */
  @Deprecated(since = "0.1")
  @Override
  public void sort(Comparator<? super T> c) {
    throw new UnsupportedOperationException();
  }
}
