package com.gameservice.sdk.push.v2.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

/**
 * SmartPushOpenUtils
 * Description: SmartPush 开放工具类，简化本地保存以及读取deviceToken的操作.增加转换token的方法
 */
public class SmartPushOpenUtils {
    public static final String DEVICE_TOKEN_NOTIFY_ACTION = "com.gameservice.sdk.push.devicetoken";
    public static final String DEVICE_TOKEN = "com.gameservice.sdk.push.devicetoken";


    private static final String TAG = "SmartPushOpenUtils";
    private final static String KEY_NGDS_DEVICETOKEN = "key_ngds_devicetoken";
    private static final char HEX_DIGITS[] = {
        '0',
        '1',
        '2',
        '3',
        '4',
        '5',
        '6',
        '7',
        '8',
        '9',
        'A',
        'B',
        'C',
        'D',
        'E',
        'F'
    };
    public static void saveDeviceToken(Context context, String deviceToken) {
        if (TextUtils.isEmpty(deviceToken)) {
            Log.e(TAG, "saveDeviceToken-> deviceToken is null");
            return;
        }
        if (null == context) {
            Log.e(TAG, "saveDeviceToken-> context is null");
            return;
        }
        SharedPreferences sharedPreferences =
            context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(KEY_NGDS_DEVICETOKEN, deviceToken).commit();
    }


    /**
     * 返回SmartPush的deviceToken
     *
     * @param context 上下文环境
     * @return 设备token，当不存在则返回null
     */
    public static String loadDeviceToken(Context context) {
        if (null == context) {
            Log.e(TAG, "loadDeviceToken-> context is null");
            return null;
        }
        SharedPreferences sharedPreferences =
            context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        return sharedPreferences.getString(KEY_NGDS_DEVICETOKEN, null);
    }

    /**
     * 将返回的token数组按规则转换成deviceToken str 以供上报至服务器
     *
     * @param deviceToken
     * @return
     */
    public static String convertDeviceTokenArrary(byte[] deviceToken) {
        StringBuilder sb = new StringBuilder(deviceToken.length * 2);
        for (int i = 0; i < deviceToken.length; i++) {
            sb.append(HEX_DIGITS[(deviceToken[i] & 0xf0) >>> 4]);
            sb.append(HEX_DIGITS[deviceToken[i] & 0x0f]);
        }
        return sb.toString();
    }

}
