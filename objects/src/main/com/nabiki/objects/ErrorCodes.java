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

package com.nabiki.objects;

public class ErrorCodes {
  final public static int NONE = 0;
  final public static int INVALID_DATA_SYNC_STATUS = 1;
  final public static int INCONSISTENT_INFORMATION = 2;
  final public static int INVALID_LOGIN = 3;
  final public static int USER_NOT_ACTIVE = 4;
  final public static int DUPLICATE_LOGIN = 5;
  final public static int NOT_LOGIN_YET = 6;
  final public static int NOT_INITED = 7;
  final public static int FRONT_NOT_ACTIVE = 8;
  final public static int NO_PRIVILEGE = 9;
  final public static int CHANGE_OTHER_PASSWORD = 10;
  final public static int USER_NOT_FOUND = 11;
  final public static int BROKER_NOT_FOUND = 12;
  final public static int INVESTOR_NOT_FOUND = 13;
  final public static int OLD_PASSWORD_MISMATCH = 14;
  final public static int BAD_FIELD = 15;
  final public static int INSTRUMENT_NOT_FOUND = 16;
  final public static int INSTRUMENT_NOT_TRADING = 17;
  final public static int NOT_EXCHANGE_PARTICIPANT = 18;
  final public static int INVESTOR_NOT_ACTIVE = 19;
  final public static int NOT_EXCHANGE_CLIENT = 20;
  final public static int NO_VALID_TRADER_AVAILABLE = 21;
  final public static int DUPLICATE_ORDER_REF = 22;
  final public static int BAD_ORDER_ACTION_FIELD = 23;
  final public static int DUPLICATE_ORDER_ACTION_REF = 24;
  final public static int ORDER_NOT_FOUND = 25;
  final public static int INSUITABLE_ORDER_STATUS = 26;
  final public static int UNSUPPORTED_FUNCTION = 27;
  final public static int NO_TRADING_RIGHT = 28;
  final public static int CLOSE_ONLY = 29;
  final public static int OVER_CLOSE_POSITION = 30;
  final public static int INSUFFICIENT_MONEY = 31;
  final public static int DUPLICATE_PK = 32;
  final public static int CANNOT_FIND_PK = 33;
  final public static int CAN_NOT_INACTIVE_BROKER = 34;
  final public static int BROKER_SYNCHRONIZING = 35;
  final public static int BROKER_SYNCHRONIZED = 36;
  final public static int SHORT_SELL = 37;
  final public static int INVALID_SETTLEMENT_REF = 38;
  final public static int CFFEX_NETWORK_ERROR = 39;
  final public static int CFFEX_OVER_REQUEST = 40;
  final public static int CFFEX_OVER_REQUEST_PER_SECOND = 41;
  final public static int SETTLEMENT_INFO_NOT_CONFIRMED = 42;
  final public static int DEPOSIT_NOT_FOUND = 43;
  final public static int EXCHANG_TRADING = 44;
  final public static int PARKEDORDER_NOT_FOUND = 45;
  final public static int PARKEDORDER_HASSENDED = 46;
  final public static int PARKEDORDER_HASDELETE = 47;
  final public static int INVALID_INVESTORIDORPASSWORD = 48;
  final public static int INVALID_LOGIN_IPADDRESS = 49;
  final public static int OVER_CLOSETODAY_POSITION = 50;
  final public static int OVER_CLOSEYESTERDAY_POSITION = 51;
  final public static int BROKER_NOT_ENOUGH_CONDORDER = 52;
  final public static int INVESTOR_NOT_ENOUGH_CONDORDER = 53;
  final public static int BROKER_NOT_SUPPORT_CONDORDER = 54;
  final public static int RESEND_ORDER_BROKERINVESTOR_NOTMATCH = 55;
  final public static int SYC_OTP_FAILED = 56;
  final public static int OTP_MISMATCH = 57;
  final public static int OTPPARAM_NOT_FOUND = 58;
  final public static int UNSUPPORTED_OTPTYPE = 59;
  final public static int SINGLEUSERSESSION_EXCEED_LIMIT = 60;
  final public static int EXCHANGE_UNSUPPORTED_ARBITRAGE = 61;
  final public static int NO_CONDITIONAL_ORDER_RIGHT = 62;
  final public static int AUTH_FAILED = 63;
  final public static int NOT_AUTHENT = 64;
  final public static int SWAPORDER_UNSUPPORTED = 65;
  final public static int OPTIONS_ONLY_SUPPORT_SPEC = 66;
  final public static int DUPLICATE_EXECORDER_REF = 67;
  final public static int RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH = 68;
  final public static int EXECORDER_NOTOPTIONS = 69;
  final public static int OPTIONS_NOT_SUPPORT_EXEC = 70;
  final public static int BAD_EXECORDER_ACTION_FIELD = 71;
  final public static int DUPLICATE_EXECORDER_ACTION_REF = 72;
  final public static int EXECORDER_NOT_FOUND = 73;
  final public static int OVER_EXECUTE_POSITION = 74;
  final public static int LOGIN_FORBIDDEN = 75;
  final public static int INVALID_TRANSFER_AGENT = 76;
  final public static int NO_FOUND_FUNCTION = 77;
  final public static int SEND_EXCHANGEORDER_FAILED = 78;
  final public static int SEND_EXCHANGEORDERACTION_FAILED = 79;
  final public static int PRICETYPE_NOTSUPPORT_BYEXCHANGE = 80;
  final public static int BAD_EXECUTE_TYPE = 81;
  final public static int BAD_OPTION_INSTR = 82;
  final public static int INSTR_NOTSUPPORT_FORQUOTE = 83;
  final public static int RESEND_QUOTE_BROKERINVESTOR_NOTMATCH = 84;
  final public static int INSTR_NOTSUPPORT_QUOTE = 85;
  final public static int QUOTE_NOT_FOUND = 86;
  final public static int OPTIONS_NOT_SUPPORT_ABANDON = 87;
  final public static int COMBOPTIONS_SUPPORT_IOC_ONLY = 88;
  final public static int OPEN_FILE_FAILED = 89;
  final public static int NEED_RETRY = 90;
  final public static int EXCHANGE_RTNERROR = 91;
  final public static int QUOTE_DERIVEDORDER_ACTIONERROR = 92;
  final public static int INSTRUMENTMAP_NOT_FOUND = 93;
  final public static int CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED = 94;
}
