package com.askey.widget;

import androidx.viewpager.widget.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

public class mPagerAdapter extends PagerAdapter {

    ArrayList<View> viewList;

    public mPagerAdapter(ArrayList<View> viewList) {
        this.viewList = viewList;
    }

    public int getCount() {
        return viewList.size();
    }

    public Object instantiateItem(ViewGroup container, int position) {
        container.addView(viewList.get(position));
        return viewList.get(position);
    }

    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public void destroyItem(ViewGroup container, int position, Object object) {
        container.removeView((View) object);
    }
}