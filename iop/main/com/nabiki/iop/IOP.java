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

import com.nabiki.iop.internal.IOPClientImpl;
import com.nabiki.iop.internal.IOPServerImpl;

import java.io.IOException;
import java.net.InetSocketAddress;

public class IOP {
    /**
     * Create a server bound to the specified address and talking the IOP frame.
     *
     * @param bindAddress {@link InetSocketAddress} to bind to
     * @return server instance
     * @throws IOException if fail binding the address
     */
    public static IOPServer createServer(InetSocketAddress bindAddress)
            throws IOException {
        return new IOPServerImpl(bindAddress);
    }

    /**
     * Create a client connected to the specified address and talking the IOP frame.
     *
     * @param connectAddress {@link InetSocketAddress} to connect to
     * @return client instance
     */
    public static IOPClient createClient(InetSocketAddress connectAddress)
            throws IOException {
        return new IOPClientImpl(connectAddress);
    }
}
