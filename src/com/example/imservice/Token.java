package com.example.imservice;

import com.beetle.im.Timer;

/**
 * Created by houxh on 14-8-11.
 */
public class Token {
    public String accessToken;
    public String refreshToken;
    public int expireTimestamp;
    public long uid;

    private Timer timer;

    public void save() {

    }

    public void startRefreshTimer() {

    }
    public void stopRefreshTimer() {

    }
}
