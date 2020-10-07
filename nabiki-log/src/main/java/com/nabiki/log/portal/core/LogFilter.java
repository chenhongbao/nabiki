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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.LogRecord;
import java.util.regex.Pattern;

public class LogFilter {
  private FilterConditionType loggerCondition;
  private FilterConditionType msgCondition;
  private FilterConditionType levelCondition;
  private String loggerPattern;
  private String msgPattern;
  private final Map<String, Object> levelPatterns = new ConcurrentHashMap<>();
  private final AtomicBoolean enabled = new AtomicBoolean(false);

  public void setLoggerCondition(FilterConditionType type, String pattern) {
    if (pattern == null || pattern.trim().length() == 0) {
      loggerCondition = FilterConditionType.NO_FILTER;
    } else {
      loggerCondition = type;
    }
    loggerPattern = pattern.trim();
  }

  public void setLevelCondition(FilterConditionType type, String pattern) {
    if (pattern == null || pattern.trim().length() == 0) {
      levelCondition = FilterConditionType.NO_FILTER;
    } else {
      levelCondition = type;
    }
    levelPatterns.clear();
    for (var p : pattern.split(",")) {
      levelPatterns.put(p.trim(), new Object());
    }
  }

  public void setMsgCondition(FilterConditionType type, String pattern) {
    if (pattern == null || pattern.trim().length() == 0) {
      msgCondition = FilterConditionType.NO_FILTER;
    } else {
      msgCondition = type;
    }
    msgPattern =  pattern.trim();
  }

  public void enable(boolean b) {
    enabled.set(b);
  }

  private boolean checkCondition(FilterConditionType type, String pattern, String content) {
    if (content == null) {
      return false;
    }
    switch (type) {
      case IS:
        return content.equals(pattern);
      case INCLUDE:
        return content.contains(pattern);
      case REGEX:
        return Pattern.compile(pattern).matcher(content).matches();
      default:
        return true;
    }
  }

  private boolean checkCondition(FilterConditionType type, Map<String, Object> pattern, String content) {
    if (type == FilterConditionType.NO_FILTER) {
      return true;
    } else if (type == FilterConditionType.IS) {
      return pattern.containsKey(content);
    }
    for (var k : pattern.keySet()) {
      if (k.contains(content)) {
        return true;
      }
    }
    return false;
  }

  public boolean passed(LogRecord record) {
    if (record == null) {
      return false;
    } else if (!enabled.get()) {
      return true;
    }
    return checkCondition(loggerCondition, loggerPattern, record.getLoggerName()) &&
        checkCondition(levelCondition, levelPatterns, record.getLevel().toString()) &&
        checkCondition(msgCondition, msgPattern, record.getMessage());
  }
}
