package com.askey.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.SeekBar;

import com.askey.askeylaunchers.MainActivity;

/**
 * Created by jeff_wu on 9/10/19.
 * <p>
 * reference website:   https://blog.csdn.net/wangjinyu501/article/details/20456761
 */

@SuppressLint("AppCompatCustomView")
public class VerticalSeekBar extends SeekBar {
    private final static String TAG = "VerticalSeekBar";

    public VerticalSeekBar(Context context) {
        super(context);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public VerticalSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(h, w, oldh, oldw);
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(heightMeasureSpec, widthMeasureSpec);
        Log.d(MainActivity.TAG, "VerticalSeekBar::onMeasure()");
        setMeasuredDimension(getMeasuredHeight(), getMeasuredWidth());
    }

    protected void onDraw(Canvas c) {
        //将SeekBar转转90度
        c.rotate(-90);

        //将旋转后的视图移动回来
        c.translate(-getHeight(), 0);

        super.onDraw(c);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(MainActivity.TAG, "VerticalSeekBar::onTouchEvent()");

        if (!isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
            case MotionEvent.ACTION_UP:
                int i = 0;

                //获取滑动的距离
                i = getMax() - (int) (getMax() * event.getY() / getHeight());

                //设置进度
                setProgress(i);

                //每次拖动SeekBar都会调用
                onSizeChanged(getWidth(), getHeight(), 0, 0);

                break;
            case MotionEvent.ACTION_CANCEL:
                break;
        }
        return true;
    }
}
