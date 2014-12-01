package com.example.imservice.tools;

import android.text.TextUtils;

import com.example.imservice.IMessage;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

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

    public static String getFile(IMessage imsg) {
        String filePath = AudioCache.getPath() + imsg.msgLocalID;
        return filePath;
    }

    public static void download(IMessage imsg) throws IOException {
        String filePath = getFile(imsg);
        File folder = new File(getPath());
        folder.mkdirs();
        File file = new File(filePath);
        if (!file.exists()) {
            IMessage.Audio audio = (IMessage.Audio) imsg.content;

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(audio.url)
                    .build();
            Response response = client.newCall(request).execute();
            if (response.isSuccessful() && file.createNewFile()) {
                InputStream inputStream = response.body().byteStream();
                FileOutputStream fileOutputStream = new FileOutputStream(filePath);
                IOUtils.copy(inputStream, fileOutputStream);
                inputStream.close();
                fileOutputStream.close();
            }
        }
    }

    public static boolean exists(IMessage imsg) {
        String filePath = getFile(imsg);
        File file = new File(filePath);
        return file.exists();
    }
}
