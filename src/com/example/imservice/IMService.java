package com.example.imservice;
import android.util.Log;
import com.beetle.AsyncTCP;
import com.beetle.TCPConnectCallback;
import com.beetle.TCPReadCallback;

import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 14-7-21.
 */
public class IMService {
    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL
    }

    private final String TAG = "imservice";
    private AsyncTCP tcp;
    private boolean stopped;
    private Timer connectTimer;
    private Timer heartbeatTimer;
    private int connectFailCount = 0;
    private int seq = 0;
    private ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private byte[] data;

    public IMService() {
        connectTimer = new Timer() {
            @Override
            protected void fire() {
                IMService.this.connect();
            }
        };

        heartbeatTimer = new Timer() {
            @Override
            protected void fire() {
                Log.i(TAG, "heartbeat timer fire");
                IMService.this.sendHeartbeat();
            }
        };
    }

    public void start() {
        this.stopped = false;
        connectTimer.setTimer(uptimeMillis());
        connectTimer.resume();

        heartbeatTimer.setTimer(uptimeMillis(), 8*1000);
        heartbeatTimer.resume();
    }

    private void close() {
        this.tcp.close();
        this.tcp = null;
        if (this.stopped) {
            return;
        }

        Log.d(TAG, "start connect timer");

        long t;
        if (this.connectFailCount > 60) {
            t = uptimeMillis() + 60*1000;
        } else {
            t = uptimeMillis() + this.connectFailCount*1000;
        }
        connectTimer.setTimer(t);
    }

    private void connect() {
        if (this.tcp != null) {
            return;
        }
        if (this.stopped) {
            Log.e(TAG, "opps....");
            return;
        }
        this.connectState = ConnectState.STATE_CONNECTING;
        this.tcp = new AsyncTCP();
        Log.i(TAG, "new tcp...");

        this.tcp.setConnectCallback(new TCPConnectCallback() {
            @Override
            public void onConnect(Object tcp, int status) {
                if (status != 0) {
                    Log.i(TAG, "connect err:" + status);
                    IMService.this.connectFailCount++;
                    IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                    IMService.this.close();
                    return;
                } else {
                    Log.i(TAG, "tcp connected");
                    IMService.this.connectFailCount = 0;
                    IMService.this.connectState = ConnectState.STATE_CONNECTED;
                    IMService.this.sendAuth();
                    IMService.this.tcp.startRead();
                }
            }
        });

        this.tcp.setReadCallback(new TCPReadCallback() {
            @Override
            public void onRead(Object tcp, byte[] data) {
                if (data.length == 0) {
                    IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                    IMService.this.close();
                } else {
                    boolean b = IMService.this.handleData(data);
                    if (!b) {
                        IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                        IMService.this.close();
                    }
                }
            }
        });

        this.tcp.connect("106.186.122.158", 23000);
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer)msg.body;
        Log.d(TAG, "auth status:" + status);
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleMessage(Message msg) {
        if (msg.cmd == Command.MSG_AUTH_STATUS) {
            handleAuthStatus(msg);
        } else if (msg.cmd == Command.MSG_IM) {
            handleIMMessage(msg);
        } else {
            Log.i(TAG, "unknown message cmd:"+msg.cmd);
        }
    }

    private void appendData(byte[] data) {
        if (this.data != null) {
            int l = this.data.length + data.length;
            byte[] buf = new byte[l];
            System.arraycopy(this.data, 0, buf, 0, this.data.length);
            System.arraycopy(data, 0, buf, this.data.length, data.length);
            this.data = buf;
        } else {
            this.data = data;
        }
    }

    private boolean handleData(byte[] data) {
        appendData(data);

        int pos = 0;
        while (true) {
            if (this.data.length < pos + 4) {
                break;
            }
            int len = BytePacket.readInt32(this.data, pos);
            if (this.data.length < 4 + Message.HEAD_SIZE + len) {
                break;
            }
            Message msg = new Message();
            byte[] buf = new byte[Message.HEAD_SIZE + len];
            System.arraycopy(this.data, pos+4, buf, 0, Message.HEAD_SIZE+len);
            if (!msg.unpack(buf)) {
                Log.i(TAG, "unpack message error");
                return false;
            }
            handleMessage(msg);
            pos += 4 + Message.HEAD_SIZE + len;
        }

        byte[] left = new byte[this.data.length - pos];
        System.arraycopy(this.data, pos, left, 0, left.length);
        this.data = left;
        return true;
    }

    public void stop() {
        stopped = true;
        heartbeatTimer.suspend();
        connectTimer.suspend();
        this.close();
    }

    private void sendAuth() {
        Message msg = new Message();
        msg.cmd = Command.MSG_AUTH;
        msg.body = new Long(86013635273143L);
        sendMessage(msg);
    }

    private void sendHeartbeat() {
        Message msg = new Message();
        msg.cmd = Command.MSG_HEARTBEAT;
        sendMessage(msg);
    }

    private boolean sendMessage(Message msg) {
        if (this.tcp == null) return false;
        this.seq++;
        msg.seq = this.seq;
        byte[] p = msg.pack();
        int l = p.length - Message.HEAD_SIZE;
        byte[] buf = new byte[p.length + 4];
        BytePacket.writeInt32(l, buf, 0);
        System.arraycopy(p, 0, buf, 4, p.length);
        this.tcp.writeData(buf);
        return true;
    }
}
