package com.example.imservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.example.imservice.api.IMHttp;
import com.example.imservice.api.IMHttpFactory;
import com.example.imservice.api.body.PostAuthToken;
import com.example.imservice.api.types.Code;
import com.example.imservice.api.types.User;
import com.example.imservice.model.UserDB;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-11.
 */
public class LoginActivity extends Activity {
    private final String TAG = "beetle";

    private EditText phoneText;
    private EditText codeText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        phoneText = (EditText)findViewById(R.id.login_edit_userName);
        codeText = (EditText)findViewById(R.id.login_edit_password);

        Token t = Token.getInstance();
        if (t.accessToken != null) {
            Log.i(TAG, "current uid:" + t.uid);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }

    public void onLogin(View v) {
        Log.i(TAG, "on login");
        final String phone = phoneText.getText().toString();
        final String code = codeText.getText().toString();
        if (phone.length() == 0 || code.length() == 0) {
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, null, "Request...");

        PostAuthToken postAuthToken = new PostAuthToken();
        postAuthToken.code = code;
        postAuthToken.zone = "86";
        postAuthToken.number = phone;
        IMApplication app = (IMApplication)getApplication();
        postAuthToken.ng_device_token = app.deviceToken;
        Log.i(TAG, "auth device token:" + app.deviceToken);
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.postAuthToken(postAuthToken)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Token>() {
                    @Override
                    public void call(Token token) {
                        dialog.dismiss();

                        Token t = Token.getInstance();
                        t.accessToken = token.accessToken;
                        t.refreshToken = token.refreshToken;
                        t.expireTimestamp = token.expireTimestamp;
                        t.uid = token.uid;

                        t.save();

                        User u = new User();
                        u.uid = token.uid;
                        u.number = phone;
                        u.zone = "86";
                        UserDB.getInstance().addUser(u);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "auth token fail");
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
                    }
                });
        Log.i(TAG, "code:" + code);
    }

    public void getVerifyCode(View v) {
        Log.i(TAG, "get verify code");
        final String phone = phoneText.getText().toString();
        if (phone.length() != 11) {
            Toast.makeText(getApplicationContext(), "非法的手机号码", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, null, "Request...");
        IMHttp imHttp = IMHttpFactory.Singleton();
        imHttp.getVerifyCode("86", phone)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Code>() {
                    @Override
                    public void call(Code code) {
                        dialog.dismiss();
                        codeText.setText(code.code);
                        Log.i(TAG, "code:" + code.code);
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        Log.i(TAG, "request code fail");
                        dialog.dismiss();
                        Toast.makeText(getApplicationContext(), "获取验证码失败", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
