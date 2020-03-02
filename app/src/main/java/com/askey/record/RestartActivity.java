package com.askey.record;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class RestartActivity extends Activity {
    public static final String EXTRA_MAIN_PID = "RestartActivity.main_pid";
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), RestartActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // メインプロセスの PID を Intent に保存しておく
        intent.putExtra(EXTRA_MAIN_PID, android.os.Process.myPid());
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 1. メインプロセスを Kill する
        Intent intent = getIntent();
        int mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1);
        int EXTRA_RUN = intent.getIntExtra(EXTRA_VIDEO_RUN, 0);
        int EXTRA_RESET = intent.getIntExtra(EXTRA_VIDEO_RESET, 0);
        boolean EXTRA_RECORD = intent.getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        android.os.Process.killProcess(mainPid);
        // 2. MainActivity を再起動する
        Context context = getApplicationContext();
        Intent restartIntent = new Intent(Intent.ACTION_MAIN);
        restartIntent.setClassName(context.getPackageName(), VideoRecordActivity.class.getName());
        restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        restartIntent.putExtra(EXTRA_VIDEO_RUN, EXTRA_RUN);
        restartIntent.putExtra(EXTRA_VIDEO_RESET, EXTRA_RESET);
        restartIntent.putExtra(EXTRA_VIDEO_RECORD, EXTRA_RECORD);
        context.startActivity(restartIntent);
        // 3. RestartActivity を終了する
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}