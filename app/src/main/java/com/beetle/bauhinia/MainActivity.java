package com.beetle.bauhinia;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.beetle.bauhinia.activity.ZBarActivity;
import com.beetle.bauhinia.api.body.PostDeviceToken;
import com.beetle.bauhinia.api.body.PostQRCode;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.Timer;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.api.IMHttp;
import com.beetle.bauhinia.api.IMHttpFactory;
import com.beetle.bauhinia.api.body.PostPhone;
import com.beetle.bauhinia.formatter.MessageFormatter;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.UserDB;
import com.beetle.bauhinia.tools.*;
import com.beetle.bauhinia.tools.Notification;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-8.
 */


public class MainActivity extends BaseActivity implements IMServiceObserver, AdapterView.OnItemClickListener,
        ContactDB.ContactObserver, NotificationCenter.NotificationCenterObserver {

    private static final String SEND_MESSAGE_NAME = "send_message";

    private static final int QRCODE_SCAN_REQUEST = 100;

    List<Conversation> conversations;

    ListView lv;

    private static final String TAG = "beetle";

    private Timer refreshTimer;

    private android.support.v7.app.ActionBar actionBar;
    private BaseAdapter adapter;
    class ConversationAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return conversations.size();
        }
        @Override
        public Object getItem(int position) {
            return conversations.get(position);
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
            Conversation c = conversations.get(position);
            tv.setText(c.name);

            tv = (TextView)view.findViewById(R.id.content);
            if (c.message != null) {
                tv.setText(MessageFormatter.messageContentToString(c.message.content));
            }

            if (c.avatar != null && c.avatar.length() > 0) {
                ImageView imageView = (ImageView) view.findViewById(R.id.header);
                Picasso.with(getBaseContext())
                        .load(c.avatar)
                        .into(imageView);
            }
            return view;
        }
    }

    // 初始化组件
    private void initWidget() {
        actionBar = getSupportActionBar();
        actionBar.show();

        lv = (ListView) findViewById(R.id.list);
        adapter = new ConversationAdapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new_conversation || id == R.id.action_new_conversation2) {
            Intent intent = new Intent(MainActivity.this, NewConversation.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_qrcode) {
            Intent intent = new Intent(MainActivity.this, ZBarActivity.class);
            startActivityForResult(intent, QRCODE_SCAN_REQUEST);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "result:" + resultCode + " request:" + requestCode);

        if (requestCode == QRCODE_SCAN_REQUEST && resultCode == RESULT_OK) {
            String symbol = data.getStringExtra("symbol");
            Log.i(TAG, "symbol:"+symbol);
            if (TextUtils.isEmpty(symbol)) {
                return;
            }

            PostQRCode qrcode = new PostQRCode();
            qrcode.sid = symbol;
            IMHttp imHttp = IMHttpFactory.Singleton();
            imHttp.postQRCode(qrcode)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Object>() {
                        @Override
                        public void call(Object obj) {
                            Log.i(TAG, "sweep success");
                            Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Log.i(TAG, "sweep fail");
                            Toast.makeText(MainActivity.this, "登录失败", Toast.LENGTH_SHORT).show();
                        }
                    });

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "main activity create...");

        setContentView(R.layout.activity_main);

        ContactDB.getInstance().loadContacts();
        ContactDB.getInstance().addObserver(this);

        IMService im =  IMService.getInstance();
        im.addObserver(this);

        refreshConversations();
        initWidget();

        this.refreshTimer = new Timer() {
            @Override
            protected  void fire() {
                MainActivity.this.refreshUsers();
            }
        };
        this.refreshTimer.setTimer(1000*1, 1000*3600);
        this.refreshTimer.resume();
        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.addObserver(this, SEND_MESSAGE_NAME);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ContactDB.getInstance().removeObserver(this);
        IMService im =  IMService.getInstance();
        im.removeObserver(this);
        this.refreshTimer.suspend();
        NotificationCenter nc = NotificationCenter.defaultCenter();
        nc.removeObserver(this);
        Log.i(TAG, "main activity destroyed");
    }

    @Override
    public void OnExternalChange() {
        Log.i(TAG, "contactdb changed");

        for (Conversation conv : conversations) {
            User u = getUser(conv.cid);
            conv.name = u.name;
            conv.avatar = u.avatar;
        }
        adapter.notifyDataSetChanged();

        refreshUsers();
    }

    void refreshUsers() {
        Log.i(TAG, "refresh user...");
        final ArrayList<Contact> contacts = ContactDB.getInstance().copyContacts();

        List<PostPhone> phoneList = new ArrayList<PostPhone>();
        HashSet<String> sets = new HashSet<String>();
        for (Contact contact : contacts) {
            if (contact.phoneNumbers != null && contact.phoneNumbers.size() > 0) {
                for (Contact.ContactData contactData : contact.phoneNumbers) {
                    PhoneNumber n = new PhoneNumber();
                    if (!n.parsePhoneNumber(contactData.value)) {
                        continue;
                    }
                    if (sets.contains(n.getZoneNumber())) {
                        continue;
                    }
                    sets.add(n.getZoneNumber());

                    PostPhone phone = new PostPhone();
                    phone.number = n.getNumber();
                    phone.zone = n.getZone();
                    if (contact.displayName != null) {
                        phone.name = contact.displayName;
                    } else {
                        phone.name = "";
                    }
                    phoneList.add(phone);
                }
            }
        }
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.postUsers(phoneList)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<ArrayList<User>>() {
                    @Override
                    public void call(ArrayList<User> users) {
                        if (users == null) return;
                        UserDB userDB = UserDB.getInstance();
                        for (int i = 0; i < users.size(); i++) {
                            User u = users.get(i);
                            if (u.uid > 0) {
                                userDB.addUser(u);
                                Log.i(TAG, "user:"+ u.uid + " number:" + u.number);
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.e(TAG, throwable.getMessage());
                    }
                });
    }

    void refreshConversations() {
        conversations = new ArrayList<Conversation>();
        ConversationIterator iter = PeerMessageDB.getInstance().newConversationIterator();
        while (true) {
            Conversation conv = iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }
            User u = getUser(conv.cid);
            if (TextUtils.isEmpty(u.name)) {
                conv.name = u.number;
            } else {
                conv.name = u.name;
            }
            conv.avatar = u.avatar;
            conversations.add(conv);
        }
    }

    private User getUser(long uid) {
        User u = UserDB.getInstance().loadUser(uid);
        Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
        if (c != null) {
            u.name = c.displayName;
        } else {
            u.name = u.number;
        }
        return u;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.name);

        Intent intent = new Intent(this, IMActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("peer_uid", conv.cid);
        startActivity(intent);
    }

    public void onConnectState(IMService.ConnectState state) {

    }
    public void onPeerInputting(long uid) {

    }

    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "on peer message");
        Conversation conversation = null;
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == msg.sender) {
                conversation = conv;
                break;
            }
        }

        if (conversation == null) {
            conversation = new Conversation();
            conversation.cid = msg.sender;
            User u = getUser(msg.sender);
            conversation.name = u.name;
            conversation.avatar = u.avatar;
            conversations.add(conversation);
        }

        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        conversation.message = imsg;

        adapter.notifyDataSetChanged();

    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack on main");
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {
    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {
    }

    @Override
    public void onNotification(Notification notification) {
        IMessage imsg = (IMessage)notification.obj;
        Conversation conversation = null;
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == imsg.receiver) {
                conversation = conv;
                break;
            }
        }

        if (conversation == null) {
            conversation = new Conversation();
            conversation.cid = imsg.receiver;
            User u = getUser(imsg.receiver);
            conversation.name = u.name;
            conversation.avatar = u.avatar;
            conversations.add(conversation);
        }
        conversation.message = imsg;
        adapter.notifyDataSetChanged();
    }

    public boolean canBack() {
        return false;
    }
}
