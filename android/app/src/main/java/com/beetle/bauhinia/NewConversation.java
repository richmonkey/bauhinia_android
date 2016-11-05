package com.beetle.bauhinia;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.*;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.ButterKnife;

/**
 * Created by houxh on 14-8-12.
 */
public class NewConversation extends BaseActivity implements AdapterView.OnItemClickListener {
    private final  static String TAG = "im";

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
            TextView content = ButterKnife.findById(view, R.id.content);
            content.setText(c.number);

            if (c.avatar != null && c.avatar.length() > 0) {
                ImageView imageView = (ImageView) view.findViewById(R.id.header);
                Picasso.with(getBaseContext())
                        .load(c.avatar)
                        .placeholder(R.drawable.avatar_contact)
                        .into(imageView);
            } else {
                ImageView imageView = (ImageView) view.findViewById(R.id.header);
                imageView.setImageResource(R.drawable.avatar_contact);
            }
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

        adapter = new ConversationAdapter();
        lv = (ListView)findViewById(R.id.list);

        View footView  = getLayoutInflater().inflate(R.layout.share_item, null);
        lv.addFooterView(footView);
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {

        if (id == -1 && position == users.size()) {
            Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("sms:"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            String body = String.format("我正在使用“羊蹄甲”。 %s 可以给您的联系人发送消息，分享图片和音频。", Config.DOWNLOAD_URL);
            intent.putExtra("sms_body", body);
            startActivity(intent);
            return;
        }
        User u = users.get(position);

        Intent intent = new Intent(this, PeerMessageActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", u.uid);
        intent.putExtra("peer_name", u.name);
        intent.putExtra("peer_avatar", u.avatar);
        intent.putExtra("peer_up_timestamp", u.up_timestamp);
        intent.putExtra("current_uid", Profile.getInstance().uid);
        intent.putExtra("avatar", Profile.getInstance().avatar);
        startActivity(intent);

        finish();
    }
}
