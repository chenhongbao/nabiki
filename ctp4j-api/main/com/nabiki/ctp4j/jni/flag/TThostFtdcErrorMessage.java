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

package com.nabiki.ctp4j.jni.flag;

public class TThostFtdcErrorMessage {
    final public static String NONE = "NONE";
    final public static String INVALID_DATA_SYNC_STATUS = "INVALID_DATA_SYNC_STATUS";
    final public static String INCONSISTENT_INFORMATION = "INCONSISTENT_INFORMATION";
    final public static String INVALID_LOGIN = "INVALID_LOGIN";
    final public static String USER_NOT_ACTIVE = "USER_NOT_ACTIVE";
    final public static String DUPLICATE_LOGIN = "DUPLICATE_LOGIN";
    final public static String NOT_LOGIN_YET = "NOT_LOGIN_YET";
    final public static String NOT_INITED = "NOT_INITED";
    final public static String FRONT_NOT_ACTIVE = "FRONT_NOT_ACTIVE";
    final public static String NO_PRIVILEGE = "NO_PRIVILEGE";
    final public static String CHANGE_OTHER_PASSWORD = "CHANGE_OTHER_PASSWORD";
    final public static String USER_NOT_FOUND = "USER_NOT_FOUND";
    final public static String BROKER_NOT_FOUND = "BROKER_NOT_FOUND";
    final public static String INVESTOR_NOT_FOUND = "INVESTOR_NOT_FOUND";
    final public static String OLD_PASSWORD_MISMATCH = "OLD_PASSWORD_MISMATCH";
    final public static String BAD_FIELD = "BAD_FIELD";
    final public static String INSTRUMENT_NOT_FOUND = "INSTRUMENT_NOT_FOUND";
    final public static String INSTRUMENT_NOT_TRADING = "INSTRUMENT_NOT_TRADING";
    final public static String NOT_EXCHANGE_PARTICIPANT = "NOT_EXCHANGE_PARTICIPANT";
    final public static String INVESTOR_NOT_ACTIVE = "INVESTOR_NOT_ACTIVE";
    final public static String NOT_EXCHANGE_CLIENT = "NOT_EXCHANGE_CLIENT";
    final public static String NO_VALID_TRADER_AVAILABLE = "NO_VALID_TRADER_AVAILABLE";
    final public static String DUPLICATE_ORDER_REF = "DUPLICATE_ORDER_REF";
    final public static String BAD_ORDER_ACTION_FIELD = "BAD_ORDER_ACTION_FIELD";
    final public static String DUPLICATE_ORDER_ACTION_REF = "DUPLICATE_ORDER_ACTION_REF";
    final public static String ORDER_NOT_FOUND = "ORDER_NOT_FOUND";
    final public static String INSUITABLE_ORDER_STATUS = "INSUITABLE_ORDER_STATUS";
    final public static String UNSUPPORTED_FUNCTION = "UNSUPPORTED_FUNCTION";
    final public static String NO_TRADING_RIGHT = "NO_TRADING_RIGHT";
    final public static String CLOSE_ONLY = "CLOSE_ONLY";
    final public static String OVER_CLOSE_POSITION = "OVER_CLOSE_POSITION";
    final public static String INSUFFICIENT_MONEY = "INSUFFICIENT_MONEY";
    final public static String DUPLICATE_PK = "DUPLICATE_PK";
    final public static String CANNOT_FIND_PK = "CANNOT_FIND_PK";
    final public static String CAN_NOT_INACTIVE_BROKER = "CAN_NOT_INACTIVE_BROKER";
    final public static String BROKER_SYNCHRONIZING = "BROKER_SYNCHRONIZING";
    final public static String BROKER_SYNCHRONIZED = "BROKER_SYNCHRONIZED";
    final public static String SHORT_SELL = "SHORT_SELL";
    final public static String INVALID_SETTLEMENT_REF = "INVALID_SETTLEMENT_REF";
    final public static String CFFEX_NETWORK_ERROR = "CFFEX_NETWORK_ERROR";
    final public static String CFFEX_OVER_REQUEST = "CFFEX_OVER_REQUEST";
    final public static String CFFEX_OVER_REQUEST_PER_SECOND = "CFFEX_OVER_REQUEST_PER_SECOND";
    final public static String SETTLEMENT_INFO_NOT_CONFIRMED = "SETTLEMENT_INFO_NOT_CONFIRMED";
    final public static String DEPOSIT_NOT_FOUND = "DEPOSIT_NOT_FOUND";
    final public static String EXCHANG_TRADING = "EXCHANG_TRADING";
    final public static String PARKEDORDER_NOT_FOUND = "PARKEDORDER_NOT_FOUND";
    final public static String PARKEDORDER_HASSENDED = "PARKEDORDER_HASSENDED";
    final public static String PARKEDORDER_HASDELETE = "PARKEDORDER_HASDELETE";
    final public static String INVALID_INVESTORIDORPASSWORD = "INVALID_INVESTORIDORPASSWORD";
    final public static String INVALID_LOGIN_IPADDRESS = "INVALID_LOGIN_IPADDRESS";
    final public static String OVER_CLOSETODAY_POSITION = "OVER_CLOSETODAY_POSITION";
    final public static String OVER_CLOSEYESTERDAY_POSITION = "OVER_CLOSEYESTERDAY_POSITION";
    final public static String BROKER_NOT_ENOUGH_CONDORDER = "BROKER_NOT_ENOUGH_CONDORDER";
    final public static String INVESTOR_NOT_ENOUGH_CONDORDER = "INVESTOR_NOT_ENOUGH_CONDORDER";
    final public static String BROKER_NOT_SUPPORT_CONDORDER = "BROKER_NOT_SUPPORT_CONDORDER";
    final public static String RESEND_ORDER_BROKERINVESTOR_NOTMATCH = "RESEND_ORDER_BROKERINVESTOR_NOTMATCH";
    final public static String SYC_OTP_FAILED = "SYC_OTP_FAILED";
    final public static String OTP_MISMATCH = "OTP_MISMATCH";
    final public static String OTPPARAM_NOT_FOUND = "OTPPARAM_NOT_FOUND";
    final public static String UNSUPPORTED_OTPTYPE = "UNSUPPORTED_OTPTYPE";
    final public static String SINGLEUSERSESSION_EXCEED_LIMIT = "SINGLEUSERSESSION_EXCEED_LIMIT";
    final public static String EXCHANGE_UNSUPPORTED_ARBITRAGE = "EXCHANGE_UNSUPPORTED_ARBITRAGE";
    final public static String NO_CONDITIONAL_ORDER_RIGHT = "NO_CONDITIONAL_ORDER_RIGHT";
    final public static String AUTH_FAILED = "AUTH_FAILED";
    final public static String NOT_AUTHENT = "NOT_AUTHENT";
    final public static String SWAPORDER_UNSUPPORTED = "SWAPORDER_UNSUPPORTED";
    final public static String OPTIONS_ONLY_SUPPORT_SPEC = "OPTIONS_ONLY_SUPPORT_SPEC";
    final public static String DUPLICATE_EXECORDER_REF = "DUPLICATE_EXECORDER_REF";
    final public static String RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH = "RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH";
    final public static String EXECORDER_NOTOPTIONS = "EXECORDER_NOTOPTIONS";
    final public static String OPTIONS_NOT_SUPPORT_EXEC = "OPTIONS_NOT_SUPPORT_EXEC";
    final public static String BAD_EXECORDER_ACTION_FIELD = "BAD_EXECORDER_ACTION_FIELD";
    final public static String DUPLICATE_EXECORDER_ACTION_REF = "DUPLICATE_EXECORDER_ACTION_REF";
    final public static String EXECORDER_NOT_FOUND = "EXECORDER_NOT_FOUND";
    final public static String OVER_EXECUTE_POSITION = "OVER_EXECUTE_POSITION";
    final public static String LOGIN_FORBIDDEN = "LOGIN_FORBIDDEN";
    final public static String INVALID_TRANSFER_AGENT = "INVALID_TRANSFER_AGENT";
    final public static String NO_FOUND_FUNCTION = "NO_FOUND_FUNCTION";
    final public static String SEND_EXCHANGEORDER_FAILED = "SEND_EXCHANGEORDER_FAILED";
    final public static String SEND_EXCHANGEORDERACTION_FAILED = "SEND_EXCHANGEORDERACTION_FAILED";
    final public static String PRICETYPE_NOTSUPPORT_BYEXCHANGE = "PRICETYPE_NOTSUPPORT_BYEXCHANGE";
    final public static String BAD_EXECUTE_TYPE = "BAD_EXECUTE_TYPE";
    final public static String BAD_OPTION_INSTR = "BAD_OPTION_INSTR";
    final public static String INSTR_NOTSUPPORT_FORQUOTE = "INSTR_NOTSUPPORT_FORQUOTE";
    final public static String RESEND_QUOTE_BROKERINVESTOR_NOTMATCH = "RESEND_QUOTE_BROKERINVESTOR_NOTMATCH";
    final public static String INSTR_NOTSUPPORT_QUOTE = "INSTR_NOTSUPPORT_QUOTE";
    final public static String QUOTE_NOT_FOUND = "QUOTE_NOT_FOUND";
    final public static String OPTIONS_NOT_SUPPORT_ABANDON = "OPTIONS_NOT_SUPPORT_ABANDON";
    final public static String COMBOPTIONS_SUPPORT_IOC_ONLY = "COMBOPTIONS_SUPPORT_IOC_ONLY";
    final public static String OPEN_FILE_FAILED = "OPEN_FILE_FAILED";
    final public static String NEED_RETRY = "NEED_RETRY";
    final public static String EXCHANGE_RTNERROR = "EXCHANGE_RTNERROR";
    final public static String QUOTE_DERIVEDORDER_ACTIONERROR = "QUOTE_DERIVEDORDER_ACTIONERROR";
    final public static String INSTRUMENTMAP_NOT_FOUND = "INSTRUMENTMAP_NOT_FOUND";
    final public static String CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED = "CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED";
}
