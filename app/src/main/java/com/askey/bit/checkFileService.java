package com.askey.bit;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.askey.bit.Utils.*;

@SuppressLint("NewApi")
public class checkFileService extends IntentService {
    private String path;

    public checkFileService() {
        super("checkFileService");
    }

    public static int getFrameRate(File file) {
        int frameRate = 0;
        if (!getSDPath().equals("")) {
            try {
                MediaExtractor extractor;
                FileInputStream fis;
                try {
                    extractor = new MediaExtractor();
                    fis = new FileInputStream(file);
                    extractor.setDataSource(fis.getFD());
                } catch (IOException e) {
                    e.printStackTrace();
                    if (null != videoLogList)
                        videoLogList.add(new LogMsg("getFrameRate failed on MediaExtractor.<============ Crash here", mLog.e));
                    return 0;
                }
                int numTracks = extractor.getTrackCount();
                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                    }
                }
                extractor.release();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
                if (null != videoLogList)
                    videoLogList.add(new LogMsg("getFrameRate failed.<============ Crash here", mLog.e));
            }
        } else {
            if (null != videoLogList)
                videoLogList.add(new LogMsg("getFrameRate failed " + NO_SD_CARD + ".", mLog.e));
        }
        return frameRate;
    }

    private void checkFile(String path) {
        try {
            File video = new File(path);
            int frameRate = 0;

            if (video.exists()) {
                try {
                    frameRate = getFrameRate(video);

                } catch (Exception e) {
                    e.printStackTrace();
                    if (null != videoLogList)
                        videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
                }
                if (isNew) {
                    boolean check = false;
                    double[] range = NEW_DFRAME_RATE;
                    if (frameRate >= range[isFrame]) {
                        if (frameRate <= range[isFrame] + 3) {
                            check = true;
                        }
                    } else if (frameRate < range[isFrame]) {
                        if (frameRate >= range[isFrame] - 3) {
                            check = true;
                        }
                    }
                    if (check) Success++;
                    else Fail++;
                } else Success++;
            } else {
                Fail++;
            }
            if (null != videoLogList)
                videoLogList.add(new LogMsg("CheckFile:(" + path.split("/")[3] +
                        ") video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        ") wifi_success/fail:(" + getWifiSuccess() + "/" + getWifiFail() +
                        ") bt_success/fail:(" + getBtSuccess() + "/" + getBtFail() +
                        ") app_reset:(" + getReset() + ")", mLog.i));
        } catch (Exception e) {
            e.printStackTrace();
            if (null != videoLogList)
                videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
            Fail++;
        }
    }

    private void saveLog() {
        Intent intent = new Intent();
        intent.setClassName(this.getPackageName(), saveLogService.class.getName());
        startService(intent);
    }

    protected void onHandleIntent(Intent intent) {
        try {
            path = intent.getStringExtra(EXTRA_VIDEO_PATH);

            Thread t = new Thread(() -> {

                try {
                    checkFile(path);
                    saveLog();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            t.start();
            t.join();
        } catch (Exception e) {
            e.printStackTrace();
            if (null != videoLogList)
                videoLogList.add(new LogMsg("CheckFileService error.", mLog.e));
        } finally {
            saveLog();
        }
    }
}