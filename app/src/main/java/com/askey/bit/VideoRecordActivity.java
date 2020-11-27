package com.askey.bit;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.media.CamcorderProfile;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.StatFs;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.app.ActivityCompat;

import com.askey.widget.CustomTextView;
import com.askey.widget.HomeListen;
import com.askey.widget.mListAdapter;
import com.askey.widget.mLog;
import com.askey.widget.mLogMsg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static com.askey.bit.Utils.EXTRA_VIDEO_BT_FAIL;
import static com.askey.bit.Utils.EXTRA_VIDEO_BT_SUCCESS;
import static com.askey.bit.Utils.EXTRA_VIDEO_FAIL;
import static com.askey.bit.Utils.EXTRA_VIDEO_RECORD;
import static com.askey.bit.Utils.EXTRA_VIDEO_RESET;
import static com.askey.bit.Utils.EXTRA_VIDEO_RUN;
import static com.askey.bit.Utils.EXTRA_VIDEO_SUCCESS;
import static com.askey.bit.Utils.EXTRA_VIDEO_WIFI_FAIL;
import static com.askey.bit.Utils.EXTRA_VIDEO_WIFI_SUCCESS;
import static com.askey.bit.Utils.Fail;
import static com.askey.bit.Utils.LOG_TITLE;
import static com.askey.bit.Utils.NO_SD_CARD;
import static com.askey.bit.Utils.Success;
import static com.askey.bit.Utils.TAG;
import static com.askey.bit.Utils.btFail;
import static com.askey.bit.Utils.btSuccess;
import static com.askey.bit.Utils.checkConfigFile;
import static com.askey.bit.Utils.configName;
import static com.askey.bit.Utils.delayTime;
import static com.askey.bit.Utils.errorMessage;
import static com.askey.bit.Utils.fCamera;
import static com.askey.bit.Utils.firstCamera;
import static com.askey.bit.Utils.firstFile;
import static com.askey.bit.Utils.firstFilePath;
import static com.askey.bit.Utils.getBtFail;
import static com.askey.bit.Utils.getBtSuccess;
import static com.askey.bit.Utils.getCalendarTime;
import static com.askey.bit.Utils.getFail;
import static com.askey.bit.Utils.getIsRun;
import static com.askey.bit.Utils.getPath;
import static com.askey.bit.Utils.getReset;
import static com.askey.bit.Utils.getSDPath;
import static com.askey.bit.Utils.getSdCard;
import static com.askey.bit.Utils.getSuccess;
import static com.askey.bit.Utils.getWifiFail;
import static com.askey.bit.Utils.getWifiSuccess;
import static com.askey.bit.Utils.isCameraOne;
import static com.askey.bit.Utils.isError;
import static com.askey.bit.Utils.isFinish;
import static com.askey.bit.Utils.isReady;
import static com.askey.bit.Utils.isRecord;
import static com.askey.bit.Utils.isRun;
import static com.askey.bit.Utils.lastfirstCamera;
import static com.askey.bit.Utils.lastsecondCamera;
import static com.askey.bit.Utils.logName;
import static com.askey.bit.Utils.readConfigFile;
import static com.askey.bit.Utils.reformatConfigFile;
import static com.askey.bit.Utils.sCamera;
import static com.askey.bit.Utils.sdData;
import static com.askey.bit.Utils.secondCamera;
import static com.askey.bit.Utils.secondFile;
import static com.askey.bit.Utils.secondFilePath;
import static com.askey.bit.Utils.setConfigFile;
import static com.askey.bit.Utils.videoLogList;
import static com.askey.bit.Utils.wifiFail;
import static com.askey.bit.Utils.wifiSuccess;

@SuppressLint("SetTextI18n")
public class VideoRecordActivity extends Activity {
    private WifiManager wifiManager;
    private BluetoothAdapter mbtAdapter;
    // 啟用測試後顯示預覽畫面preview設置為 true
    public static boolean preview = false;
    // 啟用BurnIn測試burnInTest設置為 true
    public static boolean burnInTest = true;
    // 使用SD Card儲存SD_Mode設置為 true
    public static boolean SD_Mode = true;
    // 使用錯誤重啟autoRestart設置為 true
    public static boolean autoRestart = true;
    // 使用自動停止錄影autoStopRecord設置為 true
    public static boolean autoStopRecord = true;
    public static boolean extraRecordStatus = false, onRestart = false;
    private final int delayMillis = 3000;
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    public static int onWifiSuccess = 0, onWifiFail = 0, onBtSuccess = 0, onBtFail = 0;
    private static String codeDate0, codeDate1;
    private Size mPreviewSize = null;
    private TextureView mTextureView0, mTextureView1;
    private CameraDevice mCameraDevice0, mCameraDevice1;
    private CameraCaptureSession mPreviewSession0, mPreviewSession1;
    private CameraDevice.StateCallback mStateCallback0, mStateCallback1;
    private MediaRecorder mMediaRecorder0, mMediaRecorder1;
    private Handler mainHandler, resetHandler;
    private Handler recordHandler0, stopRecordHandler0, backgroundHandler0;
    private Handler recordHandler1, stopRecordHandler1, backgroundHandler1;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float mLaptime = 0.0f;

    private void setRecord() {
        isRecord = true;
        checkConfigFile(VideoRecordActivity.this, new File(getPath(), configName), false);
        if (extraRecordStatus) {
            isRun = onRun;
            Success = onSuccess;
            Fail = onFail;
            wifiSuccess = onWifiSuccess;
            wifiFail = onWifiFail;
            btSuccess = onBtSuccess;
            btFail = onBtFail;
        } else {
            onReset = 0;
            isRun = 0;
            Success = 0;
            Fail = 0;
            wifiSuccess = 0;
            wifiFail = 0;
            btSuccess = 0;
            btFail = 0;
        }
        firstFile = "";
        secondFile = "";
        firstFilePath.clear();
        secondFilePath.clear();
        extraRecordStatus = true;
    }

    private void isRecordStart(boolean auto, boolean testComplete) {
        if (!isError && getSdCard) {
            if (isReady) {
                if (!isRecord) {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Start record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Start record", mLog.v));
                    if (burnInTest)
                        videoLogList.add(new mLogMsg("#Wifi_BT Test", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#No Wifi_BT Test", mLog.v));
                    setRecord();
                    takeRecord();
                } else {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Stop record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Stop record", mLog.v));
                    new Handler().post(() -> stopRecord(preview, testComplete, false));
                }
            } else {
                showDialogLog(false);
                videoLogList.add(new mLogMsg("#Camera is not ready.", mLog.v));
            }
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private boolean checkPermission() {
        videoLogList.add(new mLogMsg("#checkPermission", mLog.v));
        int CAMERA = checkSelfPermission(Manifest.permission.CAMERA);
        int STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int INTERNET = checkSelfPermission(Manifest.permission.INTERNET);
        int BLUETOOTH = checkSelfPermission(Manifest.permission.BLUETOOTH);

        return permission(CAMERA) || permission(STORAGE) || permission(INTERNET) || permission(BLUETOOTH);
    }

    @TargetApi(23)
    @SuppressLint("NewApi")
    private void showPermission() {
        videoLogList.add(new mLogMsg("#showPermission", mLog.v));
        // We don't have permission so prompt the user
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.INTERNET);
        permissions.add(Manifest.permission.BLUETOOTH);
        requestPermissions(permissions.toArray(new String[0]), 0);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setStart();
            } else {
                showPermission();
                videoLogList.add(new mLogMsg("#no permissions!", mLog.e));
                videoLogList.add(new mLogMsg("No permissions!"));
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean permission(int mp) {
        return mp != PackageManager.PERMISSION_GRANTED;
    }

    private void setProp() {
        try {
            //TODO adb shell setprop persist.logd.logpersistd.size 1024
            //TODO adb shell setprop persist.logd.logpersistd logcatd
            if (!SystemProperties.get("persist.logd.logpersistd").equals("logcatd")) {
                Log.e(TAG, "persist.logd.logpersistd.size: 1024");
                SystemProperties.set("persist.logd.logpersistd.size", "1024");
                Log.e(TAG, "persist.logd.logpersistd: logcatd");
                SystemProperties.set("persist.logd.logpersistd", "logcatd");
            }
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new mLogMsg("logcatd error.", mLog.e));
            new Handler().post(() -> saveLog(this, false, false));
        }
    }

    @SuppressLint("InflateParams")
    private void setStart() {
        setContentView(R.layout.activity_video_record);
        setHomeListener();
        initial();
    }

    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.e(TAG, isRun + " onConfigurationChanged: E");
        // do nothing
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRun = 0;
        videoLogList = new ArrayList<>();
        setProp();
        if (checkPermission()) {
            showPermission();
        } else {
            checkConfigFile(this, true);
            setStart();
        }
    }

    public void onBackPressed() {
        videoLogList.add(new mLogMsg("@back", mLog.v));
        stopRecordAndSaveLog(true);
    }

    private void setHomeListener() {
        try {
            home = new HomeListen(this);
            home.setOnHomeBtnPressListener(new HomeListen.OnHomeBtnPressLitener() {
                public void onHomeBtnPress() {
                    videoLogList.add(new mLogMsg("@home", mLog.v));
                    stopRecordAndSaveLog(true);
                }

                public void onHomeBtnLongPress() {
                    videoLogList.add(new mLogMsg("@recent", mLog.v));
                    stopRecordAndSaveLog(true);
                }
            });
            home.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private CameraDevice.StateCallback setCallback(boolean CameraOne) {
        try {
            CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
                public void onOpened(CameraDevice camera) {
                    Log.e(TAG, "onOpened Camera " + (CameraOne ? firstCamera : secondCamera));
                    if (CameraOne) {
                        fCamera = true;
                        mCameraDevice0 = camera;
                    } else {
                        sCamera = true;
                        mCameraDevice1 = camera;
                    }
                    takePreview((CameraOne ? firstCamera : secondCamera));
                    videoLogList.add(new mLogMsg("Camera " + (CameraOne ? firstCamera : secondCamera) + " is opened.", mLog.i));
                    if (!CameraOne && !isReady) {
                        isReady = true;
                        new Handler().postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), 500);
                    }
                }

                public void onDisconnected(CameraDevice camera) {
                    Log.e(TAG, "onDisconnected Camera " + (CameraOne ? lastfirstCamera : lastsecondCamera));
                    try {
                        camera.close();
                        videoLogList.add(new mLogMsg("Camera " + (CameraOne ? lastfirstCamera : lastsecondCamera) + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("closeCameraDevices " + (CameraOne ? lastfirstCamera : lastsecondCamera) + " close error.", true, e);
                    }
                }

                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "onError Camera " + (CameraOne ? firstCamera : secondCamera));
                    if (CameraOne) {
                        fCamera = false;
                    } else {
                        sCamera = false;
                    }
                    try {
                        camera.close();
                        videoLogList.add(new mLogMsg("Camera " + (CameraOne ? firstCamera : secondCamera) + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    errorMessage("Camera " + (CameraOne ? firstCamera : secondCamera) + " is error. <============ Crash here", true, null);
                }
            };
            return callback;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("CameraDevice.StateCallback " + (CameraOne ? firstCamera : secondCamera) + " error. <============ Crash here", true, e);
            return null;
        }
    }

    private void errorMessage(String msg, boolean reset, Exception e) {
        if (e != null)
            Log.e(TAG, e.toString());
        getSdCard = !getSDPath().equals("");
        isError = true;
        isRecord = false;
        runOnUiThread(() -> ((TextView) findViewById(R.id.record_status)).setText("Error"));
        if (null != videoLogList)
            videoLogList.add(new mLogMsg(msg, mLog.e));
        errorMessage = msg;
        new Handler().post(() -> stopRecordAndSaveLog(false));
        if (reset) {
            new Handler().postDelayed(this::restartApp, delayMillis);
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        isFinish = 0;
        if (isRecord) stopRecord(false, false, true);
        saveLog(getApplicationContext(), false, kill);
    }

    private void restartApp() {
        try {
            home.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        onRestart = true;
        onReset++;
        Context context = getApplicationContext();
        Intent intent = restartActivity.createIntent(context);
        intent.putExtra(EXTRA_VIDEO_RUN, onRun);
        intent.putExtra(EXTRA_VIDEO_FAIL, onFail);
        intent.putExtra(EXTRA_VIDEO_SUCCESS, onSuccess);
        intent.putExtra(EXTRA_VIDEO_WIFI_FAIL, onWifiFail);
        intent.putExtra(EXTRA_VIDEO_WIFI_SUCCESS, onWifiSuccess);
        intent.putExtra(EXTRA_VIDEO_BT_FAIL, onBtFail);
        intent.putExtra(EXTRA_VIDEO_BT_SUCCESS, onBtSuccess);
        intent.putExtra(EXTRA_VIDEO_RESET, onReset);
        intent.putExtra(EXTRA_VIDEO_RECORD, extraRecordStatus);
        context.startActivity(intent);
    }

    @SuppressLint("HandlerLeak")
    private void initial() {
        try {
            wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new mLogMsg("wifiManager error.", mLog.e));
            errorMessage = "wifiManager error.";
        }
        try {
            mbtAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new mLogMsg("bluetoothAdapter error.", mLog.e));
            errorMessage = "bluetoothAdapter error.";
        }
        getSdCard = !getSDPath().equals("");
        firstFilePath = new ArrayList<>();
        secondFilePath = new ArrayList<>();
        // findViewById
        videoLogList.add(new mLogMsg("Initial now.", mLog.v));
        HandlerThread thread0 = new HandlerThread("CameraPreview0");
        thread0.start();
        HandlerThread thread1 = new HandlerThread("CameraPreview1");
        thread1.start();
        backgroundHandler0 = new Handler(thread0.getLooper());
        backgroundHandler1 = new Handler(thread1.getLooper());
        mainHandler = new Handler(getMainLooper());
        recordHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(firstCamera);
                    videoLogList.add(new mLogMsg("startRecord " + firstCamera + ".", mLog.w));
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("startRecord " + firstCamera + " is error.", mLog.w));
                }
            }
        };
        recordHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(secondCamera);
                    videoLogList.add(new mLogMsg("startRecord " + secondCamera + ".", mLog.w));
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("startRecord " + secondCamera + " is error.", mLog.w));
                }
            }
        };
        stopRecordHandler0 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), msg.arg1 + "");
                        videoLogList.add(new mLogMsg("stopRecord " + firstCamera + ".", mLog.w));
                    } catch (Exception e) {
                        videoLogList.add(new mLogMsg("stopRecord " + firstCamera + " is error.", mLog.w));
                        e.printStackTrace();
                    }
            }
        };
        stopRecordHandler1 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), msg.arg1 + "");
                        videoLogList.add(new mLogMsg("stopRecord " + secondCamera + ".", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        videoLogList.add(new mLogMsg("stopRecord " + secondCamera + " is error.", mLog.w));
                    }
            }
        };
        codeDate0 = getCalendarTime();
        codeDate1 = getCalendarTime();
        try {
            mStateCallback0 = setCallback(true);
            mTextureView0 = findViewById(R.id.surfaceView0);
            mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
            videoLogList.add(new mLogMsg("setCallback " + firstCamera + ".", mLog.w));
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new mLogMsg("setCallback " + firstCamera + " is error.", mLog.w));
        }
        try {
            mStateCallback1 = setCallback(false);
            mTextureView1 = findViewById(R.id.surfaceView1);
            mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));
            videoLogList.add(new mLogMsg("setCallback " + secondCamera + ".", mLog.w));
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new mLogMsg("setCallback " + secondCamera + " is error.", mLog.w));
        }
        checkSdCardFromFileList();
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new mLogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
        });
        findViewById(R.id.record).setOnClickListener((View v) -> isRecordStart(false, false));
        findViewById(R.id.setting).setOnClickListener((View v) -> {
            if (getSdCard && !isError) {
                videoLogList.add(new mLogMsg("@setting_show", mLog.v));
                View view = LayoutInflater.from(this).inflate(R.layout.layout_setting, null);
                final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
                view.findViewById(R.id.dialog_button_1).setOnClickListener((View vs) -> { // reset
                    videoLogList.add(new mLogMsg("@setting_reset", mLog.v));
                    setConfigFile(this, new File(getPath(), configName), view, true);
                    getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                            view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
                    setSetting();
                });
                view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // cancel
                    videoLogList.add(new mLogMsg("@setting_cancel", mLog.v));
                    dialog.dismiss();
                });
                view.findViewById(R.id.dialog_button_3).setOnClickListener((View vs) -> { // ok
                    videoLogList.add(new mLogMsg("@setting_ok", mLog.v));
                    setConfigFile(this, new File(getPath(), configName), view, false);
                    setSetting();
                    dialog.dismiss();
                });
                getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                        view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
                dialog.show();
            } else {
                showDialogLog(false);
            }
        });
        ((TextView) findViewById(R.id.record_status)).setText(getSDPath().equals("") ? "Error" : "Ready");
        videoLogList.add(new mLogMsg("#initial complete", mLog.v));
        onRun = getIntent().getIntExtra(EXTRA_VIDEO_RUN, 0);
        onFail = getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        onSuccess = getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);
        onReset = getIntent().getIntExtra(EXTRA_VIDEO_RESET, 0);
        onWifiFail = getIntent().getIntExtra(EXTRA_VIDEO_WIFI_FAIL, 0);
        onWifiSuccess = getIntent().getIntExtra(EXTRA_VIDEO_WIFI_SUCCESS, 0);
        onBtFail = getIntent().getIntExtra(EXTRA_VIDEO_BT_FAIL, 0);
        onBtSuccess = getIntent().getIntExtra(EXTRA_VIDEO_BT_SUCCESS, 0);
        if (onReset != 0)
            videoLogList.add(new mLogMsg("#noReset:" + onReset, mLog.v));
        extraRecordStatus = getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        resetHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (extraRecordStatus) {
                    isRecordStart(true, false);
                } else {
                    saveLog(getApplicationContext(), false, false);
                }
            }
        };
        this.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    videoLogList.add(new mLogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    saveLog(getApplicationContext(), false, false);
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void getSetting(Context context, EditText editText1, EditText editText2, EditText editText3, TextView editText4) {
        String input = readConfigFile(context, new File(getPath(), configName));
        if (input.length() > 0) {
            String[] read = input.split("\r\n");
            int t;
            String first = "firstCameraID = ", second = "secondCameraID = ";
            String code = "numberOfRuns = ", prop = "setProperty = ";
            for (String s : read)
                if (s.contains(first)) {
                    t = s.indexOf(first) + first.length();
                    first = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.contains(second)) {
                    t = s.indexOf(second) + second.length();
                    second = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.contains(code)) {
                    t = s.indexOf(code) + code.length();
                    code = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.contains(prop)) {
                    t = s.indexOf(prop) + prop.length();
                    prop = s.substring(t);
                    break;
                }
            editText1.setText(first);
            editText2.setText(second);
            editText3.setText(code);
            editText4.setText(prop);
        } else {
            videoLogList.add(new mLogMsg("Error reading config file."));
            reformatConfigFile(context, new File(getPath(), configName));
        }
    }

    private void setSetting() {
        if (!isRecord) {
            boolean[] check = checkConfigFile(this, new File(getPath(), configName), false);
            if (check[0]) {
                new Handler().post(() -> {
                    mStateCallback0.onDisconnected(mCameraDevice0);
                    mStateCallback1.onDisconnected(mCameraDevice1);
                    openCamera(firstCamera);
                    openCamera(secondCamera);
                });
            }
        } else {
            saveLog(getApplicationContext(), false, false);
        }
    }

    private void showDialogLog(boolean real) {
        videoLogList.add(new mLogMsg("#Log_show", mLog.v));
        View view = LayoutInflater.from(this).inflate(R.layout.layout_getlog, null);
        final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view).setCancelable(true).create();

        view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
            videoLogList.add(new mLogMsg("@Log_ok", mLog.v));
            dialog.dismiss();
        });
        ArrayList<String> list = new ArrayList<>();

        String bit = ") wifi_success/fail:(" + getWifiSuccess() + "/" + getWifiFail() +
                ") bt_success/fail:(" + getBtSuccess() + "/" + getBtFail();

        if (getSdCard)
            if (real)
                list.add("CheckFile -> video_success/fail:(" + getSuccess() + "/" + getFail() +
                        (burnInTest ? bit : "") + ") app_reset:(" + getReset() + ")");
            else
                list.add("App Version:" + this.getString(R.string.app_name));
        else list.add(NO_SD_CARD);
        if (!fCamera)
            list.add("Camera Access error, Please check camera " + firstCamera + ". <============ Crash here");
        if (!sCamera)
            list.add("Camera Access error, Please check camera " + secondCamera + ". <=========== Crash here");
        if ((!fCamera && firstCamera.equals("2")) || !sCamera && firstCamera.equals("2"))
            list.add("You can try Reboot device to walk up external camera.");
        if (!errorMessage.equals(""))
            list.add(errorMessage);
        if (list.size() > 0) {
            ArrayList<View> items = new ArrayList<>();
            for (String s : list) {
                @SuppressLint("InflateParams") View item_layout = LayoutInflater.from(this).inflate(R.layout.style_text_item, null);
                ((CustomTextView) item_layout.findViewById(R.id.customTextView)).setText(s);
                items.add(item_layout);
            }
            ((ListView) view.findViewById(R.id.dialog_listview)).setAdapter(new mListAdapter(items));
            ((ListView) view.findViewById(R.id.dialog_listview)).setSelection(items.size() - 1);
        }
        dialog.show();
    }

    private void takeRecord() {
        if (!isError && getSdCard) {
            videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
            videoLogList.add(new mLogMsg("#takeRecord FrameRate:" + 14, mLog.v));
            int delay = 0;
            if (!lastfirstCamera.equals(firstCamera) || !lastsecondCamera.equals(secondCamera)) {
                lastfirstCamera = firstCamera; // String
                lastsecondCamera = secondCamera;
                new Handler().post(() -> {
                    mStateCallback0.onDisconnected(mCameraDevice0);
                    mStateCallback1.onDisconnected(mCameraDevice1);
                    openCamera(firstCamera);
                    openCamera(secondCamera);
                });
                delay = 1000;
            }
            recordHandler0.obtainMessage().sendToTarget();
            recordHandler1.obtainMessage().sendToTarget();
            new Handler().postDelayed(() -> saveLog(getApplicationContext(), false, false), delay);
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    public static void saveLog(Context context, boolean reFormat, boolean kill) {
        if (null != videoLogList)
            if (!getSDPath().equals("")) {
                String version = context.getString(R.string.app_name);
                StringBuilder logString;
                assert videoLogList != null;
                File file = new File(getPath(), logName);
                if (!file.exists()) {
                    logString = new StringBuilder(LOG_TITLE + version + "\r\n");
                    try {
                        file.createNewFile();
                        if (null != videoLogList)
                            videoLogList.add(new mLogMsg("Create the log file.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (null != videoLogList)
                            videoLogList.add(new mLogMsg("Create file failed.", mLog.w));
                    }
                } else {
                    logString = new StringBuilder();
                }
                if (null != videoLogList)
                    try {
                        for (mLogMsg logs : videoLogList) {
                            String time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                    + " run:" + logs.runTime + " -> ";
                            logString.append(time).append(logs.msg).append("\r\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getPath(), logName), !reFormat);
                    output.write(logString.toString().getBytes());
                    output.close();
                    videoLogList.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                isError = true;
                getSdCard = !getSDPath().equals("");
            }
        if (kill)
            android.os.Process.killProcess(android.os.Process.myPid());
    }


    protected void onDestroy() {
        super.onDestroy();
        isFinish = 0;
        closeStateCallback(firstCamera);
        closeStateCallback(secondCamera);
        closePreviewSession(firstCamera);
        closePreviewSession(secondCamera);
        closeMediaRecorder(firstCamera);
        closeMediaRecorder(secondCamera);
        new Handler().post(() -> stopRecordAndSaveLog(false));
    }

    private void openCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.e(TAG, isRun + " camera ID: " + cameraId);
            if (isCameraOne(cameraId))
                mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                        .getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.e(TAG, isRun + " camera " + cameraId + " is open");
            manager.openCamera(cameraId, isCameraOne(cameraId) ? mStateCallback0 : mStateCallback1, mainHandler);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("Camera Access error, Please check camera " + cameraId + ". <============ Crash here", false, e);
        }
    }

    private String getCodeDate(String CameraID) {
        return (isCameraOne(CameraID)) ? codeDate0 : codeDate1;
    }

    private void stopRecord(String date, String cameraID) {
        try {
            if (date.equals(getCodeDate(cameraID))) {
                isRun++;
                videoLogList.add(new mLogMsg("#stopRecord " + cameraID, mLog.v));
                Log.e(TAG, isRun + " stopRecord " + cameraID);
                if (isCameraOne(cameraID)) {
                    runOnUiThread(() -> {
                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        ((TextView) findViewById(R.id.record_status)).setText("Stop");
                    });
                    codeDate0 = getCalendarTime();
                    closeMediaRecorder(firstCamera);
                } else {
                    codeDate1 = getCalendarTime();
                    closeMediaRecorder(secondCamera);
                }
                try {
                    checkAndClear(cameraID);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("Check file is fail."));
                }
                if (isFinish == 999 || isRun < isFinish - (isCameraOne(cameraID) ? 1 : 0)) {
                    startRecord(cameraID);
                } else {
                    if (!isCameraOne(cameraID)) isRecordStart(true, true);
                }
                if (isError || !getSdCard) {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("Camera " + cameraID + " stopRecord error. <============ Crash here", true, e);
        }
    }

    private void stopRecord(boolean preview, boolean testComplete, boolean reset) {
        try {
            if (!isError && getSdCard) {
                if (testComplete) {
                    if (getFail() + getReset() == 0) {
                        videoLogList.add(new mLogMsg("#Pass"));
                        ((TextView) findViewById(R.id.record_status)).setText("Pass");
                    } else {
                        videoLogList.add(new mLogMsg("#Fail"));
                        ((TextView) findViewById(R.id.record_status)).setText("Fail " + getFail() + getReset());
                    }
                    showDialogLog(true);
                } else {
                    for (String cameraID : new String[]{firstCamera, secondCamera}) {
                        videoLogList.add(new mLogMsg("#stopRecord " + cameraID, mLog.v));
                        Log.e(TAG, isRun + " stopRecord " + cameraID);
                        try {
                            if (isCameraOne(cameraID)) {
                                runOnUiThread(() -> {
                                    if (mTimer != null) {
                                        mTimer.cancel();
                                        mTimer = null;
                                    }
                                    ((TextView) findViewById(R.id.record_status)).setText("Stop");
                                });
                                codeDate0 = getCalendarTime();
                                closeMediaRecorder(firstCamera);
                            } else {
                                codeDate1 = getCalendarTime();
                                closeMediaRecorder(secondCamera);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorMessage("Camera " + cameraID + " closeMediaRecorder error. <============ Crash here", true, e);
                        }
                        checkAndClear(cameraID);
                    }
                }
                isRun = 0;
                isFinish = 0;
                isRecord = false;
                videoLogList.add(new mLogMsg("#Complete"));
                videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
                if (!reset)
                    extraRecordStatus = false;
                saveLog(getApplicationContext(), false, false);
                if (preview) {
                    takePreview();
                }
            } else {
                errorMessage("stopRecord all camera !isError && getSdCard. <============ Crash here", true, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("stopRecord all camera error. <============ Crash here", true, e);
        }
    }

    public static int getFrameRate(File file) {
        int frameRate = 0;
        if (!getSDPath().equals("")) {
            try {
                MediaExtractor extractor;
                FileInputStream fis;
                try {
                    fis = new FileInputStream(file);
                    extractor = new MediaExtractor();
                    extractor.setDataSource(fis.getFD());
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("getFrameRate failed on MediaExtractor.<============ Crash here", mLog.e));
                    return 0;
                }
                int numTracks = extractor.getTrackCount();
                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        try {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                        } catch (Exception e) {
                            e.printStackTrace();
                            videoLogList.add(new mLogMsg("getFrameRate failed on MediaExtractor.<============ Crash here", mLog.e));
                        }
                    }
                }
                extractor.release();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("getFrameRate failed.<============ Crash here", mLog.e));
            }
        } else {
            videoLogList.add(new mLogMsg("getFrameRate failed " + NO_SD_CARD + ".", mLog.e));
        }
        return frameRate;
    }

    public static void checkFile(String path) {
        try {
            File video = new File(path);
            int frameRate = 0;
            if (video.exists()) {
                try {
                    frameRate = getFrameRate(video);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (frameRate != 0) Success++;
                else Fail++;
            } else {
                Fail++;
                if (null != videoLogList)
                    videoLogList.add(new mLogMsg("video not exists.", mLog.e));
            }
            String bit = ") wifi_success/fail:(" + getWifiSuccess() + "/" + getWifiFail() +
                    ") bt_success/fail:(" + getBtSuccess() + "/" + getBtFail();
            if (null != videoLogList)
                videoLogList.add(new mLogMsg("CheckFile:(" + path.split("/")[3] +
                        ") video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        (burnInTest ? bit : "") + ") app_reset:(" + getReset() + ")", mLog.i));
        } catch (Exception e) {
            e.printStackTrace();
            Fail++;
            if (null != videoLogList)
                videoLogList.add(new mLogMsg("CheckFile error.", mLog.e));
        }
    }

    private void closeStateCallback(String cameraId) {
        try {
            if (isCameraOne(cameraId)) {
                if (mStateCallback0 != null) {
                    mStateCallback0.onDisconnected(mCameraDevice0);
                }
            } else {
                if (mStateCallback1 != null) {
                    mStateCallback1.onDisconnected(mCameraDevice1);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closeStateCallback" + cameraId + " is error.", true, e);
        }
    }

    private void closePreviewSession(String cameraId) {
        try {
            if (isCameraOne(cameraId)) {
                if (mPreviewSession0 != null) {
                    mPreviewSession0.close();
                    mPreviewSession0 = null;
                }
            } else {
                if (mPreviewSession1 != null) {
                    mPreviewSession1.close();
                    mPreviewSession1 = null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closePreviewSession" + cameraId + " is error.", false, e);
        }
    }

    private void closeMediaRecorder(String cameraId) {
        try {
            if (isCameraOne(cameraId)) {
                if (mMediaRecorder0 != null) {
                    mMediaRecorder0.stop();
                    mMediaRecorder0.release();
                    videoLogList.add(new mLogMsg("Record " + cameraId + " finish."));
                }
            } else {
                if (mMediaRecorder1 != null) {
                    mMediaRecorder1.stop();
                    mMediaRecorder1.release();
                    videoLogList.add(new mLogMsg("Record " + cameraId + " finish."));
                }
            }
        } catch (Exception e) {
            errorMessage("mMediaRecorder " + cameraId + " is error.", false, e);
            e.printStackTrace();
        }
    }

    private void startRecord(String cameraId) {
        if (!isError) {
            Log.e(TAG, isRun + " startRecord " + cameraId);
            try {
                if (isCameraOne(cameraId)) {
                    if (burnInTest)
                        wifiEnableOrDisable();
                    codeDate0 = getCalendarTime();
                } else {
                    if (burnInTest)
                        btEnableOrDisable();
                    codeDate1 = getCalendarTime();
                }

                if (isCameraOne(cameraId)) {
                    checkSdCardFromFileList();
                    runOnUiThread(() -> {
                        if (mTimer == null) {
                            onRun = getIsRun();
                            onFail = getFail();
                            onSuccess = getSuccess();
                            if (burnInTest) {
                                onWifiFail = getWifiFail();
                                onWifiSuccess = getWifiSuccess();
                                onBtFail = getBtFail();
                                onBtSuccess = getBtSuccess();
                            }
                            timerTask = new mTimerTask();
                            mLaptime = 0.0f;
                            mTimer = new Timer(true);
                            ((TextView) findViewById(R.id.record_timer)).setText("00");
                            ((TextView) findViewById(R.id.record_status)).setText("Recording");
                        }
                    });
                }
                closePreviewSession(cameraId);
                SurfaceTexture texture = null;
                try {
                    if (isCameraOne(cameraId)) {
                        texture = mTextureView0.getSurfaceTexture();
                    } else {
                        texture = mTextureView1.getSurfaceTexture();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("getSurfaceTexture" + cameraId + " is error."));
                }
                if (null == texture) {
                    Log.e(TAG, isRun + " texture is null, return");
                    return;
                }
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);
                List<Surface> surfaces = new ArrayList<>();
                CameraDevice mCameraDevice;
                CaptureRequest.Builder mPreviewBuilder;
                Surface recorderSurface;
                Handler backgroundHandler;
                if (isCameraOne(cameraId)) {
                    backgroundHandler = backgroundHandler0;
                    mCameraDevice = mCameraDevice0;
                    mMediaRecorder0 = setUpMediaRecorder(firstCamera);
                    recorderSurface = mMediaRecorder0.getSurface();
                } else {
                    backgroundHandler = backgroundHandler1;
                    mCameraDevice = mCameraDevice1;
                    mMediaRecorder1 = setUpMediaRecorder(secondCamera);
                    recorderSurface = mMediaRecorder1.getSurface();
                }
                surfaces.add(surface);
                surfaces.add(recorderSurface);
                mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                mPreviewBuilder.addTarget(surface);
                mPreviewBuilder.addTarget(recorderSurface);
                try {
                    mCameraDevice.createCaptureSession(surfaces,
                            new CameraCaptureSession.StateCallback() {
                                public void onConfigured(CameraCaptureSession session) {
                                    try {
                                        // Camera is ready.
                                        if (isCameraOne(cameraId)) {
                                            mPreviewSession0 = session;
                                        } else {
                                            mPreviewSession1 = session;
                                        }
                                        updatePreview(mPreviewBuilder, session, backgroundHandler);
                                        if (isCameraOne(cameraId)) {
                                            mTimer.schedule(timerTask, 100, 100);
                                            if (mMediaRecorder0 != null)
                                                mMediaRecorder0.start();
                                            Message msg = stopRecordHandler0.obtainMessage();
                                            msg.arg1 = Integer.parseInt(cameraId);
                                            msg.obj = getCodeDate(cameraId);
                                            if (autoStopRecord)
                                                stopRecordHandler0.sendMessageDelayed(msg, delayTime);
                                        } else {
                                            if (mMediaRecorder1 != null)
                                                mMediaRecorder1.start();
                                            Message msg = stopRecordHandler1.obtainMessage();
                                            msg.arg1 = Integer.parseInt(cameraId);
                                            msg.obj = getCodeDate(cameraId);
                                            if (autoStopRecord)
                                                stopRecordHandler1.sendMessageDelayed(msg, delayTime + 500);
                                        }
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                        errorMessage("Camera " + cameraId + " can't record. <============ Crash here", false, e);
                                    }
                                }

                                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                                    errorMessage("Camera " + cameraId + " Record onConfigureFailed.", true, null);
                                }
                            }, backgroundHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage("Camera " + cameraId + " CameraCaptureSession.StateCallback() error. <============ Crash here", true, e);
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage("Camera " + cameraId + " startRecord error. <============ Crash here", true, e);
            }
        } else {
            if (autoRestart) {
                new Handler().postDelayed(this::restartApp, delayMillis);
            }
        }
    }

    private void checkSdCardFromFileList() {
        getSdCard = !getSDPath().equals("");
        if (getSdCard) {
            try {
                StatFs stat = new StatFs(getSDPath());
                long sdAvailSize = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                if (gigaAvailable < sdData) {
                    videoLogList.add(new mLogMsg("SD Card is Full."));
                    ArrayList<String> tmp = new ArrayList<>();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (File file : fileList) {
                        if (!file.isDirectory() && Utils.getFileExtension(file.toString()).equals("mp4"))
                            if (!(file.toString().equals(firstFile) || file.toString().equals(secondFile)))
                                tmp.add(file.toString());
                    }
                    if (tmp.size() >= 4) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        for (int i = 0; i < 4; i++)
                            delete((String) (list != null ? list[i] : null), SD_Mode);
                        checkSdCardFromFileList();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!getSDPath().equals("")) {
                    errorMessage("error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", false, null);
                } else {
                    errorMessage(NO_SD_CARD, false, null);
                }
            }
        } else {
            errorMessage(NO_SD_CARD, false, null);
        }
    }

    private void delete(String path, boolean fromSDcard) {
        File video = new File(path);
        try {
            if (!path.equals("")) {
                if (video.exists()) {
                    if (fromSDcard)
                        videoLogList.add(new mLogMsg("Delete: " + path.split("/")[3], mLog.w));
                    else
                        videoLogList.add(new mLogMsg("Delete: " + path.split("/")[5], mLog.w));
                    video.delete();
                } else {
                    videoLogList.add(new mLogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("#delete " + video.getName() + " error. <============ Crash here", false, e);
        }
    }

    private void checkAndClear(String cameraID) {
        try {
            if ((isCameraOne(cameraID) ? firstFilePath : secondFilePath) != null) {
                if (isRecord)
                    for (String f : isCameraOne(cameraID) ? firstFilePath : secondFilePath) {
                        checkFile(f);
                    }
                (isCameraOne(cameraID) ? firstFilePath : secondFilePath).clear();
            }
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new mLogMsg("CheckFile " + cameraID + " error.", mLog.e));
        }
    }

    private MediaRecorder setUpMediaRecorder(String cameraId) {
        boolean micError = false;
        MediaRecorder mediaRecorder = null;
        try {
            String file = "";
            if (!getSDPath().equals("")) {
                file = getSDPath() + getCalendarTime(isCameraOne(cameraId)) + ".mp4";
                videoLogList.add(new mLogMsg("Create: " + file.split("/")[3], mLog.w));
                if (isCameraOne(cameraId)) {
                    firstFile = file + "";
                    firstFilePath.add(file);
                } else {
                    secondFile = file + "";
                    secondFilePath.add(file);
                }
                /* CamcorderProfile.QUALITY_HIGH:质量等级对应于最高可用分辨率*/// 1080p, 720p
                CamcorderProfile profile_720 = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                CamcorderProfile profile_1080 = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
                // Step 1: Unlock and set camera to MediaRecorder
                CamcorderProfile profile = profile_1080;
                mediaRecorder = new MediaRecorder();
                // Step 2: Set sources
                if (isCameraOne(cameraId)) {
                    try {
                        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    } catch (Exception e) {
                        micError = true;
                    }
                }
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                if (isCameraOne(cameraId) && !micError) {
                    try {
                        mediaRecorder.setAudioEncoder(profile.audioCodec);
                    } catch (Exception e) {
                        micError = true;
                    }
                }
                mediaRecorder.setVideoEncoder(profile.videoCodec);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                if (isCameraOne(cameraId) && !micError) {
                    try {
                        mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
                    } catch (Exception e) {
                        micError = true;
                    }
                }
                mediaRecorder.setVideoEncodingBitRate((int) (profile.videoBitRate / 3.3));
                if (isCameraOne(cameraId) && !micError) {
                    try {
                        mediaRecorder.setAudioChannels(profile.audioChannels);
                        mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
                    } catch (Exception e) {
                        micError = true;
                    }
                }
                // Step 3: Set video fps
                mediaRecorder.setVideoFrameRate(10); // 1 -> 12fps, 10 -> 16fps
                // Step 4: Set output file
                mediaRecorder.setOutputFile(file);
                // Step 5: Prepare configured MediaRecorder
                mediaRecorder.prepare();
                if (micError)
                    videoLogList.add(new mLogMsg("MediaRecorder MIC error. <============ Crash here", mLog.e));
            } else {
                errorMessage("MediaRecorder error. " + NO_SD_CARD + " <============ Crash here", false, null);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("MediaRecorder " + cameraId + " error. <============ Crash here", false, e);
        }
        return mediaRecorder;
    }

    private void takePreview() {
        if (!isError && getSdCard) {
            openCamera(firstCamera);
            new Handler().postDelayed(() -> openCamera(secondCamera), 1000);
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private void takePreview(String cameraId) {
        Log.e(TAG, isRun + " takePreview " + cameraId);
        videoLogList.add(new mLogMsg("Preview " + cameraId + " Camera.", mLog.i));
        SurfaceTexture texture = null;
        try {
            texture = isCameraOne(cameraId) ? mTextureView0.getSurfaceTexture() : mTextureView1.getSurfaceTexture();
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("takePreview " + cameraId + " error. <============ Crash here", true, e);
        }
        if (null != texture) {
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            CaptureRequest.Builder mPreviewBuilder;
            CameraDevice mCameraDevice;
            Handler backgroundHandler;
            if (isCameraOne(cameraId)) {
                backgroundHandler = backgroundHandler0;
                mCameraDevice = mCameraDevice0;
            } else {
                backgroundHandler = backgroundHandler1;
                mCameraDevice = mCameraDevice1;
            }
            if (!isError) {
                try {
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewBuilder.addTarget(surface);
                    // CaptureRequest.CONTROL_MODE
                    mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                            new CameraCaptureSession.StateCallback() {
                                public void onConfigured(CameraCaptureSession session) {
                                    if (isCameraOne(cameraId)) {
                                        mPreviewSession0 = session;
                                    } else {
                                        mPreviewSession1 = session;
                                    }
                                    updatePreview(mPreviewBuilder, session, backgroundHandler);
                                }

                                public void onConfigureFailed(CameraCaptureSession session) {
                                    runOnUiThread(() ->
                                            errorMessage("Preview " + cameraId + " onConfigureFailed", true, null));
                                }
                            }, backgroundHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage("Preview " + cameraId + " error.", true, e);
                }
            }
        }
    }

    protected void updatePreview(CaptureRequest.Builder mPreviewBuilder, CameraCaptureSession mPreviewSession, Handler backgroundHandler) {
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("setCaptureRequest error.", true, e);
        }
    }

    private class mTimerTask extends TimerTask {
        public void run() {
            // mHandlerを通じてUI Threadへ処理をキューイング
            runOnUiThread(() -> {
                //実行間隔分を加算処理
                mLaptime += 0.1d;
                //計算にゆらぎがあるので小数点第1位で丸める
                BigDecimal bi = new BigDecimal(mLaptime);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
                if (autoStopRecord && outputValue >= 65) {
                    errorMessage("Application has timed out.", true, null);
                }
                //現在のLapTime
                ((TextView) findViewById(R.id.record_timer)).setText(Float.toString(outputValue));
            });
        }
    }

    private class mSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        String CameraID;

        public mSurfaceTextureListener(String CameraID) {
            this.CameraID = CameraID;
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(CameraID);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }

    private void wifiEnableOrDisable() {
        if (wifiManager != null) {
            try {
                wifiManager.setWifiEnabled(WifiManager.WIFI_STATE_ENABLED != wifiManager.getWifiState());
                wifiSuccess++;
            } catch (Exception e) {
                e.printStackTrace();
                wifiFail++;
                videoLogList.add(new mLogMsg("Error wifiEnableOrDisable fail."));
            }
        } else {
            try {
                wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                wifiManager.setWifiEnabled(WifiManager.WIFI_STATE_ENABLED != wifiManager.getWifiState());
                wifiSuccess++;
            } catch (Exception e) {
                e.printStackTrace();
                wifiFail++;
                videoLogList.add(new mLogMsg("Error wifiEnableOrDisable fail."));
            }
        }
    }

    private void btEnableOrDisable() {
        if (mbtAdapter != null) {
            try {
                if (!mbtAdapter.isEnabled()) {
                    mbtAdapter.enable();
                } else {
                    mbtAdapter.disable();
                }
                btSuccess++;
            } catch (Exception e) {
                e.printStackTrace();
                btFail++;
                videoLogList.add(new mLogMsg("Error btEnableOrDisable fail."));
            }
        } else {
            try {
                mbtAdapter = BluetoothAdapter.getDefaultAdapter();
                if (!mbtAdapter.isEnabled()) {
                    mbtAdapter.enable();
                } else {
                    mbtAdapter.disable();
                }
                btSuccess++;
            } catch (Exception e) {
                e.printStackTrace();
                btFail++;
                videoLogList.add(new mLogMsg("Error btEnableOrDisable fail."));
            }
        }
    }
}