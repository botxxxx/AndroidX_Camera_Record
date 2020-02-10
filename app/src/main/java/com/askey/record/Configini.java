package com.askey.record;

public class Configini {

    protected String firstCamera = "0";
    protected String secondCamera = "1";
    protected int isFinish = 1;

    public Configini(){

    }

    public Configini(String firstCamera, String secondCamera , int isFinish){
        this.firstCamera = firstCamera;
        this.secondCamera = secondCamera;
        this.isFinish = isFinish;
    }

    protected String[] config() {
        return new String[]{
                "[VIDEO_RECORD_TESTING]\r\n",
                "#CameraID(0:BACK, 1:FRONT, 2:EXTERNAL)\r\n",
                "firstCameraID = "+firstCamera+"\r\n",
                "secondCameraID = "+secondCamera+"\r\n", "\r\n",
                "#Camera Device total minute: one day has minutes(*10) = 144\r\n",
                "total_test_minute = "+isFinish+"\r\n", "\r\n",
                "#Play video path(can't change video path)\r\n",
                "video1_path = /sdcard/(ddhhmmss)f.mp4\r\n",
                "video2_path = /sdcard/(ddhhmmss)b.mp4\r\n", "\r\n",
                "#Start application\r\n",
                "adb shell am start -n com.askey.record/.VideoRecordActivity\r\n", "\r\n",
                "#Start test record(no audio with 5s)\r\n",
                "adb shell am broadcast -a com.askey.record.t\r\n", "\r\n",
                "#Start/Stop record(default is 10 min)\r\n",
                "adb shell am broadcast -a com.askey.record.s\r\n", "\r\n",
                "#Finish applcation\r\n",
                "adb shell am broadcast -a com.askey.record.f\r\n", "\r\n",
                "#At least 3.5Gb memory needs to be available to record, \r\n",
                "#Please check the SD card.\r\n",
//            "#Frame rate switch will delay 3s to restart the camera device. \r\n", "\r\n"
        };
    }
}
