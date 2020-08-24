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

package com.nabiki.centre.ctp;

import com.nabiki.objects.CQryInstrumentCommissionRate;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

public class JniConvertionGenerator {

    Collection<Field> getFields(Class<?> clz) {
        return Arrays.asList(clz.getDeclaredFields());
    }

    void createToLocal(Class<?> clz) {
        System.out.println(clz.getName());
        System.out.println("==========================");
        for(var f : getFields(clz)) {
            if (f.getType() == byte.class || f.getType() == Byte.class)
                System.out.println("local." + f.getName() + " = (byte)jni.get" + f.getName() + "();");
            else
                System.out.println("local." + f.getName() + " = jni.get" + f.getName() + "();");
        }
        System.out.println();
    }

    void createToJni(Class<?> clz) {
        System.out.println(clz.getName());
        System.out.println("==========================");
        for(var f : getFields(clz)) {
            if (f.getType() == byte.class || f.getType() == Byte.class)
                System.out.println("jni.set" + f.getName() + "((char)local." + f.getName() + ");");
            else
                System.out.println("jni.set" + f.getName() + "(local." + f.getName() + ");");
        }
        System.out.println();
    }

    @Test
    public void basic() {
        createToJni(CQryInstrumentCommissionRate.class);
    }

}