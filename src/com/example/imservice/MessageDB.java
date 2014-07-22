package com.example.imservice;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by houxh on 14-7-22.
 */
public class MessageDB {
    protected static final int HEADER_SIZE = 32;
    protected static final int IMMAGIC = 0x494d494d;
    protected static final int IMVERSION = (1<<16);

    protected boolean writeHeader(RandomAccessFile f) {
        try {
            byte[] buf = new byte[HEADER_SIZE];
            BytePacket.writeInt32(IMMAGIC, buf, 0);
            BytePacket.writeInt32(IMVERSION, buf, 4);
            f.write(buf);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    protected boolean writeMessage(RandomAccessFile f, IMessage msg) {
        try {
            byte[] buf = new byte[64 * 1024];
            int pos = 0;

            byte[] content = msg.content.raw.getBytes("UTF-8");
            int len = content.length + 8 + 8 + 4 + 4;
            if (4 + 4 + len + 4 + 4 > 64*1024) return false;

            BytePacket.writeInt32(IMMAGIC, buf, pos);
            pos += 4;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.flags, buf, pos);
            pos += 4;
            BytePacket.writeInt32(msg.timestamp, buf, pos);
            pos += 4;
            BytePacket.writeInt64(msg.sender, buf, pos);
            pos += 8;
            BytePacket.writeInt64(msg.receiver, buf, pos);
            pos += 8;
            System.arraycopy(content, 0, buf, pos, content.length);
            pos += content.length;
            BytePacket.writeInt32(len, buf, pos);
            pos += 4;
            BytePacket.writeInt32(IMMAGIC, buf, pos);
            f.write(buf, 0, 4 + 4 + len + 4 + 4);
            return true;
        } catch(Exception e) {
            return false;
        }
    }

    public boolean insertMessage(RandomAccessFile f, IMessage msg) throws IOException{
        long size = f.length();
        if (size < HEADER_SIZE && size > 0) {
            f.setLength(0);
            size = 0;
        }
        if (size == 0) {
            writeHeader(f);
        }
        msg.msgLocalID = (int)size;
        writeMessage(f, msg);
        f.close();
        return true;
    }

    protected boolean addFlag(RandomAccessFile f, int msgLocalID, int flag) {
        try {
            f.seek(msgLocalID);
            byte[] buf = new byte[12];
            int n = f.read(buf);
            if (n != 12) {
                return false;
            }
            int magic = BytePacket.readInt32(buf, 0);
            if (magic != IMMAGIC) {
                return false;
            }
            int flags = BytePacket.readInt32(buf, 8);
            flags |= flag;
            f.seek(msgLocalID + 8);
            f.writeInt(flags);
            return true;
        } catch(Exception e) {
            return false;
        }
    }
}

class PeerMessageDB extends MessageDB {

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

    public boolean insertMessage(IMessage msg) {
        try {
            File file = new File(this.dir, fileName(msg.receiver));
            RandomAccessFile f = new RandomAccessFile(file, "rw");
            return insertMessage(f, msg);
        } catch (Exception e) {
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

}