package com.askey.askeylaunchers;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.askey.askeylaunchers.MonitoringActivity.heartRateParsIndex;
import com.askey.askeylaunchers.MonitoringActivity.walkParsIndex;
import com.qualcomm.qti.watchhome.sensor.SensorListener;
import com.qualcomm.qti.watchhome.sensor.SensorService;

//import android.os.AsyncResult;

public class MainActivity extends BaseActivity {
    public static final String TAG = "AskeyLauncher";

    public static final String PREF_NAME_ASKEYLAUNCHED = "pref_name_askeylauncher";

    public static final String KEY_LAUNCH_SCREEN = "pref_key_launch_screen";
    public static final String KEY_LAUNCH_LOCKED = "pref_key_launch_locked";
    public static final String KEY_LAUNCH_PASSWORD = "pref_key_launch_password";

    public static final String KEY_LOCKING_SCREEN = "locking_screen";
    public static final String ASKEY_ACTION_SENSOR_SETTINGS = "askey.intent.action.SENSOR_SETTINGS";
    public static final String ASKEY_ACTION_SEND_TEST_NOTIFICATION = "askey.intent.action.SEND_TEST_NOTIFICATION";
    private static final int EVENT_SENSORCHANGED = 1;
    private static final int VIEW_ANALOGCLOCK = 0;
    private static final int VIEW_DIGITALCLOCK = 1;
    private static final int VIEW_NOTIFICATION = 2;
    private static final int VIEW_WIDGETICON = 3;
    private static int flipperindex;
    private static boolean isLocked;
    private static boolean isLaunch = true;
    private GestureDetectorCompat gestureDetectorCompat = null;
    private ImageView locked;
    private ViewFlipper clock;
    private RelativeLayout heartRateInfo, walkInfo;
    private TextView heartRateValue, walkValue;
    private GestureDetector gestureDetector;

    private NotificationContent notiContent;
    private WidgetOnOff scroll;

    //jace_ho@20190919, add WidgetOnOff (start)
    private WifiManager mWifiManager;
    private NfcAdapter mNfcAdapter;
    private IntentFilter mIntentFilter;
    private BluetoothAdapter btAdapter;
    private ContentObserver airplaneModeObserver;
    private Boolean wifiOn = false;
    //jace_ho@20190919, add WidgetOnOff (end)
    //jace_ho@20190918, add wifiOnOff (start)
    private BroadcastReceiver mWifiStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context wifiState_context, Intent wifiState_intent) {
            String action = wifiState_intent.getAction();

            if (wifiState_intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION)) {
                int wifistate = wifiState_intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_DISABLED);
                if (wifistate == WifiManager.WIFI_STATE_DISABLED) {
                    wifiOn = false;
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.WIFI.ordinal(), WidgetOnOff.imgBtnStatusDEF.OFF.ordinal());
                } else if (wifistate == WifiManager.WIFI_STATE_ENABLED) {
                    wifiOn = true;
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.WIFI.ordinal(), WidgetOnOff.imgBtnStatusDEF.ON.ordinal());
                }
            }

            if (wifiOn) {
                if (wifiState_intent.getAction().equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)) {
                    NetworkInfo info = wifiState_intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    if (info.getState().equals(NetworkInfo.State.DISCONNECTED)) {
                        WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.WIFI.ordinal(), WidgetOnOff.imgBtnStatusDEF.UNKNOWN.ordinal());
                    }
                }
            }
        }
    };
    //jace_ho@20190919, add NfcOnOff (start)
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context nfcState_context, Intent nfcState_intent) {
            String action = nfcState_intent.getAction();
            int nfcState = nfcState_intent.getIntExtra(NfcAdapter.EXTRA_ADAPTER_STATE, 0);

            switch (nfcState) {
                case NfcAdapter.STATE_OFF:
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.NFC.ordinal(), WidgetOnOff.imgBtnStatusDEF.OFF.ordinal());
                    break;
                case NfcAdapter.STATE_ON:
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.NFC.ordinal(), WidgetOnOff.imgBtnStatusDEF.ON.ordinal());
                    break;
            }
        }
    };
    //jace_ho@20190919, add BTOnOff (start)
    private BroadcastReceiver mBTReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context BTState_context, Intent BTState_intent) {
            String action = BTState_intent.getAction();
            Log.d(TAG, "mBTReceiver: got " + action);

            if (BTState_intent.getAction().equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int BTState = BTState_intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
                if (BTState == BluetoothAdapter.STATE_OFF) {
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.BLUETOOTH.ordinal(), WidgetOnOff.imgBtnStatusDEF.OFF.ordinal());
                } else if (BTState == BluetoothAdapter.STATE_ON) {
                    WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.BLUETOOTH.ordinal(), WidgetOnOff.imgBtnStatusDEF.ON.ordinal());
                }
            }

            if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action) || BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                BluetoothDevice device = BTState_intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (action) {
                    case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                        WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.BLUETOOTH.ordinal(), WidgetOnOff.imgBtnStatusDEF.UNKNOWN.ordinal());
                        break;
                    case BluetoothDevice.ACTION_ACL_CONNECTED:
                        WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.BLUETOOTH.ordinal(), WidgetOnOff.imgBtnStatusDEF.ON.ordinal());
                        break;
                    default:
                        break;
                }
            }
        }
    };
    //jace_ho@20190918, add wifiOnOff (end)
    private View.OnTouchListener onTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub
            Log.d(MainActivity.TAG, "MainActivity::onTouch: X, event=" + event);
            return gestureDetector.onTouchEvent(event);
        }
    };
    //jace_ho@20190919, add NfcOnOff (end)
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
//            AsyncResult ar;

            switch (msg.what) {
                case EVENT_SENSORCHANGED: {
//                    ar = (AsyncResult) msg.obj;
//                    if (ar.exception == null && ar.result != null) {
//                        val getVal = (val) ar.result;
//
//                        switch (getVal.sensorType) {
//                            case Sensor.TYPE_HEART_RATE:
//                                heartRateValue.setText(String.valueOf((int)getVal.sensorValue));
//                                break;
//                            case Sensor.TYPE_STEP_DETECTOR:
//                                walkValue.setText(String.valueOf((int)getVal.sensorValue));
//                                break;
//                        }
//                    }
                }
                break;
            }
        }
    };
    //jace_ho@20190919, add BTOnOff (end)
    private String notification_channel_id = "0800";
    private String notification_channel_title = "askey_launcher";
    private int notification_id = 0;
    //jace_ho@20190919, add APOnOff (end)
    // Get the state of checkbox in monitoring item
    public final BroadcastReceiver SystemBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(MainActivity.ASKEY_ACTION_SENSOR_SETTINGS)) { // Sensor settings
                int[] heartRateParameters = intent.getIntArrayExtra("heartRateSettings");
                int[] walkParameters = intent.getIntArrayExtra("walkSettings");

                if (heartRateParameters != null) {
                    heartRateValue.setText(Integer.toString(0));
                    heartRateInfo.setVisibility(heartRateParameters[
                            heartRateParsIndex.VISIBLE.ordinal()] == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
                } else if (walkParameters != null) {
                    walkValue.setText(Integer.toString(walkParameters[walkParsIndex.INITIALVALUE.ordinal()]));
                    walkInfo.setVisibility(walkParameters[
                            walkParsIndex.VISIBLE.ordinal()] == View.VISIBLE ? View.VISIBLE : View.INVISIBLE);
                }
            } else if (action.equals(MainActivity.ASKEY_ACTION_SEND_TEST_NOTIFICATION)) {
                String ticker = (intent.getStringExtra("notiticker") == null) ? "" : intent.getStringExtra("notiticker");
                String title = (intent.getStringExtra("notititle") == null) ? "gavin.tsao@askey.com" : intent.getStringExtra("notititle");
                String text = (intent.getStringExtra("notitext") == null) ? "aA bB cC dD eE fF gG hH iI jJ" : intent.getStringExtra("notitext");

                sendNotification(ticker, title, text);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(TAG, "MainActivity::onCreate: X");

        clock = (ViewFlipper) findViewById(R.id.clock_view);
        locked = (ImageView) findViewById(R.id.locked_view);
        heartRateInfo = (RelativeLayout) findViewById(R.id.heartRateInfo);
        heartRateValue = (TextView) findViewById(R.id.heartRate_text);
        walkInfo = (RelativeLayout) findViewById(R.id.walkInfo);
        walkValue = (TextView) findViewById(R.id.walk_text);

        if (isLaunch) {
            isLaunch = false;

            PasswordManager.initKeyStore();
            PasswordManager.initPassword(this);
        }

        SharedPreferences prefs = getSharedPreferences(PREF_NAME_ASKEYLAUNCHED, Context.MODE_PRIVATE);

        //if (getIntent().getBooleanExtra(KEY_LOCKING_SCREEN, false) || prefs.getBoolean(KEY_LAUNCH_LOCKED, false)) {
        if (getIntent().getBooleanExtra(KEY_LOCKING_SCREEN, false)) {
            //need the password
            locked.setVisibility(View.VISIBLE);
            isLocked = true;

            LockedGestureListener lockedgesture = new LockedGestureListener();
            gestureDetectorCompat = new GestureDetectorCompat(this, lockedgesture);

            // jeff_wu@20190530, cancel the password function and disable the active device admin app message (Start)
            /*if(devicepolicy == null) devicepolicy = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            devicepolicy.lockNow();*/
            // jeff_wu@20190530, cancel the password function and disable the active device admin app message (End)
        } else {
            //don't need the password
            locked.setVisibility(View.INVISIBLE);
            isLocked = false;

            GestureListener gestureListener = new MainGestureListener();
            gestureDetector = new GestureDetector(gestureListener);
            gestureDetectorCompat = new GestureDetectorCompat(this, gestureListener);
        }

        if (prefs.getBoolean(KEY_LAUNCH_SCREEN, true)) {
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean(KEY_LAUNCH_SCREEN, false);
            editor.commit();

            flipperindex = clock.getDisplayedChild();
        }

        // jeff_wu@20190902, skip analog clock
        if (flipperindex == VIEW_ANALOGCLOCK) {
            flipperindex = VIEW_DIGITALCLOCK;
        }
        clock.setDisplayedChild(flipperindex);

        if (flipperindex == 1) {
            float x = locked.getX() - 120f;
            float y = locked.getY() - 180f;

            locked.setX(x);
            locked.setY(y);
        }

        notiContent = new NotificationContent(this);
        ListView listView = (ListView) findViewById(R.id.notification_content);
        TextView textView = (TextView) findViewById(R.id.nomessage_text);
        listView.setOnTouchListener(onTouchListener);

        scroll = (WidgetOnOff) findViewById(R.id.settings_scroller);
        scroll.setOnTouchListener(onTouchListener);
        scroll.setGestureDetector(gestureDetector);
    }

    //jace_ho@20190919, add APOnOff (start)
    private void updateSwitchState() {
        WidgetOnOff.imgBtnStatus[WidgetOnOff.imgBtnItems.AIRPLANE.ordinal()] = isAirplaneModeOn();
        //airplaneModeSwitch.setChecked(isAirplaneModeOn());
        updateCheckedText();
    }

    // set the text state of the switch based on its checked state
    private void updateCheckedText() {
        if (WidgetOnOff.imgBtnStatus[WidgetOnOff.imgBtnItems.AIRPLANE.ordinal()] == WidgetOnOff.imgBtnStatusDEF.ON.ordinal() ? true : false) {
            WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.AIRPLANE.ordinal(), WidgetOnOff.imgBtnStatusDEF.ON.ordinal());
        } else {
            WidgetOnOff.setImgBtnIcon(WidgetOnOff.imgBtnItems.AIRPLANE.ordinal(), WidgetOnOff.imgBtnStatusDEF.OFF.ordinal());
        }
    }

    private int isAirplaneModeOn() {
        Boolean AP = (Settings.Global.getInt(getContentResolver(),
                Settings.Global.AIRPLANE_MODE_ON, 0) != 0);

        return AP == true ? WidgetOnOff.imgBtnStatusDEF.ON.ordinal() : WidgetOnOff.imgBtnStatusDEF.OFF.ordinal();
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(MainActivity.TAG, "MainActivity::onStart: X");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(MainActivity.TAG, "MainActivity::onResume: X");

        SensorListener sensorListener = SensorService.getSensorListenerInstance();
        if (sensorListener != null) {
            sensorListener.registerForSensorChanged(mHandler, EVENT_SENSORCHANGED, null);
        }

        // Registered broadcast for show monitoring item
        IntentFilter filter = new IntentFilter(ASKEY_ACTION_SENSOR_SETTINGS);
        filter.addAction(ASKEY_ACTION_SEND_TEST_NOTIFICATION);
        registerReceiver(SystemBroadcastReceiver, filter);

        //jace_ho@20190919, add wifiOnOff (start)
        IntentFilter mWifiStateIntent = new IntentFilter();
        mWifiStateIntent.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        mWifiStateIntent.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        registerReceiver(mWifiStateReceiver, mWifiStateIntent);
        //jace_ho@20190919, add wifiOnOff (end)

        //jace_ho@20190919, add NfcOnOff (start)
        mIntentFilter = new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);
        registerReceiver(mReceiver, mIntentFilter);
        //jace_ho@20190919, add NfcOnOff (end)

        //jace_ho@20190919, add BTOnOff (start)
        IntentFilter btEventsFilter = new IntentFilter();
        btEventsFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);      // BT Enable/Disable
        btEventsFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);  // Discoverable
        btEventsFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);       // device connected
        btEventsFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);    // device disconnected
        registerReceiver(mBTReceiver, btEventsFilter);
        //jace_ho@20190919, add BTOnOff (end)

        //jace_ho@20190919, add APOnOff (start)
        // create an observer for airplane mode changes that happen outside of this app
        airplaneModeObserver = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange) {
                updateSwitchState();
            }
        };

        getContentResolver().registerContentObserver(
                Settings.Global.getUriFor(Settings.Global.AIRPLANE_MODE_ON), true, airplaneModeObserver);
        //jace_ho@20190919, add APOnOff (end)
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(MainActivity.TAG, "MainActivity::onStop: X");

        SensorListener sensorListener = SensorService.getSensorListenerInstance();
        if (sensorListener != null) {
            sensorListener.unregisterForSensorChanged(mHandler);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(MainActivity.TAG, "MainActivity::onDestroy: X");

        unregisterReceiver(SystemBroadcastReceiver);

        //jace_ho@20190919, add BroadcastReceiver (start)
        if (mBTReceiver != null) {
            unregisterReceiver(mBTReceiver);
        }

        if (mWifiStateReceiver != null) {
            unregisterReceiver(mWifiStateReceiver);
        }

        if (mReceiver != null) {
            unregisterReceiver(mReceiver);
        }

        if (airplaneModeObserver != null) {
            getContentResolver().unregisterContentObserver(airplaneModeObserver);
        }
        //jace_ho@20190919, add BroadcastReceiver (end)
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetectorCompat.onTouchEvent(event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return true;
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (!isLocked) return true;

        return super.onKeyLongPress(keyCode, event);
    }

    private void sendNotification(String ticker, String title, String text) {
        notification_id += 1;

        Log.d(TAG, "MainActivity::sendNotification: X, id=" + notification_id);

        NotificationManager notiManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notiManager == null) {
            Log.d(TAG, "MainActivity::sendNotification: NotificationManager is null.");
        }

        Notification.Builder builder = new Notification.Builder(MainActivity.this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setTicker(ticker);
        builder.setWhen(System.currentTimeMillis());
        builder.setContentTitle(title);
        builder.setContentText(text);
        //Intent intent = new Intent(this, Main2Activity.class);
        //PendingIntent ma = PendingIntent.getActivity(this, 0, intent, 0);
        //builder.setContentIntent(ma);

        //builder.setDefaults(Notification.DEFAULT_SOUND);//设置声音
        //builder.setDefaults(Notification.DEFAULT_LIGHTS);//设置指示灯
        //builder.setDefaults(Notification.DEFAULT_VIBRATE);//设置震动
        builder.setDefaults(Notification.DEFAULT_ALL);//设置全部

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel notiChannel = new NotificationChannel(notification_channel_id, notification_channel_title, NotificationManager.IMPORTANCE_DEFAULT);
            notiChannel.enableLights(false); //是否在桌面icon右上角展示小红点
            //notiChannel.setLightColor(Color.GREEN); //小红点颜色
            notiChannel.setShowBadge(false); //是否在久按桌面图标时显示此渠道的通知

            notiManager.createNotificationChannel(notiChannel);
            builder.setChannelId(notification_channel_id);
        }

        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;
        notiManager.notify(notification_id, notification);
    }

    private class MainGestureListener extends GestureListener {
        int durationSlide = getResources().getInteger(R.integer.config_SlideAnimTime);

        @Override
        protected void swipeUp() {
            Log.d(TAG, "MainActivity::swipeUp: X");
            Intent intent = new Intent(MainActivity.this, ControlCenterActivity.class);
            intent.putExtra("heartRateInfo", heartRateInfo.getVisibility());
            intent.putExtra("walkInfo", walkInfo.getVisibility());
            startActivity(intent);
            // jake_su@20191205, avoid screen flicker when finish activity
            //overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top);
        }

        @Override
        protected void swipeDown() {
            Log.d(TAG, "MainActivity::swipeDown: X");
            // jake_su@20191205, avoid affecting status bar extensions
            /*Intent intent = new Intent(MainActivity.this, ControlCenterActivity.class);
            intent.putExtra("heartRateInfo", heartRateInfo.getVisibility());
            intent.putExtra("walkInfo", walkInfo.getVisibility());
            startActivity(intent);
            overridePendingTransition(R.anim.slide_from_top, R.anim.slide_to_bottom);*/
        }

        @Override
        protected void swipeLeft() {
            Log.d(TAG, "MainActivity::swipeLeft: X");
            Animation clockAnimationIn = AnimationUtils.loadAnimation(MainActivity.this.getApplicationContext(), R.anim.slide_from_right);
            clockAnimationIn.setDuration(durationSlide);
            clock.setInAnimation(clockAnimationIn);

            Animation clockAnimationOut = AnimationUtils.loadAnimation(MainActivity.this.getApplicationContext(), R.anim.slide_to_left);
            clockAnimationOut.setDuration(durationSlide);
            clock.setOutAnimation(clockAnimationOut);

            // jeff_wu@20190829, skip analog clock
            if (clock.getDisplayedChild() == clock.getChildCount() - 1) {
                clock.setDisplayedChild(VIEW_DIGITALCLOCK);
            } else {
                clock.showNext();
            }
            flipperindex = clock.getDisplayedChild();
        }

        @Override
        protected void swipeRight() {
            Log.d(TAG, "MainActivity::swipeRight: X");
            Animation clockAnimationIn = AnimationUtils.loadAnimation(MainActivity.this.getApplicationContext(), R.anim.slide_from_left);
            clockAnimationIn.setDuration(durationSlide);
            clock.setInAnimation(clockAnimationIn);

            Animation clockAnimationOut = AnimationUtils.loadAnimation(MainActivity.this.getApplicationContext(), R.anim.slide_to_right);
            clockAnimationOut.setDuration(durationSlide);
            clock.setOutAnimation(clockAnimationOut);

            // jeff_wu@20190829, skip analog clock
            if (clock.getDisplayedChild() == VIEW_DIGITALCLOCK) {
                clock.setDisplayedChild(clock.getChildCount() - 1);
            } else {
                clock.showPrevious();
            }
            flipperindex = clock.getDisplayedChild();
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(TAG, "MainActivity::onSingleTapConfirmed: X");
            return super.onSingleTapConfirmed(e);
        }

        @Override
        public void onLongPress(MotionEvent e) {
            Log.d(TAG, "MainActivity::onLongPress: X, view=" + clock.getDisplayedChild());
            // jeff_wu@20190530, cancel the password function and disable the active device admin app message (Start)
            /*DevicePolicyManager devicepolicy = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            devicepolicy.lockNow();*/
            // jeff_wu@20190530, cancel the password function and disable the active device admin app message (End)

            // On long press, go to launcher
            if ((clock.getDisplayedChild() == VIEW_ANALOGCLOCK) || (clock.getDisplayedChild() == VIEW_DIGITALCLOCK)) {
                PackageManager pm = getPackageManager();
                final String LAUNCHER_TAG = getResources().getString(R.string.launcher_package_name);
                Intent launchIntent = pm.getLaunchIntentForPackage(LAUNCHER_TAG);
                if (launchIntent == null) {
                    Log.e(TAG, "Launch intent for custom launcher not found");
                } else {
                    startActivity(launchIntent);
                }
            }
        }
    }

    private class LockedGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            PasswordManager pwdmgr = new PasswordManager() {
                @Override
                public void verifiedAction() {
                    password_popup.dismiss();

                    locked.setVisibility(View.INVISIBLE);
                    isLocked = false;

                    GestureListener gestureListener = new MainGestureListener();
                    gestureDetector = new GestureDetector(gestureListener);
                    gestureDetectorCompat = new GestureDetectorCompat(MainActivity.this, gestureListener);
                }
            };
            pwdmgr.showPasswordInput(MainActivity.this);

            return true;
        }
    }
}

