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

package com.nabiki.client.sdk.internal;

import com.nabiki.client.sdk.DataPersistence;
import com.nabiki.client.sdk.DataPersistenceFactory;
import org.junit.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DataPersistenceTest {
    static class MyInfo implements java.io.Serializable {
        public String category;
        public LocalDateTime updateTime;
    }

    static Integer i = 12345;
    static Boolean b = true;
    static Long l = 54321L;
    static Double d = 0.12345D;
    static LinkedList<String> id = new LinkedList<>();
    static HashMap<String, String> idMap = new HashMap<>();
    static MyInfo info = new MyInfo();
    static DataPersistenceFactory factory = new DataPersistenceFactoryImpl();

    static {
        initValue();
        // Comment the line below to test init a existing cache.
        putValue(factory.get());
    }

    private static void putValue(DataPersistence pers) {
        // Test primitive types.
        pers.put("integer", i);
        pers.put("boolean", b);
        pers.put("long", l);
        pers.put("double", d);

        // Test container.
        pers.put("list", id);
        pers.put("map", idMap);

        // Test user-defined object.
        pers.put("myinfo", info);
    }

    private static void initValue() {
        // Test container.
        for (int x = 0; x < 10; ++x) {
            var str = String.valueOf(x);
            id.add(str);
            idMap.put(str, str);
        }

        // Test user-defined object.
        info.category = "schedule";
        info.updateTime = LocalDateTime.of(1, 2, 3, 4, 5, 6);
    }

    private void checkValues(DataPersistence pers) {
        var integer = pers.get("integer");
        assertTrue(integer instanceof  Integer);
        assertEquals(integer, i);

        var bool = pers.get("boolean");
        assertTrue(bool instanceof  Boolean);
        assertEquals(bool, b);

        var ln = pers.get("long");
        assertTrue(ln instanceof Long);
        assertEquals(ln, l);

        var dou = pers.get("double");
        assertTrue(dou instanceof Double);
        assertEquals(dou, d);

        var list = pers.get("list");
        assertTrue(list instanceof LinkedList);

        LinkedList<String> idList = (LinkedList<String>)list;
        assertEquals(idList.size(), id.size());

        for (int index = 0; index < id.size(); ++index)
            assertEquals(idList.get(index), id.get(index));

        var map = pers.get("map");
        assertTrue(map instanceof HashMap);

        HashMap<String, String> ids = (HashMap<String, String>)map;
        for (var key : ids.keySet())
            assertEquals(ids.get(key), idMap.get(key));

        var inf = pers.get("myinfo");
        assertTrue(inf instanceof  MyInfo);
        assertEquals(((MyInfo) inf).category, info.category);
        assertEquals(((MyInfo) inf).updateTime, info.updateTime);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void basic() {
        sleep(1000);
        checkValues(factory.get());
    }

    @Test
    public void loadExist() {
        sleep(1000);
        checkValues(new DataPersistenceFactoryImpl().get());
    }
}