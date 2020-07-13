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

package com.nabiki.iop.x;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorCode;
import com.nabiki.ctp4j.jni.flag.TThostFtdcErrorMessage;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OP {
    // GSON.
    private final static Gson gson;
    static {
        gson = new GsonBuilder()
                .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                .serializeNulls()
                .setPrettyPrinting()
                .create();
    }

    /**
     * Parse the specified JSON string to object of the specified {@link Class}.
     *
     * @param json JSON string
     * @param clz {@link Class} of the object
     * @param <T> generic type of the object
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

    private static final Map<Integer, String> errorMsg = new ConcurrentHashMap<>();
    static {
        errorMsg.put(TThostFtdcErrorCode.NONE, TThostFtdcErrorMessage.NONE);
        errorMsg.put(TThostFtdcErrorCode.INVALID_DATA_SYNC_STATUS, TThostFtdcErrorMessage.INVALID_DATA_SYNC_STATUS);
        errorMsg.put(TThostFtdcErrorCode.INCONSISTENT_INFORMATION, TThostFtdcErrorMessage.INCONSISTENT_INFORMATION);
        errorMsg.put(TThostFtdcErrorCode.INVALID_LOGIN, TThostFtdcErrorMessage.INVALID_LOGIN);
        errorMsg.put(TThostFtdcErrorCode.USER_NOT_ACTIVE, TThostFtdcErrorMessage.USER_NOT_ACTIVE);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_LOGIN, TThostFtdcErrorMessage.DUPLICATE_LOGIN);
        errorMsg.put(TThostFtdcErrorCode.NOT_LOGIN_YET, TThostFtdcErrorMessage.NOT_LOGIN_YET);
        errorMsg.put(TThostFtdcErrorCode.NOT_INITED, TThostFtdcErrorMessage.NOT_INITED);
        errorMsg.put(TThostFtdcErrorCode.FRONT_NOT_ACTIVE, TThostFtdcErrorMessage.FRONT_NOT_ACTIVE);
        errorMsg.put(TThostFtdcErrorCode.NO_PRIVILEGE, TThostFtdcErrorMessage.NO_PRIVILEGE);
        errorMsg.put(TThostFtdcErrorCode.CHANGE_OTHER_PASSWORD, TThostFtdcErrorMessage.CHANGE_OTHER_PASSWORD);
        errorMsg.put(TThostFtdcErrorCode.USER_NOT_FOUND, TThostFtdcErrorMessage.USER_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.BROKER_NOT_FOUND, TThostFtdcErrorMessage.BROKER_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.INVESTOR_NOT_FOUND, TThostFtdcErrorMessage.INVESTOR_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.OLD_PASSWORD_MISMATCH, TThostFtdcErrorMessage.OLD_PASSWORD_MISMATCH);
        errorMsg.put(TThostFtdcErrorCode.BAD_FIELD, TThostFtdcErrorMessage.BAD_FIELD);
        errorMsg.put(TThostFtdcErrorCode.INSTRUMENT_NOT_FOUND, TThostFtdcErrorMessage.INSTRUMENT_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.INSTRUMENT_NOT_TRADING, TThostFtdcErrorMessage.INSTRUMENT_NOT_TRADING);
        errorMsg.put(TThostFtdcErrorCode.NOT_EXCHANGE_PARTICIPANT, TThostFtdcErrorMessage.NOT_EXCHANGE_PARTICIPANT);
        errorMsg.put(TThostFtdcErrorCode.INVESTOR_NOT_ACTIVE, TThostFtdcErrorMessage.INVESTOR_NOT_ACTIVE);
        errorMsg.put(TThostFtdcErrorCode.NOT_EXCHANGE_CLIENT, TThostFtdcErrorMessage.NOT_EXCHANGE_CLIENT);
        errorMsg.put(TThostFtdcErrorCode.NO_VALID_TRADER_AVAILABLE, TThostFtdcErrorMessage.NO_VALID_TRADER_AVAILABLE);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_ORDER_REF, TThostFtdcErrorMessage.DUPLICATE_ORDER_REF);
        errorMsg.put(TThostFtdcErrorCode.BAD_ORDER_ACTION_FIELD, TThostFtdcErrorMessage.BAD_ORDER_ACTION_FIELD);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_ORDER_ACTION_REF, TThostFtdcErrorMessage.DUPLICATE_ORDER_ACTION_REF);
        errorMsg.put(TThostFtdcErrorCode.ORDER_NOT_FOUND, TThostFtdcErrorMessage.ORDER_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.INSUITABLE_ORDER_STATUS, TThostFtdcErrorMessage.INSUITABLE_ORDER_STATUS);
        errorMsg.put(TThostFtdcErrorCode.UNSUPPORTED_FUNCTION, TThostFtdcErrorMessage.UNSUPPORTED_FUNCTION);
        errorMsg.put(TThostFtdcErrorCode.NO_TRADING_RIGHT, TThostFtdcErrorMessage.NO_TRADING_RIGHT);
        errorMsg.put(TThostFtdcErrorCode.CLOSE_ONLY, TThostFtdcErrorMessage.CLOSE_ONLY);
        errorMsg.put(TThostFtdcErrorCode.OVER_CLOSE_POSITION, TThostFtdcErrorMessage.OVER_CLOSE_POSITION);
        errorMsg.put(TThostFtdcErrorCode.INSUFFICIENT_MONEY, TThostFtdcErrorMessage.INSUFFICIENT_MONEY);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_PK, TThostFtdcErrorMessage.DUPLICATE_PK);
        errorMsg.put(TThostFtdcErrorCode.CANNOT_FIND_PK, TThostFtdcErrorMessage.CANNOT_FIND_PK);
        errorMsg.put(TThostFtdcErrorCode.CAN_NOT_INACTIVE_BROKER, TThostFtdcErrorMessage.CAN_NOT_INACTIVE_BROKER);
        errorMsg.put(TThostFtdcErrorCode.BROKER_SYNCHRONIZING, TThostFtdcErrorMessage.BROKER_SYNCHRONIZING);
        errorMsg.put(TThostFtdcErrorCode.BROKER_SYNCHRONIZED, TThostFtdcErrorMessage.BROKER_SYNCHRONIZED);
        errorMsg.put(TThostFtdcErrorCode.SHORT_SELL, TThostFtdcErrorMessage.SHORT_SELL);
        errorMsg.put(TThostFtdcErrorCode.INVALID_SETTLEMENT_REF, TThostFtdcErrorMessage.INVALID_SETTLEMENT_REF);
        errorMsg.put(TThostFtdcErrorCode.CFFEX_NETWORK_ERROR, TThostFtdcErrorMessage.CFFEX_NETWORK_ERROR);
        errorMsg.put(TThostFtdcErrorCode.CFFEX_OVER_REQUEST, TThostFtdcErrorMessage.CFFEX_OVER_REQUEST);
        errorMsg.put(TThostFtdcErrorCode.CFFEX_OVER_REQUEST_PER_SECOND, TThostFtdcErrorMessage.CFFEX_OVER_REQUEST_PER_SECOND);
        errorMsg.put(TThostFtdcErrorCode.SETTLEMENT_INFO_NOT_CONFIRMED, TThostFtdcErrorMessage.SETTLEMENT_INFO_NOT_CONFIRMED);
        errorMsg.put(TThostFtdcErrorCode.DEPOSIT_NOT_FOUND, TThostFtdcErrorMessage.DEPOSIT_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.EXCHANG_TRADING, TThostFtdcErrorMessage.EXCHANG_TRADING);
        errorMsg.put(TThostFtdcErrorCode.PARKEDORDER_NOT_FOUND, TThostFtdcErrorMessage.PARKEDORDER_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.PARKEDORDER_HASSENDED, TThostFtdcErrorMessage.PARKEDORDER_HASSENDED);
        errorMsg.put(TThostFtdcErrorCode.PARKEDORDER_HASDELETE, TThostFtdcErrorMessage.PARKEDORDER_HASDELETE);
        errorMsg.put(TThostFtdcErrorCode.INVALID_INVESTORIDORPASSWORD, TThostFtdcErrorMessage.INVALID_INVESTORIDORPASSWORD);
        errorMsg.put(TThostFtdcErrorCode.INVALID_LOGIN_IPADDRESS, TThostFtdcErrorMessage.INVALID_LOGIN_IPADDRESS);
        errorMsg.put(TThostFtdcErrorCode.OVER_CLOSETODAY_POSITION, TThostFtdcErrorMessage.OVER_CLOSETODAY_POSITION);
        errorMsg.put(TThostFtdcErrorCode.OVER_CLOSEYESTERDAY_POSITION, TThostFtdcErrorMessage.OVER_CLOSEYESTERDAY_POSITION);
        errorMsg.put(TThostFtdcErrorCode.BROKER_NOT_ENOUGH_CONDORDER, TThostFtdcErrorMessage.BROKER_NOT_ENOUGH_CONDORDER);
        errorMsg.put(TThostFtdcErrorCode.INVESTOR_NOT_ENOUGH_CONDORDER, TThostFtdcErrorMessage.INVESTOR_NOT_ENOUGH_CONDORDER);
        errorMsg.put(TThostFtdcErrorCode.BROKER_NOT_SUPPORT_CONDORDER, TThostFtdcErrorMessage.BROKER_NOT_SUPPORT_CONDORDER);
        errorMsg.put(TThostFtdcErrorCode.RESEND_ORDER_BROKERINVESTOR_NOTMATCH, TThostFtdcErrorMessage.RESEND_ORDER_BROKERINVESTOR_NOTMATCH);
        errorMsg.put(TThostFtdcErrorCode.SYC_OTP_FAILED, TThostFtdcErrorMessage.SYC_OTP_FAILED);
        errorMsg.put(TThostFtdcErrorCode.OTP_MISMATCH, TThostFtdcErrorMessage.OTP_MISMATCH);
        errorMsg.put(TThostFtdcErrorCode.OTPPARAM_NOT_FOUND, TThostFtdcErrorMessage.OTPPARAM_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.UNSUPPORTED_OTPTYPE, TThostFtdcErrorMessage.UNSUPPORTED_OTPTYPE);
        errorMsg.put(TThostFtdcErrorCode.SINGLEUSERSESSION_EXCEED_LIMIT, TThostFtdcErrorMessage.SINGLEUSERSESSION_EXCEED_LIMIT);
        errorMsg.put(TThostFtdcErrorCode.EXCHANGE_UNSUPPORTED_ARBITRAGE, TThostFtdcErrorMessage.EXCHANGE_UNSUPPORTED_ARBITRAGE);
        errorMsg.put(TThostFtdcErrorCode.NO_CONDITIONAL_ORDER_RIGHT, TThostFtdcErrorMessage.NO_CONDITIONAL_ORDER_RIGHT);
        errorMsg.put(TThostFtdcErrorCode.AUTH_FAILED, TThostFtdcErrorMessage.AUTH_FAILED);
        errorMsg.put(TThostFtdcErrorCode.NOT_AUTHENT, TThostFtdcErrorMessage.NOT_AUTHENT);
        errorMsg.put(TThostFtdcErrorCode.SWAPORDER_UNSUPPORTED, TThostFtdcErrorMessage.SWAPORDER_UNSUPPORTED);
        errorMsg.put(TThostFtdcErrorCode.OPTIONS_ONLY_SUPPORT_SPEC, TThostFtdcErrorMessage.OPTIONS_ONLY_SUPPORT_SPEC);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_EXECORDER_REF, TThostFtdcErrorMessage.DUPLICATE_EXECORDER_REF);
        errorMsg.put(TThostFtdcErrorCode.RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH, TThostFtdcErrorMessage.RESEND_EXECORDER_BROKERINVESTOR_NOTMATCH);
        errorMsg.put(TThostFtdcErrorCode.EXECORDER_NOTOPTIONS, TThostFtdcErrorMessage.EXECORDER_NOTOPTIONS);
        errorMsg.put(TThostFtdcErrorCode.OPTIONS_NOT_SUPPORT_EXEC, TThostFtdcErrorMessage.OPTIONS_NOT_SUPPORT_EXEC);
        errorMsg.put(TThostFtdcErrorCode.BAD_EXECORDER_ACTION_FIELD, TThostFtdcErrorMessage.BAD_EXECORDER_ACTION_FIELD);
        errorMsg.put(TThostFtdcErrorCode.DUPLICATE_EXECORDER_ACTION_REF, TThostFtdcErrorMessage.DUPLICATE_EXECORDER_ACTION_REF);
        errorMsg.put(TThostFtdcErrorCode.EXECORDER_NOT_FOUND, TThostFtdcErrorMessage.EXECORDER_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.OVER_EXECUTE_POSITION, TThostFtdcErrorMessage.OVER_EXECUTE_POSITION);
        errorMsg.put(TThostFtdcErrorCode.LOGIN_FORBIDDEN, TThostFtdcErrorMessage.LOGIN_FORBIDDEN);
        errorMsg.put(TThostFtdcErrorCode.INVALID_TRANSFER_AGENT, TThostFtdcErrorMessage.INVALID_TRANSFER_AGENT);
        errorMsg.put(TThostFtdcErrorCode.NO_FOUND_FUNCTION, TThostFtdcErrorMessage.NO_FOUND_FUNCTION);
        errorMsg.put(TThostFtdcErrorCode.SEND_EXCHANGEORDER_FAILED, TThostFtdcErrorMessage.SEND_EXCHANGEORDER_FAILED);
        errorMsg.put(TThostFtdcErrorCode.SEND_EXCHANGEORDERACTION_FAILED, TThostFtdcErrorMessage.SEND_EXCHANGEORDERACTION_FAILED);
        errorMsg.put(TThostFtdcErrorCode.PRICETYPE_NOTSUPPORT_BYEXCHANGE, TThostFtdcErrorMessage.PRICETYPE_NOTSUPPORT_BYEXCHANGE);
        errorMsg.put(TThostFtdcErrorCode.BAD_EXECUTE_TYPE, TThostFtdcErrorMessage.BAD_EXECUTE_TYPE);
        errorMsg.put(TThostFtdcErrorCode.BAD_OPTION_INSTR, TThostFtdcErrorMessage.BAD_OPTION_INSTR);
        errorMsg.put(TThostFtdcErrorCode.INSTR_NOTSUPPORT_FORQUOTE, TThostFtdcErrorMessage.INSTR_NOTSUPPORT_FORQUOTE);
        errorMsg.put(TThostFtdcErrorCode.RESEND_QUOTE_BROKERINVESTOR_NOTMATCH, TThostFtdcErrorMessage.RESEND_QUOTE_BROKERINVESTOR_NOTMATCH);
        errorMsg.put(TThostFtdcErrorCode.INSTR_NOTSUPPORT_QUOTE, TThostFtdcErrorMessage.INSTR_NOTSUPPORT_QUOTE);
        errorMsg.put(TThostFtdcErrorCode.QUOTE_NOT_FOUND, TThostFtdcErrorMessage.QUOTE_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.OPTIONS_NOT_SUPPORT_ABANDON, TThostFtdcErrorMessage.OPTIONS_NOT_SUPPORT_ABANDON);
        errorMsg.put(TThostFtdcErrorCode.COMBOPTIONS_SUPPORT_IOC_ONLY, TThostFtdcErrorMessage.COMBOPTIONS_SUPPORT_IOC_ONLY);
        errorMsg.put(TThostFtdcErrorCode.OPEN_FILE_FAILED, TThostFtdcErrorMessage.OPEN_FILE_FAILED);
        errorMsg.put(TThostFtdcErrorCode.NEED_RETRY, TThostFtdcErrorMessage.NEED_RETRY);
        errorMsg.put(TThostFtdcErrorCode.EXCHANGE_RTNERROR, TThostFtdcErrorMessage.EXCHANGE_RTNERROR);
        errorMsg.put(TThostFtdcErrorCode.QUOTE_DERIVEDORDER_ACTIONERROR, TThostFtdcErrorMessage.QUOTE_DERIVEDORDER_ACTIONERROR);
        errorMsg.put(TThostFtdcErrorCode.INSTRUMENTMAP_NOT_FOUND, TThostFtdcErrorMessage.INSTRUMENTMAP_NOT_FOUND);
        errorMsg.put(TThostFtdcErrorCode.CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED, TThostFtdcErrorMessage.CANCELLATION_OF_OTC_DERIVED_ORDER_NOT_ALLOWED);
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
