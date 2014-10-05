package com.example.imservice;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.example.imservice.model.*;

import java.lang.reflect.Array;
import java.util.ArrayList;

/**
 * Created by houxh on 14-8-12.
 */
public class NewConversation extends Activity implements AdapterView.OnItemClickListener {

    private Handler handler = new Handler();

    ArrayList<User> users;

    private ListView lv;
    private BaseAdapter adapter;

    class ConversationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return users.size();
        }

        @Override
        public Object getItem(int position) {
            return users.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = getLayoutInflater().inflate(R.layout.message, null);
            } else {
                view = convertView;
            }
            TextView tv = (TextView) view.findViewById(R.id.name);
            User c = users.get(position);
            tv.setText(c.name);
            return view;
        }
    }

    private void loadUsers() {
        users = new ArrayList<User>();
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
                    users.add(u);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_conversation);

        loadUsers();

        final ArrayList<Contact> contacts = ContactDB.getInstance().copyContacts();

        adapter = new ConversationAdapter();
        lv = (ListView)findViewById(R.id.list);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);

        Thread t = new Thread() {
            @Override
            public void run() {
                final ArrayList<User> users = APIRequest.requestUsers(contacts);
                if (users == null || users.size() == 0) {
                    return;
                }

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        UserDB userDB = UserDB.getInstance();
                        for (int i = 0; i < users.size(); i++) {
                            userDB.addUser(users.get(i));
                        }
                        loadUsers();
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        };
        t.start();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        User u = users.get(position);

        Intent intent = new Intent(this, IMActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", u.uid);
        startActivity(intent);

        finish();
    }
}
