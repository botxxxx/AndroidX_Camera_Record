package com.d160.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.widget.TextView;

import com.d160.bit.R;

@SuppressLint("AppCompatCustomView")
public class CustomTextView extends TextView {
//    private static final String TAG = "CustomTextView";

    public CustomTextView(Context context) {
        super(context);
    }

    public CustomTextView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setCustomFont(context, attrs);
    }

    public CustomTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setCustomFont(context, attrs);
    }

    private void setCustomFont(Context ctx, AttributeSet attrs) {
        TypedArray a = ctx.obtainStyledAttributes(attrs, R.styleable.CustomTextView);
        String customFont = a.getString(R.styleable.CustomTextView_customFont);
        setCustomFont(ctx, customFont);
        a.recycle();
    }

    public void setCustomFont(Context ctx, String asset) {
        Typeface typeface = Typeface.createFromAsset(ctx.getAssets(), "fonts/" + asset);
        setTypeface(typeface);
    }
}
