/*
 * Copyright (c) 2016 Qualcomm Technologies, Inc.
 * All Rights Reserved.
 * Confidential and Proprietary - Qualcomm Technologies, Inc.
 */

package com.qualcomm.qti.watchhome.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.util.Log;

public class SensorManager {
    private static final String APP_TAG = "WatchHome";

    private Context context;
    private android.hardware.SensorManager sensorManager;

    public SensorManager(Context context) {
        this.context = context;
    }

    public Sensor initializeSensor(int type) {

        sensorManager = (android.hardware.SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

        if (sensorManager.getDefaultSensor(type) != null) {
            return sensorManager.getDefaultSensor(type);
        } else {
            return null;
        }
    }

    public void registerListener(Sensor sensor, SensorListener sensorListener) {
        // jeff_wu@20190705, registering the sensor depend on the sensor type
        if (sensor != null) {
            switch (sensor.getType()) {
//                case Sensor.TYPE_WRIST_TILT_GESTURE:
//                    sensorManager.registerListener(sensorListener, sensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
//                    Log.d(APP_TAG, "Registering wake on sensor listener");
//                    break;
                case Sensor.TYPE_HEART_RATE:
                    sensorManager.registerListener(sensorListener, sensor, android.hardware.SensorManager.SENSOR_DELAY_NORMAL);
                    Log.d(APP_TAG, "Registering the biosensor");
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    sensorManager.registerListener(sensorListener, sensor, android.hardware.SensorManager.SENSOR_DELAY_FASTEST);
                    Log.d(APP_TAG, "Registering the pedometer");
                    break;
                default:
                    break;
            }
        }

    }

    public void removeListener(Sensor sensor, SensorListener sensorListener) {
        // jeff_wu@20190705, Un-registering the sensor
        /*if (sensor != null) {
            sensorManager.unregisterListener(sensorListener, sensor);
            Log.d(APP_TAG, "Un-registering wake on sensor listener");
        }*/
        if (sensor != null) {
            sensorManager.unregisterListener(sensorListener, sensor);
            switch (sensor.getType()) {
//                case Sensor.TYPE_WRIST_TILT_GESTURE:
//                    Log.d(APP_TAG, "Un-registering wake on sensor listener");
//                    break;
                case Sensor.TYPE_HEART_RATE:
                    Log.d(APP_TAG, "Un-registering the biosensor");
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    Log.d(APP_TAG, "Un-registering the pedometer");
                    break;
                default:
                    break;
            }
        }
    }
}

