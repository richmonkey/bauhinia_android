package com.example.imservice;

import android.app.Activity;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.im.IMService;
import com.example.imservice.api.IMHttp;
import com.example.imservice.api.IMHttpFactory;
import com.example.imservice.api.body.PostAuthRefreshToken;
import com.example.imservice.model.ContactDB;
import com.example.imservice.tools.BinAscii;
import com.example.imservice.tools.FileCache;
import com.gameservice.sdk.push.api.IMsgReceiver;
import com.gameservice.sdk.push.api.SmartPush;
import com.gameservice.sdk.push.api.SmartPushOpenUtils;

import com.google.code.p.leveldb.LevelDB;

import java.io.File;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-24.
 */
public class IMApplication extends Application implements Application.ActivityLifecycleCallbacks {



    public String deviceToken;
    @Override
    public void onCreate() {
        super.onCreate();

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        ldb.open(dir);

        FileCache fc = FileCache.getInstance();
        fc.setDir(this.getDir("cache", MODE_PRIVATE));
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        ContactDB cdb = ContactDB.getInstance();
        cdb.setContentResolver(getApplicationContext().getContentResolver());
        cdb.monitorConctat(getApplicationContext());

        SmartPush.registerReceiver(new IMsgReceiver() {
            @Override
            public void onMessage(String message) {
                // message为透传消息，需开发者在此处理
                Log.i("PUSH", "透传消息:" + message);
            }

            @Override
            public void onDeviceToken(byte[] tokenArray) {
                if (null != tokenArray && tokenArray.length == 0) {
                    return;
                }
                String deviceTokenStr = null;
                deviceTokenStr = BinAscii.bin2Hex(tokenArray);
                Log.i(TAG, "device token:" + deviceTokenStr);
                IMApplication.this.deviceToken = deviceTokenStr;
            }
        });
        // 注册服务，并启动服务
        Log.i(TAG, "start push service");
        SmartPush.registerService(this);

        registerActivityLifecycleCallbacks(this);

        IMService im =  IMService.getInstance();
        im.setHost(Config.HOST);
        im.setPort(Config.PORT);
        im.setPeerMessageHandler(PeerMessageHandler.getInstance());

        //already login
        if (Token.getInstance().uid > 0) {
            im.setUid(Token.getInstance().uid);
            im.start();
        }
    }


    private final static String TAG = "beetle";
    private int resumed = 0;
    private int stopped = 0;

    public void onActivityCreated(Activity activity, Bundle bundle) {
        Log.e("","onActivityCreated:" + activity.getLocalClassName());
    }

    public void onActivityDestroyed(Activity activity) {
        Log.e("","onActivityDestroyed:" + activity.getLocalClassName());
    }

    public void onActivityPaused(Activity activity) {
        Log.e("","onActivityPaused:" + activity.getLocalClassName());
    }

    public void onActivityResumed(Activity activity) {
        Log.e("","onActivityResumed:" + activity.getLocalClassName());
        ++resumed;

        //resumed from backgroud
        if (resumed - stopped == 1 && stopped > 0 ) {
            if (Token.getInstance().uid > 0 && isNetworkConnected(this)) {
                Log.i(TAG, "app enter foreground start imservice");
                IMService.getInstance().start();
            }
        }
        if (resumed - stopped == 1) {
            Log.i(TAG, "register network broadcast receiver");
            registerBroadcastReceiver();

            if (!TextUtils.isEmpty(Token.getInstance().refreshToken)) {
                refreshToken();
            }
        }
    }

    public void onActivitySaveInstanceState(Activity activity,
                                            Bundle outState) {
        Log.e("","onActivitySaveInstanceState:" + activity.getLocalClassName());
    }

    public void onActivityStarted(Activity activity) {
        Log.e("","onActivityStarted:" + activity.getLocalClassName());
    }

    public void onActivityStopped(Activity activity) {
        Log.e("","onActivityStopped:" + activity.getLocalClassName());
        ++stopped;
        if (stopped == resumed) {
            Log.i(TAG, "app enter background stop imservice");
            IMService.getInstance().stop();
            Log.i(TAG, "unregister network broadcast receiver");
            unregisterBroadcastReceiver();
        }
    }

    public boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    public void registerBroadcastReceiver() {
        this.registerReceiver(networdStateReceiver, new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
    }


    public void unregisterBroadcastReceiver() {
        this.unregisterReceiver(networdStateReceiver);
    }


    BroadcastReceiver networdStateReceiver = new BroadcastReceiver(){

        @Override
        public void onReceive(Context context, Intent intent) {
            if (IMApplication.this.isNetworkConnected(context)) {
                if (Token.getInstance().uid > 0) {
                    Log.i(TAG, "network connected start im service");
                    IMService.getInstance().stop();
                    IMService.getInstance().start();
                }
            } else {
                Log.i(TAG, "network disconnected stop im service");
                IMService.getInstance().stop();
            }
        }
    };

    private void refreshToken() {
        PostAuthRefreshToken refreshToken = new PostAuthRefreshToken();
        refreshToken.refreshToken = Token.getInstance().refreshToken;
        IMHttp imHttp = IMHttpFactory.Singleton();
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
                        Log.i(TAG, "refresh token error");
                    }
                });
    }

    protected void onTokenRefreshed(Token token) {
        Token t = Token.getInstance();
        t.accessToken = token.accessToken;
        t.refreshToken = token.refreshToken;
        t.expireTimestamp = token.expireTimestamp;
        t.save();
        Log.i(TAG, "token refreshed");
    }

}
