/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.watchhome.sensor;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.os.IBinder;
import android.util.Log;

public class SensorService extends Service {
    private static final String APP_TAG = "WatchHome";

    private static SensorManager sensorManager;
    private static SensorListener sensorListener;

    //    private Sensor tiltSensor;
    private static Sensor bioSensor;
    private static Sensor pedometer;

    //gavin_tsao@20
    public static SensorListener getSensorListenerInstance() {
        if (sensorListener != null)
            return sensorListener;

        return null;
    }

    public static void regMonitoringSensor(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_HEART_RATE:
                bioSensor = sensorManager.initializeSensor(Sensor.TYPE_HEART_RATE);
                if (sensorManager != null)
                    sensorManager.registerListener(bioSensor, sensorListener);
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                pedometer = sensorManager.initializeSensor(Sensor.TYPE_STEP_DETECTOR);
                if (sensorManager != null)
                    sensorManager.registerListener(pedometer, sensorListener);
                break;
            default:
                break;
        }
    }

    public static void unregMonitoringSensor(int sensorType) {
        switch (sensorType) {
            case Sensor.TYPE_HEART_RATE:
                sensorManager.removeListener(bioSensor, sensorListener);
                break;
            case Sensor.TYPE_STEP_DETECTOR:
                sensorManager.removeListener(pedometer, sensorListener);
                break;
            default:
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // started on boot from a BroadcastReceiver
        Log.d(APP_TAG, " SensorService onStartCommand ");

        sensorManager = new SensorManager(this.getApplicationContext());
//        tiltSensor = sensorManager.initializeSensor(Sensor.TYPE_WRIST_TILT_GESTURE);

        sensorListener = new SensorListener(this);
        if (sensorManager != null) {
//            sensorManager.registerListener(tiltSensor, sensorListener);
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
//            sensorManager.removeListener(tiltSensor, sensorListener);
        }

        super.onDestroy();
    }
}
