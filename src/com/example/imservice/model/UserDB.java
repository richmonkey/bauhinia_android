package com.example.imservice.model;

import com.google.code.p.leveldb.LevelDB;

/**
 * Created by houxh on 14-8-10.
 */
public class UserDB {
    private static UserDB instance = new UserDB();

    public static UserDB getInstance() {
        return instance;
    }

    public User loadUser(long uid) {
        LevelDB db = LevelDB.getDefaultDB();
        User u = new User();
        u.uid = uid;
        String key = getUserKey(uid);
        try {
            String zoneNumber = db.get(key + "_number");
            if (zoneNumber != null && zoneNumber.length() > 0) {
                u.number = new PhoneNumber(zoneNumber);
            }
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    public boolean addUser(User user) {
        LevelDB db = LevelDB.getDefaultDB();
        String key = getUserKey(user.uid);

        try {
            if (user.number.isValid()) {
                db.set(key + "_number", user.number.getZoneNumber());
                db.setLong("numbers_" + user.number.getZoneNumber(), user.uid);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getUserKey(long uid) {
        return "users_" + uid;
    }
}
