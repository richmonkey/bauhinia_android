package com.beetle.bauhinia;

import android.content.Intent;
import android.view.Menu;
import android.view.MenuItem;

import com.beetle.bauhinia.activity.GroupSettingActivity;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.model.UserDB;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;

import java.util.ArrayList;

/**
 * Created by houxh on 15/3/21.
 */
public class AppGroupMessageActivity extends GroupMessageActivity {
    protected String getUserName(long uid) {
        if (uid == 0) {
            return null;
        }
        com.beetle.bauhinia.api.types.User u = UserDB.getInstance().loadUser(uid);
        Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
        if (c != null) {
            u.name = c.displayName;
        } else {
            u.name = u.number;
        }
        return u.name;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_group_chat, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_setting) {
            Intent intent = new Intent(this, GroupSettingActivity.class);
            intent.putExtra("group_id", this.groupID);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
