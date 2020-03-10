package com.askey.widget;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import com.askey.record.R;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class mLogListAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<LogMsg> arrayList;

    public mLogListAdapter(Context context, ArrayList<LogMsg> arrayList) {
        this.arrayList = arrayList;
        inflater = LayoutInflater.from(context);
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
        ViewHolder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.style_text_item, null);
            holder = new ViewHolder();
            holder.tv = convertView.findViewById(R.id.customTextView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        if (arrayList.size() > 0) {
            LogMsg log = arrayList.get(position);
            String time = log.time.format(DateTimeFormatter.ofPattern("HH:mm:ss")) + " ";
            holder.tv.setText(time + log.msg);
            mLog type = log.type;
            int color = type == mLog.v ? Color.BLACK : type == mLog.d ? Color.BLUE :
                    type == mLog.i ? Color.GREEN : type == mLog.w ? Color.YELLOW : Color.RED;
            holder.tv.setTextColor(color);
        }
        return convertView;
    }
}
