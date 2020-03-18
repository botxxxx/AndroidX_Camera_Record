package com.askey.record;

import android.content.Context;

public class Configini {

    protected Context context;
    protected String firstCamera = "0";
    protected String secondCamera = "1";
    protected int numberOfRuns = 999;
    protected boolean isNew = true;

    public Configini(Context context) {
        this.context = context;
    }

    public Configini(Context context, String firstCamera, String secondCamera, int isRuns, boolean isNew) {
        this.context = context;
        this.firstCamera = firstCamera;
        this.secondCamera = secondCamera;
        this.numberOfRuns = isRuns;
        this.isNew = isNew;
    }

    protected String[] config() {
        return new String[]{
                "[VIDEO_RECORD_CONFIG]" + context.getString(R.string.app_name) + "\r\n",
                "#CameraID (0:Outer, 1:Inner, 2:External)\r\n",
                "firstCameraID = " + firstCamera + "\r\n",
                "secondCameraID = " + secondCamera + "\r\n", "\r\n",
                "#Total number of runs (1 record is 1 min)\r\n",
                "numberOfRuns = " + numberOfRuns + "\r\n", "\r\n",
                "#Set Property\r\n",
                "setProperty = " + isNew + "\r\n", "\r\n",
                "#Video path\r\n",
                "video1_path = /sdcard/(ddhhmmss)f.mp4\r\n",
                "video2_path = /sdcard/(ddhhmmss)s.mp4\r\n", "\r\n",
                "#Start\r\n",
                "adb shell am start -n com.askey.record/.VideoRecordActivity\r\n", "\r\n",
                "#Start test record(no audio, 5s)\r\n",
                "adb shell am broadcast -a com.askey.record.t\r\n", "\r\n",
                "#Start/Stop record\r\n",
                "adb shell am broadcast -a com.askey.record.s\r\n", "\r\n",
                "#Finish\r\n",
                "adb shell am broadcast -a com.askey.record.f\r\n", "\r\n",
                "#At the recording video, please don't copy the sd card file.\r\n",
                "#At least 3.5Gb memory needs to be available to record.\r\n",
                "#Sometimes need reboot device to wake up the External camera.\r\n",
                "#Inner and External can't be used at the same time.\r\n",
                "\r\n"
        };
    }
}
