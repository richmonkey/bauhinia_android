package com.example.imservice.activity;

import android.app.Activity;
import android.os.Bundle;

import com.example.imservice.tools.event.BusProvider;
import com.example.imservice.tools.event.LoginSuccessEvent;
import com.squareup.otto.Subscribe;

/**
 * Created by tsung on 12/2/14.
 */
public class AccountActivity extends Activity {
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
