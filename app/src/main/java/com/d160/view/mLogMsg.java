package com.d160.view;

import com.d160.thermal.Utils;

import java.time.LocalDateTime;

public class mLogMsg {
    public int run;
    public LocalDateTime date; //Added in API level 26
    public String msg;
    public mLog type;

    public mLogMsg(String msg) {
        this.run = Utils.getIsRun();
        this.date = LocalDateTime.now();
        this.msg = msg;
        this.type = mLog.d;
    }

    public mLogMsg(String msg, mLog type) {
        this.run = Utils.getIsRun();
        this.date = LocalDateTime.now();
        this.msg = msg;
        this.type = type;
    }
}
