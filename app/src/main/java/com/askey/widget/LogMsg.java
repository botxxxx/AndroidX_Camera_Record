package com.askey.widget;

import com.askey.record.Utils;

import java.time.LocalDateTime;

public class LogMsg {
    public int runTime;
    public LocalDateTime time;
    public String msg;
    public mLog type;

    public LogMsg(String msg, mLog type) {
        this.runTime = Utils.getIsRun();
        this.time = LocalDateTime.now();
        this.msg = msg;
        this.type = type;
    }
}
