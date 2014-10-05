package com.example.imservice.formatter;

import com.example.imservice.IMessage;

/**
 * Created by tsung on 10/5/14.
 */
public class MessageFormatter {
    public static String messageContentToString(IMessage.MessageContent content) {
        if (content instanceof IMessage.Text) {
            return ((IMessage.Text) content).text;
        } else if (content instanceof IMessage.Image) {
            return "Sent a photo";
        } else {
            return content.getRaw();
        }
    }
}
