package com.d160.view;

import com.d160.bit.Utils;

import java.util.*;

public class mLogMsg {
    public int runTime;
    public Date time;
    public String msg;
    public mLog type;

    public mLogMsg(String msg) {
        this.runTime = Utils.getIsRun();
        this.time = Calendar.getInstance().getTime();
        this.msg = msg;
        this.type = mLog.d;
    }

    public mLogMsg(String msg, mLog type) {
        this.runTime = Utils.getIsRun();
        this.time = Calendar.getInstance().getTime();
        this.msg = msg;
        this.type = type;
    }
}
