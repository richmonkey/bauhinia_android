package com.beetle.bauhinia.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import com.beetle.bauhinia.MainActivity;
import com.beetle.bauhinia.R;


/**
 * Created by houxh on 16/7/19.
 */
public class ForegroundService extends Service {
    private final String TAG = "face";

    private final int PID = android.os.Process.myPid();

    private AssistServiceConnection mConnection;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "start foreground service");

        startForeground(PID, getNotification());

        // sdk < 18 , 直接调用startForeground即可,不会在通知栏创建通知
        if (Build.VERSION.SDK_INT >= 18) {
            startAssistService();
        }
    }

    public void startAssistService() {
        // sdk >=18
        // 的，会在通知栏显示service正在运行，这里不要让用户感知，所以这里的实现方式是利用2个同进程的service，利用相同的notificationID，
        // 2个service分别startForeground，然后只在1个service里stopForeground，这样即可去掉通知栏的显示
        if (null == mConnection) {
            mConnection = new AssistServiceConnection();
        }

        this.bindService(new Intent(this, AssistService.class), mConnection,
                Service.BIND_AUTO_CREATE);
    }


    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private class AssistServiceConnection implements ServiceConnection {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "MyService: onServiceDisconnected");
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "MyService: onServiceConnected");
            ForegroundService.this.unbindService(mConnection);
            mConnection = null;
        }
    }

    private Notification getNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        String appName = getResources().getString(R.string.app_name);
        String running = getResources().getString(R.string.running);
        Notification notification = new Notification.Builder(this)
                .setAutoCancel(true)
                .setContentTitle(appName)
                .setContentText(running)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .build();
        return notification;
    }
}
