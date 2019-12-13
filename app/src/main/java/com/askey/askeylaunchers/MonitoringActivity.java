package com.askey.askeylaunchers;

import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.view.GestureDetectorCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.askey.widget.RangeSeekBar;
import com.qualcomm.qti.watchhome.sensor.SensorListener;
import com.qualcomm.qti.watchhome.sensor.SensorService;

import java.util.Arrays;

public class MonitoringActivity extends BaseActivity {
    private static final String TAG = "MonitoringActivity";
    private static final int DISPLAY_CHILD_MAIN = 0;
    private static Boolean[] isChoosed = new Boolean[monitoring.values().length];
    ViewFlipper flipper = null;
    ScrollView scroll = null;
    CheckBox heartRateChkBx, walkChkBx;
    private GestureDetectorCompat gesturedetector = null;
    private RelativeLayout heartRateInitialLayout, walkInitialLayout;
    private EditText walkInitialValue, heartRateMeasurementEditTxt, heartRateBreakEditTxt;
    private RangeSeekBar heartRateRSB;
    private TextView heartRateNormalRangeTxt;
    private int[] heartRateParameters = new int[heartRateParsIndex.values().length];
    private int[] walkParameters = new int[walkParsIndex.values().length];
    private TextWatcher mTextWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {

        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (walkInitialValue.getText().toString().length() == 5) {
                walkInitialValue.clearFocus();
                IBinder mIBinder = MonitoringActivity.this.getCurrentFocus().getWindowToken();
                InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                mInputMethodManager.hideSoftInputFromWindow(mIBinder, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    };
    private RangeSeekBar.OnRangeChangedListener callback = new RangeSeekBar.OnRangeChangedListener() {
        @Override
        public void onRangeChanged(RangeSeekBar view, float min, float max) {
            heartRateNormalRangeTxt.setText(getResources().getString(R.string.heartRate_normalrange_item) + "\n" +
                    Integer.toString((int) min) + " - " + Integer.toString((int) max));
        }
    };
    private CheckBox.OnCheckedChangeListener checkboxListener = new CheckBox.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton btnView, boolean isChecked) {
            Log.d(TAG, "onCheckedChanged()");

            switch (btnView.getId()) {
                case R.id.heart_rate_checkbox:
                    isChoosed[monitoring.HEARTRATE.ordinal()] = isChecked;
                    showHeartRateSettings(isChecked);
                    if (isChecked) {
                        SensorService.regMonitoringSensor(Sensor.TYPE_HEART_RATE);
                    } else {
                        SensorService.unregMonitoringSensor(Sensor.TYPE_HEART_RATE);
                    }
                    break;
                case R.id.walk_checkbox:
                    isChoosed[monitoring.WALK.ordinal()] = isChecked;
                    showWalkSettings(isChecked);
                    if (isChecked) {
                        SensorService.regMonitoringSensor(Sensor.TYPE_STEP_DETECTOR);
                    } else {
                        SensorService.unregMonitoringSensor(Sensor.TYPE_STEP_DETECTOR);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring);

        Log.d(MainActivity.TAG, "MonitoringActivity::onCreate: X");

        flipper = (ViewFlipper) findViewById(R.id.settings_flipper);
        scroll = (ScrollView) findViewById(R.id.settings_scroller);
        heartRateChkBx = (CheckBox) findViewById(R.id.heart_rate_checkbox);
        heartRateInitialLayout = (RelativeLayout) findViewById(R.id.heartRate_initial_layout);
        heartRateNormalRangeTxt = (TextView) findViewById(R.id.heartRate_normalrange_item);
        heartRateRSB = (RangeSeekBar) findViewById(R.id.heartRate_normalrange_rsb);
        walkChkBx = (CheckBox) findViewById(R.id.walk_checkbox);
        walkInitialLayout = (RelativeLayout) findViewById(R.id.walk_initial_layout);
        walkInitialValue = (EditText) findViewById(R.id.walk_initial_value);

        GestureListener mygesture = new MonitoringActivity.MyGestureListener();
        gesturedetector = new GestureDetectorCompat(this, mygesture);

        heartRateChkBx.setOnCheckedChangeListener(checkboxListener);
        walkChkBx.setOnCheckedChangeListener(checkboxListener);

        Arrays.fill(isChoosed, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (this.getIntent().getIntExtra("heartRateInfo", -1) == View.VISIBLE) {
            float[] normalRange = SensorListener.getCurrentNormalRange();

            showHeartRateSettings(true);
            heartRateChkBx.setChecked(true);
        }
        if (this.getIntent().getIntExtra("walkInfo", -1) == View.VISIBLE) {
            showWalkSettings(true);
            walkChkBx.setChecked(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (walkInitialValue.getText().toString().length() == 0) {
            walkInitialValue.setText("0");
        }

        for (monitoring i : monitoring.values()) {
            Boolean isChecked = isChoosed[i.ordinal()];

            switch (i) {
                case HEARTRATE:
                    if (isChecked) {
                        SensorListener.setInitNormalRange(heartRateRSB.getCurrentRange());
                    }
                    heartRateParameters[heartRateParsIndex.VISIBLE.ordinal()] = isChecked ? View.VISIBLE : View.INVISIBLE;
                    sentSensorSettingsBroadcast("heartRateSettings", heartRateParameters);
                    break;
                case WALK:
                    if (isChecked) {
                        SensorListener.setInitStepValue(Float.valueOf(walkInitialValue.getText().toString()));
                    }
                    walkParameters[walkParsIndex.VISIBLE.ordinal()] = isChecked ? View.VISIBLE : View.INVISIBLE;
                    walkParameters[walkParsIndex.INITIALVALUE.ordinal()] = Integer.valueOf(walkInitialValue.getText().toString());
                    sentSensorSettingsBroadcast("walkSettings", walkParameters);
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(MainActivity.TAG, "MonitoringActivity::onTouchEvent: X");
        return gesturedetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(MainActivity.TAG, "MonitoringActivity::dispatchTouchEvent: X");
        super.dispatchTouchEvent(event);
        return gesturedetector.onTouchEvent(event);
    }

    private void onBackPage() {
        if (flipper == null) flipper = (ViewFlipper) findViewById(R.id.settings_flipper);
        int currindex = flipper.getDisplayedChild();
        if (currindex == DISPLAY_CHILD_MAIN) {
            goBack();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        }
    }

    private void showHeartRateSettings(Boolean shown) {
        if (shown) {
            float[] normalRange = SensorListener.getCurrentNormalRange();

            heartRateNormalRangeTxt.setText(getResources().getString(R.string.heartRate_normalrange_item) + "\n" +
                    Integer.toString((int) normalRange[0]) + " - " + Integer.toString((int) normalRange[1]));
            heartRateRSB.setValue(normalRange[0], normalRange[1]);
            heartRateInitialLayout.setVisibility(View.VISIBLE);
            heartRateRSB.setOnRangeChangedListener(callback);
        } else {
            heartRateInitialLayout.setVisibility(View.GONE);
        }
    }

    private void showWalkSettings(Boolean shown) {
        if (shown) {
            walkInitialValue.setText(Integer.toString((int) SensorListener.getCurrentStepValue()));
            walkInitialLayout.setVisibility(View.VISIBLE);
            walkInitialValue.addTextChangedListener(mTextWatcher);
        } else {
            walkInitialLayout.setVisibility(View.GONE);
        }
    }

    private void sentSensorSettingsBroadcast(String sensorSettings, int[] sensorParameters) {
        Intent intent = new Intent(MainActivity.ASKEY_ACTION_SENSOR_SETTINGS);
        intent.putExtra(sensorSettings, sensorParameters);
        sendBroadcast(intent);
    }

    private enum monitoring {HEARTRATE, WALK}

    public enum heartRateParsIndex {VISIBLE}

    public enum walkParsIndex {VISIBLE, INITIALVALUE}

    private class MyGestureListener extends GestureListener {
        int durationSlide = getResources().getInteger(R.integer.config_SlideAnimTime);

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            Log.d(MainActivity.TAG, "ControlCenterActivity::onSingleTapConfirmed: X");
            return super.onSingleTapConfirmed(e);
        }

        @Override
        protected void swipeUp() {
            Log.d(MainActivity.TAG, "MonitoringActivity::swipeUp: X");
        }

        @Override
        protected void swipeDown() {
            Log.d(MainActivity.TAG, "MonitoringActivity::swipeDown: X");
        }

        @Override
        protected void swipeLeft() {
            Log.d(MainActivity.TAG, "MonitoringActivity::swipeLeft: X");
        }

        @Override
        protected void swipeRight() {
            Log.d(MainActivity.TAG, "MonitoringActivity::swipeRight: X");
            onBackPage();
        }
    }
}
