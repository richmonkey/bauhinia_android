package com.gameservice.sdk.push.v2.api;

public interface IMsgReceiver {
    // 接收DeviceToken
    public void onDeviceToken(byte[] tokenArrary);
}
