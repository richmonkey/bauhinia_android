package com.google.code.p.leveldb;

/**
 * Created by houxh on 14-7-26.
 */
public class LevelDBIterator {
    private long iter;//native iterator

    public LevelDBIterator(long iter) {
        this.iter = iter;
    }

    public native void close();
    public native void seekToLast();
    public native void seekToFirst();
    public native void seek(String target);
    public native boolean isValid();
    public native void next();
    public native void prev();
    public native String getKey();
    public native String getValue();
}
