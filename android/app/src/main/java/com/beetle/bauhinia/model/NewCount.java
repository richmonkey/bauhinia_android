package com.beetle.bauhinia.model;

import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 16/5/3.
 */
public class NewCount {
    public static int getNewCount(long uid) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_peer_%d", uid);
            return (int)db.getLong(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setNewCount(long uid, int count) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_group_%d", uid);
            db.setLong(key, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static int getGroupNewCount(long gid) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_group_%d", gid);
            return (int)db.getLong(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static void setGroupNewCount(long gid, int count) {
        LevelDB db = LevelDB.getDefaultDB();

        try {
            String key = String.format("news_group_%d", gid);
            db.setLong(key, count);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
