package com.beetle.bauhinia;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.db.MessageIterator;
import com.beetle.im.BytePacket;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.*;
import com.google.code.p.leveldb.LevelDB;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;

/**
 * Created by houxh on 14-8-8.
 */
public class UnitTestActivity extends Activity {
    private final String TAG = "imservice";
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.unittest);
        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));
        ContactDB cdb = ContactDB.getInstance();
        cdb.setContentResolver(getContentResolver());

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        ldb.open(dir);

        cdb.loadContacts();

        runUnitTest();
    }

    private void runUnitTest() {
/*
testUserDB();
        testContact();
        testFile();
        testBytePacket();
        testMessageDB();*/
    }


    private void testContact() {
        ContactDB db = ContactDB.getInstance();
        db.loadContacts();
        ArrayList<Contact> contacts = db.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            Log.i(TAG, "name:" + c.displayName);
        }
        db.refreshContacts();
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
        msg.setContent("11");
        boolean r = db.insertMessage(msg, msg.receiver);
        Log.i(TAG, "insert:" + r);

        MessageIterator iter = db.newMessageIterator(msg.receiver);
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

    private void testUserDB() {
        User u = new User();
        u.uid = 86013635273142L;
        //u.number = new PhoneNumber("86_13635273142");
        u.number = "13635273142";
        u.zone = "86";
        UserDB db = UserDB.getInstance();
        db.addUser(u);
        User u2 = db.loadUser(u.uid);
        //Log.i(TAG, "" + u2.number.getZone() + " " + u2.number.getNumber());
        Log.i(TAG, "" + u2.zone + " " + u2.number);
    }
    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }
}
