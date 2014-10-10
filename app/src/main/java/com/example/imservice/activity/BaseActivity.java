package com.example.imservice.activity;

import android.app.Activity;
import android.os.Handler;

import com.example.imservice.api.IMHttp;
import com.example.imservice.api.IMHttpFactory;

/**
 * Created by tsung on 10/10/14.
 */
public class BaseActivity extends Activity {
    protected final IMHttp imHttp = IMHttpFactory.Singleton();
    protected Handler handler = new Handler();
}
