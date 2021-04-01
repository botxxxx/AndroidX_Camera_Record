package com.d160.view;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

public class mHomeListen {
    private final Context mContext;
    private final IntentFilter mHomeBtnIntentFilter;
    private final HomeBtnReceiver mHomeBtnReceiver;
    private boolean isRun = false;
    private OnHomeBtnPressListener mOnHomeBtnPressListener = null;

    public mHomeListen(Context context) {
        mContext = context;
        mHomeBtnReceiver = new HomeBtnReceiver();
        mHomeBtnIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    }

    public void setOnHomeBtnPressListener(OnHomeBtnPressListener onHomeBtnPressListener) {
        mOnHomeBtnPressListener = onHomeBtnPressListener;
    }

    public void start() {
        isRun = true;
        try {
            mContext.registerReceiver(mHomeBtnReceiver, mHomeBtnIntentFilter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        try {
            if (null != mHomeBtnReceiver && isRun)
                mContext.unregisterReceiver(mHomeBtnReceiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
        isRun = false;
    }

    public void receive(Intent intent) {
        String action = intent.getAction();

        if (action.equals(Intent.ACTION_CLOSE_SYSTEM_DIALOGS)) {
            String reason = intent.getStringExtra("reason");
            if (null != reason) {
                if (null != mOnHomeBtnPressListener) {
                    if (reason.equals("homekey")) {
                        // 按Home按键
                        mOnHomeBtnPressListener.onHomeBtnPress();
                    } else if (reason.equals("recentapps")) {
                        // 长按Home按键
                        mOnHomeBtnPressListener.onHomeBtnLongPress();
                    }
                }
            }
        }
    }

    public interface OnHomeBtnPressListener {
        void onHomeBtnPress();

        void onHomeBtnLongPress();
    }

    public class HomeBtnReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            receive(intent);
        }
    }
}