package com.example.imservice;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import com.beetle.im.Timer;
import com.example.imservice.model.ContactDB;
import com.example.imservice.model.PhoneNumber;
import com.example.imservice.model.User;
import com.example.imservice.model.UserDB;
import com.google.code.p.leveldb.LevelDB;

import java.io.File;

import static android.os.SystemClock.uptimeMillis;
/**
 * Created by houxh on 14-8-11.
 */
public class LoginActivity extends Activity{
    private final String TAG = "imservice";

    private EditText phoneText;
    private EditText codeText;
    private Handler handler = new Handler();
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        ContactDB cdb = ContactDB.getInstance();
        cdb.setContentResolver(getApplicationContext().getContentResolver());

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        ldb.open(dir);

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

        Thread thread = new Thread() {
            @Override
            public void run() {
                final Token token = APIRequest.requestAuthToken("86", phone, code);
                if (token == null) {
                    Log.i(TAG, "auth token fail");
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "登录失败", Toast.LENGTH_SHORT).show();
                        }
                    };
                    handler.post(r);
                    return;
                }
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        Token t = Token.getInstance();
                        t.accessToken = token.accessToken;
                        t.refreshToken = token.refreshToken;
                        t.expireTimestamp = token.expireTimestamp;
                        t.uid = token.uid;
                        t.save();

                        User u = new User();
                        u.uid = t.uid;
                        u.number = new PhoneNumber("86", phone);
                        UserDB.getInstance().addUser(u);

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                };
                handler.post(r);
                Log.i(TAG, "code:" + code);
            }
        };
        thread.start();
    }

    public void getVerifyCode(View v) {
        Log.i(TAG, "get verify code");
        final String phone = phoneText.getText().toString();
        if (phone.length() != 11) {
            Toast.makeText(getApplicationContext(), "非法的手机号码", Toast.LENGTH_SHORT).show();
            return;
        }

        final ProgressDialog dialog = ProgressDialog.show(this, null, "Request...");
        Thread thread = new Thread() {
            @Override
            public void run() {
                final String code = APIRequest.requestVerifyCode("86", phone);
                if (code == null || code.length() == 0) {
                    Log.i(TAG, "request code fail");
                    final Runnable r = new Runnable() {
                        @Override
                        public void run() {
                            dialog.dismiss();
                            Toast.makeText(getApplicationContext(), "获取验证码失败", Toast.LENGTH_SHORT).show();
                        }
                    };
                    handler.post(r);
                    return;
                }
                final Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        codeText.setText(code);
                    }
                };
                handler.post(r);
                Log.i(TAG, "code:" + code);
            }
        };
        thread.start();
    }
}
