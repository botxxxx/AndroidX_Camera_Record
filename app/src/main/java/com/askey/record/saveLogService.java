package com.askey.record;


import android.app.IntentService;
import android.content.Intent;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.askey.record.RestartActivity.EXTRA_MAIN_PID;
import static com.askey.record.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.record.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.record.Utils.NO_SD_CARD;
import static com.askey.record.Utils.getPath;
import static com.askey.record.Utils.logName;
import static com.askey.record.Utils.videoLogList;

public class saveLogService extends IntentService {
    private String version;
    private boolean reFormat;

    public saveLogService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("saveLogService");
    }

    private void saveLog(ArrayList<LogMsg> mLogList, boolean reFormat) {
        String logString;

        File file = new File(getPath(), logName);
        if (!file.exists()) {
            logString = "[VIDEO_RECORD_LOG]" + version + "\r\n";
            try {
                file.createNewFile();
                mLogList.add(new LogMsg("Create the log file.", mLog.w));
            } catch (Exception e) {
                e.printStackTrace();
                mLogList.add(new LogMsg("Create the log file error. <============ Error here", mLog.e));
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
            mLogList.add(new LogMsg("Write failed. " + NO_SD_CARD + ". <============ Error here", mLog.e));
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            int mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1);
            version = intent.getStringExtra(EXTRA_VIDEO_VERSION);
            reFormat = intent.getBooleanExtra(EXTRA_VIDEO_REFORMAT, false);
            Thread t = new Thread(() -> {
                try {
                    saveLog(videoLogList, reFormat);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new LogMsg("saveLog error.", mLog.e));
                }
            });
            t.start();
            t.join();
            if (mainPid > 0) android.os.Process.killProcess(mainPid);
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new LogMsg("saveLog Service error.", mLog.e));
        }
    }
}

