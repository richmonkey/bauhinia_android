package com.beetle.bauhinia;

import android.app.Activity;
import android.app.ActivityManager;
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

import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.im.IMService;
import com.beetle.bauhinia.api.IMHttp;
import com.beetle.bauhinia.api.IMHttpFactory;
import com.beetle.bauhinia.api.body.PostAuthRefreshToken;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.tools.BinAscii;
import com.beetle.bauhinia.tools.FileCache;
import com.gameservice.sdk.crashdump.NgdsCrashHandler;
import com.gameservice.sdk.push.api.IMsgReceiver;
import com.gameservice.sdk.push.api.SmartPush;

import com.google.code.p.leveldb.LevelDB;

import java.io.File;
import java.util.List;

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

        if (!isAppProcess()) {
            Log.i(TAG, "service application create");
            return;
        }
        Log.i(TAG, "app application create");

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        Log.i(TAG, "dir:" + dir);
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
                IMApplication.this.bindDeviceToken(deviceTokenStr);
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

        }
        initErrorHandler();
    }

    private void initErrorHandler() {
        //交给crashHandler自行判断,默认路径 /mnt/sdcard/.ngdsCrashDump/{your pacakagename}
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler =
                Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(
                new NgdsCrashHandler(this, defaultUncaughtExceptionHandler));
    }

    private boolean isAppProcess() {
        Context context = getApplicationContext();
        int pid = android.os.Process.myPid();
        Log.i(TAG, "pid:" + pid + "package name:" + context.getPackageName());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i(TAG, "package name:" + appProcess.processName + " importance:" + appProcess.importance + " pid:" + appProcess.pid);
            if (pid == appProcess.pid) {
                if (appProcess.processName.equals(context.getPackageName())) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    private final static String TAG = "beetle";
    private int started = 0;
    private int stopped = 0;

    public void onActivityCreated(Activity activity, Bundle bundle) {
        Log.i("","onActivityCreated:" + activity.getLocalClassName());
    }

    public void onActivityDestroyed(Activity activity) {
        Log.i("","onActivityDestroyed:" + activity.getLocalClassName());
    }

    public void onActivityPaused(Activity activity) {
        Log.i("","onActivityPaused:" + activity.getLocalClassName());
    }

    public void onActivityResumed(Activity activity) {
        Log.i("","onActivityResumed:" + activity.getLocalClassName());
    }

    public void onActivitySaveInstanceState(Activity activity,
                                            Bundle outState) {
        Log.i("","onActivitySaveInstanceState:" + activity.getLocalClassName());
    }

    public void onActivityStarted(Activity activity) {
        Log.i("","onActivityStarted:" + activity.getLocalClassName());
        ++started;

        if (started - stopped == 1 ) {
            if (Token.getInstance().uid > 0 && isNetworkConnected(this)) {
                if (stopped == 0) {
                    Log.i(TAG, "app startup start imservice");
                } else {
                    Log.i(TAG, "app enter foreground start imservice");
                }
                IMService.getInstance().start();
            }
        }
        if (started - stopped == 1) {
            Log.i(TAG, "register network broadcast receiver");
            registerBroadcastReceiver();

            if (!TextUtils.isEmpty(Token.getInstance().refreshToken)) {
                refreshToken();
            }
        }
    }

    public void onActivityStopped(Activity activity) {
        Log.i("","onActivityStopped:" + activity.getLocalClassName());
        ++stopped;
        if (stopped == started) {
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

    private void bindDeviceToken(String deviceToken) {
        PostDeviceToken postToken = new PostDeviceToken();
        postToken.deviceToken = deviceToken;
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.postDeviceToken(postToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                    @Override
                    public void call(Object obj) {
                        Log.i(TAG, "bind success");
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "bind fail");
                    }
                });

    }
}
