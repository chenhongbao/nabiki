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

package com.nabiki.iop;

public enum MessageType implements java.io.Serializable {
    HEARTBEAT,
    SUB_MD,
    RSP_SUB_MD,
    UNSUB_MD,
    RSP_UNSUB_MD,
    FLOW_DEPTH,
    FLOW_CANDLE,
    REQ_AUTHENTICATE,
    RSP_REQ_AUTHENTICATE,
    REQ_LOGIN,
    RSP_REQ_LOGIN,
    REQ_LOGOUT,
    RSP_REQ_LOGOUT,
    REQ_SETTLEMENT,
    RSP_REQ_SETTLEMENT,
    REQ_ORDER_INSERT,
    RSP_REQ_ORDER_INSERT,
    REQ_ORDER_ACTION,
    RSP_REQ_ORDER_ACTION,
    QRY_MD,
    RSP_QRY_MD,
    QRY_ACCOUNT,
    RSP_QRY_ACCOUNT,
    QRY_ORDER,
    RSP_QRY_ORDER,
    QRY_POSITION,
    RSP_QRY_POSITION,
    QRY_POSI_DETAIL,
    RSP_QRY_POSI_DETAIL,
    QRY_INSTRUMENT,
    RSP_QRY_INSTRUMENT,
    QRY_COMMISSION,
    RSP_QRY_COMMISSION,
    QRY_MARGIN,
    RSP_QRY_MARGIN,
    RTN_ORDER,
    RTN_TRADE,
    RTN_ORDER_ACTION,
    RTN_ORDER_INSERT,
    RSP_ORDER_ACTION,
    RSP_ORDER_INSERT,
    RSP_ERROR,
    RSP_CONNECT,
    RSP_DISCONNECT
}
