package com.example.imservice;

/**
 * Created by houxh on 14-7-22.
 */
enum MessageType {
    MESSAGE_TEXT
}

class MessageFlag {
    public static final int MESSAGE_FLAG_DELETE = 1;
    public static final int MESSAGE_FLAG_ACK = 2;
    public static final int MESSAGE_FLAG_PEER_ACK = 4;
    public static final int MESSAGE_FLAG_FAILURE = 8;
}

class MessageContent {

    public String raw;

    public MessageType getType() {
        return MessageType.MESSAGE_TEXT;
    }

    public String getText() {
        return raw;
    }
}

public class IMessage {
    public int msgLocalID;
    public int flags;
    public long sender;
    public long receiver;
    public MessageContent content;
    public int timestamp;
}
