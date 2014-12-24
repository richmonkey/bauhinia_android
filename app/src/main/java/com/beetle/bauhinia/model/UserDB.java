package com.beetle.bauhinia.model;

import com.beetle.bauhinia.api.types.User;
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
                PhoneNumber phoneNumber = new PhoneNumber(zoneNumber);
                u.number = phoneNumber.getNumber();
                u.zone = phoneNumber.getZone();
            } else {
                String[] t = ("" + uid).split("0", 2);
                PhoneNumber phoneNumber = new PhoneNumber(t[0], t[1]);
                u.number = phoneNumber.getNumber();
                u.zone = phoneNumber.getZone();
            }
            u.avatar = db.get(key + "_avatar");
            u.state = db.get(key + "_state");
            try {
                u.up_timestamp = db.getLong(key + "_up_timestamp");
            } catch (Exception e) {

            }
            return u;
        } catch (Exception e) {
            return null;
        }
    }

    public User loadUser(PhoneNumber number) {
        LevelDB db = LevelDB.getDefaultDB();
        try {
            long uid = db.getLong("numbers_" + number.getZoneNumber());
            if (uid == 0) {
                return null;
            }
            return loadUser(uid);
        } catch(Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addUser(User user) {
        LevelDB db = LevelDB.getDefaultDB();
        String key = getUserKey(user.uid);

        try {
            PhoneNumber phoneNumber = new PhoneNumber(user.zone, user.number);
            if (phoneNumber.isValid()) {
                db.set(key + "_number", phoneNumber.getZoneNumber());
                db.setLong("numbers_" + phoneNumber.getZoneNumber(), user.uid);
            }
            if (user.avatar != null && user.avatar.length() > 0) {
                db.set(key + "_avatar", user.avatar);
            } else {
                db.set(key + "_avatar", "");
            }
            if (user.state != null && user.state.length() > 0) {
                db.set(key + "_state", user.state);
            } else {
                db.set(key + "_state", "");
            }
            db.setLong(key + "_up_timestamp", user.up_timestamp);
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
