package com.askey.askeylaunchers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.askey.askeylaunchers.MonitoringActivity.heartRateParsIndex;
import com.askey.askeylaunchers.MonitoringActivity.walkParsIndex;
import com.askey.widget.CircleIndicator;
import com.askey.widget.LoopViewPager;
import com.askey.widget.SamplePagerAdapter;

import java.util.Arrays;
import java.util.Locale;

public class ControlCenterActivity extends BaseActivity {
    private static final String TAG = "ControlCenterActivity";
    private static int heartRateInfo = 0;
    private static int walkInfo = 0;
    private GestureDetectorCompat gestureDetectorCompat = null;
    private ViewFlipper flipper;
    private LoopViewPager pager;
    private BatteryManager batteryManager = null;
    private TextView popupWindowText = null;
    private PopupWindow popupWindow = null;
    private String[] batteryInfoItem = null, batteryInfo = null;
    private int[] batteryStatusConstants = null, batteryHealthConstants = null, batteryPluggedConstants = null;
    private String[] batteryStatusContents = null, batteryHealthContents = null, batteryPluggedContents = null;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.d(TAG, "ControlCenterActivity::receiver: got an action, " + action);

            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                handleBatteryStateChanged(intent);
            } else if (action.equals(MainActivity.ASKEY_ACTION_SENSOR_SETTINGS)) { // Sensor settings
                int[] heartRateParameters = intent.getIntArrayExtra("heartRateSettings");
                int[] walkParameters = intent.getIntArrayExtra("walkSettings");
                if (heartRateParameters != null) {
                    heartRateInfo = heartRateParameters[heartRateParsIndex.VISIBLE.ordinal()];
                } else if (walkParameters != null) {
                    walkInfo = walkParameters[walkParsIndex.VISIBLE.ordinal()];
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_center);
        Log.d(MainActivity.TAG, "ControlCenterActivity::onCreate: X");
        pager = findViewById(R.id.control_loop_viewpager);
        CircleIndicator indicator = findViewById(R.id.control_circle_indicator);

        pager.setAdapter(new SamplePagerAdapter(2));
        pager.setPagingEnabled(false);
        indicator.setViewPager(pager);

        flipper = getWindow().getDecorView().findViewById(R.id.control_view_flipper);

        GestureListener gestureListener = new MyGestureListener();
        gestureDetectorCompat = new GestureDetectorCompat(this, gestureListener);

        if (!hasWriteSettingsPermission(this)) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                startActivity(intent);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "ControlCenterActivity::onCreate: " + e);
            }
        }

        //init
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        ifilter.addAction(Intent.ACTION_BATTERY_CHANGED);  //battery
        ifilter.addAction(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED);  //NFC
        ifilter.addAction(MainActivity.ASKEY_ACTION_SENSOR_SETTINGS);
        registerReceiver(receiver, ifilter);

        batteryManager = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);

        // battery info item
        batteryInfoItem = getResources().getStringArray(R.array.batteryInfoItem);
        batteryInfo = new String[batteryInfoItem.length];

        // battery status and its correspond contents
        batteryStatusConstants = new int[]{BatteryManager.BATTERY_STATUS_UNKNOWN,
                BatteryManager.BATTERY_STATUS_CHARGING,
                BatteryManager.BATTERY_STATUS_DISCHARGING,
                BatteryManager.BATTERY_STATUS_NOT_CHARGING,
                BatteryManager.BATTERY_STATUS_FULL};
        batteryStatusContents = getResources().getStringArray(R.array.batteryStatusContents);

        // battery health and its correspond contents
        batteryHealthConstants = new int[]{BatteryManager.BATTERY_HEALTH_UNKNOWN,
                BatteryManager.BATTERY_HEALTH_GOOD,
                BatteryManager.BATTERY_HEALTH_OVERHEAT,
                BatteryManager.BATTERY_HEALTH_DEAD,
                BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE,
                BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE};
        batteryHealthContents = getResources().getStringArray(R.array.batteryHealthContents);

        // battery plugged and its correspond contents
        batteryPluggedConstants = new int[]{BatteryManager.BATTERY_PLUGGED_AC,
                BatteryManager.BATTERY_PLUGGED_USB};
        batteryPluggedContents = getResources().getStringArray(R.array.batteryPluggedContents);

        // get sensor settings
        heartRateInfo = this.getIntent().getIntExtra("heartRateInfo", -1);
        walkInfo = this.getIntent().getIntExtra("walkInfo", -1);
    }

    private boolean hasWriteSettingsPermission(Context context) {
        boolean rest;
        rest = Settings.System.canWrite(context);

        return rest;
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(MainActivity.TAG, "ControlCenterActivity::onResume: X");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(MainActivity.TAG, "ControlCenterActivity::onPause: X");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(MainActivity.TAG, "ControlCenterActivity::onDestroy: X");

        unregisterReceiver(receiver);
    }

    public void album(View view) {
        Intent intent = new Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivity(intent);
    }

    public void calculator(View view) {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.calculator2_package_name));
        startActivity(launchIntent);
    }

    public void clock(View view) {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.deskclock_package_name));
        startActivity(launchIntent);
    }

    public void calendar(View view) {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.calendar_package_name));
        startActivity(launchIntent);
    }

    public void fileManager(View view) {
        PackageManager packageManager = getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.documentsui_package_name));
        if (intent != null) {
            startActivity(intent);
        }
    }

    public void recorder(View view) {
        // jeff_wu@20191005, using package name to start soundrecorder app (Start)
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.recorder_package_name));
        startActivity(launchIntent);
        // jeff_wu@20191005, using package name to start soundrecorder app (End)
    }

    public void scanner(View view) {
        PackageManager packageManager = getPackageManager();
        Intent launchIntent = packageManager.getLaunchIntentForPackage(getResources().getString(R.string.honeywellscanner_package_name));
        startActivity(launchIntent);
    }

    // jeff_wu@1080722, use qualcomm settings (Start)
    public void settings(View view) {
        Intent intent = new Intent(Settings.ACTION_SETTINGS);
        startActivity(intent);

        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }
    // jeff_wu@1080722, use qualcomm settings (End)

    public void camera(View view) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
        startActivity(intent);
    }

    public void sensors(View view) {
        Intent intent = new Intent(this, SensorsActivity.class);
        startActivity(intent);

        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    public void battery(View view) {
        String content = "";

        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("INST current")] =
                Long.toString(batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)) + "μA";
        for (int i = 0; i < batteryInfoItem.length; i++) {
            if (i > 0) {
                content += "\n";
            }
            content += batteryInfoItem[i] + ": " + batteryInfo[i];
        }
        CharSequence c = String.format(Locale.getDefault(), "%s", content);

        popupWithText(view, c);
    }

    private void popupWithText(View view, CharSequence c) {
        LayoutInflater layoutinflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View popup = layoutinflater.inflate(R.layout.popup_window, null);

        popupWindow = new PopupWindow(popup,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true);

        popupWindowText = (TextView) popupWindow.getContentView().findViewById(R.id.popup_textview);
        popupWindowText.setText(c);

        popupWindow.setFocusable(false);
        popupWindow.update();
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);
        setFullScreen(popupWindow.getContentView());
        popupWindow.setFocusable(true);
        popupWindow.update();
    }

    public void music(View view) {
        Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        startActivity(intent);
    }

    public void monitoring(View view) {
        Intent intent = new Intent(this, MonitoringActivity.class);
        intent.putExtra("heartRateInfo", heartRateInfo);
        intent.putExtra("walkInfo", walkInfo);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
    }

    private void handleBatteryStateChanged(Intent intent) {
        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Level")] = Integer.toString(intent.getIntExtra("level", 0)) + "%";
        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Voltage")] = Integer.toString(intent.getIntExtra("voltage", 0)) + "mV";
        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("INST current")] = Long.toString(batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)) + "μA";
        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Temperature")] = Double.toString((double) intent.getIntExtra("temperature", 0) * 0.1) + "°C";
        batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Technology")] = intent.getStringExtra("technology");

        // status
        for (int i = 0; i < batteryStatusConstants.length; i++) {
            if (intent.getIntExtra("status", 0) == batteryStatusConstants[i]) {
                batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Status")] = batteryStatusContents[i];
            }
        }

        // health
        for (int i = 0; i < batteryHealthConstants.length; i++) {
            if (intent.getIntExtra("health", 0) == batteryHealthConstants[i]) {
                batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Health")] = batteryHealthContents[i];
            }
        }

        // plugged
        for (int i = 0; i < batteryPluggedConstants.length; i++) {
            if (intent.getIntExtra("plugged", 0) == batteryPluggedConstants[i]) {
                batteryInfo[Arrays.asList(batteryInfoItem).indexOf("Plugged")] = batteryPluggedContents[i];
            }
        }

        // show in popupwindow
        if (popupWindow != null) {
            String content = "";
            for (int i = 0; i < batteryInfoItem.length; i++) {
                if (i > 0) {
                    content += "\n";
                }
                content += batteryInfoItem[i] + ": " + batteryInfo[i];
            }

            CharSequence c = String.format(Locale.getDefault(), "%s", content);
            popupWindowText.setText(c);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetectorCompat.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        super.dispatchTouchEvent(event);
        return gestureDetectorCompat.onTouchEvent(event);
    }

    private class MyGestureListener extends GestureListener {
        int durationSlide = getResources().getInteger(R.integer.config_SlideAnimTime);

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(MainActivity.TAG, "ControlCenterActivity::onSingleTapConfirmed: X");
            return super.onSingleTapConfirmed(e);
        }

        @Override
        protected void swipeUp() {
            Log.d(MainActivity.TAG, "ControlCenterActivity::swipeUp: X");
            goBack();
            // jake_su@20191205, avoid screen flicker when finish activity
            //overridePendingTransition(R.anim.slide_from_bottom, R.anim.slide_to_top);
        }
//
//        @Override
//        protected void swipeDown() {
//            Log.d(MainActivity.TAG, "ControlCenterActivity::swipeDown: X");
//            goBack();
//            overridePendingTransition(R.anim.slide_from_top, R.anim.slide_to_bottom);
//        }

        @Override
        protected void swipeLeft() {
            Log.d(MainActivity.TAG, "ControlCenterActivity::swipeLeft: X");
            Animation clockAnimationIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_from_right);
            clockAnimationIn.setDuration(durationSlide);
            flipper.setInAnimation(clockAnimationIn);

            Animation clockAnimationOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_to_left);
            clockAnimationOut.setDuration(durationSlide);
            flipper.setOutAnimation(clockAnimationOut);

            flipper.showNext();
            pager.setCurrentItem(pager.getCurrentItem() == 0 ? 1 : 0);
        }

        @Override
        protected void swipeRight() {
            Log.d(MainActivity.TAG, "ControlCenterActivity::swipeRight: X");
            Animation clockAnimationIn = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_from_left);
            clockAnimationIn.setDuration(durationSlide);
            flipper.setInAnimation(clockAnimationIn);

            Animation clockAnimationOut = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_to_right);
            clockAnimationOut.setDuration(durationSlide);
            flipper.setOutAnimation(clockAnimationOut);

            flipper.showPrevious();
            pager.setCurrentItem(pager.getCurrentItem() == 0 ? 1 : 0);
        }
    }
}
