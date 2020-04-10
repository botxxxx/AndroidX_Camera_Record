package com.askey.record;


import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.StatFs;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static com.askey.record.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.record.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.record.Utils.NO_SD_CARD;
import static com.askey.record.Utils.errorMessage;
import static com.askey.record.Utils.firstFile;
import static com.askey.record.Utils.getSDPath;
import static com.askey.record.Utils.getSdCard;
import static com.askey.record.Utils.isError;
import static com.askey.record.Utils.sdData;
import static com.askey.record.Utils.secondFile;
import static com.askey.record.Utils.videoLogList;

public class checkSdCardService extends IntentService {

    public checkSdCardService() {
        // ActivityのstartService(intent);で呼び出されるコンストラクタはこちら
        super("checkSdCardService");
    }

    private void saveLog(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), saveLogService.class.getName());
        intent.putExtra(EXTRA_VIDEO_VERSION, context.getString(R.string.app_name));
        intent.putExtra(EXTRA_VIDEO_REFORMAT, false);
        context.startService(intent);
    }

    private void checkSdCardFromFileList() {
        getSdCard = !getSDPath().equals("");
        if (getSdCard) {
            try {
                StatFs stat = new StatFs(getSDPath());
                long sdAvailSize = stat.getAvailableBlocksLong()
                        * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                if (gigaAvailable < sdData) {
                    if (null != videoLogList) {
                        videoLogList.add(new LogMsg("SD Card is Full."));
                    }
                    ArrayList<String> tmp = new ArrayList();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (int i = 0; i < fileList.length; i++) {
                        // Recursive call if it's a directory
                        File file = fileList[i];
                        if (!fileList[i].isDirectory()) {
                            if (Utils.getFileExtension(file.toString()).equals("mp4"))
                                if (!file.toString().equals(firstFile) || !file.toString().equals(secondFile))
                                    tmp.add(file.toString());
                        }
                    }
                    if (tmp.size() >= 2) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        delete((String) list[0], true);
                        delete((String) list[1], true);
                        checkSdCardFromFileList();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                getSdCard = !getSDPath().equals("");
                if (getSdCard) {
                    if (null != videoLogList) {
                        videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
                        saveLog(this);
                    }
                    errorMessage = "error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.";
//                ((TextView) findViewById(R.id.record_status)).setText("Error");
                } else {
                    if (null != videoLogList) {
                        videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
                        saveLog(this);
                    }
                    errorMessage = NO_SD_CARD;
                }
            }
        } else {
            isError = true;
            if (null != videoLogList) {
                videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
                saveLog(this);
            }
            errorMessage = NO_SD_CARD;
        }
    }

    private void delete(String path, boolean fromSDcard) {
        try {
            if (path != "") {
                File video = new File(path);
                if (video.exists()) {
                    if (null != videoLogList)
                        if (fromSDcard)
                            videoLogList.add(new LogMsg("Delete: " + path.split("/")[3], mLog.w));
                        else
                            videoLogList.add(new LogMsg("Delete: " + path.split("/")[5], mLog.w));
                    video.delete();
                } else {
                    if (null != videoLogList)
                        videoLogList.add(new LogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            getSdCard = !getSDPath().equals("");
            if (null != videoLogList) {
                videoLogList.add(new LogMsg("#delete " + path + " error. <============ Crash here", mLog.e));
                saveLog(this);
            }
            errorMessage = "Delete file error. <============ Crash here";
//            ((TextView) findViewById(R.id.record_status)).setText("Error");
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
                    if (null != videoLogList) {
                        videoLogList.add(new LogMsg("checkSdCardFromFileList error.", mLog.e));
                        saveLog(this);
                    }
                }
            });
            t.start();
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
            if (null != videoLogList) {
                videoLogList.add(new LogMsg("checkSdCardService error.", mLog.e));
                saveLog(this);
            }
        }
    }
}

