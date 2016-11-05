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
import com.beetle.bauhinia.model.Profile;
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
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DefaultHardwareBackBtnHandler;
import com.facebook.react.shell.MainReactPackage;
import com.facebook.react.uimanager.ViewManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GroupCreatorActivity extends Activity implements DefaultHardwareBackBtnHandler {
    private ReactRootView mReactRootView;
    private ReactInstanceManager mReactInstanceManager;

    public class GroupCreatorModule extends ReactContextBaseJavaModule {
        public GroupCreatorModule(ReactApplicationContext reactContext) {
            super(reactContext);
        }

        @Override
        public String getName() {
            return "GroupCreatorModule";
        }


        @ReactMethod
        public void finish() {
            GroupCreatorActivity.this.finish();
        }
        @ReactMethod
        public void finishWithGroupID(String strGroupID) {
            long groupID = Long.parseLong(strGroupID);
            Intent intent = new Intent();
            intent.putExtra("group_id", groupID);
            GroupCreatorActivity.this.setResult(RESULT_OK, intent);
            GroupCreatorActivity.this.finish();
        }
    }

    class GroupCreatorPackage implements ReactPackage {

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

            modules.add(new GroupCreatorModule(reactContext));

            return modules;
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mReactRootView = new ReactRootView(this);
        mReactInstanceManager = ReactInstanceManager.builder()
                .setApplication(getApplication())
                .setBundleAssetName("index.android.bundle")
                .setJSMainModuleName("index.android")
                .addPackage(new MainReactPackage())
                .addPackage(new GroupCreatorPackage())
                .addPackage(new ReactNativeDialogsPackage())
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.RESUMED)
                .build();

        Bundle props = new Bundle();
        props.putLong("uid", Profile.getInstance().uid);
        props.putString("token", Token.getInstance().accessToken);
        props.putString("url", Config.SDK_API_URL);
        ArrayList<Bundle> users = new ArrayList<Bundle>();
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

                    Bundle bundle = new Bundle();
                    bundle.putLong("uid", u.uid);
                    bundle.putString("name", u.name);
                    users.add(bundle);
                }
            }
        }
        props.putParcelableArrayList("users", users);
        mReactRootView.startReactApplication(mReactInstanceManager, "GroupCreatorIndex", props);
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
