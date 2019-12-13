package com.askey.askeylaunchers;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Created by gavin_tsao on 2019.09.16.
 */

public class NotiAdapter extends BaseAdapter {

    private static final int UNIT_MIN = 60;
    private static final int UNIT_HOUR = 3600;
    private static final int UNIT_DAY = 86400;
    private Context mContext;
    private NotificationContent.NotificationDetail[] notificationsForAdapter;

    public NotiAdapter(Context c, NotificationContent.NotificationDetail notis[]) {
        mContext = c;
        notificationsForAdapter = notis;
    }

    @Override
    public int getCount() {
        return notificationsForAdapter.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        LayoutInflater li = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (convertView == null) {
            convertView = li.inflate(R.layout.notify_list_item, null);

            viewHolder = new ViewHolder();
            //viewHolder.icon = (ImageView) convertView.findViewById(R.id.icon_image);
            viewHolder.txLine1 = (TextView) convertView.findViewById(R.id.line1_text);
            viewHolder.txLine2 = (TextView) convertView.findViewById(R.id.line2_text);
            viewHolder.txLine3 = (TextView) convertView.findViewById(R.id.line3_text);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //viewHolder.icon.setImageDrawable(packagesForAdapter[position].icon);
        viewHolder.txLine1.setText(notificationsForAdapter[position].title);
        viewHolder.txLine2.setText(notificationsForAdapter[position].text);
        viewHolder.txLine3.setText(formatDuration(notificationsForAdapter[position].posttime));

        return convertView;
    }

    private String formatDuration(long posttime) {
        long duration = System.currentTimeMillis() - posttime;
        duration = (long) ((duration + 1000) / 1000);
        if (duration < UNIT_MIN) {
            return String.valueOf(duration) + " s ago";
        } else if (duration >= UNIT_MIN && duration < UNIT_HOUR) {
            int mins = (int) (duration / 60);
            return String.valueOf(mins) + " m ago";
        } else if (duration >= UNIT_HOUR && duration < UNIT_DAY) {
            int mins = (int) (duration / 60);
            int hrs = (int) (mins / 60);
            return String.valueOf(mins) + " h ago";
        } else if (duration >= UNIT_DAY) {
            int mins = (int) (duration / 60);
            int hrs = (int) (mins / 60);
            int days = (int) (hrs / 24);
            return String.valueOf(mins) + " days ago";
        }

        return "";
    }

    static class ViewHolder {
        ImageView icon;
        TextView txLine1;
        TextView txLine2;
        TextView txLine3;
    }
}
