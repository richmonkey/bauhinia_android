package com.beetle.im;


import android.os.AsyncTask;
import android.util.Log;
import com.beetle.AsyncTCP;
import com.beetle.TCPConnectCallback;
import com.beetle.TCPReadCallback;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;



import static android.os.SystemClock.uptimeMillis;

/**
 * Created by houxh on 14-7-21.
 */
public class IMService {

    public enum ConnectState {
        STATE_UNCONNECTED,
        STATE_CONNECTING,
        STATE_CONNECTED,
        STATE_CONNECTFAIL,
    }

    private final String TAG = "imservice";
    private final int HEARTBEAT = 60*3;
    private AsyncTCP tcp;
    private boolean stopped = true;
    private Timer connectTimer;
    private Timer heartbeatTimer;
    private int connectFailCount = 0;
    private int seq = 0;
    private ConnectState connectState = ConnectState.STATE_UNCONNECTED;

    private String hostIP;
    private int timestamp;

    private String host;
    private int port;
    private long uid;

    IMPeerMessageHandler peerMessageHandler;
    ArrayList<IMServiceObserver> observers = new ArrayList<IMServiceObserver>();
    HashMap<Integer, IMMessage> peerMessages = new HashMap<Integer, IMMessage>();
    private HashMap<Long, Boolean> subs = new HashMap<Long, Boolean>();

    private byte[] data;

    private static IMService im = new IMService();

    public static IMService getInstance() {
        return im;
    }

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
                IMService.this.sendHeartbeat();
            }
        };
    }

    public ConnectState getConnectState() {
        return connectState;
    }
    public void setHost(String host) {
        this.host = host;
    }
    public void setPort(int port) {
        this.port = port;
    }
    public void setUid(long uid) {
        this.uid = uid;
    }

    public void setPeerMessageHandler(IMPeerMessageHandler handler) {
        this.peerMessageHandler = handler;
    }

    public void addObserver(IMServiceObserver ob) {
        if (observers.contains(ob)) {
            return;
        }
        observers.add(ob);
    }

    public void removeObserver(IMServiceObserver ob) {
        observers.remove(ob);
    }

    public void start() {
        if (this.uid == 0) {
            throw new RuntimeException("NO UID PROVIDED");
        }

        if (!this.stopped) {
            Log.i(TAG, "already started");
            return;
        }
        this.stopped = false;
        connectTimer.setTimer(uptimeMillis());
        connectTimer.resume();

        heartbeatTimer.setTimer(uptimeMillis(), HEARTBEAT*1000);
        heartbeatTimer.resume();
    }

    public void stop() {
        if (this.stopped) {
            Log.i(TAG, "already stopped");
            return;
        }
        stopped = true;
        heartbeatTimer.suspend();
        connectTimer.suspend();
        this.close();
    }

    public boolean sendPeerMessage(IMMessage im) {
        Message msg = new Message();
        msg.cmd = Command.MSG_IM;
        msg.body = im;
        if (!sendMessage(msg)) {
            return false;
        }

        peerMessages.put(new Integer(msg.seq), im);
        return true;
    }

    //订阅用户在线状态通知消息
    public void subscribeState(long uid) {
        Long n = new Long(uid);
        if (!subs.containsKey(n)) {
            MessageSubscribe sub = new MessageSubscribe();
            sub.uids = new ArrayList<Long>();
            sub.uids.add(n);
            if (sendSubscribe(sub)) {
                subs.put(n, new Boolean(false));
            }
        } else {
            Boolean online = subs.get(n);
            for (int i = 0; i < observers.size(); i++ ) {
                IMServiceObserver ob = observers.get(i);
                ob.onOnlineState(uid, online);
            }
        }
    }

    public void unsubscribeState(long uid) {
        Long n = new Long(uid);
        subs.remove(n);
    }

    private boolean sendSubscribe(MessageSubscribe sub) {
        Message msg = new Message();
        msg.cmd = Command.MSG_SUBSCRIBE_ONLINE_STATE;
        msg.body = sub;
        return sendMessage(msg);
    }

    private void close() {
        if (this.tcp != null) {
            Log.i(TAG, "close tcp");
            this.tcp.close();
            this.tcp = null;
        }
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

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    private void refreshHost() {
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... urls) {
                return lookupHost(IMService.this.host);
            }

            private String lookupHost(String host) {
                try {
                    InetAddress inetAddress = InetAddress.getByName(host);
                    Log.i(TAG, "host name:" + inetAddress.getHostName() + " " + inetAddress.getHostAddress());
                    return inetAddress.getHostAddress();
                } catch (UnknownHostException exception) {
                    exception.printStackTrace();
                    return "";
                }
            }

            @Override
            protected void onPostExecute(String result) {
                if (result.length() > 0) {
                    IMService.this.hostIP = result;
                    IMService.this.timestamp = now();
                }
            }
        }.execute();
    }

    private void connect() {
        if (this.tcp != null) {
            return;
        }
        if (this.stopped) {
            Log.e(TAG, "opps....");
            return;
        }

        if (hostIP == null || hostIP.length() == 0) {
            refreshHost();
            IMService.this.connectFailCount++;
            Log.i(TAG, "host ip is't resolved");

            long t;
            if (this.connectFailCount > 60) {
                t = uptimeMillis() + 60*1000;
            } else {
                t = uptimeMillis() + this.connectFailCount*1000;
            }
            connectTimer.setTimer(t);
            return;
        }

        if (now() - timestamp > 5*60) {
            refreshHost();
        }

        this.connectState = ConnectState.STATE_CONNECTING;
        IMService.this.publishConnectState();
        this.tcp = new AsyncTCP();
        Log.i(TAG, "new tcp...");

        this.tcp.setConnectCallback(new TCPConnectCallback() {
            @Override
            public void onConnect(Object tcp, int status) {
                if (status != 0) {
                    Log.i(TAG, "connect err:" + status);
                    IMService.this.connectFailCount++;
                    IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
                    IMService.this.publishConnectState();
                    IMService.this.close();
                } else {
                    Log.i(TAG, "tcp connected");
                    IMService.this.connectFailCount = 0;
                    IMService.this.connectState = ConnectState.STATE_CONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.sendAuth();
                    IMService.this.tcp.startRead();
                    subs.clear();
                }
            }
        });

        this.tcp.setReadCallback(new TCPReadCallback() {
            @Override
            public void onRead(Object tcp, byte[] data) {
                if (data.length == 0) {
                    IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                    IMService.this.publishConnectState();
                    IMService.this.handleClose();
                } else {
                    boolean b = IMService.this.handleData(data);
                    if (!b) {
                        IMService.this.connectState = ConnectState.STATE_UNCONNECTED;
                        IMService.this.publishConnectState();
                        IMService.this.handleClose();
                    }
                }
            }
        });

        boolean r = this.tcp.connect(this.hostIP, this.port);
        Log.i(TAG, "tcp connect:" + r);
        if (!r) {
            this.tcp = null;
            IMService.this.connectFailCount++;
            IMService.this.connectState = ConnectState.STATE_CONNECTFAIL;
            publishConnectState();
            Log.d(TAG, "start connect timer");

            long t;
            if (this.connectFailCount > 60) {
                t = uptimeMillis() + 60*1000;
            } else {
                t = uptimeMillis() + this.connectFailCount*1000;
            }
            connectTimer.setTimer(t);
        }
    }

    private void handleAuthStatus(Message msg) {
        Integer status = (Integer)msg.body;
        Log.d(TAG, "auth status:" + status);
    }

    private void handleIMMessage(Message msg) {
        IMMessage im = (IMMessage)msg.body;
        Log.d(TAG, "im message sender:" + im.sender + " receiver:" + im.receiver + " content:" + im.content);
        if (!peerMessageHandler.handleMessage(im)) {
            Log.i(TAG, "handle im message fail");
            return;
        }
        publishPeerMessage(im);
        Message ack = new Message();
        ack.cmd = Command.MSG_ACK;
        ack.body = new Integer(msg.seq);
        sendMessage(ack);
    }

    private void handleClose() {
        Iterator iter = peerMessages.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, IMMessage> entry = (Map.Entry<Integer, IMMessage>)iter.next();
            IMMessage im = entry.getValue();
            peerMessageHandler.handleMessageFailure(im.msgLocalID, im.receiver);
            publishPeerMessageFailure(im.msgLocalID, im.receiver);
        }
        peerMessages.clear();
        close();
    }

    private void handleACK(Message msg) {
        Integer seq = (Integer)msg.body;
        IMMessage im = peerMessages.get(seq);
        if (im == null) {
            return;
        }

        if (!peerMessageHandler.handleMessageACK(im.msgLocalID, im.receiver)) {
            Log.w(TAG, "handle message ack fail");
            return;
        }
        peerMessages.remove(seq);
        publishPeerMessageACK(im.msgLocalID, im.receiver);
    }

    private void handlePeerACK(Message msg) {
        MessagePeerACK ack = (MessagePeerACK)msg.body;
        this.peerMessageHandler.handleMessageRemoteACK(ack.msgLocalID, ack.sender);

        publishPeerMessageRemoteACK(ack.msgLocalID, ack.sender);

    }

    private void handleOnlineState(Message msg) {
        MessageOnlineState state = (MessageOnlineState)msg.body;
        boolean on = state.online != 0 ? true : false;

        if (subs.containsKey(new Long(state.sender))) {
            subs.put(new Long(state.sender), new Boolean(on));
        }
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onOnlineState(state.sender, on);
        }
    }

    private void handleInputting(Message msg) {
        MessageInputing inputting = (MessageInputing)msg.body;
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerInputting(inputting.sender);
        }
    }

    private void handleMessage(Message msg) {
        if (msg.cmd == Command.MSG_AUTH_STATUS) {
            handleAuthStatus(msg);
        } else if (msg.cmd == Command.MSG_IM) {
            handleIMMessage(msg);
        } else if (msg.cmd == Command.MSG_ACK) {
            handleACK(msg);
        } else if (msg.cmd == Command.MSG_ONLINE_STATE) {
            handleOnlineState(msg);
        } else if (msg.cmd == Command.MSG_PEER_ACK) {
            handlePeerACK(msg);
        } else if (msg.cmd == Command.MSG_INPUTTING) {
            handleInputting(msg);
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
            if (this.data.length < pos + 4 + Message.HEAD_SIZE + len) {
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

    private void sendAuth() {
        Message msg = new Message();
        msg.cmd = Command.MSG_AUTH;
        msg.body = new Long(this.uid);
        sendMessage(msg);
    }

    private void sendHeartbeat() {
        Log.i(TAG, "send heartbeat");
        Message msg = new Message();
        msg.cmd = Command.MSG_HEARTBEAT;
        sendMessage(msg);
    }

    private boolean sendMessage(Message msg) {
        if (this.tcp == null || connectState != ConnectState.STATE_CONNECTED) return false;
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

    private void publishPeerMessage(IMMessage msg) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessage(msg);
        }
    }

    private void publishPeerMessageACK(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageACK(msgLocalID, uid);
        }
    }

    private void publishPeerMessageRemoteACK(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageRemoteACK(msgLocalID, uid);
        }
    }
    private void publishPeerMessageFailure(int msgLocalID, long uid) {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onPeerMessageFailure(msgLocalID, uid);
        }
    }

    private void publishConnectState() {
        for (int i = 0; i < observers.size(); i++ ) {
            IMServiceObserver ob = observers.get(i);
            ob.onConnectState(connectState);
        }
    }
}
