package com.example.imservice;

/**
 * Created by houxh on 14-7-22.
 */
public class PeerMessageHandler implements IMPeerMessageHandler {
    private static PeerMessageHandler instance = new PeerMessageHandler();

    public static PeerMessageHandler getInstance() {
        return instance;
    }

    public boolean handleMessage(IMMessage msg) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        IMessage imsg = new IMessage();
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.content = new MessageContent();
        imsg.content.raw = msg.content;
        return db.insertMessage(imsg);
    }

    public boolean handleMessageACK(int msgLocalID, long uid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        return db.acknowledgeMessage(msgLocalID, uid);
    }
    public boolean handleMessageRemoteACK(int msgLocalID, long uid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        return db.acknowledgeMessageFromRemote(msgLocalID, uid);
    }
    public boolean handleMessageFailure(int msgLocalID, long uid) {
        PeerMessageDB db = PeerMessageDB.getInstance();
        return db.markMessageFailure(msgLocalID, uid);
    }
}
