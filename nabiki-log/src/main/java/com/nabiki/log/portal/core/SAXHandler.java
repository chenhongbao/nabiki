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

package com.nabiki.log.portal.core;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.time.Instant;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class SAXHandler extends DefaultHandler {
  private String element;
  private LogRecord record;
  private final Queue<LogRecord> records = new LinkedList<>();

  public LogRecord pop() {
    return records.poll();
  }

  @Override
  public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    element = qName;
    if (qName.equals("record")) {
      record = new LogRecord(Level.ALL, "");
    }
  }

  @Override
  public void endElement(String uri, String localName, String qName) throws SAXException {
    element = null;
    if (qName.equals("record")) {
      records.offer(record);
    }
  }

  @Override
  public void characters(char[] ch, int start, int length) throws SAXException {
    if (element == null) {
      return;
    }
    if (element.equals("date")) {
      record.setInstant(Instant.parse(String.valueOf(ch, start, length)));
    } else if (element.equals("sequence")) {
      record.setSequenceNumber(Long.parseLong(String.valueOf(ch, start, length)));
    } else if (element.equals("logger")) {
      record.setLoggerName(String.valueOf(ch, start, length));
    } else if (element.equals("level")) {
      record.setLevel(Level.parse(String.valueOf(ch, start, length)));
    } else if (element.equals("class")) {
      record.setSourceClassName(String.valueOf(ch, start, length));
    } else if (element.equals("method")) {
      record.setSourceMethodName(String.valueOf(ch, start, length));
    } else if (element.equals("thread")) {
      record.setThreadID(Integer.parseInt(String.valueOf(ch, start, length)));
    } else if (element.equals("message")) {
      var prev = record.getMessage();
      if (prev == null) {
        prev = "";
      }
      record.setMessage(prev + String.valueOf(ch, start, length));
    }
  }

  @Override
  public void fatalError(SAXParseException e) throws SAXException {
    e.printStackTrace();
  }
}
