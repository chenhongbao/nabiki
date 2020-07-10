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

package com.nabiki.iop.frame;

import com.nabiki.iop.MessageType;

import java.util.UUID;

public class Body implements java.io.Serializable {
    /**
     * If the message is a request, the request ID identifies this request. If the
     * message is a response, the request ID identifies that the request that
     * it responds to.
     */
    public UUID RequestID;

    /**
     * If the message is a response, the response ID identifies this response. Else
     * the field is set to null.
     */
    public UUID ResponseID;

    /**
     * Message type {@link MessageType}.
     */
    public MessageType Type;

    /**
     * The counter counts how many messages have ben sent for this request or
     * response. It starts from 1, to total count. It can't be larger than total
     * count.
     */
    public int CurrentCount;

    /**
     * How many messages to be sent in total. Must be at least 1.
     */
    public int TotalCount;

    /**
     * JSON string representation of the data in this message.
     */
    public String Body;

    /**
     * JSON string representation of rsp info in this message.
     */
    public String RspInfo;

    public Body() {}
}
