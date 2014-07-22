package com.example.imservice;

import android.app.Activity;

import android.os.*;
import android.util.Log;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;


public class IMActivity extends Activity {
    private IMService im;
    private Timer timer;
    private final String TAG = "imservice";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView bt = new TextView(this);
        bt.setText( "hello world" );
        setContentView(bt);

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
        testMessageDB();
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

    private void testMessageDB() {
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        IMessage msg = new IMessage();
        msg.sender = 1;
        msg.receiver = 2;
        msg.content = new MessageContent();
        msg.content.raw = "11";
        boolean r = db.insertMessage(msg, 2);
        Log.i(TAG, "insert:" + r);

        PeerMessageIterator iter = db.newMessageIterator(2);
        while (true) {
            IMessage msg2 = iter.next();
            if (msg2 == null) break;
            Log.i(TAG, "msg sender:" + msg2.sender + " receiver:" + msg2.receiver);
        }
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
