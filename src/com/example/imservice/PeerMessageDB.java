package com.example.imservice;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by houxh on 14-7-22.
 */



class PeerMessageIterator {

    private RandomAccessFile file;
    private ReverseFile revFile;

    public PeerMessageIterator(RandomAccessFile f) throws IOException {
        if (!MessageDB.checkHeader(f)) {
            Log.i("imservice", "check header fail");
            return;
        }
        this.file = f;
        this.revFile = new ReverseFile(f);
    }

    public IMessage next() {
        if (this.revFile == null) return null;
        return MessageDB.readMessage(this.revFile);
    }
}

public class PeerMessageDB extends MessageDB {

    private static PeerMessageDB instance = new PeerMessageDB();

    public static PeerMessageDB getInstance() {
        return instance;
    }

    private File dir;

    public void setDir(File dir) {
        this.dir = dir;
    }

    private String fileName(long uid) {
        return ""+uid;
    }

    public boolean insertMessage(IMessage msg, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            boolean b = insertMessage(f, msg);
            f.close();
            return b;
        } catch (Exception e) {
            Log.i("imservice", "excp:" + e);
            return false;
        }
    }

    public boolean acknowledgeMessage(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_ACK);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean acknowledgeMessageFromRemote(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_PEER_ACK);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean markMessageFailure(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_FAILURE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean removeMessage(int msgLocalID, long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            addFlag(f, msgLocalID, MessageFlag.MESSAGE_FLAG_DELETE);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public PeerMessageIterator newMessageIterator(long uid) {
        try {
            File file = new File(this.dir, fileName(uid));
            RandomAccessFile f = new RandomAccessFile(file, "r");
            return new PeerMessageIterator(f);
        } catch (Exception e) {
            return null;
        }
    }
}
