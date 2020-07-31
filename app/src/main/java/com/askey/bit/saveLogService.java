package com.askey.bit;

import android.app.IntentService;
import android.content.Intent;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static com.askey.bit.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.bit.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.bit.Utils.LOG_TITLE;
import static com.askey.bit.Utils.getPath;
import static com.askey.bit.Utils.logName;
import static com.askey.bit.Utils.videoLogList;
import static com.askey.bit.restartActivity.EXTRA_MAIN_PID;

public class saveLogService extends IntentService {
    public saveLogService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("saveLogService");
    }

    private void saveLog(ArrayList<LogMsg> mLogList, boolean reFormat, String version) {
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
    }

    protected void onHandleIntent(Intent intent) {
        int mainPid = 0;
        try {
            mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1);
            new Thread(() -> {
                try {
                    saveLog(videoLogList, intent.getBooleanExtra(EXTRA_VIDEO_REFORMAT, false),
                            intent.getStringExtra(EXTRA_VIDEO_VERSION));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            if (null != videoLogList)
                videoLogList.add(new LogMsg("saveLog Service error.", mLog.e));
        } finally {
            if (mainPid > 0) android.os.Process.killProcess(mainPid);
        }
    }
}

