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

package com.nabiki.commons.utils.frame;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;

public class FrameParser extends LinkedList<Frame> {
  public static int DEFAULT_BUFFER_SIZE = 128 * 1024;
  public static int SYNC_ZERO_BYTES = 32;

  private Frame decoding = new Frame();
  private int bodyPosition = 0, syncCount = 0;
  private ByteBuffer buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
  private ParsingState state = ParsingState.WAIT_HEADER_TYPE;

  public FrameParser() {
  }

  /**
   * Get parser internal state {@link ParsingState}.
   *
   * @return {@link ParsingState}
   */
  public ParsingState getState() {
    return this.state;
  }

  /**
   * Parse the specified bytes to frame. The bytes can neither be a whole frame
   * or a segment of a frame. If the frame is broken, the parser skips all bytes
   * until it finds continuous zeros of {@link FrameParser#SYNC_ZERO_BYTES}
   * in length.
   *
   * <p>If the parser produces a frame after parsing the specified bytes, return
   * {@code true}, else return {@code false}.
   * </p>
   *
   * <p>The method doesn't throw exception.
   * </p>
   *
   * @param bytes byte array
   * @return {@code true} if the method gets a frame, {@code false} otherwise
   */
  public boolean parse(byte[] bytes) {
    if (bytes == null || bytes.length == 0)
      throw new IllegalArgumentException("no input bytes");
    try {
      store(bytes);
      parseBuffer();
      return super.size() > 0;
    } catch (Throwable th) {
      var m = getFrameDigest(th.getMessage(), decoding);
      resetDecoding();
      buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
      throw new RuntimeException(m);
    }
  }

  /*
  Save the specified bytes to internal buffer and flip the buffer for reading.
   */
  private void store(byte[] bytes) {
    // Ensure the buffer can hold the input bytes.
    if (this.buffer.remaining() < bytes.length) {
      var newBuffer = ByteBuffer.allocate(
          2 * (this.buffer.capacity() + bytes.length));
      this.buffer.flip();
      newBuffer.put(this.buffer);
      this.buffer = newBuffer;
    }
    // Write bytes to buffer for parsing.
    this.buffer.put(bytes);
    this.buffer.flip();
  }

  /*
  Parse the bytes in the internal buffer and compact the buffer at the end for
  next writing.
   */
  private void parseBuffer() {
    do {
      switch (this.state) {
        case WAIT_HEADER_TYPE:
          if (this.buffer.remaining() < 4) {
            this.state = ParsingState.WAIT_HEADER_TYPE;
            break;
          }
          setFrameType();
          this.state = ParsingState.WAIT_HEADER_LENGTH;
          break;
        case WAIT_HEADER_LENGTH:
          if (this.buffer.remaining() < 4) {
            this.state = ParsingState.WAIT_HEADER_LENGTH;
            break;
          }
          setFrameLength();
          // Prepare for bytes.
          if (this.decoding.Length < 1) {
            this.state = ParsingState.WAIT_SYNC;
            break;
          }
          this.decoding.Body = new byte[this.decoding.Length];
          this.state = ParsingState.WAIT_BODY;
          break;
        case WAIT_BODY:
          // If the body is filled, a frame is successfully decoded.
          if (setBody()) {
            super.add(this.decoding);
            resetDecoding();
          }
          break;
        case WAIT_SYNC:
          if (checkSync()) {
            this.state = ParsingState.SYNC_UP;
            this.syncCount = 0;
          }
          break;
        case SYNC_UP:
          if (clearSync())
            this.state = ParsingState.WAIT_HEADER_TYPE;
          break;
      }
    } while (this.buffer.remaining() >= 4);
    // Compact the buffer so that next write starts from position, which
    // is after the previous element.
    this.buffer.compact();
  }

  private void checkFrameType(int type) {
    switch (type) {
      case FrameType.HEARTBEAT:
      case FrameType.REQUEST:
      case FrameType.RESPONSE:
      case FrameType.LOGIN:
        break;
      default:
        throw new IllegalArgumentException(
            String.format("invalid frame type: %X", type));
    }
  }

  private void resetDecoding() {
    this.decoding = new Frame();
    this.bodyPosition = 0;
    this.state = ParsingState.WAIT_HEADER_TYPE;
  }

  private void setFrameType() {
    var order = this.buffer.order();
    this.buffer.order(ByteOrder.BIG_ENDIAN);
    this.decoding.Type = this.buffer.getInt();
    this.buffer.order(order);
    checkFrameType(decoding.Type);
  }

  private void setFrameLength() {
    var order = this.buffer.order();
    this.buffer.order(ByteOrder.BIG_ENDIAN);
    this.decoding.Length = this.buffer.getInt();
    this.buffer.order(order);
  }

  private boolean setBody() {
    int bodyRemain = this.decoding.Body.length - bodyPosition;
    int length = Math.min(bodyRemain, this.buffer.remaining());
    this.buffer.get(this.decoding.Body, this.bodyPosition, length);
    this.bodyPosition += length;
    return this.bodyPosition == this.decoding.Length;
  }

  /*
  Check the sync-up zero bytes.
   */
  private boolean checkSync() {
    while (this.buffer.hasRemaining()) {
      if (this.buffer.get() == 0)
        ++this.syncCount;
      else
        this.syncCount = 0;
      if (this.syncCount >= SYNC_ZERO_BYTES)
        return true;
    }
    return false;
  }

  /*
  Clear all sync-up bytes which can be more than the threshold, and then set state
  to parse next frame.
   */
  private boolean clearSync() {
    // Find the first non-zero byte.
    // Don't consume that byte.
    byte b = 0;
    while (this.buffer.hasRemaining()) {
      b = this.buffer.get();
      if (b != 0)
        break;
    }
    if (b != 0) {
      this.buffer.position(this.buffer.position() - 1);
      return true;
    } else
      return false;
  }

  private String summarizeHex(byte[] bytes) {
    StringBuilder m = new StringBuilder();
    int n = Math.min(bytes.length, 1024);
    int count = 0, index = 0;
    m.append(System.lineSeparator()).append("\t");
    while (index < n) {
      ++count;
      m.append(String.format("%02x ", bytes[index++]));
      if (count >= 32) {
        m.append(System.lineSeparator()).append("\t");
        count = 0;
      }
    }
    return m.toString();
  }

  private String getFrameDigest(String error, Frame decoding) {
    StringBuilder m = new StringBuilder(String.format(
        "Frame[type:%X][length:%d] %s.",
        decoding.Type,
        decoding.Length,
        error));
    // If parser fails to parse input, frame body could be null. Must check.
    if (decoding.Body != null) {
      m.append(summarizeHex(decoding.Body));
    }
    return m.toString();
  }
}
