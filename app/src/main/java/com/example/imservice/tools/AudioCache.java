package com.example.imservice.tools;

import android.text.TextUtils;

import java.io.File;
import java.io.IOException;

/**
 * Created by tsung on 11/16/14.
 */
public class AudioCache {
    static String tempFolder = "";

    public static String getPath() {
        if (TextUtils.isEmpty(tempFolder)) {
            try {
                File file = File.createTempFile("audios_", ".amr");
                tempFolder = file.getParent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return tempFolder + "/.audio/";
    }
}
