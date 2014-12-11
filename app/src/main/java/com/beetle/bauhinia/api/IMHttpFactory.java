package com.beetle.bauhinia.api;

/**
 * Created by tsung on 10/10/14.
 */
public class IMHttpFactory {
    static final Object monitor = new Object();
    static IMHttp singleton;

    public static IMHttp Singleton() {
        if (singleton == null) {
            synchronized (monitor) {
                if (singleton == null) {
                    singleton = new IMHttpRetrofit().getService();
                }
            }
        }

        return singleton;
    }
}
