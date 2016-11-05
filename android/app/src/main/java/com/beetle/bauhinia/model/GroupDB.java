package com.beetle.bauhinia.model;

import com.google.code.p.leveldb.LevelDB;
import com.google.code.p.leveldb.LevelDBIterator;

import java.lang.reflect.Array;
import java.util.ArrayList;

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

    private String leavedKey(long groupID) {
        return String.format("groups_%d_leaved", groupID);
    }

    private String groupMemberKey(long groupID, long uid) {
        return String.format("group_member_%d_%d", groupID, uid);
    }

    public boolean addGroupMember(long groupID, long uid)  {
        String key = groupMemberKey(groupID, uid);
        LevelDB db = LevelDB.getDefaultDB();
        try {
            db.set(key, "1");
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public boolean removeGroupMember(long groupID, long uid) {
        String key = groupMemberKey(groupID, uid);
        LevelDB db = LevelDB.getDefaultDB();
        try {
            db.delete(key);
            return true;
        } catch (Exception e) {
            return false;
        }
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

            ArrayList<Long> members = group.getMembers();
            for (Long member: members) {
                addGroupMember(group.groupID, member);
            }
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

    private ArrayList<Long> loadGroupMember(long groupID) throws Exception {

        LevelDB db = LevelDB.getDefaultDB();
        LevelDBIterator iter = db.newIterator();

        ArrayList<Long> members = new ArrayList<Long>();
        String target = String.format("group_member_%d_", groupID);
        for (iter.seek(target); iter.isValid(); iter.next()) {
            String key = iter.getKey();
            String value = iter.getValue();

            String[] array = key.split("_");
            if (array.length != 4) {
                break;
            }

            if (Long.parseLong(array[2]) != groupID) {
                break;
            }

            Long uid = Long.parseLong(array[3]);

            members.add(uid);
        }

        return members;

    }
    public Group loadGroup(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();
        String k1 = topicKey(groupID);
        String k2 = masterKey(groupID);
        String k3 = disbandedKey(groupID);
        Group group = new Group();

        try {
            group.topic = db.get(k1);
            group.master = db.getLong(k2);
            group.disbanded = db.getLong(k3) == 1 ? true : false;
            group.setMembers(loadGroupMember(groupID));
            return group;
        } catch (Exception e) {
            return null;
        }
    }

    //退出群的标志
    public void leaveGroup(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = leavedKey(groupID);
        try {
            db.setLong(k3, 1);
        } catch (Exception e) {

        }
    }

    //清空退出群的标志
    public void joinGroup(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = leavedKey(groupID);
        try {
            db.setLong(k3, 0);
        } catch (Exception e) {

        }
    }

    public boolean isLeaved(long groupID) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = leavedKey(groupID);
        try {
            long leaved = db.getLong(k3);
            return (leaved == 1);
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

    public boolean setGroupTopic(long groupID, String name) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = topicKey(groupID);
        try {
            db.set(k3, name);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean setGroupMaster(long groupID, long master) {
        LevelDB db = LevelDB.getDefaultDB();

        String k3 = masterKey(groupID);
        try {
            db.setLong(k3, master);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
