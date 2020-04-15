package com.askey.widget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * Home按键监听类 使用说明： 1、初始化HomeListen HomeListen homeListen = new HomeListen( this
 * ); homeListen.setOnHomeBtnPressListener( new setOnHomeBtnPressListener(){
 *
 * @Override public void onHomeBtnPress( ){ // 按下Home按键回调 }
 * @Override public void onHomeBtnLongPress( ){ // 长按Home按键回调 } });
 * <p>
 * 2、在onResume方法中启动HomeListen广播： homeListen.start();
 * <p>
 * 3、在onPause方法中停止HomeListen广播： homeListen.stop( );
 */

public class HomeListen {
    private Context mContext = null;
    private IntentFilter mHomeBtnIntentFilter = null;
    private OnHomeBtnPressLitener mOnHomeBtnPressListener = null;
    private HomeBtnReceiver mHomeBtnReceiver = null;

    public HomeListen(Context context) {
        mContext = context;
        mHomeBtnReceiver = new HomeBtnReceiver();
        mHomeBtnIntentFilter = new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
    }

    public void setOnHomeBtnPressListener(OnHomeBtnPressLitener onHomeBtnPressListener) {
        mOnHomeBtnPressListener = onHomeBtnPressListener;
    }

    public void start() {
        try{
            mContext.registerReceiver(mHomeBtnReceiver, mHomeBtnIntentFilter);
        }catch (Exception e){

        }
    }

    public void stop() {
        try{
            mContext.unregisterReceiver(mHomeBtnReceiver);
        }catch (Exception e){

        }
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

    public interface OnHomeBtnPressLitener {
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