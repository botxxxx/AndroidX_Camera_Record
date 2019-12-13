package com.askey.widget;

import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.Random;

//import androidx.annotation.NonNull;
//import androidx.viewpager.widget.PagerAdapter;

public class SamplePagerAdapter extends PagerAdapter {

    private final Random random = new Random();
    private int mSize;

    public SamplePagerAdapter() {
        mSize = 1;
    }

    public SamplePagerAdapter(int count) {
        mSize = count;
    }

    @Override
    public int getCount() {
        return mSize;
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup view, int position, @NonNull Object object) {
        view.removeView((View) object);
    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup view, int position) {
        TextView textView = new TextView(view.getContext());
//        textView.setBackgroundColor(0x00000000);
//        textView.setText(String.valueOf(position + 1));
//        textView.setBackgroundColor(0xff000000 | random.nextInt(0x00ffffff));
//        textView.setGravity(Gravity.CENTER);
//        textView.setTextColor(Color.WHITE);
//        textView.setTextSize(48);
        view.addView(textView, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        return textView;
    }

    public void addItem() {
        mSize++;
        notifyDataSetChanged();
    }

    public void removeItem() {
        mSize--;
        mSize = mSize < 0 ? 0 : mSize;
        notifyDataSetChanged();
    }
}