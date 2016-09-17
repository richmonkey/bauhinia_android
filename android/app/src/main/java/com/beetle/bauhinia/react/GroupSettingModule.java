package com.beetle.bauhinia.react;

import android.app.Activity;

import com.beetle.bauhinia.activity.GroupSettingActivity;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.model.UserDB;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by houxh on 16/8/29.
 */



public class GroupSettingModule extends ReactContextBaseJavaModule {

    private ArrayList<Activity> activities = new ArrayList<Activity>();

    public GroupSettingModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    @Override
    public String getName() {
        return "GroupSettingModule";
    }


    @ReactMethod
    public void finish(int hashCode) {
        for (int i = 0; i < activities.size(); i++) {
            Activity activity = activities.get(i);
            if (activity.hashCode() == hashCode) {
                activity.finish();
                break;
            }
        }
    }


    public void addActivity(Activity activity) {
        activities.add(activity);
    }

    public void removeActivity(Activity activity) {
        activities.remove(activity);
    }


    @ReactMethod
    public void loadUsers(Callback successCallback) {
        WritableArray users = new WritableNativeArray();

        ContactDB db = ContactDB.getInstance();
        final ArrayList<Contact> contacts = db.getContacts();
        for (int i = 0; i < contacts.size(); i++) {
            Contact c = contacts.get(i);
            for (int j = 0; j < c.phoneNumbers.size(); j++) {
                Contact.ContactData data = c.phoneNumbers.get(j);
                PhoneNumber number = new PhoneNumber();
                if (!number.parsePhoneNumber(data.value)) {
                    continue;
                }

                UserDB userDB = UserDB.getInstance();
                User u = userDB.loadUser(number);
                if (u != null) {
                    u.name = c.displayName;

                    WritableMap obj = new WritableNativeMap();
                    obj.putDouble("uid", (double)u.uid);
                    obj.putString("name", u.name);
                    users.pushMap(obj);
                }
            }
        }
        successCallback.invoke(users);
    }

}
