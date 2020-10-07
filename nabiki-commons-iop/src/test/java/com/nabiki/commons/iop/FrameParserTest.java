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

package com.nabiki.commons.iop;

import com.google.gson.Gson;
import com.nabiki.commons.iop.frame.*;
import com.nabiki.commons.ctpobj.CRspInfo;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.junit.Assert.*;

public class FrameParserTest {
  static class TestMessage implements java.io.Serializable {
    public String Title;
    public String Content;
    public LocalDate Date;
    public LocalTime Time;

    TestMessage() {
    }
  }

  static TestMessage msg;
  static Body body = new Body();
  static Frame frame = new Frame();
  static byte[] frameBytes;

  static CRspInfo rspInfo;

  static {
    body.RequestID = UUID.randomUUID().toString();
    body.ResponseID = UUID.randomUUID().toString();
    body.Type = MessageType.QRY_POSITION;

    msg = new TestMessage();
    msg.Title = "Happy Holiday";
    msg.Content = "Happy Christmas!";
    msg.Date = LocalDate.now();
    msg.Time = LocalTime.now();

    rspInfo = new CRspInfo();
    rspInfo.ErrorID = 1;
    rspInfo.ErrorMsg = "error message";

    body.Body = new Gson().toJson(msg);
    body.RspInfo = new Gson().toJson(rspInfo);

    // Construct frame.
    var bytes = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);

    frame.Type = FrameType.REQUEST;
    frame.Length = bytes.length;
    frame.Body = bytes;

    // Get bytes of frame.
    frameBytes = new byte[8 + frame.Length];
    var buffer = ByteBuffer.wrap(frameBytes);

    buffer.order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(frame.Type);
    buffer.putInt(frame.Length);
    buffer.put(frame.Body);
  }

  @Test
  public void whole() {
    var parser = new FrameParser();

    var r = parser.parse(frameBytes);
    assertTrue("Should get the frame", r);

    var frame1 = parser.poll();
    assertNotNull("Should retrieve the first frame", frame1);
    checkFrame(frame1);
  }

  public void checkFrame(Frame frame1) {
    // Test frame.
    assertEquals(frame1.Type, frame.Type);
    assertEquals(frame1.Length, frame.Length);
    assertEquals(frame1.Body.length, frame.Body.length);

    for (int i = 0; i < frame1.Body.length; ++i)
      assertEquals(frame1.Body[i], frame.Body[i]);

    var body1 = new Gson().fromJson(
        new String(frame1.Body, StandardCharsets.UTF_8),
        Body.class);
    assertNotNull("Should parse the body", body1);

    // Test body.
    Assert.assertEquals(body1.RequestID, body.RequestID);
    Assert.assertEquals(body1.ResponseID, body.ResponseID);
    Assert.assertEquals(body1.Body, body.Body);
    Assert.assertEquals(body1.Type, body.Type);

    var msg1 = new Gson().fromJson(body1.Body, TestMessage.class);
    assertNotNull("Should parse body message", msg1);

    // Test body json.
    assertNotNull(msg1.Title, msg.Title);
    assertEquals(msg1.Content, msg.Content);
    assertEquals(msg1.Date, msg.Date);
    assertEquals(msg1.Time, msg.Time);
  }

  @Test
  public void partial() {
    for (int len = 0; len < frameBytes.length; ++len)
      partial1(len);
  }

  public void partial1(int len1) {
    var parser = new FrameParser();
    if (len1 < 1)
      len1 = 1;
    if (len1 >= frameBytes.length - 1)
      len1 = frameBytes.length - 2;

    var part1 = new byte[len1];
    var part2 = new byte[frameBytes.length - len1];
    int i = 0;
    for (; i < len1; ++i)
      part1[i] = frameBytes[i];
    for (int j = 0; i < frameBytes.length; ++i, ++j)
      part2[j] = frameBytes[i];

    var part3 = new byte[frameBytes.length];
    i = 0;
    for (; i < part2.length; ++i)
      part3[i] = part2[i];
    for (int j = 0; j < part1.length; ++j, ++i)
      part3[i] = part1[j];

    boolean r = parser.parse(part1);
    assertFalse("Partial frame, should be false", r);

    r = parser.parse(part2);
    assertTrue("Complete the whole, should be true", r);
    checkFrame(parser.poll());

    r = parser.parse(part1);
    assertFalse("Partial frame, should be false", r);

    r = parser.parse(part3);
    assertTrue("Complete the whole, and more, should be true", r);
    checkFrame(parser.poll());

    r = parser.parse(part2);
    assertTrue("Complete the whole, should be true", r);
    checkFrame(parser.poll());
  }

  static Frame wrongFrame = new Frame();
  static byte[] wrongBytes;

  static {
    // Construct frame.
    var bytes = new Gson().toJson(body).getBytes(StandardCharsets.UTF_8);

    wrongFrame.Type = FrameType.REQUEST;
    wrongFrame.Length = -1;
    wrongFrame.Body = bytes;

    // Get bytes of frame.
    wrongBytes = new byte[8 + bytes.length];
    var buffer = ByteBuffer.wrap(wrongBytes);

    buffer.order(ByteOrder.BIG_ENDIAN);
    buffer.putInt(wrongFrame.Type);
    buffer.putInt(wrongFrame.Length);
    buffer.put(wrongFrame.Body);
  }

  @Test
  public void sync() {
    var parser = new FrameParser();
    var r = parser.parse(wrongBytes);

    assertFalse("Should be wrong, return false", r);
    assertEquals(ParsingState.WAIT_SYNC, parser.getState());

    // Test sync up.
    var zeros = new byte[16];
    r = parser.parse(zeros);
    assertFalse("Not sync up, return false", r);
    assertEquals(ParsingState.WAIT_SYNC, parser.getState());

    r = parser.parse(frameBytes);
    assertFalse("Still wrong, return false", r);
    assertEquals(ParsingState.WAIT_SYNC, parser.getState());

    var zeros1 = new byte[24];
    r = parser.parse(zeros1);
    assertFalse("Not sync up, return false", r);
    assertEquals(ParsingState.WAIT_SYNC, parser.getState());

    r = parser.parse(zeros1);
    assertEquals(ParsingState.SYNC_UP, parser.getState());

    r = parser.parse(frameBytes);
    assertTrue("Should get the frame", r);

    var frame1 = parser.poll();
    assertNotNull("Should retrieve the first frame", frame1);
    checkFrame(frame1);
  }
}