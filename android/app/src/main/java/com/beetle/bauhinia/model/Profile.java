package com.beetle.bauhinia.model;

/**
 * Created by houxh on 2016/11/5.
 */


import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by houxh on 2016/10/28.
 */

public class Profile {
    private static Profile instance;
    public static Profile getInstance() {
        if (instance == null) {
            instance = new Profile();
        }
        return instance;
    }

    public long uid;
    public String name;
    public String avatar;

    public void save(Context context) {
        SharedPreferences pref = context.getSharedPreferences("profile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();

        editor.putLong("uid", this.uid);
        editor.putString("name", (this.name != null ? this.name : ""));
        editor.putString("avatar", this.avatar != null ? this.avatar : "");

        editor.commit();
    }

    public void load(Context context) {
        SharedPreferences customer = context.getSharedPreferences("profile", Context.MODE_PRIVATE);

        this.uid = customer.getLong("uid", 0);
        this.name = customer.getString("name", "");
        this.avatar = customer.getString("avatar", "");
    }
}

