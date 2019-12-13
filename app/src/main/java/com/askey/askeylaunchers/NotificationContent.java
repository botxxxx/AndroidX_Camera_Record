package com.askey.askeylaunchers;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

//import android.os.AsyncResult;

public class NotificationContent {
    public static final String TAG = "NotificationContent";

    private static final int EVENT_ITEM_CLICK = 0;
    private static final int EVENT_ITEM_LONG_CLICK = 1;
    private Context mContext;
    private NotificationDetail[] notificationDetails;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            AsyncResult ar;
//
//            switch (msg.what) {
//                case EVENT_ITEM_CLICK:
//                    ar = (AsyncResult) msg.obj;
//                    if (ar.exception == null && ar.result != null) {
//                        int index = (int) ar.result;
//                        Log.i(NotificationContent.TAG, "EVENT_ITEM_CLICK: index=" + index);
//                        if (notificationDetails[index] != null) {
//                            Notification notification = notificationDetails[index].sbn.getNotification();
//                            final PendingIntent intent = notification.contentIntent != null ?
//                                    notification.contentIntent : notification.fullScreenIntent;
//                            mContext.startActivity(intent.getIntent());
//                        }
//                    }
//                    break;
//                case EVENT_ITEM_LONG_CLICK:
//                    ar = (AsyncResult) msg.obj;
//                    if (ar.exception == null && ar.result != null) {
//                        int index = (int) ar.result;
//                        Log.i(NotificationContent.TAG, "EVENT_ITEM_LONG_CLICK: index=" + index);
//                    }
//                    break;
//            }
        }
    };
    private final NotificationListener mNotificationListener = new NotificationListener() {
        @Override
        public void onListenerConnected() {
            Log.i(NotificationContent.TAG, "onListenerConnected: X");
            super.onListenerConnected();
            updateNotificationDetail(getActiveNotifications());
        }

        @Override
        public void onNotificationPosted(final StatusBarNotification sbn,
                                         final RankingMap rankingMap) {
            Log.d(NotificationContent.TAG, "onNotificationPosted: X, " + sbn);
            super.onNotificationPosted(sbn);
            updateNotificationDetail(getActiveNotifications());
        }

        @Override
        public void onNotificationRemoved(StatusBarNotification sbn,
                                          final RankingMap rankingMap) {
            Log.d(NotificationContent.TAG, "onNotificationRemoved: X, " + sbn);
            super.onNotificationRemoved(sbn);
            updateNotificationDetail(getActiveNotifications());
        }
    };

    public NotificationContent(Context c) {
        this.mContext = c;

        // Set up the initial notification state.
//        try {
//            mNotificationListener.registerAsSystemService(c,
//                    new ComponentName(c.getPackageName(), getClass().getCanonicalName()),
//                    UserHandle.USER_ALL);
//        } catch (RemoteException e) {
//        }
    }

    public void updateNotificationDetail(StatusBarNotification[] sbns) {
        Log.v(TAG, "updateNotificationDetail: X");
        setNotificationDetails(sbns);
    }

    private View getView(int id) {
        View rootView = ((Activity) mContext).getWindow().getDecorView().findViewById(android.R.id.content);
        View view = rootView.findViewById(id);

        return view;
    }

    private boolean setNotificationDetails(StatusBarNotification[] sbns) {
        int size = 0;

        Log.v(TAG, "setNotificationDetails: X");

        NotificationManager notiManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notiManager != null) {
            if (sbns != null) {
                size = getValidNotificationCount(sbns);
                Log.v(TAG, "setNotificationDetails: size=" + size);

                if (size > 0) {
                    // store notification details in array
                    int index = 0;
                    notificationDetails = new NotificationDetail[size];
                    for (StatusBarNotification sbn : sbns) {
                        //Log.v(TAG, "Notification " + sbns[i]);
                        if (!shouldFilterOut(sbn)) {
                            notificationDetails[index] = new NotificationDetail();
                            notificationDetails[index].icon = sbn.getNotification().getSmallIcon().loadDrawable(mContext);
                            notificationDetails[index].title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
                            notificationDetails[index].text = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
                            notificationDetails[index].posttime = sbn.getPostTime();
                            notificationDetails[index].sbn = sbn;

                            index++;
                        }
                    }
                    setNotificationData();
                }
            }
        }

        ListView listView = (ListView) getView(R.id.notification_content);
        TextView textView = (TextView) getView(R.id.nomessage_text);
        if (listView == null || textView == null) {
            return false;
        }

        if (size <= 0) {
            listView.setVisibility(View.GONE);
            textView.setVisibility(View.VISIBLE);
        } else {
            listView.setVisibility(View.VISIBLE);
            textView.setVisibility(View.GONE);
        }

        return true;
    }

    private int getValidNotificationCount(StatusBarNotification[] sbns) {
        int count = 0;

        for (StatusBarNotification sbn : sbns) {
            Log.v(TAG, "setNotificationDetails: sbn=" + sbn);
            if (!shouldFilterOut(sbn)) {
                count++;
            }
        }

        return count;
    }

    public boolean shouldFilterOut(StatusBarNotification sbn) {
        // To check notification content
        String title = sbn.getNotification().extras.getString(Notification.EXTRA_TITLE);
        String text = sbn.getNotification().extras.getString(Notification.EXTRA_TEXT);
        if ((title == null) && (text == null)) {
            return true;
        } else {
            boolean bTitleEmpty = (title != null) ? (title.length() <= 0) : true;
            boolean bTextEmpty = (text != null) ? (text.length() <= 0) : true;
            if (bTitleEmpty && bTextEmpty) {
                return true;
            }
        }

        // To check notification

        return false;
    }

    private void setNotificationData() {
        ListView listView = (ListView) getView(R.id.notification_content);
        listView.setAdapter(new NotiAdapter(mContext, notificationDetails));
        listView.setOnItemClickListener(new NotiClickListener(mContext, mHandler, EVENT_ITEM_CLICK, null));
        listView.setLongClickable(true);
        listView.setOnItemLongClickListener(new NotiLongClickListener(mContext, mHandler, EVENT_ITEM_LONG_CLICK, null));
    }

    class NotificationDetail {
        Drawable icon;
        String title;
        String text;
        String pkgname;
        long posttime;
        StatusBarNotification sbn;
    }
}
