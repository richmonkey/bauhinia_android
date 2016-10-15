package com.beetle.bauhinia.react;

import android.app.Application;

import com.aakashns.reactnativedialogs.ReactNativeDialogsPackage;
import com.beetle.bauhinia.BuildConfig;
import com.facebook.react.LifecycleState;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.shell.MainReactPackage;

/**
 * Created by houxh on 16/8/29.
 */
public class ReactInstance {

    private static ReactInstance instance = new ReactInstance();
    public static ReactInstance getInstance() {
        return instance;
    }


    private ReactInstanceManager mReactInstanceManager;
    public void build( Application app) {
        mReactInstanceManager = ReactInstanceManager.builder()
                .setApplication(app)
                .setBundleAssetName("index.android.bundle")
                .setJSMainModuleName("index.android")
                .addPackage(new MainReactPackage())
                .addPackage(new GroupSettingPackage())
                .addPackage(new ReactNativeDialogsPackage())
                .setUseDeveloperSupport(BuildConfig.DEBUG)
                .setInitialLifecycleState(LifecycleState.BEFORE_CREATE)
                .build();

        if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
            mReactInstanceManager.createReactContextInBackground();
        }
    }

    public ReactInstanceManager getReactInstanceManager() {
        return mReactInstanceManager;
    }


}
