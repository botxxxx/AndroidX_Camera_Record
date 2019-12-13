/*
 * Copyright (c) 2016-2017 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.watchhome.sensor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.PowerManager;
import android.util.Log;

//import android.os.AsyncResult;
//import android.os.Registrant;
//import android.os.RegistrantList;

public class SensorListener implements SensorEventListener {
    private static final String APP_TAG = "WatchHome";
    // jeff_wu@20190705, store the sensor settings parameters
    private static float[] normalRange = new float[]{60, 100};
    private static float stepDetector = 0;
    private Context context;
    private DisplayManager displayManager;
    private PowerManager powerManager;
    private PowerManager.WakeLock wakeLock;
    private long WAKE_LOCK_TIME_MS = 10;

    // gavin_tsao@20190703, add it
//    protected static RegistrantList mSensorChangedRegistrants = new RegistrantList();

    @SuppressLint("InvalidWakeLockTag")
    public SensorListener(Context context) {
        this.context = context;
        this.displayManager = (DisplayManager) context.getSystemService(
                context.DISPLAY_SERVICE);
        this.powerManager = (PowerManager) context.getSystemService(
                context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK |
                        PowerManager.ACQUIRE_CAUSES_WAKEUP, APP_TAG);
    }

    public static void setInitNormalRange(float[] initValue) {
        for (int i = 0; i < normalRange.length; i++) {
            normalRange[i] = initValue[i];
        }
    }

    public static float[] getCurrentNormalRange() {
        return normalRange;
    }

    public static void setInitStepValue(float initValue) {
        stepDetector = initValue;
    }

    public static float getCurrentStepValue() {
        return stepDetector;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
//        if (event.sensor.getType() == Sensor.TYPE_WRIST_TILT_GESTURE) {
//            Log.d(APP_TAG, "onSensorChanged. Received wake on tilt event");
//
//            for (Display display : displayManager.getDisplays()) {
//                int displayState = display.getState();
//                Log.d(APP_TAG, "displayState: " + displayState);
//
//                if (displayState != Display.STATE_ON && !isTheaterModeOn()) {
//                    wakeLock.acquire(WAKE_LOCK_TIME_MS);
//                }
//            }
//        } else if (event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
//            Log.d(APP_TAG, "onSensorChanged. Received heart rate on biosensor event");
//
//            //gavin_tsao@20190703, add it
//            if (mSensorChangedRegistrants != null) {
//                mSensorChangedRegistrants.notifyRegistrants(
//                        new AsyncResult(null, new val(Sensor.TYPE_HEART_RATE, event.values[0]), null));
//            }
//
//            // jeff_wu@20190823, vibrate when the heart rate is not in normal range
//            if (event.values[0] < normalRange[0] || event.values[0] > normalRange[1]) {
//                ((Vibrator) this.context.getSystemService(Service.VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
//            }
//        } else if (event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR) {
//            Log.d(APP_TAG, "onSensorChanged. Received heart rate on biosensor event");
//
//            if (event.values[0] == 1.0f) {
//                stepDetector++;
//            }
//
//            //gavin_tsao@20190703, add it
//            if (mSensorChangedRegistrants != null) {
//                mSensorChangedRegistrants.notifyRegistrants(
//                        new AsyncResult(null, new val(Sensor.TYPE_STEP_DETECTOR, stepDetector), null));
//            }
//        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(APP_TAG, "onAccuracyChanged. accuracy: " + accuracy);
    }

    private boolean isTheaterModeOn() {
//        return Settings.Global.getInt(context.getContentResolver(), Settings.Global.THEATER_MODE_ON, 0) == 1;
        return false;
    }

    // gavin_tsao@20190703, add it
    public void registerForSensorChanged(Handler h, int what, Object obj) {
//        mSensorChangedRegistrants.add(new Registrant(h, what, obj));
    }

    // gavin_tsao@20190703, add it
    public void unregisterForSensorChanged(Handler h) {
//        mSensorChangedRegistrants.remove(h);
    }

    // jeff_wu@20190715, store the sensor type and the sensor value
    public static class val {
        public int sensorType;
        public float sensorValue;

        public val(int type, float value) {
            sensorType = type;
            sensorValue = value;
        }
    }
}

