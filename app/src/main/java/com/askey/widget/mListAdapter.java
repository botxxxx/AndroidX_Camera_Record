package com.askey.widget;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;

public class mListAdapter extends BaseAdapter {

    private ArrayList<View> arrayList;

    public mListAdapter(ArrayList<View> arrayList) {
        this.arrayList = arrayList;
    }

    public int getCount() {
        return arrayList.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        return arrayList.get(position);
    }
}
