package com.d160.wa034;

import android.content.Context;

import static com.d160.wa034.CameraFragment.*;
import static com.d160.wa034.Utils.*;

public class Config {

    protected Context context;
    protected int numberOfRuns = defaultRun;

    public Config(Context context) {
        this.context = context;
    }

    public Config(Context context, int isRuns) {
        this.context = context;
        this.numberOfRuns = isRuns;
    }

    protected String[] config() {
        return new String[]{
                CONFIG_TITLE+ context.getString(R.string.app_name) + "\r\n",
                "#Total number of runs (1 record is 1 min)\r\n",
                "numberOfRuns = " + numberOfRuns + "\r\n"
        };
    }
}
