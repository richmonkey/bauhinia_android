package com.beetle.bauhinia.activity;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.beetle.bauhinia.model.UserDB;
import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactPackage;
import com.facebook.react.ReactRootView;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaScriptModule;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
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

    private long groupID;
    private ReactRootView mReactRootView;
    private ReactInstanceManager mReactInstanceManager;


    public class GroupSettingModule extends ReactContextBaseJavaModule {

        public GroupSettingModule(ReactApplicationContext reactContext) {
            super(reactContext);
        }

        @Override
        public String getName() {
            return "GroupSettingModule";
        }


        @ReactMethod
        public void finish() {
            GroupSettingActivity.this.finish();
        }

        @ReactMethod
        public void loadUsers(Callback successCallback) {
            WritableArray users = new WritableNativeArray();

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

                        WritableMap obj = new WritableNativeMap();
                        obj.putDouble("uid", (double)u.uid);
                        obj.putString("name", u.name);
                        users.pushMap(obj);
                    }
                }
            }
            successCallback.invoke(users);
        }

    }

    class GroupSettingPackage implements ReactPackage {

        @Override
        public List<Class<? extends JavaScriptModule>> createJSModules() {
            return Collections.emptyList();
        }

        @Override
        public List<ViewManager> createViewManagers(ReactApplicationContext reactContext) {
            return Collections.emptyList();
        }

        @Override
        public List<NativeModule> createNativeModules(
                ReactApplicationContext reactContext) {
            List<NativeModule> modules = new ArrayList<NativeModule>();

            modules.add(new GroupSettingModule(reactContext));

            return modules;
        }
    }



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
        mReactInstanceManager = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setBundleAssetName("index.android.bundle")
                .setJSMainModuleName("index.android")
                .addPackage(new MainReactPackage())
                .addPackage(new GroupSettingPackage())
                .addPackage(new ReactNativeDialogsPackage())
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        Bundle props = new Bundle();
        props.putLong("group_id", groupID);
        props.putBoolean("disbanded", group.disbanded);
        props.putString("topic", group.topic);
        props.putLong("master_id", group.master);
        props.putLong("uid", Token.getInstance().uid);
        props.putString("token", Token.getInstance().accessToken);
        props.putString("url", Config.SDK_API_URL);

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
            b.putLong("member_id", uid);

            bundles[i] = b;
        }

        props.putParcelableArray("members", bundles);

        mReactRootView.startReactApplication(mReactInstanceManager, "GroupSettingIndex", props);

        setContentView(mReactRootView);
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
