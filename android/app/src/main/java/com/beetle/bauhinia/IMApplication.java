package com.beetle.bauhinia;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import com.beetle.bauhinia.api.IMHttpAPI;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.GroupMessageHandler;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.PeerMessageHandler;
import com.beetle.bauhinia.model.Profile;
import com.beetle.bauhinia.react.ReactInstance;
import com.beetle.im.IMService;
import com.beetle.bauhinia.api.IMHttp;
import com.beetle.bauhinia.api.IMHttpFactory;
import com.beetle.bauhinia.api.body.PostAuthRefreshToken;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.tools.BinAscii;
import com.beetle.bauhinia.tools.FileCache;
import com.beetle.push.Push;
import com.beetle.push.instance.SmartPushServiceProvider;
import com.facebook.react.ReactInstanceManager;
import com.gameservice.sdk.crashdump.NgdsCrashHandler;
import com.beetle.push.IMsgReceiver;

import com.google.code.p.leveldb.LevelDB;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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


        ReactInstance.getInstance().build(this);

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        Log.i(TAG, "dir:" + dir);
        ldb.open(dir);

        FileCache fc = FileCache.getInstance();
        fc.setDir(this.getDir("cache", MODE_PRIVATE));
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        GroupMessageDB groupDB = GroupMessageDB.getInstance();
        groupDB.setDir(this.getDir("group", MODE_PRIVATE));


        ContactDB cdb = ContactDB.getInstance();
        cdb.setContentResolver(getApplicationContext().getContentResolver());
        cdb.monitorConctat(getApplicationContext());

        Push.registerReceiver(new IMsgReceiver() {
            @Override
            public void onDeviceToken(byte[] tokenArray) {
                if (null != tokenArray && tokenArray.length == 0) {
                    return;
                }
                String deviceTokenStr = null;
                deviceTokenStr = BinAscii.bin2Hex(tokenArray);
                Log.i(TAG, "device token:" + deviceTokenStr);
                IMApplication.this.deviceToken = deviceTokenStr;
                if (Profile.getInstance().uid > 0) {
                    IMApplication.this.bindDeviceToken(deviceTokenStr);
                }
            }
        });
        // 注册服务，并启动服务
        Log.i(TAG, "start push service");
        Push.registerService(this);

        registerActivityLifecycleCallbacks(this);

        IMHttpAPI.setAPIURL(Config.SDK_API_URL);
        SmartPushServiceProvider.setHost(Config.SDK_PUSH_HOST);
        IMService im =  IMService.getInstance();
        im.setHost(Config.SDK_IM_HOST);
        String androidID = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);

        im.setDeviceID(androidID);
        im.setPeerMessageHandler(PeerMessageHandler.getInstance());
        im.setGroupMessageHandler(GroupMessageHandler.getInstance());
        im.registerConnectivityChangeReceiver(getApplicationContext());

        Profile profile = Profile.getInstance();
        profile.load(this);

        //already login
        if (Profile.getInstance().uid > 0) {
            im.setToken(Token.getInstance().accessToken);
            im.setUID(Profile.getInstance().uid);
            IMHttpAPI.setToken(Token.getInstance().accessToken);
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

    private int getAppImportance() {
        Context context = getApplicationContext();
        int pid = android.os.Process.myPid();
        Log.i(TAG, "pid:" + pid + "package name:" + context.getPackageName());
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            Log.i(TAG, "package name:" + appProcess.processName + " importance:" + appProcess.importance + " pid:" + appProcess.pid);
            if (pid == appProcess.pid) {
                return appProcess.importance;
            }
        }
        return 0;
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
            if (Profile.getInstance().uid > 0) {
                if (stopped == 0) {
                    Log.i(TAG, "app startup");
                } else {
                    Log.i(TAG, "app enter foreground");
                }
                IMService.getInstance().enterForeground();
            }
        }
        if (started - stopped == 1) {
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
            IMService.getInstance().enterBackground();
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




    private void refreshToken() {
        PostAuthRefreshToken refreshToken = new PostAuthRefreshToken();
        refreshToken.refreshToken = Token.getInstance().refreshToken;
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.postAuthRefreshToken(refreshToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<IMHttp.Token>() {
                    @Override
                    public void call(IMHttp.Token token) {
                        onTokenRefreshed(token);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "refresh token error");
                    }
                });
    }

    protected void onTokenRefreshed(IMHttp.Token token) {
        Token t = Token.getInstance();
        t.accessToken = token.accessToken;
        t.refreshToken = token.refreshToken;
        t.expireTimestamp = token.expireTimestamp;
        t.save();

        IMService im = IMService.getInstance();
        im.setToken(token.accessToken);
        IMHttpAPI.setToken(token.accessToken);
        Log.i(TAG, "token refreshed");
    }

    private void bindDeviceToken(String deviceToken) {
        PostDeviceToken postToken = new PostDeviceToken();
        postToken.deviceToken = deviceToken;
        IMHttpAPI.IMHttp imHttp = IMHttpAPI.Singleton();
        imHttp.bindDeviceToken(postToken)
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
