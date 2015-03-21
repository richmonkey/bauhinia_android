package com.beetle.bauhinia;

import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.model.UserDB;

/**
 * Created by houxh on 15/3/21.
 */
public class AppGroupMessageActivity extends GroupMessageActivity {
    protected String getUserName(long uid) {
        if (uid == 0) {
            return null;
        }
        User u = UserDB.getInstance().loadUser(uid);
        Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
        if (c != null) {
            u.name = c.displayName;
        } else {
            u.name = u.number;
        }
        return u.name;
    }
}
