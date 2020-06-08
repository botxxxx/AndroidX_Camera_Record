package com.askey.bit;

import android.content.Context;

import static com.askey.bit.Utils.CONFIG_TITLE;
import static com.askey.bit.Utils.configName;
import static com.askey.bit.Utils.defaultProp;
import static com.askey.bit.Utils.defaultRun;
import static com.askey.bit.Utils.logName;

public class Config {

    protected Context context;
    protected String firstCamera = "0";
    protected String secondCamera = "1";
    protected int numberOfRuns = defaultRun;
    protected boolean New = defaultProp;

    public Config(Context context) {
        this.context = context;
    }

    public Config(Context context, String firstCamera, String secondCamera, int isRuns, boolean isNew) {
        this.context = context;
        this.firstCamera = firstCamera;
        this.secondCamera = secondCamera;
        this.numberOfRuns = isRuns;
        this.New = isNew;
    }

    protected String[] config() {
        return new String[]{
                CONFIG_TITLE+ context.getString(R.string.app_name) + "\r\n",
                "#CameraID (0:Outer, 1:Inner, 2:External)\r\n",
                "firstCameraID = " + firstCamera + "\r\n",
                "secondCameraID = " + secondCamera + "\r\n", "\r\n",
                "#Total number of runs (1 record is 1 min)\r\n",
                "numberOfRuns = " + numberOfRuns + "\r\n", "\r\n",
                "#Set Property\r\n",
                "setProperty = " + New + "\r\n", "\r\n",
                "#Video path\r\n",
                "first camera = /sdcard/v(yyMMddHHmmss)f.mp4\r\n",
                "second camera = /sdcard/v(yyMMddHHmmss)s.mp4\r\n", "\r\n",
                "#Recording in SD card for front, back or external camera\r\n",
                "#Recording high quality videos with 1080p / 720p\r\n",
                "#Recording custom frame rates from 13.7 / 27.5\r\n",
                "#Recording custom video Bit Rate 6Mbps\r\n",
                "#" + configName + " will build in Internal shared storage/DCIM/ for initial camera device or record time\r\n",
                "#" + configName + " failed to read column string or not exists will rebuild\r\n",
                "#" + logName + " will build in Internal shared storage/DCIM/ for save application log message\r\n",
                "#" + logName + " not exists will rebuild\r\n",
                "#" + logName + " will copy to SD card when record complete\r\n",
                "#Delete old videos when SD card full\r\n",
                "#At the recording video, please don't copy the SD card file\r\n",
                "#At least 1 Gb memory needs to be available to record\r\n",
                "#Need reboot device to wake up the External camera\r\n",
                "#Inner and External can't be used at the same time\r\n",
                "#Application will reset when external camera error\r\n", "\r\n"
        };
    }
}
