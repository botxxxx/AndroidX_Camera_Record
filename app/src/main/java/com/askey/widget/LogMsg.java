package com.askey.widget;

import com.askey.bit.Utils;

import java.time.LocalDateTime;

public class LogMsg {
    public int runTime;
    public LocalDateTime time;
    public String msg;
    public mLog type;

    public LogMsg(String msg) {
        this.runTime = Utils.getIsRun();
        this.time = LocalDateTime.now();
        this.msg = msg;
        this.type = mLog.d;
    }

    public LogMsg(String msg, mLog type) {
        this.runTime = Utils.getIsRun();
        this.time = LocalDateTime.now();
        this.msg = msg;
        this.type = type;
    }
}
