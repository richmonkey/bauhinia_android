package com.beetle;

import android.app.Application;
import android.content.Context;
import android.test.ApplicationTestCase;
import android.util.Log;

import com.beetle.bauhinia.tools.FileCache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest extends ApplicationTestCase<Application> {

    private final  static String TAG = "beetle";

    public ApplicationTest() {
        super(Application.class);
    }

    public void testFileCache() {
        File dir = getSystemContext().getDir("cache", Context.MODE_PRIVATE);

        FileCache fc = FileCache.getInstance();
        fc.setDir(dir);
        String url = "http://localhost://tt";
        boolean r = fc.isCached(url);
        Log.i(TAG, "cached:" + r);

        InputStream is = new ByteArrayInputStream("11".getBytes());
        try {
            fc.storeFile(url, is);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }

        r = fc.isCached(url);
        assertTrue(r);

        String path = fc.getCachedFilePath(url);
        Log.i(TAG, "path:" + path);
        try {
            is = new FileInputStream(new File(path));
            byte[] buf = new byte[2];
            is.read(buf);
            String s = new String(buf);
            is.close();
            assertEquals(s, "11");
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            assertTrue(false);
        } catch (IOException e) {
            e.printStackTrace();
            assertTrue(false);
        }
    }
}