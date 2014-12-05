package com.example.imservice;

import android.app.ActionBar;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.*;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.beetle.im.*;
import com.example.imservice.activity.PhotoActivity;

import com.example.imservice.api.types.Audio;

import com.example.imservice.constant.MessageKeys;

import com.example.imservice.formatter.MessageFormatter;
import com.example.imservice.model.Contact;
import com.example.imservice.model.ContactDB;
import com.example.imservice.model.PhoneNumber;
import com.example.imservice.api.types.User;
import com.example.imservice.model.UserDB;
import com.example.imservice.tools.AudioDownloader;
import com.example.imservice.tools.FileCache;
import com.example.imservice.tools.Notification;
import com.example.imservice.tools.NotificationCenter;
import com.example.imservice.tools.Outbox;
import com.google.gson.JsonObject;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import org.joda.time.Period;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.File;
import java.io.FileInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnItemClick;
import bz.tsung.media.audio.AudioRecorder;
import bz.tsung.media.audio.AudioUtil;
import bz.tsung.media.audio.converters.AmrWaveConverter;


import static android.os.SystemClock.uptimeMillis;
import static com.example.imservice.constant.RequestCodes.*;


public class IMActivity extends Activity implements IMServiceObserver, MessageKeys, AudioRecorder.IAudioRecorderListener,
        AdapterView.OnItemClickListener, AudioDownloader.AudioDownloaderObserver, Outbox.OutboxObserver {
    private static final String SEND_MESSAGE_NAME = "send_message";
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
    public void onRecordComplete(String file, final long mduration) {
        Audio audio = new Audio();
        audio.srcUrl = localAudioURL();

        long duration = mduration/1000;

        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", duration);
        audioJson.addProperty("url", audio.srcUrl);
        content.add(AUDIO, audioJson);

        IMessage imsg = new IMessage();
        imsg.sender = this.currentUID;
        imsg.receiver = peerUID;
        imsg.setContent(content.toString());
        imsg.timestamp = now();
        PeerMessageDB.getInstance().insertMessage(imsg, peerUID);

        Log.i(TAG, "msg local id:" + imsg.msgLocalID);


        messages.add(imsg);

        adapter.notifyDataSetChanged();
        listview.smoothScrollToPosition(messages.size()-1);

        Outbox ob = Outbox.getInstance();
        try {
            FileInputStream is = new FileInputStream(new File(file));
            FileCache.getInstance().storeFile(audio.srcUrl, is);
            ob.uploadAudio(imsg, FileCache.getInstance().getCachedFilePath(audio.srcUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(imsg, SEND_MESSAGE_NAME);
        nc.postNotification(notification);
    }

    @Override
    public void onRecordConvertedBuffer(byte[] buffer) {

    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        onItemClick(i);
    }

    private String localImageURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/images/"+ uuid.toString() + ".png";
    }

    private String localAudioURL() {
        UUID uuid = UUID.randomUUID();
        return "http://localhost/audios/" + uuid.toString() + ".amr";
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
/*
            if (isOutMsg(position)) {
                if ((msg.flags & MessageFlag.MESSAGE_FLAG_PEER_ACK) != 0) {
                    //对方已收到 2个勾
                    Log.i(TAG, "flag remote ack");
                    ImageView flagView = (ImageView)convertView.findViewById(R.id.flag);
                    flagView.setImageResource(R.drawable.msg_status_client_received);
                } else if ((msg.flags & MessageFlag.MESSAGE_FLAG_ACK) != 0) {
                    //服务器已收到 1个勾
                    Log.i(TAG, "flag server ack");
                    ImageView flagView = (ImageView)convertView.findViewById(R.id.flag);
                    flagView.setImageResource(R.drawable.msg_status_server_receive);
                } else if ((msg.flags & MessageFlag.MESSAGE_FLAG_FAILURE) != 0) {
                    //发送失败
                    Log.i(TAG, "flag failure");
                    //发送中 2个灰色的勾
                    ImageView flagView = (ImageView)convertView.findViewById(R.id.flag);
                    flagView.setImageResource(R.drawable.msg_status_send_error);
                } else {
                    //发送中 2个灰色的勾
                    ImageView flagView = (ImageView)convertView.findViewById(R.id.flag);
                    flagView.setImageResource(R.drawable.msg_status_gray_waiting);
                }
            }
*/
            switch (getMediaType(position)) {
                case IMAGE:
                    ImageView imageView = ButterKnife.findById(convertView, R.id.image);
                    Picasso.with(getBaseContext())
                            .load(((IMessage.Image) msg.content).image + "@256w_256h_0c")
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

        AudioDownloader.getInstance().addObserver(this);

        Outbox.getInstance().addObserver(this);
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
            takePicture();
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

    private boolean IsSameDay(Calendar c1, Calendar c2) {
        int year = c1.get(Calendar.YEAR);
        int month = c1.get(Calendar.MONTH);
        int day = c1.get(Calendar.DAY_OF_MONTH);

        int year2 = c2.get(Calendar.YEAR);
        int month2 = c2.get(Calendar.MONTH);
        int day2 = c2.get(Calendar.DAY_OF_MONTH);

        return (year == year2 && month == month2 && day == day2);
    }

    private boolean IsYestoday(Calendar c1, Calendar c2) {
        c2.roll(Calendar.DAY_OF_MONTH, -1);
        return IsSameDay(c1, c2);
    }

    private boolean IsBeforeYestoday(Calendar c1, Calendar c2) {
        c2.roll(Calendar.DAY_OF_MONTH, -1);
        return IsSameDay(c1, c2);
    }

    private boolean IsInWeek(Calendar c1, Calendar c2) {
        c2.roll(Calendar.DAY_OF_MONTH, -7);
        return c1.after(c2);
    }

    private boolean IsInMonth(Calendar c1, Calendar c2) {
        c2.roll(Calendar.DAY_OF_MONTH, -30);
        return c1.after(c2);
    }

    private String getLastOnlineTimestamp() {
        Calendar lastDate = Calendar.getInstance();
        lastDate.setTime(new Date(this.peer.up_timestamp*1000));
        Calendar todayDate = Calendar.getInstance();

        int year = lastDate.get(Calendar.YEAR);
        int month = lastDate.get(Calendar.MONTH);
        int day = lastDate.get(Calendar.DAY_OF_MONTH);
        int weekDay = lastDate.get(Calendar.DAY_OF_WEEK);
        int hour = lastDate.get(Calendar.HOUR_OF_DAY);
        int minute = lastDate.get(Calendar.MINUTE);
        Log.i(TAG, String.format("date:%d %d %d %d %d", year, month, day, weekDay, hour, minute));
        String str;
        if (IsSameDay(lastDate, todayDate)) {
            str = String.format("最后上线时间: 今天%02d:%02d", hour, minute);
        } else if (IsYestoday(lastDate, todayDate)) {
            str = String.format("最后上线时间: 昨天%02d:%02d", hour, minute);
        } else if (IsBeforeYestoday(lastDate, todayDate)) {
            str = String.format("最后上线时间: 前天%02d:%02d", hour, minute);
        } else if (IsInWeek(lastDate, todayDate)) {
            String[] t = {"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            str = String.format("最后上线于%s的%02d:%02d", t[weekDay], hour, minute);
        } else if (IsInMonth(lastDate, todayDate)) {
            str = String.format("最后上线 %02d-%02d-%02d %02d:%02d", year%100, month, day, hour, minute);
        } else {
            str = String.format("最后上线%04d年%02d月%02d日", year, month, day);
        }
        return str;
    }

    private void setSubtitle() {
        IMService.ConnectState state = IMService.getInstance().getConnectState();
        if (state == IMService.ConnectState.STATE_CONNECTING) {
            setSubtitle("连线中");
        } else if (state == IMService.ConnectState.STATE_CONNECTFAIL ||
                state == IMService.ConnectState.STATE_UNCONNECTED) {
            setSubtitle("未连接");
        } else if (this.peer.up_timestamp > 0) {
            String s = getLastOnlineTimestamp();
            setSubtitle(s);
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
        AudioDownloader.getInstance().removeObserver(this);
        Outbox.getInstance().removeObserver(this);
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

        NotificationCenter nc = NotificationCenter.defaultCenter();
        Notification notification = new Notification(imsg, SEND_MESSAGE_NAME);
        nc.postNotification(notification);
    }

    public void onConnectState(IMService.ConnectState state) {
        setSubtitle();
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
                setSubtitle();
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
            try {
                AudioDownloader.getInstance().downloadAudio(imsg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void onPeerMessageACK(int msgLocalID, long uid) {
        Log.i(TAG, "message ack");
        adapter.notifyDataSetChanged();
    }
    public void onPeerMessageRemoteACK(int msgLocalID, long uid) {
        Log.i(TAG, "message remote ack");
        adapter.notifyDataSetChanged();
    }
    public void onPeerMessageFailure(int msgLocalID, long uid) {
        Log.i(TAG, "message failure");
        adapter.notifyDataSetChanged();
    }

    void getPicture() {
        if (Build.VERSION.SDK_INT <19){
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent
                    , getResources().getString(R.string.product_fotos_get_from))
                    , SELECT_PICTURE);
        } else {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, SELECT_PICTURE_KITKAT);
        }
    }

    void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(takePictureIntent, TAKE_PICTURE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.i(TAG, "take or select picture fail:" + resultCode);
            return;
        }

        Bitmap bmp;
        if (requestCode == TAKE_PICTURE) {
            bmp = (Bitmap) data.getExtras().get("data");
        } else if (requestCode == SELECT_PICTURE || requestCode == SELECT_PICTURE_KITKAT)  {
            try {
                Uri selectedImageUri = data.getData();
                Log.i(TAG, "selected image uri:" + selectedImageUri);
                InputStream is = getContentResolver().openInputStream(selectedImageUri);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                bmp = BitmapFactory.decodeStream(is, null, options);
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }
        } else {
            Log.i(TAG, "invalide request code:" + requestCode);
            return;
        }

        double w = bmp.getWidth();
        double h = bmp.getHeight();
        double newHeight = 640.0;
        double newWidth = newHeight*w/h;


        Bitmap bigBMP = Bitmap.createScaledBitmap(bmp, (int)newWidth, (int)newHeight, true);

        double sw = 256.0;
        double sh = 256.0*h/w;

        Bitmap thumbnail = Bitmap.createScaledBitmap(bmp, (int)sw, (int)sh, true);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bigBMP.compress(Bitmap.CompressFormat.JPEG, 100, os);
        ByteArrayOutputStream os2 = new ByteArrayOutputStream();
        thumbnail.compress(Bitmap.CompressFormat.JPEG, 100, os2);

        String originURL = localImageURL();
        String thumbURL = localImageURL();
        try {
            FileCache.getInstance().storeByteArray(originURL, os);
            FileCache.getInstance().storeByteArray(thumbURL, os2);

            String path = FileCache.getInstance().getCachedFilePath(originURL);
            String thumbPath = FileCache.getInstance().getCachedFilePath(thumbURL);

            String tpath = path + "@256w_256h_0c";
            File f = new File(thumbPath);
            File t = new File(tpath);
            f.renameTo(t);


            JsonObject content = new JsonObject();
            content.addProperty(IMAGE, "file:" + path);

            IMessage imsg = new IMessage();
            imsg.sender = currentUID;
            imsg.receiver = peerUID;
            imsg.setContent(content.toString());
            imsg.timestamp = now();
            PeerMessageDB.getInstance().insertMessage(imsg, peerUID);

            messages.add(imsg);

            adapter.notifyDataSetChanged();
            listview.smoothScrollToPosition(messages.size()-1);


            Outbox.getInstance().uploadImage(imsg, path);

            NotificationCenter nc = NotificationCenter.defaultCenter();
            Notification notification = new Notification(imsg, SEND_MESSAGE_NAME);
            nc.postNotification(notification);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    IMessage playingMessage;

    void play(IMessage message) {
        IMessage.Audio audio = (IMessage.Audio) message.content;
        if (FileCache.getInstance().isCached(audio.url)) {
            try {
                audioUtil.startPlay(FileCache.getInstance().getCachedFilePath(audio.url));
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
            IMessage.Audio audio = (IMessage.Audio) message.content;
            if (FileCache.getInstance().isCached(audio.url)) {
                play(message);
            } else {
                try {
                    AudioDownloader.getInstance().downloadAudio(message);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (message.content instanceof IMessage.Image) {
            IMessage.Image image = (IMessage.Image) message.content;
            startActivity(PhotoActivity.newIntent(this, image.image));
        }
    }

    @Override
    public void onAudioDownloadSuccess(IMessage msg) {
        Log.i(TAG, "audio download success");
    }
    @Override
    public void onAudioDownloadFail(IMessage msg) {
        Log.i(TAG, "audio download fail");
    }

    @Override
    public void onAudioUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "audio upload success:" + url);
        IMessage.Audio audio = (IMessage.Audio)imsg.content;
        audio.url = url;

        IMMessage msg = new IMMessage();
        msg.sender = this.currentUID;
        msg.receiver = peerUID;
        msg.msgLocalID = imsg.msgLocalID;
        JsonObject content = new JsonObject();
        JsonObject audioJson = new JsonObject();
        audioJson.addProperty("duration", audio.duration);
        audioJson.addProperty("url", url);
        content.add(AUDIO, audioJson);
        msg.content = content.toString();

        IMService im = IMService.getInstance();
        im.sendPeerMessage(msg);
    }

    @Override
    public void onAudioUploadFail(IMessage msg) {
        Log.i(TAG, "audio upload fail");
        PeerMessageDB.getInstance().markMessageFailure(msg.msgLocalID, msg.receiver);
        Toast.makeText(IMActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onImageUploadSuccess(IMessage imsg, String url) {
        Log.i(TAG, "image upload success:" + url);

        IMMessage msg = new IMMessage();
        msg.sender = this.currentUID;
        msg.receiver = peerUID;
        JsonObject content = new JsonObject();
        content.addProperty(IMAGE, url);
        msg.content = content.toString();
        msg.msgLocalID = imsg.msgLocalID;

        IMService im = IMService.getInstance();
        im.sendPeerMessage(msg);
    }

    @Override
    public void onImageUploadFail(IMessage msg) {
        Log.i(TAG, "image upload fail");
        PeerMessageDB.getInstance().markMessageFailure(msg.msgLocalID, msg.receiver);
        Toast.makeText(IMActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
    }
}
