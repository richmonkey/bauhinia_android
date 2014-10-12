package com.example.imservice;

import android.app.ActionBar;

import android.content.Context;
import android.content.Intent;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.beetle.im.*;
import com.example.imservice.activity.BaseActivity;
import com.example.imservice.constant.MessageKeys;
import com.example.imservice.formatter.MessageFormatter;
import com.example.imservice.model.Contact;
import com.example.imservice.model.ContactDB;
import com.example.imservice.model.PhoneNumber;
import com.example.imservice.api.types.User;
import com.example.imservice.model.UserDB;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;

import static android.os.SystemClock.uptimeMillis;


public class IMActivity extends BaseActivity implements IMServiceObserver, MessageKeys {
    private final String TAG = "imservice";

    private long currentUID;

    private long peerUID;
    private User peer;

    private ArrayList<IMessage> messages;

    private static final int IN_MSG = 0;
    private static final int OUT_MSG = 1;

    private EditText editText;

    private TextView titleView;
    private TextView subtitleView;

    private ActionBar actionBar;

    BaseAdapter adapter;

    static interface ContentTypes {
        public static int UNKNOWN = 0;
        public static int AUDIO = 2;
        public static int IMAGE = 4;
        public static int LOCATION = 6;
        public static int TEXT = 8;
    }

    class ChatAdapter extends BaseAdapter implements ContentTypes {
        @Override
        public int getCount() {
            return messages.size();
        }
        @Override
        public Object getItem(int position) {
            return messages.get(position);
        }
        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            final int basic;
            if (isOutMsg(position)) {
                basic = OUT_MSG;
            } else {
                basic = IN_MSG;
            }


            return getMediaType(position) + basic;
        }

        int getMediaType(int position) {
            IMessage msg = messages.get(position);
            final int media;
            if (msg.content instanceof IMessage.Text) {
                media = TEXT;
            } else if (msg.content instanceof IMessage.Image) {
                media = IMAGE;
            } else if (msg.content instanceof IMessage.Audio) {
                media = AUDIO;
            } else if (msg.content instanceof IMessage.Location) {
                media = LOCATION;
            } else {
                media = UNKNOWN;
            }

            return media;
        }

        boolean isOutMsg(int position) {
            IMessage msg = messages.get(position);
            return msg.sender == currentUID;
        }

        @Override
        public int getViewTypeCount() {
            return 10;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IMessage msg = messages.get(position);
            if (convertView == null) {
                if (isOutMsg(position)) {
                    convertView = getLayoutInflater().inflate(
                            R.layout.chat_container_right, null);
                } else {
                    convertView = getLayoutInflater().inflate(
                            R.layout.chat_container_left, null);
                }

                ViewGroup group = ButterKnife.findById(convertView, R.id.content);
                final int contentLayout;
                switch (getMediaType(position)) {
                    case TEXT:
                    case UNKNOWN:
                    default:
                        contentLayout = R.layout.chat_content_text;
                        break;
                    case IMAGE:
                        contentLayout = R.layout.chat_content_image;
                        break;
                }
                group.addView(getLayoutInflater().inflate(contentLayout, group, false));
            }

            switch (getMediaType(position)) {
                case IMAGE:
                    ImageView imageView = ButterKnife.findById(convertView, R.id.image);
                    Picasso.with(getBaseContext())
                            .load(((IMessage.Image)msg.content).image)
                            .into(imageView);
                    break;
                default:
                    TextView content = (TextView)convertView.findViewById(R.id.text);
                    content.setText(MessageFormatter.messageContentToString(msg.content));
                    break;
            }
            return convertView;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        this.currentUID = Token.getInstance().uid;
        Intent intent = getIntent();
        peerUID = intent.getLongExtra("peer_uid", 0);
        if (peerUID == 0) {
            Log.e(TAG, "peer uid is 0");
            return;
        }
        peer = loadUser(peerUID);
        if (peer == null) {
            Log.e(TAG, "load user fail");
            return;
        }
        messages = new ArrayList<IMessage>();

        PeerMessageIterator iter = PeerMessageDB.getInstance().newMessageIterator(peerUID);
        while (true && iter != null) {
            IMessage msg = iter.next();
            if (msg == null) {
                break;
            }
            messages.add(0, msg);
        }

        adapter = new ChatAdapter();
        ListView lv = (ListView)findViewById(R.id.listview);
        lv.setAdapter(adapter);
        editText = (EditText)findViewById(R.id.et_sendmessage);

        actionBar=getActionBar();
        actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        actionBar.setCustomView(R.layout.im_actionbar);
        actionBar.show();
        titleView = (TextView)actionBar.getCustomView().findViewById(R.id.title);
        subtitleView = (TextView)actionBar.getCustomView().findViewById(R.id.subtitle);
        titleView.setText(peer.name);
        setSubtitle();
        IMService.getInstance().addObserver(this);
        IMService.getInstance().subscribeState(peer.uid);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_photo) {
            return true;
        } else if(id == R.id.action_take) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private User loadUser(long uid) {
        User u = UserDB.getInstance().loadUser(uid);
        if (u == null) {
            return null;
        }
        Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
        if (c == null) {
            u.name = u.number;
        } else {
            u.name = c.displayName;
        }
        return u;
    }

    private void setSubtitle() {
        IMService.ConnectState state = IMService.getInstance().getConnectState();
        if (state == IMService.ConnectState.STATE_CONNECTING) {
            setSubtitle("连线中");
        } else if (state == IMService.ConnectState.STATE_CONNECTFAIL ||
                state == IMService.ConnectState.STATE_UNCONNECTED) {
            setSubtitle("未连接");
        } else {
            setSubtitle("");
        }
    }

    private void setSubtitle(String subtitle) {
        subtitleView.setText(subtitle);
        if (subtitle.length() > 0) {
            subtitleView.setVisibility(View.VISIBLE);
        } else {
            subtitleView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "imactivity destory");
        IMService.getInstance().removeObserver(this);
        IMService.getInstance().unsubscribeState(peerUID);
    }

    public static int now() {
        Date date = new Date();
        long t = date.getTime();
        return (int)(t/1000);
    }

    public void onSend(View v) {
        String text = editText.getText().toString();
        if (text.length() == 0) {
            return;
        }

        IMMessage msg = new IMMessage();
        msg.sender = this.currentUID;
        msg.receiver = peerUID;
        JsonObject textContent = new JsonObject();
        textContent.addProperty(TEXT, text);
        msg.content = textContent.toString();

        IMessage imsg = new IMessage();
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        imsg.timestamp = now();
        PeerMessageDB.getInstance().insertMessage(imsg, msg.receiver);

        msg.msgLocalID = imsg.msgLocalID;
        Log.i(TAG, "msg local id:" + imsg.msgLocalID);
        IMService im = IMService.getInstance();
        im.sendPeerMessage(msg);

        messages.add(imsg);

        editText.setText("");
        editText.clearFocus();
        InputMethodManager inputManager =
                (InputMethodManager)editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        inputManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        adapter.notifyDataSetChanged();
        ListView lv = (ListView)findViewById(R.id.listview);
        lv.smoothScrollToPosition(messages.size()-1);
    }

    public void onConnectState(IMService.ConnectState state) {
        if (state == IMService.ConnectState.STATE_CONNECTING) {
            titleView.setText("连线中");
        } else if (state == IMService.ConnectState.STATE_CONNECTFAIL ||
                state == IMService.ConnectState.STATE_UNCONNECTED) {
            titleView.setText("未连接");
        }
    }

    public void onPeerInputting(long uid) {
        if (uid == peerUID) {
            setSubtitle("对方正在输入");
            Timer t = new Timer() {
                @Override
                protected void fire() {
                    setSubtitle();
                }
            };
            long start = uptimeMillis() + 10*1000;
            t.setTimer(start);
            t.resume();
        }
    }

    public void onOnlineState(long uid, boolean on) {
        if (uid == peerUID) {
            if (on) {
                setSubtitle("对方在线");
            } else {
                setSubtitle("");
            }
        }
    }

    public void onPeerMessage(IMMessage msg) {
        if (msg.sender != peerUID) {
            return;
        }
        Log.i(TAG, "recv msg:" + msg.content);
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        messages.add(imsg);

        adapter.notifyDataSetChanged();
        ListView lv = (ListView)findViewById(R.id.listview);
        lv.smoothScrollToPosition(messages.size()-1);
    }
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack");
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {

    }
}
