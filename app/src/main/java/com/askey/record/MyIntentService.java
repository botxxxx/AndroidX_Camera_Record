package com.askey.record;


import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static com.askey.record.Utils.EXTRA_VIDEO_COPY;
import static com.askey.record.Utils.EXTRA_VIDEO_PASTE;
import static com.askey.record.Utils.getSdCard;
import static com.askey.record.Utils.isError;
import static com.askey.record.Utils.videoLogList;

public class MyIntentService extends IntentService {
    String video;
    String pathname;

    public MyIntentService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("MyIntentService");
    }

    public static void copy(String src, String dst) {
        try {
            File sf = new File(src);
            InputStream in = new FileInputStream(sf);
            try {
                OutputStream out = new FileOutputStream(dst);
                try {
                    // Transfer bytes from in to out
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
                sf.delete();
            }
        } catch (IOException e) {
            videoLogList.add(new LogMsg("Copy file error", mLog.e));
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        video = intent.getStringExtra(EXTRA_VIDEO_COPY);
        pathname = intent.getStringExtra(EXTRA_VIDEO_PASTE);
        // 非同期処理を行うメソッド。タスクはonHandleIntentメソッド内で実行する
        videoLogList.add(new LogMsg("#copy.", mLog.e));
        try {
            Log.d("IntentService", "onHandleIntent Start");
            Thread t = new Thread(() -> copy(video, pathname));
            t.start();
            t.join();
        } catch (Exception e) {
            videoLogList.add(new LogMsg("copy failed.", mLog.e));
            isError = true;
            getSdCard = false;
        } finally {
            videoLogList.add(new LogMsg("copy successful.", mLog.e));
        }
    }
}

