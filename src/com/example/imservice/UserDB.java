package com.example.imservice;

/**
 * Created by houxh on 14-8-10.
 */

class User {
    public long uid;
    public PhoneNumber number;
}

public class UserDB {
    private static UserDB db = new UserDB();

    public static UserDB getInstance() {
        return db;
    }

    public User loadUser(long uid) {
        User u = new User();
        u.uid = uid;
        String t = String.valueOf(uid);
        String[] ary = t.split("0", 2);
        String zone = ary[0];
        String number = ary[1];
        u.number = new PhoneNumber(zone, number);
        return u;
    }
}
