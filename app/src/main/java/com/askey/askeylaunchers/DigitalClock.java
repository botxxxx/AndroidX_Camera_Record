package com.askey.askeylaunchers;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.TextClock;

import java.util.Calendar;

public class DigitalClock extends TextClock {

    private static final String TAG = "DigitalClock";
    private Handler timerHandler;

    public DigitalClock(Context context) {
        super(context);
    }

    public DigitalClock(Context context, AttributeSet attrs) {
        super(context, attrs);

        setCustomFont(context, attrs);
    }

    public DigitalClock(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setCustomFont(context, attrs);
    }

    public DigitalClock(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        setCustomFont(context, attrs);
    }

    private void setCustomFont(Context ctx, AttributeSet attrs) {
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
//        int id = a.getResourceId(0 /* index of attribute in attrsArray */, View.NO_ID);
        final String customFont = a.getString(R.styleable.CustomTextView_customFont);
        final String customID = a.getString(R.styleable.CustomTextView_customID);
        if (customID.equals("DigitalClockHour") || customID.equals("DigitalClockMins") ||
                customID.equals("DigitalClock12_24HourFormat")) {
            timerHandler = new Handler();
            timerHandler.post(new Runnable() {
                public void run() {
                    setSpace(customID);
                    timerHandler.postDelayed(this, 500);
                }
            });
        }

        setCustomFont(ctx, customFont);
        a.recycle();
    }

    private boolean setCustomFont(Context ctx, String asset) {
        Typeface typeface;
        try {
            typeface = Typeface.createFromAsset(ctx.getAssets(), "fonts/" + asset);
        } catch (Exception e) {
            Log.e(TAG, "Unable to load typeface: " + e.getMessage());
            return false;
        }
        setTypeface(typeface);
        return true;
    }

    private void setSpace(String id) {
        boolean is12Hour = false;
        if (!DateFormat.is24HourFormat(getContext()))
            is12Hour = true;

        if (id.equals("DigitalClock12_24HourFormat")) {
            if (!is12Hour) {
                setVisibility(View.GONE);
            } else {
                setVisibility(View.VISIBLE);
            }
        } else {
            float sp = 0.03f; // Default Spacing
            String H, M, num;
            Calendar c = Calendar.getInstance();

            if (id.equals("DigitalClockHour")) {
                // HOUR_OF_DAY(24H) & HOUR(12H)
                if (is12Hour) H = c.get(Calendar.HOUR) + "";
                else H = c.get(Calendar.HOUR_OF_DAY) + "";
                if (is12Hour && H.equals("0")) {
                    H = "12";
                }
                num = H;
            } else {
                M = c.get(Calendar.MINUTE) + "";
                num = M;
            }

            if (num.length() == 1) {
                num = "0" + num;
            }
            sp += check(true, num.substring(0, 1));
            sp += check(false, num.substring(1, 2));
            if (!is12Hour) {
                sp += 0.015f;
            }
            setLetterSpacing(sp);
        }
    }

    private Float check(boolean ten, String s) {
        float sp = 0f;
        if (s.equals("1")) {
            sp += 0.05f;
            if (ten)
                sp += 0.073f;
        }
        if (s.equals("7")) {
            sp += 0.03f;
        }
        return sp;
    }
}
