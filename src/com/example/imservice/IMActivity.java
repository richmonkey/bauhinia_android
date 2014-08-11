package com.example.imservice;

import android.app.Activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.hardware.input.InputManager;
import android.os.*;
import android.provider.ContactsContract;
import android.provider.UserDictionary;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.beetle.im.*;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Date;


public class IMActivity extends Activity implements IMServiceObserver {
    private final String TAG = "imservice";

    private final long currentUID = 86013635273142L;

    private long peerUID;
    private ArrayList<IMessage> messages;

    private static final int IN_MSG = 0;
    private static final int OUT_MSG = 1;

    private EditText editText;

    BaseAdapter adapter;
    class ChatAdapter extends BaseAdapter {
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
            IMessage msg = messages.get(position);
            if (msg.sender == currentUID) {
                return OUT_MSG;
            } else {
                return IN_MSG;
            }
        }
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            IMessage msg = messages.get(position);
            if (convertView == null) {
                if (getItemViewType(position) == OUT_MSG) {
                    convertView = getLayoutInflater().inflate(
                            R.layout.chatting_item_msg_text_left, null);
                } else {
                    convertView = getLayoutInflater().inflate(
                            R.layout.chatting_item_msg_text_right, null);
                }
            }

            TextView content = (TextView)convertView.findViewById(R.id.tv_chatcontent);
            content.setText(msg.content.getText());
            return convertView;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat);

        Intent intent = getIntent();
        peerUID = intent.getLongExtra("peer_uid", 0);
        if (peerUID == 0) {
            Log.e(TAG, "peer uid is 0");
            return;
        }
        messages = new ArrayList<IMessage>();

        PeerMessageIterator iter = PeerMessageDB.getInstance().newMessageIterator(peerUID);
        while (true) {
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

        IMService.getInstance().addObserver(this);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "imactivity destory");
        IMService.getInstance().removeObserver(this);
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
        msg.content = text;

        IMessage imsg = new IMessage();
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.content = new IMessage.MessageContent();
        imsg.content.raw = msg.content;
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

    }

    public void onPeerInputting(long uid) {

    }
    public void onOnlineState(long uid, boolean on) {

    }

    public void onPeerMessage(IMMessage msg) {
        Log.i(TAG, "recv msg:" + msg.content);
        IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.content = new IMessage.MessageContent();
        imsg.content.raw = msg.content;
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
