package com.example.imservice;

import android.app.Application;
import com.example.imservice.model.ContactDB;
import com.google.code.p.leveldb.LevelDB;

import java.io.File;

/**
 * Created by houxh on 14-8-24.
 */
public class IMApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        PeerMessageDB db = PeerMessageDB.getInstance();
        db.setDir(this.getDir("peer", MODE_PRIVATE));

        ContactDB cdb = ContactDB.getInstance();
        cdb.setContentResolver(getApplicationContext().getContentResolver());

        LevelDB ldb = LevelDB.getDefaultDB();
        String dir = getFilesDir().getAbsoluteFile() + File.separator + "db";
        ldb.open(dir);
    }

}
