package com.beetle.bauhinia.api;

import com.beetle.bauhinia.Config;
import com.beetle.bauhinia.Token;
import com.google.gson.Gson;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.GsonConverter;

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
                    singleton = newIMHttp();
                }
            }
        }

        return singleton;
    }

    private static IMHttp newIMHttp() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(Config.API_URL)
                .setConverter(new GsonConverter(new Gson()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        if (Token.getInstance().accessToken != null && !Token.getInstance().accessToken.equals("")) {
                            request.addHeader("Authorization", "Bearer " + Token.getInstance().accessToken);
                        }
                    }
                })
                .build();

        return adapter.create(IMHttp.class);
    }
}
