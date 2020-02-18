package com.askey.record;

import android.content.Context;

public class Configini {

    protected Context context;
    protected String firstCamera = "0";
    protected String secondCamera = "1";
    protected int isFinish = 999;
    protected boolean isNew = false;

    public Configini(Context context) {
        this.context = context;
    }

    public Configini(Context context, String firstCamera, String secondCamera, int isFinish, boolean isNew) {
        this.context = context;
        this.firstCamera = firstCamera;
        this.secondCamera = secondCamera;
        this.isFinish = isFinish;
        this.isNew = isNew;
    }

    protected String[] config() {
        return new String[]{
                "[VIDEO_RECORD_TESTING]" + context.getString(R.string.app_name) + "\r\n",
                "#CameraID(0:BACK, 1:FRONT, 2:EXTERNAL)\r\n",
                "firstCameraID = " + firstCamera + "\r\n",
                "secondCameraID = " + secondCamera + "\r\n", "\r\n",
                "#Total number of runs: 144 = 24h\r\n",
                "numberOfRuns = " + isFinish + "\r\n", "\r\n",
                "#Set Property\r\n",
                "setProperty = " + isNew + "\r\n", "\r\n",
                "#Play video path\r\n",
                "video1_path = /sdcard/(ddhhmmss)f.mp4\r\n",
                "video2_path = /sdcard/(ddhhmmss)b.mp4\r\n", "\r\n",
                "#Start\r\n",
                "adb shell am start -n com.askey.record/.VideoRecordActivity\r\n", "\r\n",
                "#Start test record(no audio, 5s)\r\n",
                "adb shell am broadcast -a com.askey.record.t\r\n", "\r\n",
                "#Start/Stop record(10 min)\r\n",
                "adb shell am broadcast -a com.askey.record.s\r\n", "\r\n",
                "#Finish\r\n",
                "adb shell am broadcast -a com.askey.record.f\r\n", "\r\n",
                "#At least 3.5Gb memory needs to be available to record, \r\n",
                "#Please check the SD card.\r\n",
                "#Frame rate switch will delay 3s to restart the camera device. \r\n", "\r\n"
        };
    }
}
