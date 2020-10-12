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

package com.nabiki.commons.iop.x;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nabiki.commons.ctpobj.ErrorCodes;
import com.nabiki.commons.ctpobj.ErrorMessages;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

public class OP {
  // GSON.
  private final static Gson gson;

  static {
    gson = new GsonBuilder()
        .setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE)
        .serializeNulls()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues() // Handle possible NaN.
        .create();
  }

  /**
   * Parse the specified JSON string to object of the specified {@link Class}.
   *
   * @param json JSON string
   * @param clz  {@link Class} of the object
   * @param <T>  generic type of the object
   * @return object parsed from the specified JSON string
   * @throws IOException fail parsing JSON string
   */
  public static <T> T fromJson(String json, Class<T> clz) throws IOException {
    try {
      return gson.fromJson(json, clz);
    } catch (Throwable e) {
      throw new IOException("parse JSON string", e);
    }
  }

  /**
   * Encode the specified object into JSON string.
   *
   * @param obj object
   * @return JSON string representing the specified object
   */
  public static String toJson(Object obj) {
    return gson.toJson(obj);
  }

  /**
   * Schedule task at every specified period from epoch, excluding epoch.
   * @param task timer task
   * @param msPeriod period in milliseconds
   */
  public static void schedule(TimerTask task, long msPeriod) {
    long delay = msPeriod - System.currentTimeMillis() % msPeriod;
    schedule(task, delay, msPeriod);
  }

  public static void schedule(TimerTask task, Date date, long msPeriod) {
    new Timer().scheduleAtFixedRate(task, date, msPeriod);
  }

  public static void schedule(TimerTask task, long msDelay, long msPeriod) {
    new Timer().scheduleAtFixedRate(task, msDelay, msPeriod);
  }

  public static void scheduleOnce(TimerTask task, Date date) {
    new Timer().schedule(task, date);
  }

  public static void scheduleOnce(TimerTask task, long delay) {
    new Timer().schedule(task, delay);
  }

  /**
   * Get option values.
   * @param prefix option prefix
   * @param args arguments' list
   * @return {@code null} if the specified option is not defined, {@code ""} if
   *   option is defined but its value is absent, or return the value.
   */
  public static String getOption(String prefix, String[] args) {
    if (args.length < 1) {
      return null;
    }
    int i = 0;
    do {
      if (args[i].equals(prefix)) {
        break;
      }
    } while (++i < args.length);
    if (i == args.length) {
      return null;
    } else if (i == args.length - 1) {
      return "";
    } else {
      var next = args[i + 1];
      if (next.startsWith("--")) {
        return "";
      } else {
        return next;
      }
    }
  }

  private static final Map<Integer, String> errorMsg = new ConcurrentHashMap<>();

  static {
    errorMsg.put(ErrorCodes.NONE, ErrorMessages.NONE);
    errorMsg.put(ErrorCodes.INVALID_DATA_SYNC_STATUS, ErrorMessages.INVALID_DATA_SYNC_STATUS);
    errorMsg.put(ErrorCodes.INCONSISTENT_INFORMATION, ErrorMessages.INCONSISTENT_INFORMATION);
    errorMsg.put(ErrorCodes.INVALID_LOGIN, ErrorMessages.INVALID_LOGIN);
    errorMsg.put(ErrorCodes.USER_NOT_ACTIVE, ErrorMessages.USER_NOT_ACTIVE);
    errorMsg.put(ErrorCodes.DUPLICATE_LOGIN, ErrorMessages.DUPLICATE_LOGIN);
    errorMsg.put(ErrorCodes.NOT_LOGIN_YET, ErrorMessages.NOT_LOGIN_YET);
    errorMsg.put(ErrorCodes.NOT_INITED, ErrorMessages.NOT_INITED);
    errorMsg.put(ErrorCodes.FRONT_NOT_ACTIVE, ErrorMessages.FRONT_NOT_ACTIVE);
    errorMsg.put(ErrorCodes.NO_PRIVILEGE, ErrorMessages.NO_PRIVILEGE);
    errorMsg.put(ErrorCodes.CHANGE_OTHER_PASSWORD, ErrorMessages.CHANGE_OTHER_PASSWORD);
    errorMsg.put(ErrorCodes.USER_NOT_FOUND, ErrorMessages.USER_NOT_FOUND);
    errorMsg.put(ErrorCodes.BROKER_NOT_FOUND, ErrorMessages.BROKER_NOT_FOUND);
    errorMsg.put(ErrorCodes.INVESTOR_NOT_FOUND, ErrorMessages.INVESTOR_NOT_FOUND);
    errorMsg.put(ErrorCodes.OLD_PASSWORD_MISMATCH, ErrorMessages.OLD_PASSWORD_MISMATCH);
    errorMsg.put(ErrorCodes.BAD_FIELD, ErrorMessages.BAD_FIELD);
    errorMsg.put(ErrorCodes.INSTRUMENT_NOT_FOUND, ErrorMessages.INSTRUMENT_NOT_FOUND);
    errorMsg.put(ErrorCodes.INSTRUMENT_NOT_TRADING, ErrorMessages.INSTRUMENT_NOT_TRADING);
    errorMsg.put(ErrorCodes.NOT_EXCHANGE_PARTICIPANT, ErrorMessages.NOT_EXCHANGE_PARTICIPANT);
    errorMsg.put(ErrorCodes.INVESTOR_NOT_ACTIVE, ErrorMessages.INVESTOR_NOT_ACTIVE);
    errorMsg.put(ErrorCodes.NOT_EXCHANGE_CLIENT, ErrorMessages.NOT_EXCHANGE_CLIENT);
    errorMsg.put(ErrorCodes.NO_VALID_TRADER_AVAILABLE, ErrorMessages.NO_VALID_TRADER_AVAILABLE);
    errorMsg.put(ErrorCodes.DUPLICATE_ORDER_REF, ErrorMessages.DUPLICATE_ORDER_REF);
    errorMsg.put(ErrorCodes.BAD_ORDER_ACTION_FIELD, ErrorMessages.BAD_ORDER_ACTION_FIELD);
    errorMsg.put(ErrorCodes.DUPLICATE_ORDER_ACTION_REF, ErrorMessages.DUPLICATE_ORDER_ACTION_REF);
    errorMsg.put(ErrorCodes.ORDER_NOT_FOUND, ErrorMessages.ORDER_NOT_FOUND);
    errorMsg.put(ErrorCodes.INSUITABLE_ORDER_STATUS, ErrorMessages.INSUITABLE_ORDER_STATUS);
    errorMsg.put(ErrorCodes.UNSUPPORTED_FUNCTION, ErrorMessages.UNSUPPORTED_FUNCTION);
    errorMsg.put(ErrorCodes.NO_TRADING_RIGHT, ErrorMessages.NO_TRADING_RIGHT);
    errorMsg.put(ErrorCodes.CLOSE_ONLY, ErrorMessages.CLOSE_ONLY);
    errorMsg.put(ErrorCodes.OVER_CLOSE_POSITION, ErrorMessages.OVER_CLOSE_POSITION);
    errorMsg.put(ErrorCodes.INSUFFICIENT_MONEY, ErrorMessages.INSUFFICIENT_MONEY);
    errorMsg.put(ErrorCodes.DUPLICATE_PK, ErrorMessages.DUPLICATE_PK);
    errorMsg.put(ErrorCodes.CANNOT_FIND_PK, ErrorMessages.CANNOT_FIND_PK);
    errorMsg.put(ErrorCodes.CAN_NOT_INACTIVE_BROKER, ErrorMessages.CAN_NOT_INACTIVE_BROKER);
    errorMsg.put(ErrorCodes.BROKER_SYNCHRONIZING, ErrorMessages.BROKER_SYNCHRONIZING);
    errorMsg.put(ErrorCodes.BROKER_SYNCHRONIZED, ErrorMessages.BROKER_SYNCHRONIZED);
    errorMsg.put(ErrorCodes.SHORT_SELL, ErrorMessages.SHORT_SELL);
    errorMsg.put(ErrorCodes.INVALID_SETTLEMENT_REF, ErrorMessages.INVALID_SETTLEMENT_REF);
    errorMsg.put(ErrorCodes.CFFEX_NETWORK_ERROR, ErrorMessages.CFFEX_NETWORK_ERROR);
    errorMsg.put(ErrorCodes.CFFEX_OVER_REQUEST, ErrorMessages.CFFEX_OVER_REQUEST);
    errorMsg.put(ErrorCodes.CFFEX_OVER_REQUEST_PER_SECOND, ErrorMessages.CFFEX_OVER_REQUEST_PER_SECOND);
    errorMsg.put(ErrorCodes.SETTLEMENT_INFO_NOT_CONFIRMED, ErrorMessages.SETTLEMENT_INFO_NOT_CONFIRMED);
    errorMsg.put(ErrorCodes.DEPOSIT_NOT_FOUND, ErrorMessages.DEPOSIT_NOT_FOUND);
    errorMsg.put(ErrorCodes.EXCHANG_TRADING, ErrorMessages.EXCHANG_TRADING);
    errorMsg.put(ErrorCodes.PARKEDORDER_NOT_FOUND, ErrorMessages.PARKEDORDER_NOT_FOUND);
    errorMsg.put(ErrorCodes.PARKEDORDER_HASSENDED, ErrorMessages.PARKEDORDER_HASSENDED);
    errorMsg.put(ErrorCodes.PARKEDORDER_HASDELETE, ErrorMessages.PARKEDORDER_HASDELETE);
    errorMsg.put(ErrorCodes.INVALID_INVESTORIDORPASSWORD, ErrorMessages.INVALID_INVESTORIDORPASSWORD);
    errorMsg.put(ErrorCodes.INVALID_LOGIN_IPADDRESS, ErrorMessages.INVALID_LOGIN_IPADDRESS);
    errorMsg.put(ErrorCodes.OVER_CLOSETODAY_POSITION, ErrorMessages.OVER_CLOSETODAY_POSITION);
    errorMsg.put(ErrorCodes.OVER_CLOSEYESTERDAY_POSITION, ErrorMessages.OVER_CLOSEYESTERDAY_POSITION);
    errorMsg.put(ErrorCodes.BROKER_NOT_ENOUGH_CONDORDER, ErrorMessages.BROKER_NOT_ENOUGH_CONDORDER);
    errorMsg.put(ErrorCodes.INVESTOR_NOT_ENOUGH_CONDORDER, ErrorMessages.INVESTOR_NOT_ENOUGH_CONDORDER);
    errorMsg.put(ErrorCodes.BROKER_NOT_SUPPORT_CONDORDER, ErrorMessages.BROKER_NOT_SUPPORT_CONDORDER);
    errorMsg.put(ErrorCodes.RESEND_ORDER_BROKERINVESTOR_NOTMATCH, ErrorMessages.RESEND_ORDER_BROKERINVESTOR_NOTMATCH);
    errorMsg.put(ErrorCodes.SYC_OTP_FAILED, ErrorMessages.SYC_OTP_FAILED);
    errorMsg.put(ErrorCodes.OTP_MISMATCH, ErrorMessages.OTP_MISMATCH);
    errorMsg.put(ErrorCodes.OTPPARAM_NOT_FOUND, ErrorMessages.OTPPARAM_NOT_FOUND);
    errorMsg.put(ErrorCodes.UNSUPPORTED_OTPTYPE, ErrorMessages.UNSUPPORTED_OTPTYPE);
    errorMsg.put(ErrorCodes.SINGLEUSERSESSION_EXCEED_LIMIT, ErrorMessages.SINGLEUSERSESSION_EXCEED_LIMIT);
    errorMsg.put(ErrorCodes.EXCHANGE_UNSUPPORTED_ARBITRAGE, ErrorMessages.EXCHANGE_UNSUPPORTED_ARBITRAGE);
    errorMsg.put(ErrorCodes.NO_CONDITIONAL_ORDER_RIGHT, ErrorMessages.NO_CONDITIONAL_ORDER_RIGHT);
    errorMsg.put(ErrorCodes.AUTH_FAILED, ErrorMessages.AUTH_FAILED);
    errorMsg.put(ErrorCodes.NOT_AUTHENT, ErrorMessages.NOT_AUTHENT);
    errorMsg.put(ErrorCodes.SWAPORDER_UNSUPPORTED, ErrorMessages.SWAPORDER_UNSUPPORTED);
    errorMsg.put(ErrorCodes.OPTIONS_ONLY_SUPPORT_SPEC, ErrorMessages.OPTIONS_ONLY_SUPPORT_SPEC);
    errorMsg.put(ErrorCodes.DUPLICATE_EXECORDER_REF, ErrorMessages.DUPLICATE_EXECORDER_REF);
    errorMsg.put(ErrorCodes.RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH, ErrorMessages.RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH);
    errorMsg.put(ErrorCodes.EXECORDER_NOTOPTIONS, ErrorMessages.EXECORDER_NOTOPTIONS);
    errorMsg.put(ErrorCodes.OPTIONS_NOT_SUPPORT_EXEC, ErrorMessages.OPTIONS_NOT_SUPPORT_EXEC);
    errorMsg.put(ErrorCodes.BAD_EXECORDER_ACTION_FIELD, ErrorMessages.BAD_EXECORDER_ACTION_FIELD);
    errorMsg.put(ErrorCodes.DUPLICATE_EXECORDER_ACTION_REF, ErrorMessages.DUPLICATE_EXECORDER_ACTION_REF);
    errorMsg.put(ErrorCodes.EXECORDER_NOT_FOUND, ErrorMessages.EXECORDER_NOT_FOUND);
    errorMsg.put(ErrorCodes.OVER_EXECUTE_POSITION, ErrorMessages.OVER_EXECUTE_POSITION);
    errorMsg.put(ErrorCodes.LOGIN_FORBIDDEN, ErrorMessages.LOGIN_FORBIDDEN);
    errorMsg.put(ErrorCodes.INVALID_TRANSFER_AGENT, ErrorMessages.INVALID_TRANSFER_AGENT);
    errorMsg.put(ErrorCodes.NO_FOUND_FUNCTION, ErrorMessages.NO_FOUND_FUNCTION);
    errorMsg.put(ErrorCodes.SEND_EXCHANGEORDER_FAILED, ErrorMessages.SEND_EXCHANGEORDER_FAILED);
    errorMsg.put(ErrorCodes.SEND_EXCHANGEORDERACTION_FAILED, ErrorMessages.SEND_EXCHANGEORDERACTION_FAILED);
    errorMsg.put(ErrorCodes.PRICETYPE_NOTSUPPORT_BYEXCHANGE, ErrorMessages.PRICETYPE_NOTSUPPORT_BYEXCHANGE);
    errorMsg.put(ErrorCodes.BAD_EXECUTE_TYPE, ErrorMessages.BAD_EXECUTE_TYPE);
    errorMsg.put(ErrorCodes.BAD_OPTION_INSTR, ErrorMessages.BAD_OPTION_INSTR);
    errorMsg.put(ErrorCodes.INSTR_NOTSUPPORT_FORQUOTE, ErrorMessages.INSTR_NOTSUPPORT_FORQUOTE);
    errorMsg.put(ErrorCodes.RESEND_QUOTE_BROKERINVESTOR_NOTMATCH, ErrorMessages.RESEND_QUOTE_BROKERINVESTOR_NOTMATCH);
    errorMsg.put(ErrorCodes.INSTR_NOTSUPPORT_QUOTE, ErrorMessages.INSTR_NOTSUPPORT_QUOTE);
    errorMsg.put(ErrorCodes.QUOTE_NOT_FOUND, ErrorMessages.QUOTE_NOT_FOUND);
    errorMsg.put(ErrorCodes.OPTIONS_NOT_SUPPORT_ABANDON, ErrorMessages.OPTIONS_NOT_SUPPORT_ABANDON);
    errorMsg.put(ErrorCodes.COMBOPTIONS_SUPPORT_IOC_ONLY, ErrorMessages.COMBOPTIONS_SUPPORT_IOC_ONLY);
    errorMsg.put(ErrorCodes.OPEN_FILE_FAILED, ErrorMessages.OPEN_FILE_FAILED);
    errorMsg.put(ErrorCodes.NEED_RETRY, ErrorMessages.NEED_RETRY);
    errorMsg.put(ErrorCodes.EXCHANGE_RTNERROR, ErrorMessages.EXCHANGE_RTNERROR);
    errorMsg.put(ErrorCodes.QUOTE_DERIVEDORDER_ACTIONERROR, ErrorMessages.QUOTE_DERIVEDORDER_ACTIONERROR);
    errorMsg.put(ErrorCodes.INSTRUMENTMAP_NOT_FOUND, ErrorMessages.INSTRUMENTMAP_NOT_FOUND);
    errorMsg.put(ErrorCodes.CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED, ErrorMessages.CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED);
  }

  /**
   * Get error message of the specified error ID.
   *
   * @param errorID error ID
   * @return error message
   */
  public static String getErrorMsg(int errorID) {
    return errorMsg.get(errorID);
  }
}
