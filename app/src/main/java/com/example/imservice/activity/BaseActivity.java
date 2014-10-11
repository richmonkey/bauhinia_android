package com.example.imservice.activity;

import android.app.Activity;
import android.os.Handler;
import android.text.TextUtils;

import com.example.imservice.Token;
import com.example.imservice.api.IMHttp;
import com.example.imservice.api.IMHttpFactory;
import com.example.imservice.api.body.PostAuthRefreshToken;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by tsung on 10/10/14.
 */
public class BaseActivity extends Activity {
    protected final IMHttp imHttp = IMHttpFactory.Singleton();
    protected Handler handler = new Handler();

    @Override
    protected void onResume() {
        super.onResume();

        if (!TextUtils.isEmpty(Token.getInstance().refreshToken)) {
            refreshToken();
        }
    }

    private void refreshToken() {
        PostAuthRefreshToken refreshToken = new PostAuthRefreshToken();
        refreshToken.refreshToken = Token.getInstance().refreshToken;
        imHttp.postAuthRefreshToken(refreshToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Token>() {
                    @Override
                    public void call(Token token) {
                        onTokenRefreshed(token);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        //
                    }
                });
    }

    protected void onTokenRefreshed(Token token) {
        Token t = Token.getInstance();
        t.accessToken = token.accessToken;
        t.refreshToken = token.refreshToken;
        t.expireTimestamp = token.expireTimestamp;
        t.uid = token.uid;
        t.save();
    }
}
