package com.example.imservice.api;

import retrofit.RestAdapter;

/**
 * Created by tsung on 10/10/14.
 */
class IMHttpRetrofit {
    final IMHttp service;

    IMHttpRetrofit() {
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint("http://106.186.122.158:5000")
                .build();

        service = adapter.create(IMHttp.class);
    }

    public IMHttp getService() {
        return service;
    }
}
