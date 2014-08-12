package com.example.imservice;

import com.beetle.im.Timer;
import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 14-8-11.
 */
public class Token {
    private static Token instance;
    public static Token getInstance() {
        if (instance == null) {
            instance = new Token();
            instance.load();
        }
        return instance;
    }

    public String accessToken;
    public String refreshToken;
    public int expireTimestamp;
    public long uid;

    private Timer timer;

    public void save() {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            db.set("access_token", accessToken);
            db.set("refresh_token", refreshToken);
            db.setLong("token_expire", expireTimestamp);
            db.setLong("token_uid", uid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void load() {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            accessToken = db.get("access_token");
            refreshToken = db.get("refresh_token");
            expireTimestamp = (int)db.getLong("token_expire");
            uid = (int)db.getLong("token_uid");
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void startRefreshTimer() {

    }
    public void stopRefreshTimer() {

    }
}
