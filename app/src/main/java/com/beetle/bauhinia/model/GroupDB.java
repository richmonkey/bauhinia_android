package com.beetle.bauhinia.model;

import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 15/3/21.
 */
public class GroupDB {

    private static GroupDB instance = new GroupDB();
    public static GroupDB getInstance() {
        return instance;
    }

    private String topicKey(long groupID) {
        return String.format("groups_%d_topic", groupID);
    }

    private String masterKey(long groupID) {
        return String.format("groups_%d_master", groupID);
    }

    private String disbandedKey(long groupID) {
        return String.format("groups_%d_disbanded", groupID);
    }

    public boolean addGroup(Group group) {
        LevelDB db = LevelDB.getDefaultDB();

        String k1 = topicKey(group.groupID);
        String k2 = masterKey(group.groupID);
        String k3 = disbandedKey(group.groupID);
        try {
            db.set(k1, group.topic);
            db.setLong(k2, group.master);
            db.setLong(k3, group.disbanded ? 1 : 0);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean removeGroup(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k1 = topicKey(groupID);
        String k2 = masterKey(groupID);
        String k3 = disbandedKey(groupID);
        try {
            db.delete(k1);
            db.delete(k2);
            db.delete(k3);
            return true;
        } catch (Exception e) {
            return false;
        }

    }

    public boolean disbandGroup(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = disbandedKey(groupID);
        try {

            db.setLong(k3, 1);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getGroupTopic(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = topicKey(groupID);
        try {
            return db.get(k3);
        } catch (Exception e) {
            return "";
        }
    }
}
