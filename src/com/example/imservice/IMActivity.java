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

import java.io.File;
import java.io.FileOutputStream;
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


        runUnitTest();
        Log.i(TAG, "start im service");
        im = new IMService();
        im.setHost("106.186.122.158");
        im.setPort(23000);
        im.setUid(86013635273143L);
        im.setPeerMessageHandler(PeerMessageHandler.getInstance());

       // im.start();
    }
    private void runUnitTest() {
        testFile();
        testBytePacket();
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

    private void testFile() {
        try {
            this.openFileOutput("test.txt", MODE_PRIVATE);
            File dir = this.getDir("test_dir", MODE_PRIVATE);
            File f = new File(dir, "myfile");
            Log.i(TAG, "path:" + f.getAbsolutePath());
            FileOutputStream out = new FileOutputStream(f);
            String hello = "hello world";
            out.write(hello.getBytes());
            out.close();
        } catch(Exception e) {
            Log.e(TAG, "file error");
        }
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }
}
