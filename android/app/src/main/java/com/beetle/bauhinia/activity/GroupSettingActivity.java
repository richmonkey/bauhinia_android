package com.beetle.bauhinia.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;

import com.aakashns.reactnativedialogs.ReactNativeDialogsPackage;
import com.beetle.bauhinia.BuildConfig;
import com.beetle.bauhinia.Config;
import com.beetle.bauhinia.Token;
import com.beetle.bauhinia.api.types.User;
import com.beetle.bauhinia.model.Contact;
import com.beetle.bauhinia.model.ContactDB;
import com.beetle.bauhinia.model.Group;
import com.beetle.bauhinia.model.GroupDB;
import com.beetle.bauhinia.model.PhoneNumber;
import com.beetle.bauhinia.model.Profile;
import com.beetle.bauhinia.model.UserDB;
import com.beetle.bauhinia.react.GroupSettingModule;
import com.beetle.bauhinia.react.ReactInstance;
import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactPackage;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uimanager.ViewManager;
import com.google.gson.annotations.SerializedName;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupSettingActivity extends Activity implements DefaultHardwareBackBtnHandler {

    private static String TAG = "bauhinia";


    private long groupID;
    private ReactRootView mReactRootView;
    private ReactInstanceManager mReactInstanceManager;


    ReactInstanceManager.ReactInstanceEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        groupID = intent.getLongExtra("group_id", 0);
        if (groupID == 0) {
            return;
        }

        Group group = GroupDB.getInstance().loadGroup(groupID);
        if (group == null) {
            return;
        }

        mReactRootView = new ReactRootView(this);
        mReactInstanceManager = ReactInstance.getInstance().getReactInstanceManager();

        Bundle props = new Bundle();
        props.putLong("group_id", groupID);
        props.putBoolean("disbanded", group.disbanded);
        props.putString("topic", group.topic);
        props.putLong("master_id", group.master);
        props.putBoolean("is_master", group.master == Profile.getInstance().uid);
        props.putLong("uid", Profile.getInstance().uid);
        props.putString("token", Token.getInstance().accessToken);
        props.putString("url", Config.SDK_API_URL);
        props.putInt("hash_code", hashCode());

        ArrayList<Long> members = group.getMembers();
        Bundle bundles[] = new Bundle[members.size()];
        for (int i = 0; i < members.size(); i++) {
            Long uid = members.get(i);
            User u = UserDB.getInstance().loadUser(uid);
            Contact c = ContactDB.getInstance().loadContact(new PhoneNumber(u.zone, u.number));
            if (c != null) {
                u.name = c.displayName;
            } else {
                u.name = u.number;
            }

            Bundle b = new Bundle();
            b.putString("name", u.name);
            b.putLong("uid", uid);

            bundles[i] = b;
        }

        props.putParcelableArray("members", bundles);

        mReactRootView.startReactApplication(mReactInstanceManager, "GroupSettingIndex", props);
        setContentView(mReactRootView);

        listener = new ReactInstanceManager.ReactInstanceEventListener() {
            @Override
            public void onReactContextInitialized(ReactContext context) {
                GroupSettingModule m = context.getNativeModule(GroupSettingModule.class);
                Log.i(TAG, "module:" + m);
                m.addActivity(GroupSettingActivity.this);
            }
        };

        mReactInstanceManager.addReactInstanceEventListener(listener);

        ReactContext context = mReactInstanceManager.getCurrentReactContext();
        if (context != null) {
            GroupSettingModule m = context.getNativeModule(GroupSettingModule.class);
            Log.i(TAG, "module:" + m);
            m.addActivity(this);
        }
    }

    @Override
    public void invokeDefaultOnBackPressed() {
        super.onBackPressed();
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostPause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostResume(this, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        ReactContext context = mReactInstanceManager.getCurrentReactContext();
        if (context != null) {
            GroupSettingModule m = context.getNativeModule(GroupSettingModule.class);
            Log.i(TAG, "module:" + m);
            m.removeActivity(this);
        }

        if (listener != null) {
            mReactInstanceManager.removeReactInstanceEventListener(listener);
        }


        if (mReactInstanceManager != null) {
            mReactInstanceManager.onHostDestroy();
        }
    }


    @Override
    public void onBackPressed() {
        if (mReactInstanceManager != null) {
            mReactInstanceManager.onBackPressed();
        } else {
            super.onBackPressed();
        }
    }


    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU && mReactInstanceManager != null) {
            mReactInstanceManager.showDevOptionsDialog();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }


}
