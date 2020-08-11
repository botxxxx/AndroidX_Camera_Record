package com.askey.bit;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.StatFs;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static com.askey.bit.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.bit.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.bit.Utils.NO_SD_CARD;
import static com.askey.bit.Utils.errorMessage;
import static com.askey.bit.Utils.firstFile;
import static com.askey.bit.Utils.getSDPath;
import static com.askey.bit.Utils.getSdCard;
import static com.askey.bit.Utils.isError;
import static com.askey.bit.Utils.sdData;
import static com.askey.bit.Utils.secondFile;
import static com.askey.bit.Utils.videoLogList;
import static com.askey.bit.VideoRecordActivity.SD_Mode;

public class checkSdCardService extends IntentService {

    public checkSdCardService() {
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
                long sdAvailSize = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                if (gigaAvailable < sdData) {
                    if (null != videoLogList) {
                        videoLogList.add(new LogMsg("SD Card is Full."));
                    }
                    ArrayList<String> tmp = new ArrayList<>();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (File file : fileList) {
                        if (!file.isDirectory()&& Utils.getFileExtension(file.toString()).equals("mp4"))
                                if (!(file.toString().equals(firstFile) || file.toString().equals(secondFile)))
                                    tmp.add(file.toString());
                    }
                    if (tmp.size() >= 2) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        delete((String) (list != null ? list[0] : null), SD_Mode);
                        delete((String) (list != null ? list[1] : null), SD_Mode);
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
            if (!path.equals("")) {
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
        }
    }

    protected void onHandleIntent(Intent intent) {
            new Thread(() -> {
                try {
                    checkSdCardFromFileList();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
    }
}

