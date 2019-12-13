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

public class NotiClickListener implements AdapterView.OnItemClickListener {

    private Context mContext;
//    private Registrant mNotiClickRegistrant;

    public NotiClickListener(Context c, Handler h, int what, Object obj) {
        mContext = c;
//        mNotiClickRegistrant = new Registrant(h, what, obj);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Log.i(NotificationContent.TAG, "onItemClick: X, + pos=" + position);

//        if (mNotiClickRegistrant != null) {
//            mNotiClickRegistrant.notifyRegistrant(
//                    new AsyncResult(null, position, null));
//        }
    }
}
