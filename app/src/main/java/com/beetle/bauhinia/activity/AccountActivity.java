package com.beetle.bauhinia.activity;

import android.os.Bundle;

import com.beetle.bauhinia.tools.event.BusProvider;
import com.beetle.bauhinia.tools.event.LoginSuccessEvent;
import com.squareup.otto.Subscribe;

/**
 * Created by tsung on 12/2/14.
 */
public class AccountActivity extends BaseActivity {
    final Object messageHandler = new Object() {
        @Subscribe
        public void onLoginSuccess(LoginSuccessEvent event) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BusProvider.getInstance().register(messageHandler);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BusProvider.getInstance().unregister(messageHandler);
    }
}
