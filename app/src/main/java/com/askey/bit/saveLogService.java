package com.askey.bit;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.askey.bit.Utils.EXTRA_VIDEO_COPY;
import static com.askey.bit.Utils.EXTRA_VIDEO_PASTE;
import static com.askey.bit.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.bit.Utils.EXTRA_VIDEO_REMOVE;
import static com.askey.bit.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.bit.Utils.LOG_TITLE;
import static com.askey.bit.Utils.getPath;
import static com.askey.bit.Utils.getSDPath;
import static com.askey.bit.Utils.logName;
import static com.askey.bit.Utils.videoLogList;
import static com.askey.bit.VideoRecordActivity.SD_Mode;
import static com.askey.bit.restartActivity.EXTRA_MAIN_PID;

public class saveLogService extends IntentService {
    private String version;
    private boolean reFormat;

    public saveLogService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("saveLogService");
    }

    private void saveLog(ArrayList<LogMsg> mLogList, boolean reFormat, boolean move) {
        String logString;
        assert mLogList!=null;
        File file = new File(getPath(), logName);
        if (!file.exists()) {
            logString = LOG_TITLE + version + "\r\n";
            try {
                file.createNewFile();
                mLogList.add(new LogMsg("Create the log file.", mLog.w));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            logString = "";
        }

        for (LogMsg logs : mLogList) {
            String time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + " run:" + logs.runTime + " -> ";
            logString += (time + logs.msg + "\r\n");
        }
        try {
            FileOutputStream output = new FileOutputStream(new File(getPath(), logName), !reFormat);
            output.write(logString.getBytes());
            output.close();
            mLogList.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
//        if (SD_Mode) {
//            if (move)
//                try {
//                    Thread tMove = new Thread(() -> {
//                        moveFile(getPath() + logName, getSDPath() + logName, false);
//                    });
//                    tMove.start();
//                    tMove.join();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//        }
    }

    private void moveFile(String video, String pathname, boolean remove) {
        Context context = getApplicationContext();
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), copyFileService.class.getName());
        intent.putExtra(EXTRA_VIDEO_COPY, video);
        intent.putExtra(EXTRA_VIDEO_PASTE, pathname);
        intent.putExtra(EXTRA_VIDEO_REMOVE, remove);
        context.startService(intent);
    }


    protected void onHandleIntent(Intent intent) {
        int mainPid = 0;
        try {
            mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1);
            version = intent.getStringExtra(EXTRA_VIDEO_VERSION);
            reFormat = intent.getBooleanExtra(EXTRA_VIDEO_REFORMAT, false);

            int finalMainPid = mainPid;
            Thread t = new Thread(() -> {
                final boolean move = finalMainPid > 0;
                try {
                    saveLog(videoLogList, reFormat, move);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
            if (null != videoLogList)
                videoLogList.add(new LogMsg("saveLog Service error.", mLog.e));
        } finally {
            if (mainPid > 0) android.os.Process.killProcess(mainPid);
        }
    }
}

