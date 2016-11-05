package com.beetle.bauhinia.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.beetle.bauhinia.MainActivity;
import com.beetle.bauhinia.R;
import com.beetle.bauhinia.Token;
import com.beetle.bauhinia.api.IMHttp;
import com.beetle.bauhinia.api.IMHttpFactory;
import com.beetle.bauhinia.api.types.Code;
import com.beetle.bauhinia.model.Profile;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

/**
 * Created by houxh on 14-8-11.
 */
public class LoginActivity extends AccountActivity implements TextView.OnEditorActionListener {
    private final String TAG = "beetle";

    @InjectView(R.id.text_phone)
    EditText phoneText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        ButterKnife.inject(this);

        Token t = Token.getInstance();
        Log.i(TAG, "access token:" + t.accessToken);
        if (t.accessToken != null) {
            Log.i(TAG, "current uid:" + Profile.getInstance().uid);
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }

        phoneText.setOnEditorActionListener(this);
    }

    @OnClick(R.id.btn_verify_code)
    void getVerifyCode() {
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
                        startActivity(VerifyActivity.newIntent(LoginActivity.this, phone));
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

    @Override
    public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        if (i == EditorInfo.IME_ACTION_NEXT) {
            getVerifyCode();
        }
        return false;
    }


    @Override
    public boolean canBack() {
        return false;
    }

}
