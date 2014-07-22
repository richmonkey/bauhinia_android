package com.example.imservice;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Date;

import static android.os.SystemClock.uptimeMillis;

public class IMActivity extends Activity {

    private IMService im;
    private Timer timer;
    private final String TAG = "imservice";
    /**
     * Some text view we are using to show state information.
     */
    TextView mCallbackText;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mCallbackText = (TextView) findViewById(R.id.text);

    /*    long now = uptimeMillis() - 10*1000;
        long t = now + 2 * 1000;
        Log.i(TAG, "start:" + now());
        timer = new Timer() {
            @Override
            protected void fire() {
                Log.i("imservice", "timer fire:" + now());
            }
        };
        timer.setTimer(t, 2*1000);*/

//        timer.resume();

        testBytePacket();
        Log.i(TAG, "start im service");
        im = new IMService();
        im.start();
    }
    private void testBytePacket() {
        byte[] buf = new byte[8];
        BytePacket.writeInt32(new Integer(10), buf, 0);
        int v1 = BytePacket.readInt32(buf, 0);
        BytePacket.writeInt64(100, buf, 0);
        long v2 = BytePacket.readInt64(buf, 0);
        assert(v1 == 10);
        assert(v2 == 1001);
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }
}
