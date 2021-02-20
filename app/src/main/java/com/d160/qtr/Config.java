package com.d160.qtr;

import android.content.Context;

import static com.d160.qtr.CameraActivity.*;
import static com.d160.qtr.Utils.*;

public class Config {

    protected Context context;
    protected String firstCamera = "0";
    protected String secondCamera = "1";
    protected int numberOfRuns = defaultRun;

    public Config(Context context) {
        this.context = context;
    }

    public Config(Context context, String firstCamera, String secondCamera, int isRuns) {
        this.context = context;
        this.firstCamera = firstCamera;
        this.secondCamera = secondCamera;
        this.numberOfRuns = isRuns;
    }

    protected String[] config() {
        return new String[]{
                CONFIG_TITLE+ context.getString(R.string.app_name) + "\r\n",
                "#CameraID (0:Outer, 1:Inner, 2:External)\r\n",
                "firstCameraID = " + firstCamera + "\r\n",
                "secondCameraID = " + secondCamera + "\r\n", "\r\n",
                "#Total number of runs (1 record is 1 min)\r\n",
                "numberOfRuns = " + numberOfRuns + "\r\n"
        };
    }
}
