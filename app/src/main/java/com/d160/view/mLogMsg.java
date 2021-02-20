package com.d160.view;

import com.d160.qtr.Utils;

import java.time.LocalDateTime;

public class mLogMsg {
    public int runTime;
    public LocalDateTime time;
    public String msg;
    public mLog type;

    public mLogMsg(String msg) {
        this.runTime = Utils.getIsRun();
        this.time = LocalDateTime.now();
        this.msg = msg;
        this.type = mLog.d;
    }

    public mLogMsg(String msg, mLog type) {
        this.runTime = Utils.getIsRun();
        this.time = LocalDateTime.now();
        this.msg = msg;
        this.type = type;
    }
}
