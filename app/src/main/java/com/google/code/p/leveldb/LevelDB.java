package com.google.code.p.leveldb;

import java.io.File;

import android.content.Context;

public class LevelDB {
    private static LevelDB defaultDB = new LevelDB();


    public static LevelDB getDefaultDB() {
        return defaultDB;
    }


    private long db;
    /*
     * Methods which wrap LevelDB calls, see jni/main.cc for details
     */
    public native boolean open(String dbpath);

    public native void close();

    public native boolean set(String key, String value) throws Exception;

    public native String get(String key1) throws Exception;

    public native boolean delete(String key1) throws Exception;

    public native LevelDBIterator newIterator() throws Exception;

    public boolean setLong(String key, long value) throws Exception {
        return set(key, ""+value);
    }

    public long getLong(String key) throws Exception {
        String v = get(key);
        if (v == null) {
            return 0;
        }
        return Long.parseLong(v);
    }

    static {
        System.loadLibrary("leveldb");
    }
}
