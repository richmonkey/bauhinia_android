package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.beetle.bauhinia.activity.GroupSettingActivity;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.GroupDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.model.UserDB;
import com.beetle.bauhinia.tools.Notification;
import com.beetle.bauhinia.tools.NotificationCenter;

import java.util.ArrayList;

/**
 * Created by houxh on 15/3/21.
 */
public class AppGroupMessageActivity extends GroupMessageActivity {
    private boolean leaved = false;


    @Override
    protected User getUser(long uid) {
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
        User user = new User();
        user.name = u.name;
        user.avatarURL = u.avatar;
        return user;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        leaved = GroupDB.getInstance().isLeaved(groupID);
        if (leaved) {
            hintView.setVisibility(View.VISIBLE);
            inputMenu.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!leaved) {
            getMenuInflater().inflate(R.menu.menu_group_chat, menu);
        }
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
