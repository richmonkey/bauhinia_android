package com.beetle.bauhinia;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.support.v7.widget.Toolbar;

import com.beetle.bauhinia.activity.GroupCreatorActivity;
import com.beetle.bauhinia.activity.ZBarActivity;
import com.beetle.bauhinia.api.body.PostQRCode;
import com.beetle.bauhinia.api.types.Version;
import com.beetle.bauhinia.db.Conversation;
import com.beetle.bauhinia.db.ConversationIterator;
import com.beetle.bauhinia.db.GroupMessageDB;
import com.beetle.bauhinia.db.IMessage;
import com.beetle.bauhinia.db.IMessage.GroupNotification;
import com.beetle.bauhinia.db.PeerMessageDB;
import com.beetle.bauhinia.model.Group;
import com.beetle.bauhinia.model.GroupDB;
import com.beetle.im.IMMessage;
import com.beetle.im.IMService;
import com.beetle.im.IMServiceObserver;
import com.beetle.im.Timer;
import com.beetle.bauhinia.activity.BaseActivity;
import com.beetle.bauhinia.api.IMHttp;
import com.beetle.bauhinia.api.IMHttpFactory;
import com.beetle.bauhinia.api.body.PostPhone;
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
import java.util.InputMismatchException;
import java.util.List;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-8.
 */


public class MainActivity extends BaseActivity implements IMServiceObserver, AdapterView.OnItemClickListener,
        ContactDB.ContactObserver, NotificationCenter.NotificationCenterObserver {

    private static final int QRCODE_SCAN_REQUEST = 100;
    private static final int GROUP_CREATOR_RESULT = 101;

    List<Conversation> conversations;

    ListView lv;

    private static final String TAG = "beetle";

    private Timer refreshTimer;

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
            tv.setText(c.getName());

            tv = (TextView)view.findViewById(R.id.content);
            if (c.message != null) {
                tv.setText(messageContentToString(c.message.content));
            }

            int placeholder;
            if (c.type == Conversation.CONVERSATION_PEER) {
                placeholder = R.drawable.avatar_contact;
            } else {
                placeholder = R.drawable.group_avatar;
            }

            if (c.getAvatar() != null && c.getAvatar().length() > 0) {
                ImageView imageView = (ImageView) view.findViewById(R.id.header);
                Picasso.with(getBaseContext())
                        .load(c.getAvatar())
                        .placeholder(placeholder)
                        .into(imageView);
            }

            return view;
        }

        public  String messageContentToString(IMessage.MessageContent content) {
            if (content instanceof IMessage.Text) {
                return ((IMessage.Text) content).text;
            } else if (content instanceof IMessage.Image) {
                return "一张图片";
            } else if (content instanceof IMessage.Audio) {
                return "一段语音";
            } else if (content instanceof IMessage.GroupNotification) {
                return ((GroupNotification) content).description;
            } else if (content instanceof IMessage.Location) {
                return "一个地理位置";
            } else {
                return content.getRaw();
            }
        }

    }

    // 初始化组件
    private void initWidget() {
        Toolbar toolbar = (Toolbar)findViewById(R.id.support_toolbar);
        setSupportActionBar(toolbar);

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
        } else if (id == R.id.action_new_group) {
            Intent intent = new Intent(MainActivity.this, GroupCreatorActivity.class);
            startActivityForResult(intent, GROUP_CREATOR_RESULT);
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

        } else if (requestCode == GROUP_CREATOR_RESULT && resultCode == RESULT_OK){
            long group_id = data.getLongExtra("group_id", 0);
            Log.i(TAG, "new group id:" + group_id);
            if (group_id == 0) {
                return;
            }

            String groupName = getGroupName(group_id);
            Intent intent = new Intent(this, AppGroupMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("group_id", group_id);
            intent.putExtra("group_name", groupName);
            intent.putExtra("current_uid", Token.getInstance().uid);
            startActivity(intent);
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
        im.setUID(Token.getInstance().uid);
        im.start();

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
        nc.addObserver(this, PeerMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, PeerMessageActivity.CLEAR_MESSAGES);
        nc.addObserver(this, GroupMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, GroupMessageActivity.CLEAR_MESSAGES);

        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.getLatestVersion()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Version>() {
                    @Override
                    public void call(Version obj) {
                        MainActivity.this.checkVersion(obj);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "get latest version fail:" + throwable.getMessage());
                    }
                });
    }

    private void checkVersion(final Version version) {
        Log.i(TAG, "latest version:" + version.major + ":" + version.minor + " url:" + version.url);
        int versionCode = version.major*10+version.minor;
        PackageManager pm = this.getPackageManager();
        try {
            PackageInfo info = pm.getPackageInfo(getPackageName(), 0);
            if (versionCode > info.versionCode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("是否更新羊蹄甲?");
                builder.setTitle("提示");
                builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(version.url));
                        startActivity(browserIntent);
                    }
                });
                builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.create().show();
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
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
            conv.setName(u.name);
            conv.setAvatar(u.avatar);
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
                conv.setName(u.number);
            } else {
                conv.setName(u.name);
            }
            conv.setAvatar(u.avatar);
            conversations.add(conv);
        }

        iter = GroupMessageDB.getInstance().newConversationIterator();
        while (true) {
            Conversation conv = iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }
            updateNotificationDesc(conv.message);
            String groupName = getGroupName(conv.cid);
            conv.setName(groupName);
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

    private String getUserName(long uid) {
        User u = UserDB.getInstance().loadUser(uid);
        Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
        if (c != null) {
            u.name = c.displayName;
        } else {
            u.name = u.number;
        }
        return u.name;
    }

    private String getGroupName(long gid) {
        GroupDB db = GroupDB.getInstance();
        String topic = db.getGroupTopic(gid);
        if (!TextUtils.isEmpty(topic)) {
            return topic;
        }


        Group group = GroupDB.getInstance().loadGroup(gid);
        if (group == null) {
            return "";
        }

        ArrayList<Long> members = group.getMembers();

        topic = "";
        long loginUID = Token.getInstance().uid;
        int count = 0;
        for (Long uid : members) {
            if (uid == loginUID) {
                continue;
            }

            User u = UserDB.getInstance().loadUser(uid);

            Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
            if (c != null) {
                u.name = c.displayName;
            } else {
                u.name = u.number;
            }


            if (topic.length() > 0) {
                topic += ",";
            }
            topic += u.name;
            count += 1;
            if (count >= 3) {
                break;
            }
        }

        if (count < 3 && members.indexOf(loginUID) != -1) {
            topic = "我" + (count > 0 ? "," : "") + topic;
        }
        return topic;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.getName());

        if (conv.type == Conversation.CONVERSATION_PEER) {

            User user = getUser(conv.cid);

            Intent intent = new Intent(this, PeerMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("peer_uid", conv.cid);
            intent.putExtra("peer_name", conv.getName());
            intent.putExtra("peer_up_timestamp", user.up_timestamp);
            intent.putExtra("current_uid", Token.getInstance().uid);
            startActivity(intent);
        } else {
            Log.i(TAG, "group conversation");

            Intent intent = new Intent(this, AppGroupMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("group_id", conv.cid);
            intent.putExtra("group_name", conv.getName());
            intent.putExtra("current_uid", Token.getInstance().uid);
            startActivity(intent);


        }
    }

    public void onConnectState(IMService.ConnectState state) {

    }



    public void onPeerInputting(long uid) {

    }

    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "on peer message");
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        long cid = 0;
        if (msg.sender == Token.getInstance().uid) {
            cid = msg.receiver;
        } else {
            cid = msg.sender;
        }

        Conversation conversation = findConversation(cid, Conversation.CONVERSATION_PEER);
        if (conversation == null) {
            conversation = newPeerConversation(cid);
            conversations.add(conversation);
        }

        conversation.message = imsg;
        adapter.notifyDataSetChanged();
    }

    public Conversation findConversation(long cid, int type) {
        for (int i = 0; i < conversations.size(); i++) {
            Conversation conv = conversations.get(i);
            if (conv.cid == cid && conv.type == type) {
                return conv;
            }
        }
        return null;
    }

    public Conversation newPeerConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_PEER;
        conversation.cid = cid;
        User u = getUser(cid);
        conversation.setName(u.name);
        conversation.setAvatar(u.avatar);
        return conversation;
    }

    public Conversation newGroupConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_GROUP;
        conversation.cid = cid;
        conversation.setName(getGroupName(cid));
        return conversation;
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack on main");
    }

    public void onPeerMessageFailure(int msgLocalID, long uid) {
    }

    public void onGroupMessage(IMMessage msg) {
        Log.i(TAG, "on group message");
        IMessage imsg = new IMessage();
        imsg.timestamp = msg.timestamp;
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        Conversation conversation = findConversation(msg.receiver, Conversation.CONVERSATION_GROUP);
        if (conversation == null) {
            conversation = newGroupConversation(msg.receiver);
            conversations.add(conversation);
        }

        conversation.message = imsg;
        adapter.notifyDataSetChanged();
    }
    public void onGroupMessageACK(int msgLocalID, long uid) {

    }
    public void onGroupMessageFailure(int msgLocalID, long uid) {

    }

    public void onGroupNotification(String text) {
        GroupNotification groupNotification = IMessage.newGroupNotification(text);

        if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_CREATED) {
            onGroupCreated(groupNotification);
        } else if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            onGroupDisband(groupNotification);
        } else if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            onGroupMemberAdd(groupNotification);
        } else if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            onGroupMemberLeave(groupNotification);
        } else if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            onGroupNameUpdate(groupNotification);
        } else {
            Log.i(TAG, "unknown notification");
            return;
        }

        IMessage imsg = new IMessage();
        imsg.sender = 0;
        imsg.receiver = groupNotification.groupID;
        imsg.timestamp = now();
        imsg.setContent(groupNotification);
        updateNotificationDesc(imsg);


        Conversation conv = findConversation(groupNotification.groupID, Conversation.CONVERSATION_GROUP);
        if (conv == null) {
            conv = newGroupConversation(groupNotification.groupID);
            conversations.add(conv);
        }
        conv.message = imsg;

        if (groupNotification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            conv.setName(groupNotification.groupName);
        }
        adapter.notifyDataSetChanged();

    }

    private void onGroupCreated(IMessage.GroupNotification notification) {
        GroupDB db = GroupDB.getInstance();
        Group group = new Group();
        group.groupID = notification.groupID;
        group.topic = notification.groupName;
        group.master = notification.master;
        group.disbanded = false;
        group.setMembers(notification.members);

        db.addGroup(group);
    }

    private void onGroupDisband(IMessage.GroupNotification notification) {
        GroupDB db = GroupDB.getInstance();
        db.disbandGroup(notification.groupID);
    }

    private void onGroupMemberAdd(IMessage.GroupNotification notification) {
        GroupDB.getInstance().addGroupMember(notification.groupID, notification.member);
    }

    private void onGroupMemberLeave(IMessage.GroupNotification notification) {
        GroupDB.getInstance().removeGroupMember(notification.groupID, notification.member);
    }

    private void onGroupNameUpdate(IMessage.GroupNotification notification) {
        GroupDB.getInstance().setGroupTopic(notification.groupID, notification.groupName);
    }

    private void updateNotificationDesc(IMessage imsg) {
        if (imsg.content.getType() != IMessage.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }
        long currentUID = Token.getInstance().uid;
        GroupNotification notification = (GroupNotification)imsg.content;
        if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_CREATED) {
            if (notification.master == currentUID) {
                notification.description = String.format("您创建了\"%s\"群组", notification.groupName);
            } else {
                notification.description = String.format("您加入了\"%s\"群组", notification.groupName);
            }
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_DISBAND) {
            notification.description = "群组已解散";
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_ADDED) {
            notification.description = String.format("\"%s\"加入群", getUserName(notification.member));
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_MEMBER_LEAVED) {
            notification.description = String.format("\"%s\"离开群", getUserName(notification.member));
        }
    }

    @Override
    public void onNotification(Notification notification) {
        if (notification.name.equals(PeerMessageActivity.SEND_MESSAGE_NAME)) {
            IMessage imsg = (IMessage) notification.obj;
            Conversation conversation = findConversation(imsg.receiver, Conversation.CONVERSATION_PEER);
            if (conversation == null) {
                conversation = newPeerConversation(imsg.receiver);
                conversations.add(conversation);
            }
            conversation.message = imsg;
            adapter.notifyDataSetChanged();

        } else if (notification.name.equals(PeerMessageActivity.CLEAR_MESSAGES)) {
            Long peerUID = (Long)notification.obj;
            Conversation conversation = findConversation(peerUID, Conversation.CONVERSATION_PEER);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }

        } else if (notification.name.equals(GroupMessageActivity.SEND_MESSAGE_NAME)) {
            IMessage imsg = (IMessage) notification.obj;
            Conversation conversation = findConversation(imsg.receiver, Conversation.CONVERSATION_GROUP);
            if (conversation == null) {
                conversation = newGroupConversation(imsg.receiver);
                conversations.add(conversation);
            }
            conversation.message = imsg;
            adapter.notifyDataSetChanged();

        }  else if (notification.name.equals(GroupMessageActivity.CLEAR_MESSAGES)) {
            Long groupID = (Long)notification.obj;
            Conversation conversation = findConversation(groupID, Conversation.CONVERSATION_GROUP);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }
        }
    }

    public boolean canBack() {
        return false;
    }

}
