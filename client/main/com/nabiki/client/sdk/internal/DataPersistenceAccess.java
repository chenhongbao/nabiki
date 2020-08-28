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

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

class DataPersistenceAccess {
    private static class KeyValue {
        final String Key;
        final Serializable Value;
        final boolean toRemove;

        KeyValue(String key, Serializable value, boolean toRemove) {
            this.Key = key;
            this.Value = value;
            this.toRemove = toRemove;
        }
    }

    private static final Path root = Path.of(".dp_cache");
    private static final File index = Path.of(root.toString(),
            ".index").toFile();

    static {
        try {
            ensure(root);
            ensure(Path.of(index.getParent()), index.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private final BlockingQueue<KeyValue> kvs = new LinkedBlockingQueue<>();
    private final Map<String, File> mapping = new ConcurrentHashMap<>();
    private final AtomicBoolean blocked = new AtomicBoolean(false);
    private final Thread daemon = new Thread(() -> {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                var kv = this.kvs.poll(1, TimeUnit.DAYS);
                if (kv != null)
                    process(kv);
            } catch (InterruptedException | IOException ignored) {
            }
        }
    });

    DataPersistenceAccess() {
        init();
        this.daemon.start();
    }

    private static Path ensure(Path dir) throws IOException {
        if (!Files.exists(dir) || !Files.isDirectory(dir))
            Files.createDirectories(dir);
        return dir;
    }

    private static String ensure(Path dir, String file) throws IOException {
        var f = Path.of(dir.toString(), file).toFile();
        if (!f.exists() || !f.isFile()) {
            if (!f.createNewFile())
                throw new IOException("fail creating data cache file");
        }
        return f.toPath().toAbsolutePath().toString();
    }

    private String getLine(BufferedReader br) throws IOException {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            if (line.length() > 0)
                break;
        }
        return line;
    }

    private void init() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(index));
            String key, value;
            while ((key = getLine(br)) != null) {
                value = getLine(br);
                if (value == null)
                    break;
                this.mapping.put(key, new File(value));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean write(String key, Serializable value) {
        if (!this.blocked.get())
            return this.kvs.offer(new KeyValue(key, value, false));
        else
            return false;
    }

    boolean remove(String key) {
        if (!this.blocked.get())
            return this.kvs.offer(new KeyValue(key, null, true));
        else
            return false;
    }

    Object read(String key) {
        var f = this.mapping.get(key);
        if (f == null)
            return null;
        else {
            synchronized (f) {
                try {
                    var o = new ObjectInputStream(new FileInputStream(f));
                    return o.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    return null;
                }
            }
        }
    }

    void flush() {
        this.blocked.set(true);
        this.daemon.interrupt();
        for (var kv : this.kvs)
            try {
                process(kv);
            } catch (IOException ignored) {
            }
    }

    private void appendKey(String key, String file) throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(index, true));
        printKv(pw, key, file);
    }

    private void rewriteKey() throws IOException {
        PrintWriter pw = new PrintWriter(new FileWriter(index, false));
        for (var kv : this.mapping.entrySet())
            printKv(pw, kv.getKey(), kv.getValue().getAbsolutePath());
    }

    private void printKv(PrintWriter pw, String key, String file) {
        pw.write(key);
        pw.write(System.lineSeparator());
        pw.write(file);
        pw.write(System.lineSeparator());
        pw.flush();
    }

    private File insertValue(Serializable value) throws IOException {
        var f = new File(ensure(root, UUID.randomUUID().toString()));
        updateValue(f, value);
        return f;
    }

    private void updateValue(File f, Serializable value) throws IOException {
        var os = new ObjectOutputStream(new FileOutputStream(f, false));
        os.writeObject(value);
        os.flush();
    }

    private void process(KeyValue kv) throws IOException {
        Objects.requireNonNull(kv, "key value null");
        Objects.requireNonNull(kv.Key, "key null");
        if (!kv.toRemove) {
            Objects.requireNonNull(kv.Value, "value null");
            if (this.mapping.containsKey(kv.Key)) {
                // Update.
                updateValue(this.mapping.get(kv.Key), kv.Value);
            } else {
                // Insert.
                var file = insertValue(kv.Value);
                appendKey(kv.Key, file.getAbsolutePath());
                this.mapping.put(kv.Key, file);
            }
        } else {
            // Remove key value.
            var f = this.mapping.get(kv.Key);
            if (f != null) {
                // Remove content, then mapping.
                Files.delete(f.toPath());
                this.mapping.remove(kv.Key);
                // Rewrite index.
                rewriteKey();
            }
        }
    }
}
