package com.askey.askeylaunchers;

import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;

//import android.os.AsyncResult;
//import android.os.Registrant;
//import android.os.RegistrantList;

/**
 * Created by gavin_tsao on 2019.09.16.
 */

public class NotiLongClickListener implements AdapterView.OnItemLongClickListener {

    private Context mContext;
//    private Registrant mNotiLongClickRegistrant;

    public NotiLongClickListener(Context c, Handler h, int what, Object obj) {
        mContext = c;
//        mNotiLongClickRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(NotificationContent.TAG, "onItemLongClick: X, pos=" + position);

//        if (mNotiLongClickRegistrant != null) {
//            mNotiLongClickRegistrant.notifyRegistrant(
//                    new AsyncResult(null, position, null));
//        }

        return true;
    }
}
