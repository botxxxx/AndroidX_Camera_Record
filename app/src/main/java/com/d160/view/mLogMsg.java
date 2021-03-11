package com.d160.view;

import com.d160.wa034.Utils;

import java.util.*;

public class mLogMsg {
    public int run;
    public Date date;
    public String msg;
    public mLog type;

    public mLogMsg(String msg) {
        this.run = Utils.getIsRun();
        this.date = Calendar.getInstance().getTime();
        this.msg = msg;
        this.type = mLog.d;
    }

    public mLogMsg(String msg, mLog type) {
        this.run = Utils.getIsRun();
        this.date = Calendar.getInstance().getTime();
        this.msg = msg;
        this.type = type;
    }
}
