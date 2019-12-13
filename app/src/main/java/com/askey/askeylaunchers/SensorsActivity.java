package com.askey.askeylaunchers;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Locale;

public class SensorsActivity extends BaseActivity {
    private static final String TAG = "SensorsActivity";
    private static final int DISPLAY_CHILD_MAIN = 0;
    private static final int DISPLAY_CHILD_PRESSURE = 1;
    private static final int DISPLAY_CHILD_BIOSENSOR = 2;
    private static final int DISPLAY_CHILD_GPS = 3;
    private static final int DISPLAY_CHILD_GSENSOR = 4;
    private static final int DISPLAY_CHILD_GYROSCOPE = 5;
    private static final int DISPLAY_CHILD_PEDOMETER = 6;
    ViewFlipper flipper = null;
    ScrollView scroll = null;
    SensorManager sensorManager;
    Sensor sensor;
    private GestureDetectorCompat gesturedetector = null;
    private LocationManager locationManager;

    private float altitude = 0;
    private String valueStr = "";
    private float stepDetector = 0;
    private TextView textView;

    private SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int i) {

        }

        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            switch (sensorEvent.sensor.getType()) {
                case Sensor.TYPE_PRESSURE:
                    altitude = sensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, sensorEvent.values[0]);
                    valueStr = "Pressure：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[0]) + " hPa" +
                            "\nAltitude：" + String.format(Locale.getDefault(), "%.2f", altitude) + " meters";
                    break;
                case Sensor.TYPE_HEART_RATE:
                    valueStr = String.format(Locale.getDefault(), "%d", (int) sensorEvent.values[0]);
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    valueStr = "X：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[0]) +
                            "\nY：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[1]) +
                            "\nZ：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[2]);
                    break;
                case Sensor.TYPE_GYROSCOPE:
                    valueStr = "X：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[0]) +
                            "\nY：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[1]) +
                            "\nZ：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[2]);
                    break;
                case Sensor.TYPE_STEP_DETECTOR:
                    if (sensorEvent.values[0] == 1.0f)
                        valueStr = String.format(Locale.getDefault(), "%d", (int) (++stepDetector));
                    break;
                case Sensor.TYPE_LIGHT:
                    valueStr = "Light intensity：" + String.format(Locale.getDefault(), "%.2f", sensorEvent.values[0]);
                    break;
                default:
                    break;
            }

            textView.setText(valueStr);
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateGPSInfo(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensors);

        Log.d(MainActivity.TAG, "SettingsActivity::onCreate: X");

        flipper = (ViewFlipper) findViewById(R.id.sensors_flipper);
        scroll = (ScrollView) findViewById(R.id.sensors_scroller);

        GestureListener mygesture = new SensorsActivity.MyGestureListener();
        gesturedetector = new GestureDetectorCompat(this, mygesture);
    }

    private void goNextPage(int index) {
        if (flipper == null) flipper = (ViewFlipper) findViewById(R.id.settings_flipper);
        flipper.setInAnimation(this, R.anim.slide_from_right);
        flipper.setOutAnimation(this, R.anim.slide_to_left);
        flipper.setDisplayedChild(index);

        if (scroll == null) scroll = (ScrollView) findViewById(R.id.settings_scroller);
        scroll.fullScroll(View.FOCUS_UP);
    }

    private void goBackPage(int index) {
        if (flipper == null) flipper = (ViewFlipper) findViewById(R.id.settings_flipper);
        flipper.setInAnimation(this, R.anim.slide_from_left);
        flipper.setOutAnimation(this, R.anim.slide_to_right);
        flipper.setDisplayedChild(index);

        if (scroll == null) scroll = (ScrollView) findViewById(R.id.settings_scroller);
        scroll.fullScroll(View.FOCUS_UP);
    }

    private void registerSensor(int sensorType, int txtView) {
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(sensorType);
        textView = (TextView) findViewById(txtView);

        textView.setText("Listening");

        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void getPressure(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getPressure: X");

        registerSensor(Sensor.TYPE_PRESSURE, R.id.pressure_value);
        goNextPage(DISPLAY_CHILD_PRESSURE);
    }

    public void getBiosensor(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getBiosensor: X");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.BODY_SENSORS}, 1
            );
        }

        registerSensor(Sensor.TYPE_HEART_RATE, R.id.biosensor_value);
        goNextPage(DISPLAY_CHILD_BIOSENSOR);
    }

    private boolean isGpsAble(LocationManager lm) {
        return lm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ? true : false;
    }

    private void openGPS() {
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivityForResult(intent, 0);
    }

    private void updateGPSInfo(Location location) {
        if (location != null) {
            valueStr = "Longitude：" + location.getLongitude() +
                    "\nLatitude：" + location.getLatitude() +
                    "\nAltitude：" + location.getAltitude() +
                    "\nSpeed：" + location.getSpeed() +
                    "\nBearing：" + location.getBearing() +
                    "\nAccuracy：" + location.getAccuracy();
        }

        textView.setText(valueStr);
    }

    public void getGPS(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getGPS: X");

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        textView = (TextView) findViewById(R.id.gps_value);

        textView.setText("Listening");

        if (!isGpsAble(locationManager)) {
            Toast.makeText(this, "Please turn on the GPS.", Toast.LENGTH_SHORT).show();
            openGPS();
        }

        if (ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    (Activity) getBaseContext(),
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION}, 1
            );
        }

        String provider = LocationManager.GPS_PROVIDER;
        locationManager.requestLocationUpdates(provider, 2000, 8, locationListener);

        goNextPage(DISPLAY_CHILD_GPS);
    }

    public void getGsensor(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getGsensor: X");

        registerSensor(Sensor.TYPE_ACCELEROMETER, R.id.gsensor_value);
        goNextPage(DISPLAY_CHILD_GSENSOR);
    }

    public void getGyroscope(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getGyroscope: X");

        registerSensor(Sensor.TYPE_GYROSCOPE, R.id.gyroscope_value);
        goNextPage(DISPLAY_CHILD_GYROSCOPE);
    }

    public void getPedometer(View view) {
        Log.d(MainActivity.TAG, "SensorsActivity::getPedometer: X");

        registerSensor(Sensor.TYPE_STEP_DETECTOR, R.id.pedometer_value);
        goNextPage(DISPLAY_CHILD_PEDOMETER);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(MainActivity.TAG, "SettingsActivity::onTouchEvent: X");
        return gesturedetector.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        Log.d(MainActivity.TAG, "SettingsActivity::dispatchTouchEvent: X");
        super.dispatchTouchEvent(event);
        return gesturedetector.onTouchEvent(event);
    }

    private void onBackPage() {
        if (flipper == null) flipper = (ViewFlipper) findViewById(R.id.sensors_flipper);
        int currindex = flipper.getDisplayedChild();
        if (currindex == DISPLAY_CHILD_MAIN) {
            goBack();
            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        }
        if (currindex >= DISPLAY_CHILD_PRESSURE && currindex <= DISPLAY_CHILD_PEDOMETER) {
            switch (currindex) {
                case DISPLAY_CHILD_PRESSURE:
                case DISPLAY_CHILD_BIOSENSOR:
                case DISPLAY_CHILD_GSENSOR:
                case DISPLAY_CHILD_GYROSCOPE:
                case DISPLAY_CHILD_PEDOMETER:
                    sensorManager.unregisterListener(listener);
                    break;
                case DISPLAY_CHILD_GPS:
                    locationManager.removeUpdates(locationListener);
                    break;
                default:
                    break;
            }

            goBackPage(DISPLAY_CHILD_MAIN);
        }
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
            Log.d(MainActivity.TAG, "SettingsActivity::swipeUp: X");
        }

        @Override
        protected void swipeDown() {
            Log.d(MainActivity.TAG, "SettingsActivity::swipeDown: X");
        }

        @Override
        protected void swipeLeft() {
            Log.d(MainActivity.TAG, "SettingsActivity::swipeLeft: X");
        }

        @Override
        protected void swipeRight() {
            Log.d(MainActivity.TAG, "SettingsActivity::swipeRight: X");
            onBackPage();

        }
    }
}
