package com.askey.widget;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

public class SamplePagerAdapter2 extends PagerAdapter {
    //    private int mSize;
    private List<View> viewList;

    public SamplePagerAdapter2(List<View> viewList) {
        this.viewList = viewList;
//        this.mSize = this.viewList.size();
    }

    public int getCount() {
        return viewList.size();
    }

    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    public Object instantiateItem(ViewGroup container, int position) {
        View view = viewList.get(position);
        container.addView(view);
        return view;
    }

    public void destroyItem(ViewGroup container, int position, Object object) { // 销毁页卡
//        container.removeView((View) object);
    }

    public CharSequence getPageTitle(int position) {
        return null;
    }

    // dynamic view
//    public void addItem() {
//        mSize++;
//        notifyDataSetChanged();
//    }
//
//    public void removeItem() {
//        mSize--;
//        mSize = mSize < 0 ? 0 : mSize;
//
//        notifyDataSetChanged();
//    }

}
