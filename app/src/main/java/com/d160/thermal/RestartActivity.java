package com.d160.thermal;

import android.app.*;
import android.content.*;
import android.os.*;

public class RestartActivity extends Activity {
    public static final String EXTRA_MAIN_PID = "RestartActivity.main_pid";
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";

    public static Intent createIntent(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), RestartActivity.class.getName());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(EXTRA_MAIN_PID, android.os.Process.myPid());
        return intent;
    }


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        int mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1);
        int EXTRA_RUN = intent.getIntExtra(EXTRA_VIDEO_RUN, 0);
        int EXTRA_RESET = intent.getIntExtra(EXTRA_VIDEO_RESET, 0);
        int EXTRA_FAIL = getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        int EXTRA_SUCCESS = getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);

        boolean EXTRA_RECORD = intent.getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        android.os.Process.killProcess(mainPid);
        // 2. MainActivity を再起動する
        Context context = getApplicationContext();
        Intent restartIntent = new Intent(Intent.ACTION_MAIN);
        restartIntent.setClassName(context.getPackageName(), CameraActivity.class.getName());
        restartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        restartIntent.putExtra(EXTRA_VIDEO_RUN, EXTRA_RUN);
        restartIntent.putExtra(EXTRA_VIDEO_RESET, EXTRA_RESET);
        restartIntent.putExtra(EXTRA_VIDEO_FAIL, EXTRA_FAIL);
        restartIntent.putExtra(EXTRA_VIDEO_SUCCESS, EXTRA_SUCCESS);

        restartIntent.putExtra(EXTRA_VIDEO_RECORD, EXTRA_RECORD);
        context.startActivity(restartIntent);
        finish();
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}