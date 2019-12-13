package com.askey.askeylaunchers;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class NotificationListener extends NotificationListenerService {
    private static final String TAG = "NotificationListener";
    private static final boolean DBG = false;

    @Override
    public void onListenerConnected() {
        if (DBG) Log.i(TAG, "onListenerConnected: X");
        super.onListenerConnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (DBG) Log.i(TAG, "onNotificationPosted: sbn=" + sbn);
        super.onNotificationPosted(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (DBG) Log.i(TAG, "onNotificationRemoved: sbn=" + sbn);
        super.onNotificationRemoved(sbn);
    }

    @Override
    public StatusBarNotification[] getActiveNotifications() {
        StatusBarNotification[] activeNotifications = super.getActiveNotifications();
        if (DBG) {
            for (StatusBarNotification sbn : activeNotifications) {
                Log.i(TAG, "getActiveNotifications: sbn=" + sbn);
            }
        }
        return activeNotifications;
    }
}
