package com.example.imservice;

import android.app.Activity;

import android.os.*;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.beetle.im.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;


public class IMActivity extends Activity implements IMServiceObserver {
    private IMService im;
    private Timer timer;
    private final String TAG = "imservice";
    private final long uid = 86013635273143L;
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        boolean unittest = false;
        if (unittest) {
            runUnitTest();
        } else {
            Log.i(TAG, "start im service");
            im = new IMService();
            im.setHost("106.186.122.158");
            im.setPort(23000);
            im.setUid(this.uid);
            im.setPeerMessageHandler(PeerMessageHandler.getInstance());
            im.addObserver(this);
            im.start();
        }
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
        msg.sender = 86013635273143L;
        msg.receiver = 86013635273142L;
        msg.content = new MessageContent();
        msg.content.raw = "11";
        boolean r = db.insertMessage(msg, msg.receiver);
        Log.i(TAG, "insert:" + r);

        PeerMessageIterator iter = db.newMessageIterator(msg.receiver);
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

    public void onSend(View v) {

        IMMessage msg = new IMMessage();
        msg.sender = this.uid;
        msg.receiver = 86013635273142L;
        msg.content = "11";

        IMessage imsg = new IMessage();
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.content = new MessageContent();
        imsg.content.raw = msg.content;
        imsg.timestamp = now();
        PeerMessageDB.getInstance().insertMessage(imsg, msg.receiver);

        msg.msgLocalID = imsg.msgLocalID;
        Log.i(TAG, "msg local id:" + imsg.msgLocalID);
        im.sendPeerMessage(msg);
    }

    public void onConnectState(IMService.ConnectState state) {

    }

    public void onPeerInputting(long uid) {

    }
    public void onOnlineState(long uid, boolean on) {

    }

    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
    }
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack");
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {

    }
}
