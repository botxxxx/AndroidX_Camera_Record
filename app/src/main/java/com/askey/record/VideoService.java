package com.askey.record;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class VideoService extends Service {

    public static final String TAG = "MyService";

    @Override
    public void onCreate() {
        super.onCreate();
        new Handler().postDelayed(() -> {
            Intent i = new Intent();
            i.setClass(this, VideoRecordActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
            startActivity(i);
        }, 5000);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand() executed");
        // 執行任務
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() executed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
