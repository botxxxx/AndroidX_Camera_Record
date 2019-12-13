package com.askey.askeylaunchers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.drawable.ClipDrawable;
import android.hardware.camera2.CameraManager;
import android.net.wifi.WifiManager;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.SeekBar;

import com.askey.widget.VerticalSeekBar;

import java.util.Arrays;

//jace_ho@20190916,add WidgetOnOff (start)
//jace_ho@20190916,add WidgetOnOff (end)

/**
 * Created by jeff_wu on 9/3/19.
 */

public class WidgetOnOff extends ScrollView {
    public static final String TAG = "WidgetOnOff";
    public static int imgBtnStatus[];
    private static Context context;
    private static Integer imgBtnID[], imgBtnOffIcon[], imgBtnOnIcon[], imgBtnUnknownIcon[];
    private static ImageButton imgBtn[];
    private static boolean mTorchMode = false;    //true:enable, false:disable
    GestureDetector gestureDetector;
    private double BRIGHTNESS_RANGE = 255; // 0~255
    private double SEEKBAR_RANGE = 100;    // 0~100
    private double SBTOBN = BRIGHTNESS_RANGE / SEEKBAR_RANGE;
    private int CLIPSDRAWABLE_LEVEL = 10000;
    //jace_ho@20190916, add WidgetOnOff(start)
    private NfcManager mNfcManager;
    private NfcAdapter mNfcAdapter;
    private boolean mNfcEnabled = false;
    private boolean mBTEnabled = false;
    private WifiManager mWifiManager;
    private BluetoothAdapter btAdapter;
    private ContentObserver airplaneModeObserver;
    private CameraManager mCameraManager = null;
    //jace_ho@20190916, add WidgetOnOff (end)
    private ImageView imgViewBtns;
    private ClipDrawable clipDrawableBtns;
    private VerticalSeekBar seekBarBtns;
    private CameraManager.TorchCallback mTorchCallback = new CameraManager.TorchCallback() {
        @Override
        public void onTorchModeUnavailable(String cameraId) {
            int index = imgBtnItems.FLASHLIGHT.ordinal();

            mTorchMode = false;
            setImgBtnIcon(index, imgBtnStatusDEF.OFF.ordinal());
        }

        @Override
        public void onTorchModeChanged(String cameraId, boolean enabled) {
            int item = Arrays.asList(imgBtnID).indexOf(R.id.widget_imgBtn05);

            mTorchMode = enabled;
            setImgBtnIcon(item, mTorchMode == false ? imgBtnStatusDEF.OFF.ordinal() : imgBtnStatusDEF.ON.ordinal());
        }
    };
    private ImageButton.OnClickListener imageButtonListener = new ImageButton.OnClickListener() {
        @Override
        public void onClick(View v) {
            int item = Arrays.asList(imgBtnID).indexOf(v.getId());

            setImgBtnIcon(item, imgBtnStatus[item] == imgBtnStatusDEF.OFF.ordinal() ? imgBtnStatusDEF.ON.ordinal() : imgBtnStatusDEF.OFF.ordinal());

            if (item == imgBtnItems.NFC.ordinal()) {
                nfc();
            } else if (item == imgBtnItems.WIFI.ordinal()) {
                wifi();
            } else if (item == imgBtnItems.AIRPLANE.ordinal()) {
                airPlaneMode();
            } else if (item == imgBtnItems.BLUETOOTH.ordinal()) {
                bluetooth();
            } else if (item == imgBtnItems.FLASHLIGHT.ordinal()) {
                flashlight();
            }
        }
    };
    private VerticalSeekBar.OnSeekBarChangeListener LightBar = new VerticalSeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
            double scale = (double) arg1 / (double) arg0.getMax();

            // ClipDrawable progress
            clipDrawableBtns.setLevel((int) (CLIPSDRAWABLE_LEVEL * scale));

            // set brightness
            Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) ((double) arg1 * SBTOBN));
        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
        }

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
        }
    };

    public WidgetOnOff(Context context) {
        super(context);
        this.context = context;
    }

    public WidgetOnOff(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public WidgetOnOff(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.context = context;
    }

    public static void setImgBtnIcon(int i, int status) {
        Log.d(TAG, "jace_ho, " + "setImgBtnIcon()");

        if (imgBtnStatus != null) {
            Log.d(TAG, "jace_ho, " + "imgBtnStatus!=null");

            imgBtnStatus[i] = status;

            if (imgBtnStatus[i] == imgBtnStatusDEF.OFF.ordinal()) {
                imgBtn[i].setImageDrawable(context.getResources().getDrawable(imgBtnOffIcon[i]));
            } else if (imgBtnStatus[i] == imgBtnStatusDEF.ON.ordinal()) {
                imgBtn[i].setImageDrawable(context.getResources().getDrawable(imgBtnOnIcon[i]));
            } else if (imgBtnStatus[i] == imgBtnStatusDEF.UNKNOWN.ordinal()) {
                if (i == imgBtnItems.WIFI.ordinal()) {
                    imgBtn[i].setImageDrawable(context.getResources().getDrawable(imgBtnUnknownIcon[0]));
                } else if (i == imgBtnItems.BLUETOOTH.ordinal()) {
                    imgBtn[i].setImageDrawable(context.getResources().getDrawable(imgBtnUnknownIcon[1]));
                }
            }
        } else {
            Log.d(TAG, "jace_ho, " + "imgBtnStatus==null");
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        return gestureDetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        gestureDetector.onTouchEvent(ev);
        super.dispatchTouchEvent(ev);
        return true;
    }

    @Override
    protected void onAttachedToWindow() {
        Log.d(TAG, "jace_ho" + "onAttachedToWindo");
        super.onAttachedToWindow();
        //setBtn();
        imgBtnID = new Integer[]{R.id.widget_imgBtn01, R.id.widget_imgBtn02,
                R.id.widget_imgBtn03, R.id.widget_imgBtn04, R.id.widget_imgBtn05};
        imgBtnOffIcon = new Integer[]{R.drawable.btn_w1_nfc_off, R.drawable.btn_w1_wifi_off,
                R.drawable.btn_w1_ap_off, R.drawable.btn_w1_bt_off, R.drawable.btn_w1_light_off};
        imgBtnOnIcon = new Integer[]{R.drawable.btn_w1_nfc_on, R.drawable.btn_w1_wifi_on,
                R.drawable.btn_w1_ap_on, R.drawable.btn_w1_bt_on, R.drawable.btn_w1_light_on};
        imgBtnUnknownIcon = new Integer[]{R.drawable.btn_w1_wifi_no, R.drawable.btn_w1_bt_no};

        // combine imageBtnStatus array to layout ImageButton
        imgBtnStatus = new int[imgBtnID.length];

        // combine imgBtn array to layout ImageButton
        imgBtn = new ImageButton[imgBtnID.length];
        for (int i = 0; i < imgBtnID.length; i++) {
            imgBtn[i] = (ImageButton) findViewById(imgBtnID[i]);
            imgBtn[i].setOnClickListener(imageButtonListener);
            setImgBtnIcon(i, imgBtnStatusDEF.OFF.ordinal());
        }
        // flashlight, the feature is not support.
        if (mCameraManager == null) {
            mCameraManager = context.getSystemService(CameraManager.class);
            mCameraManager.registerTorchCallback(mTorchCallback, null);
        }

        // brightness
        imgViewBtns = (ImageView) findViewById(R.id.widget_imgView_brightness);
        clipDrawableBtns = (ClipDrawable) imgViewBtns.getDrawable();
        seekBarBtns = (VerticalSeekBar) findViewById(R.id.widget_vrtclSeekbar);
        seekBarBtns.setOnSeekBarChangeListener(LightBar);

        // set ClipsDrawable progress
        try {
            int nowBrightnessValue = android.provider.Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
            clipDrawableBtns.setLevel((int) (CLIPSDRAWABLE_LEVEL * ((double) nowBrightnessValue / BRIGHTNESS_RANGE)));
        } catch (Exception e) {
            e.printStackTrace();
        }

        //jace_ho@20190916,add WidgetOnOff(start)
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        //jace_ho@20190916,add WidgetOnOff(end)
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mCameraManager != null) {
            mCameraManager.unregisterTorchCallback(mTorchCallback);
        }
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;
    }

    public void nfc() {
        Log.d(MainActivity.TAG, "nfc()");
        // jace_ho@20190916, check icon status(start)
        mNfcAdapter = NfcAdapter.getDefaultAdapter(context);
        if (mNfcAdapter != null) {
            mNfcEnabled = mNfcAdapter.isEnabled();
            boolean success = false;
            Log.d(TAG, "jace_ho, " + "mNfcAdapter!=null");
            if (imgBtnStatus[imgBtnItems.NFC.ordinal()] == imgBtnStatusDEF.ON.ordinal() ? true : false) {
//                success = mNfcAdapter.enable();
            } else {
//                success = mNfcAdapter.disable();
                mNfcEnabled = success;
            }
        } else {
            Log.d(TAG, "jace_ho, " + "mNfcAdapter==null");
        }
        // jace_ho@20190916, check icon status(end)
    }

    public void wifi() {
        Log.d(MainActivity.TAG, "wifi()");
        // jeff_wu@20190911, check icon status(start)

        if (imgBtnStatus[imgBtnItems.WIFI.ordinal()] == imgBtnStatusDEF.ON.ordinal() ? true : false) {

            boolean mEnableWifi = mWifiManager.setWifiEnabled(true);
            //Log.d(TAG, "mEnableWifi="+Boolean.toString(mEnableWifi));
        } else {
            boolean mWifiDisabled = mWifiManager.setWifiEnabled(false);
            //Log.d(TAG, "mEnableWifi="+Boolean.toString(mEnableWifi));
        }

        // jeff_wu@20190911, check icon status(end)
    }

    public void airPlaneMode() {
        Log.d(MainActivity.TAG, "airPlaneMode()");
        // Jace_ho@20190916, check icon status(start)

        if (imgBtnStatus[imgBtnItems.AIRPLANE.ordinal()] == imgBtnStatusDEF.ON.ordinal() ? true : false) {

            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 1);
        } else {
            Settings.Global.putInt(context.getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
        }
        // jace_ho@20190916, check icon status(end)
    }

    public void bluetooth() {
        Log.d(MainActivity.TAG, "bluetooth()");
        // jace_ho@20190916, check icon status(start)

        boolean success = false;
        if (imgBtnStatus[imgBtnItems.BLUETOOTH.ordinal()] == imgBtnStatusDEF.ON.ordinal() ? true : false) {

            success = btAdapter.enable();
        } else {
            success = btAdapter.disable();
            mBTEnabled = success;
        }
        // jace_ho@20190916, check icon status(end)
    }

    public void flashlight() {
        Log.d(MainActivity.TAG, "flashlight()");

        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            try {
                String[] idCamera = mCameraManager.getCameraIdList();

                mCameraManager.setTorchMode(idCamera[0], mTorchMode == false ? true : false);
            } catch (Exception e) {
                Log.e(MainActivity.TAG, "WidgetOnOff::flashlight: " + e);
            }
        }
    }

    public enum imgBtnItems {NFC, WIFI, AIRPLANE, BLUETOOTH, FLASHLIGHT}

    public static enum imgBtnStatusDEF {OFF, ON, UNKNOWN}
}
