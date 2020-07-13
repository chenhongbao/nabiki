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

package com.nabiki.ctp4j.cpp;

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * This class is more than a test.
 * The class reads the Java code in {@link com.nabiki.ctp4j.jni.struct} and generate
 * C++ code for JSON-Object getter and setter.
 */
public class CppCodeGenerator {
    static Path codeBase = Path.of("main\\com\\nabiki\\ctp4j\\jni\\struct");

    private File[] list(Path root) {
        assertTrue(Files.exists(root) && Files.isDirectory(root));
        return root.toFile().listFiles();
    }

    enum FieldType {
        INT, STRING, FLAG, DOUBLE, UNKNOWN
    }

    private static class FieldDescriptor {
        public FieldType Type;
        public String Name;
    }

    private static class ClassDescriptor {
        public String Name;
        public List<FieldDescriptor> Fields = new LinkedList<>();
    }

    static String[] keywords = new String[]{"class", "public", "protected", "private", "static", "default", "final", "implements", "extends", ";", "{", "}"};

    private boolean isKeyword(String token) {
        for (var key : keywords)
            if (key.compareTo(token) == 0)
                return true;
        return false;
    }

    private String getClassName(String line) {
        if (line == null || line.trim().length() == 0
                || line.trim().compareToIgnoreCase("}") == 0)
            return null;
        if (!line.contains("class"))
            return null;
        for (var token : line.trim().split(" ")) {
            if (token.trim().length() == 0 || isKeyword(token))
                continue;
            return token;
        }
        return null;
    }

    private FieldDescriptor getFieldDescriptor(String line) {
        if (line == null || line.trim().length() == 0
                || line.trim().compareToIgnoreCase("}") == 0)
            return null;

        FieldDescriptor desc = new FieldDescriptor();
        desc.Type = FieldType.UNKNOWN;

        for (var token : line.trim().split(" ")) {
            if (token.trim().length() == 0 || isKeyword(token))
                continue;
            if (token.compareTo("String") == 0)
                desc.Type = FieldType.STRING;
            else if (token.compareTo("int") == 0 || token.compareTo("Integer") == 0
                    || token.compareTo("long") == 0 || token.compareTo("Long") == 0) {
                desc.Type = FieldType.INT;
            } else if (token.compareTo("double") == 0 || token.compareTo("Double") == 0
                    || token.compareTo("float") == 0 || token.compareTo("Float") == 0) {
                desc.Type = FieldType.DOUBLE;
            } else if (token.compareTo("byte") == 0 || token.compareTo("Byte") == 0) {
                desc.Type = FieldType.FLAG;
            } else {
                if (token.endsWith(";"))
                    desc.Name = token.substring(0, token.indexOf(";"));
            }
        }

        if (desc.Type != null && desc.Type != FieldType.UNKNOWN && desc.Name != null)
            return desc;
        else
            return null;
    }

    private ClassDescriptor getClassDescriptor(File file) {
        ClassDescriptor desc = new ClassDescriptor();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (desc.Name == null) {
                    desc.Name = getClassName(line);
                    if (desc.Name == null)
                        System.err.println("[N] " + line);
                    else
                        System.out.println("[Y] " + line);
                } else {
                    var field = getFieldDescriptor(line);
                    if (field == null)
                        System.err.println("[N] " + line);
                    else {
                        System.out.println("[Y] " + line);
                        desc.Fields.add(field);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (desc.Name == null)
            return null;
        else
            return desc;
    }

    private List<ClassDescriptor> getDescriptors(File[] files) {
        List<ClassDescriptor> desc = new LinkedList<>();
        for (var file : files) {
            var d = getClassDescriptor(file);
            if (d == null)
                System.err.println("Fail parsing file " + file.getName());
            else
                desc.add(d);
        }
        return desc;
    }
    static String nl() {
        return System.lineSeparator();
    }

    private String generatePart(String prefix, List<FieldDescriptor> fields) {
        String code = "";
        for (var f : fields) {
            code += prefix;
            switch (f.Type) {
                case INT:
                    code += "int(";
                    break;
                case FLAG:
                    code += "flag(";
                    break;
                case DOUBLE:
                    code += "double(";
                    break;
                case STRING:
                    code += "string(";
                    break;
                default:
                    System.err.println("unknown field type");
                    break;
            }
            code += f.Name + ");" + nl();
        }
        return code;
    }

    private String generateCode(List<ClassDescriptor> desc) {
        String code  = "/*";
        code += "This file is generated by program." + nl();
        code += "*/" + nl();
        code += "#pragma once" + nl();
        code += "#include \"rjmacro.h\"" + nl();
        code += "#include \"ThostFtdcUserApiStruct.h\"" + nl();

        for (var d : desc) {
            code += "setter_def(" + d.Name + "){" + nl();
            code += "parse_or_throw();" + nl();
            code += generatePart("set_", d.Fields);
            code += "}" + nl();
            code += "getter_def(" + d.Name + "){" + nl();
            code += "document();" + nl();
            code += generatePart("get_", d.Fields);
            code += "write_document();" + nl();
            code += "}" + nl();
        }

        return code;
    }

    @Test
    public void basic() {
        var files = list(codeBase);
        assertNotEquals(0, files.length);
        var code = generateCode(getDescriptors(files));

        try (PrintWriter pw = new PrintWriter(new FileWriter("generated.hpp", false))) {
            pw.write(code);
            pw.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
