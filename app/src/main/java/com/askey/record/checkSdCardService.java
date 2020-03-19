package com.askey.record;


import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;
import android.os.StatFs;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static com.askey.record.Utils.DFRAME_RATE;
import static com.askey.record.Utils.NEW_DFRAME_RATE;
import static com.askey.record.Utils.NO_SD_CARD;
import static com.askey.record.Utils.errorMessage;
import static com.askey.record.Utils.failed;
import static com.askey.record.Utils.getFailed;
import static com.askey.record.Utils.getFrameRate;
import static com.askey.record.Utils.getReset;
import static com.askey.record.Utils.getSDPath;
import static com.askey.record.Utils.getSdCard;
import static com.askey.record.Utils.getSuccessful;
import static com.askey.record.Utils.isError;
import static com.askey.record.Utils.isFrame;
import static com.askey.record.Utils.isNew;
import static com.askey.record.Utils.sdData;
import static com.askey.record.Utils.successful;
import static com.askey.record.Utils.videoLogList;

public class checkSdCardService extends IntentService {

    public checkSdCardService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("checkSdCardService");
    }

    public static void checkSdCardFromFileList() {
        getSdCard = !getSDPath().equals("");
        if (getSdCard) {
            try {
                StatFs stat = new StatFs(getSDPath());
                long sdAvailSize = stat.getAvailableBlocksLong()
                        * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                if (gigaAvailable < sdData) {
                    videoLogList.add(new LogMsg("SD Card is Full."));
                    ArrayList<String> tmp = new ArrayList();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (int i = 0; i < fileList.length; i++) {
                        // Recursive call if it's a directory
                        File file = fileList[i];
                        if (!fileList[i].isDirectory()) {
                            if (Utils.getFileExtension(file.toString()).equals("mp4"))
                                tmp.add(file.toString());
                        }
                    }
                    if (tmp.size() >= 2) {
//                    runOnUiThread(() -> setAdapter(getApplicationContext()));
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        delete((String) list[0], false, true);
                        delete((String) list[1], false, true);
                        new Handler().post(() -> checkSdCardFromFileList());
                    } else {
                        videoLogList.add(new LogMsg("checkSdCardFromFileList error." + NO_SD_CARD, mLog.e));
                        isError = true;
                        errorMessage = "checkSdCardFromFileList error." + NO_SD_CARD;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
                isError = true;
                getSdCard = !getSDPath().equals("");
                errorMessage = "error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.";
            }
        }
    }

    public static void delete(String path, boolean check, boolean fromSDcard) {
        try {
            if (path != "") {
                File video = new File(path);
                if (video.exists()) {
                    if (check) {
                        fileCheck(path);
                    }
                    if (fromSDcard)
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[3], mLog.w));
                    else
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[5], mLog.w));
//                    runOnUiThread(() -> setAdapter(getApplicationContext()));
                    video.delete();
                } else {
                    videoLogList.add(new LogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new LogMsg("#delete " + path + " error. <============ Crash here", mLog.e));
//            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
            isError = true;
            errorMessage = "Delete error. <============ Crash here";
//            ((TextView) findViewById(R.id.record_status)).setText("Error");
        }
    }

    @SuppressLint("DefaultLocale")
    public static void fileCheck(String path) {
        try {
            File video = new File(path);
            int framerate = 0;
//            , duration = 0;
//            String convertMinutes = "00", convertSeconds = "00";
            if (video.exists()) {
                try {
                    framerate = getFrameRate(path);
//                    duration = getVideo(this, path);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new LogMsg("CheckFile error. <============ Crash here", mLog.e));
//                    new Handler().post(() -> saveLog(getApplicationContext(), false, false));
                    isError = true;
                    errorMessage = "CheckFile error. <============ Crash here";
//                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                }
                double[] range = isNew ? NEW_DFRAME_RATE : DFRAME_RATE;
                boolean check = false;
                if (framerate >= range[isFrame]) {
                    if (framerate <= range[isFrame] + 3) {
                        check = true;
                    }
                } else if (framerate < range[isFrame]) {
                    if (framerate >= range[isFrame] - 3) {
                        check = true;
                    }
                }
                if (check)
                    successful++;
                else
                    failed++;
            } else {
                failed++;
            }
            videoLogList.add(new LogMsg("CheckFile: " + path.split("/")[3] + " frameRate:" + framerate +
//                    " duration:" + convertMinutes + ":" + convertSeconds +
                    " success:" + getSuccessful() + " fail:" + getFailed() + " reset:" + getReset(), mLog.i));
//            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new LogMsg("CheckFile error. <============ Crash here", mLog.e));
//            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
            errorMessage = "CheckFile error. <============ Crash here";
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        try {
            Thread t = new Thread(() -> {
                try {
                    checkSdCardFromFileList();
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new LogMsg("checkSdCardFromFileList error.", mLog.e));
                }
            });
            t.start();
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new LogMsg("checkSdCardFromFileList error.", mLog.e));
        }
    }
}

