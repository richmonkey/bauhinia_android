package com.beetle.bauhinia;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.beetle.bauhinia.db.SyncKeyHandler;
import com.beetle.bauhinia.model.NewCount;
import com.beetle.bauhinia.model.Profile;
import com.beetle.bauhinia.service.ForegroundService;
import com.google.gson.Gson;

import com.beetle.bauhinia.activity.GroupCreatorActivity;
import com.beetle.bauhinia.activity.ZBarActivity;
import com.beetle.bauhinia.api.IMHttpAPI;
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
import com.beetle.im.GroupMessageObserver;
import com.beetle.im.PeerMessageObserver;

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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-8.
 */


public class MainActivity extends BaseActivity implements IMServiceObserver,
        GroupMessageObserver,
        PeerMessageObserver,
        AdapterView.OnItemClickListener,

        ContactDB.ContactObserver, NotificationCenter.NotificationCenterObserver {

    private static final int QRCODE_SCAN_REQUEST = 100;
    private static final int GROUP_CREATOR_RESULT = 101;

    //request permission id
    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 1;

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
            ConversationView view = null;
            if (convertView == null) {
                view = new ConversationView(MainActivity.this);
            } else {
                view = (ConversationView)convertView;
            }
            Conversation c = conversations.get(position);
            view.setConversation(c);;
            return view;
        }
    }

    // 初始化组件
    private void initWidget() {
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
            intent.putExtra("current_uid", Profile.getInstance().uid);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "main activity create...");

        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_CONTACTS)) {
                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        MY_PERMISSIONS_REQUEST_READ_CONTACTS);
            }
        } else {
            ContactDB.getInstance().loadContacts();
            ContactDB.getInstance().addObserver(this);
        }

        IMService im =  IMService.getInstance();
        im.addObserver(this);
        im.addPeerObserver(this);
        im.addGroupObserver(this);
        im.setUID(Profile.getInstance().uid);

        SyncKeyHandler handler = new SyncKeyHandler(this.getApplicationContext(), "sync_key");
        handler.load();

        HashMap<Long, Long> groupSyncKeys = handler.getSuperGroupSyncKeys();
        IMService.getInstance().clearSuperGroupSyncKeys();
        for (Map.Entry<Long, Long> e : groupSyncKeys.entrySet()) {
            IMService.getInstance().addSuperGroupSyncKey(e.getKey(), e.getValue());
            Log.i(TAG, "group id:" + e.getKey() + "sync key:" + e.getValue());
        }
        IMService.getInstance().setSyncKey(handler.getSyncKey());
        Log.i(TAG, "sync key:" + handler.getSyncKey());
        IMService.getInstance().setSyncKeyHandler(handler);

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
        nc.addObserver(this, PeerMessageActivity.CLEAR_NEW_MESSAGES);
        nc.addObserver(this, GroupMessageActivity.SEND_MESSAGE_NAME);
        nc.addObserver(this, GroupMessageActivity.CLEAR_MESSAGES);
        nc.addObserver(this, GroupMessageActivity.CLEAR_NEW_MESSAGES);

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

        //keep app foreground state
        Intent service = new Intent(this, ForegroundService.class);
        startService(service);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(TAG, "contact permission granted");
                    ContactDB.getInstance().loadContacts();
                    ContactDB.getInstance().addObserver(this);
                    refreshUsers();
                } else {
                    Log.i(TAG, "contact permission denied");
                }
                return;
            }
            default:
                break;
        }
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
        im.removePeerObserver(this);
        im.removeGroupObserver(this);
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
            int unread = NewCount.getNewCount(conv.cid);
            conv.setUnreadCount(unread);
            updatePeerConversationName(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        iter = GroupMessageDB.getInstance().newConversationIterator();
        while (true) {
            final Conversation conv = iter.next();
            if (conv == null) {
                break;
            }
            if (conv.message == null) {
                continue;
            }

            int unread = NewCount.getGroupNewCount(conv.cid);
            conv.setUnreadCount(unread);
            updateNotificationDesc(conv.message);
            updateGroupConversationName(conv);
            updateConversationDetail(conv);
            conversations.add(conv);
        }

        Comparator<Conversation> cmp = new Comparator<Conversation>() {
            public int compare(Conversation c1, Conversation c2) {
                if (c1.message.timestamp > c2.message.timestamp) {
                    return -1;
                } else if (c1.message.timestamp == c2.message.timestamp) {
                    return 0;
                } else {
                    return 1;
                }

            }
        };
        Collections.sort(conversations, cmp);
    }


    void updatePeerConversationName(Conversation conv) {
        User u = getUser(conv.cid);
        if (TextUtils.isEmpty(u.name)) {
            conv.setName(u.number);
        } else {
            conv.setName(u.name);
        }
        conv.setAvatar(u.avatar);
    }

    void updateGroupConversationName(final Conversation conv) {
        String groupName = getGroupName(conv.cid);
        if (!TextUtils.isEmpty(groupName)) {
            conv.setName(groupName);
        } else {
            groupName = String.format("%d", conv.cid);
            conv.setName(groupName);
            asyncGetGroup(conv.cid, new GetGroupCallback() {
                @Override
                public void onGroup(Group g) {
                    if (!TextUtils.isEmpty(g.topic)) {
                        conv.setName(g.topic);
                    }
                }
            });
        }
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

    void updateConversationDetail(Conversation conv) {
        String detail = messageContentToString(conv.message.content);
        conv.setDetail(detail);
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
        return topic;
    }
    public interface GetGroupCallback {
        void onGroup(Group g);
    }

    protected void asyncGetGroup(final long gid, final GetGroupCallback cb) {
        IMHttpAPI.Singleton().getGroup(gid)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Object>() {
                               @Override
                               public void call(Object obj) {
                                   Gson g = new Gson();
                                   JsonObject jobj = g.toJsonTree(obj).getAsJsonObject();
                                   JsonObject data = jobj.getAsJsonObject("data");
                                   String name = data.get("name").getAsString();
                                   Long masterID = data.get("master").getAsLong();
                                   JsonArray members = data.get("members").getAsJsonArray();

                                   if (masterID > 0) {
                                       GroupDB.getInstance().setGroupMaster(gid, masterID);
                                   }


                                   if (!TextUtils.isEmpty(name)) {
                                       GroupDB.getInstance().setGroupTopic(gid, name);
                                   }

                                   for (int i = 0; i < members.size(); i++) {
                                       JsonObject m = members.get(i).getAsJsonObject();
                                       long uid = m.get("uid").getAsLong();
                                       GroupDB.getInstance().addGroupMember(gid, uid);
                                   }

                                   if (!TextUtils.isEmpty(name)) {
                                       Group group = new Group();
                                       group.topic = name;
                                       cb.onGroup(group);
                                   }
                                   Log.i(TAG, "get group success");
                               }
                           }, new Action1<Throwable>() {
                               @Override
                               public void call(Throwable throwable) {
                                   Log.i(TAG, "get group fail");
                               }
                           }
                );


    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
                            long id) {
        Conversation conv = conversations.get(position);
        Log.i(TAG, "conv:" + conv.getName());
        Profile profile = Profile.getInstance();

        if (conv.type == Conversation.CONVERSATION_PEER) {
            User user = getUser(conv.cid);
            Intent intent = new Intent(this, PeerMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("peer_uid", conv.cid);
            intent.putExtra("peer_name", conv.getName());
            intent.putExtra("peer_avatar", conv.getAvatar());
            intent.putExtra("peer_up_timestamp", user.up_timestamp);
            intent.putExtra("current_uid", profile.uid);
            intent.putExtra("avatar", profile.avatar);

            startActivity(intent);
        } else {
            Log.i(TAG, "group conversation");

            Intent intent = new Intent(this, AppGroupMessageActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("group_id", conv.cid);
            intent.putExtra("group_name", conv.getName());
            intent.putExtra("current_uid", profile.uid);
            startActivity(intent);


        }
    }

    public void onConnectState(IMService.ConnectState state) {

    }



    public void onPeerInputting(long uid) {

    }

    @Override
    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "on peer message");
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);

        long cid = 0;
        if (msg.sender == Profile.getInstance().uid) {
            cid = msg.receiver;
        } else {
            cid = msg.sender;
        }

        Conversation conversation = findConversation(cid, Conversation.CONVERSATION_PEER);
        if (conversation == null) {
            conversation = newPeerConversation(cid);
            conversations.add(0, conversation);
        } else {
            conversations.remove(conversation);
            conversations.add(0, conversation);
        }
        conversation.message = imsg;

        if (msg.sender != Profile.getInstance().uid) {
            int unread = conversation.getUnreadCount() + 1;
            conversation.setUnreadCount(unread);
            NewCount.setNewCount(conversation.cid, unread);
        }
        updateConversationDetail(conversation);
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
        updatePeerConversationName(conversation);
        return conversation;
    }

    public Conversation newGroupConversation(long cid) {
        Conversation conversation = new Conversation();
        conversation.type = Conversation.CONVERSATION_GROUP;
        conversation.cid = cid;
        updateGroupConversationName(conversation);
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
            conversations.add(0, conversation);
        } else {
            conversations.remove(conversation);
            conversations.add(0, conversation);
        }
        conversation.message = imsg;

        if (msg.sender != Profile.getInstance().uid) {
            int unread = conversation.getUnreadCount() + 1;
            conversation.setUnreadCount(unread);
            NewCount.setGroupNewCount(conversation.cid, unread);
        }
        updateConversationDetail(conversation);
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
            conversations.add(0, conv);
        } else {
            conversations.remove(conv);
            conversations.add(0, conv);
        }

        int unread = conv.getUnreadCount() + 1;
        conv.setUnreadCount(unread);
        NewCount.setGroupNewCount(conv.cid, unread);

        conv.message = imsg;
        updateConversationDetail(conv);

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
        if (notification.member == Profile.getInstance().uid) {
            GroupDB.getInstance().joinGroup(notification.groupID);
        }
        GroupDB.getInstance().addGroupMember(notification.groupID, notification.member);
    }

    private void onGroupMemberLeave(IMessage.GroupNotification notification) {
        if (notification.member == Profile.getInstance().uid) {
            GroupDB.getInstance().leaveGroup(notification.groupID);
        }
        GroupDB.getInstance().removeGroupMember(notification.groupID, notification.member);
    }

    private void onGroupNameUpdate(IMessage.GroupNotification notification) {
        GroupDB.getInstance().setGroupTopic(notification.groupID, notification.groupName);
    }

    private void updateNotificationDesc(IMessage imsg) {
        if (imsg.content.getType() != IMessage.MessageType.MESSAGE_GROUP_NOTIFICATION) {
            return;
        }
        long currentUID = Profile.getInstance().uid;
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
        } else if (notification.notificationType == GroupNotification.NOTIFICATION_GROUP_NAME_UPDATED) {
            notification.description = String.format("群组改名为:\"%s\"", notification.groupName);
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
            updateConversationDetail(conversation);
        } else if (notification.name.equals(PeerMessageActivity.CLEAR_MESSAGES)) {
            Long peerUID = (Long) notification.obj;
            Conversation conversation = findConversation(peerUID, Conversation.CONVERSATION_PEER);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }
        } else if (notification.name.equals(PeerMessageActivity.CLEAR_NEW_MESSAGES)) {
            Long peerUID = (Long) notification.obj;
            Conversation conversation = findConversation(peerUID, Conversation.CONVERSATION_PEER);
            if (conversation != null) {
                conversation.setUnreadCount(0);
                NewCount.setNewCount(conversation.cid, 0);
            }
        } else if (notification.name.equals(GroupMessageActivity.SEND_MESSAGE_NAME)) {
            IMessage imsg = (IMessage) notification.obj;
            Conversation conversation = findConversation(imsg.receiver, Conversation.CONVERSATION_GROUP);
            if (conversation == null) {
                conversation = newGroupConversation(imsg.receiver);
                conversations.add(conversation);
            }
            conversation.message = imsg;
            updateConversationDetail(conversation);
        }  else if (notification.name.equals(GroupMessageActivity.CLEAR_MESSAGES)) {
            Long groupID = (Long)notification.obj;
            Conversation conversation = findConversation(groupID, Conversation.CONVERSATION_GROUP);
            if (conversation != null) {
                conversations.remove(conversation);
                adapter.notifyDataSetChanged();
            }
        } else if (notification.name.equals(GroupMessageActivity.CLEAR_NEW_MESSAGES)) {
            Long groupID = (Long)notification.obj;
            Conversation conversation = findConversation(groupID, Conversation.CONVERSATION_GROUP);
            if (conversation != null) {
                conversation.setUnreadCount(0);
                NewCount.setGroupNewCount(conversation.cid, 0);
            }
        }
    }

    public boolean canBack() {
        return false;
    }

}
