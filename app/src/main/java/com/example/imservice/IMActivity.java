package com.example.imservice;

import android.app.ActionBar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.beetle.im.*;
import com.example.imservice.api.IMHttp;
import com.example.imservice.api.IMHttpFactory;
import com.example.imservice.api.types.Audio;
import com.example.imservice.api.types.Image;
import com.example.imservice.constant.MessageKeys;
import com.example.imservice.constant.RequestCodes;
import com.example.imservice.formatter.MessageFormatter;
import com.example.imservice.model.Contact;
import com.example.imservice.model.ContactDB;
import com.example.imservice.model.PhoneNumber;
import com.example.imservice.api.types.User;
import com.example.imservice.model.UserDB;
import com.example.imservice.tools.AudioCache;
import com.example.imservice.tools.ImageMIME;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import org.apache.commons.io.IOUtils;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import bz.tsung.media.audio.AudioRecorder;
import bz.tsung.media.audio.AudioUtil;
import bz.tsung.media.audio.converters.AmrWaveConverter;
import retrofit.mime.TypedFile;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import static android.os.SystemClock.uptimeMillis;


public class IMActivity extends Activity implements IMServiceObserver, MessageKeys, AudioRecorder.IAudioRecorderListener, AdapterView.OnItemClickListener {
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

    @Override
    public void onRecordFail() {

    }

    @Override
    public void onRecordComplete(String file, final long duration) {
        String type = "audio/amr";
        TypedFile typedFile = new TypedFile(type, new File(file));
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.postAudios(type, typedFile)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Audio>() {
                    @Override
                    public void call(Audio audio) {
                        onAudio(duration/1000, audio);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Toast.makeText(IMActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onRecordConvertedBuffer(byte[] buffer) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        onItemClick(i);
    }

    static interface ContentTypes {
        public static int UNKNOWN = 0;
        public static int AUDIO = 2;
        public static int IMAGE = 4;
        public static int LOCATION = 6;
        public static int TEXT = 8;
    }

    @InjectView(android.R.id.list)
    ListView listview;
    @InjectView(R.id.audio_recorder)
    AudioRecorder audioRecorder;
    AudioUtil audioUtil;

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

        private class ViewHolder {
            ViewHolder(View view) {
                ButterKnife.inject(this, view);
            }
        }

        class AudioHolder extends ViewHolder {
            @InjectView(R.id.play_control)
            ImageView control;
            @InjectView(R.id.progress)
            ProgressBar progress;
            @InjectView(R.id.duration)
            TextView duration;

            AudioHolder(View view) {
                super(view);
            }
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
                    case AUDIO:
                        contentLayout = R.layout.chat_content_audio;
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
                            .load(((IMessage.Image) msg.content).image + "@256w_256h_0c")
                            //.load(((IMessage.Image) msg.content).image + "@512w_512h_0c")
                            .into(imageView);
                    break;
                case AUDIO:
                    final IMessage.Audio audio = (IMessage.Audio) msg.content;
                    AudioHolder audioHolder =  new AudioHolder(convertView);
                    audioHolder.progress.setMax((int) audio.duration);
                    if (audioUtil.isPlaying() && playingMessage != null && msg.msgLocalID == playingMessage.msgLocalID) {
                        audioHolder.control.setImageResource(R.drawable.chatto_voice_playing_f2);
                        //audioHolder.progress.setProgress(audioUtil.get);
                    } else {
                        audioHolder.control.setImageResource(R.drawable.chatto_voice_playing);
                        audioHolder.progress.setProgress(0);
                    }
                    Period period = new Period().withSeconds((int) audio.duration);
                    PeriodFormatter periodFormatter = new PeriodFormatterBuilder()
                            .appendMinutes()
                            .appendSeparator(":")
                            .appendSeconds()
                            .appendSuffix("\"")
                            .toFormatter();
                    audioHolder.duration.setText(periodFormatter.print(period));
                    break;
                default:
                    TextView content = (TextView)convertView.findViewById(R.id.text);
                    content.setFocusable(false);
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
        ButterKnife.inject(this);

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
        listview.setAdapter(adapter);
        //listview.setOnItemClickListener(this);
        editText = (EditText)findViewById(R.id.text_message);

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

        audioUtil = new AudioUtil(this);
        audioUtil.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                adapter.notifyDataSetChanged();
            }
        });
        audioUtil.setOnStopListener(new AudioUtil.OnStopListener() {
            @Override
            public void onStop(int reason) {
                adapter.notifyDataSetChanged();
            }
        });
        audioRecorder.setAudioUtil(audioUtil);
        audioRecorder.setAudioRecorderListener(this);
        audioRecorder.setWaveConverter(new AmrWaveConverter());
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
            getPicture();
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
        audioUtil.release();
    }

    @OnClick(R.id.button_switch)
    void switchButtons() {
        if (audioRecorder.getVisibility() == View.VISIBLE) {
            audioRecorder.setVisibility(View.GONE);
            editText.setVisibility(View.VISIBLE);
        } else {
            audioRecorder.setVisibility(View.VISIBLE);
            editText.setVisibility(View.GONE);
        }
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
        listview.smoothScrollToPosition(messages.size()-1);
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
        final IMessage imsg = new IMessage();
        imsg.timestamp = now();
        imsg.msgLocalID = msg.msgLocalID;
        imsg.sender = msg.sender;
        imsg.receiver = msg.receiver;
        imsg.setContent(msg.content);
        messages.add(imsg);

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);

        if (imsg.content instanceof IMessage.Audio) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AudioCache.download(imsg);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack");
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {

    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {

    }

    void getPicture() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent
                    , getResources().getString(R.string.product_fotos_get_from))
                    , RequestCodes.SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, RequestCodes.SELECT_PICTURE_KITKAT);
        }

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            return;
        }

        if (requestCode == RequestCodes.SELECT_PICTURE || requestCode == RequestCodes.SELECT_PICTURE_KITKAT) {
            Uri selectedImageUri = data.getData();
            onImageUri(selectedImageUri);
        }
    }

    void onImageUri(Uri selectedImageUri) {
        try {
            InputStream is = getContentResolver().openInputStream(selectedImageUri);

            File file = File.createTempFile("temp_foto", ".image", null);
            FileOutputStream fos = new FileOutputStream(file);

            IOUtils.copy(is, fos);

            is.close();
            fos.close();

            String type = ImageMIME.getMimeType(file);
            TypedFile typedFile = new TypedFile(type, file);
            IMHttp imHttp = IMHttpFactory.Singleton();
            imHttp.postImages(type// + "; charset=binary"
                    , typedFile)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Action1<Image>() {
                        @Override
                        public void call(Image image) {
                            onImage(image);
                        }
                    }, new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {

                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    void onImage(Image image) {
        IMMessage msg = new IMMessage();
        msg.sender = this.currentUID;
        msg.receiver = peerUID;
        JsonObject content = new JsonObject();
        content.addProperty(IMAGE, image.srcUrl);
        msg.content = content.toString();

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

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);
    }

    void onAudio(long duration, Audio audio) {
        IMMessage msg = new IMMessage();
        msg.sender = this.currentUID;
        msg.receiver = peerUID;
        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", audio.srcUrl);
        content.add(AUDIO, audioJson);
        msg.content = content.toString();

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

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);
    }

    IMessage playingMessage;

    void play(IMessage message) {
        if (AudioCache.exists(message)) {
            try {
                audioUtil.startPlay(AudioCache.getFile(message));
                playingMessage = message;
                adapter.notifyDataSetChanged();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @OnItemClick(android.R.id.list)
    void onItemClick(int position) {
        final IMessage message = messages.get(position);
        if (message.content instanceof IMessage.Audio) {
            if (AudioCache.exists(message)) {
                play(message);
            } else {
                Toast.makeText(this, "Download audio..", Toast.LENGTH_LONG).show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            AudioCache.download(message);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    play(message);
                                }
                            });
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        }
    }
}
