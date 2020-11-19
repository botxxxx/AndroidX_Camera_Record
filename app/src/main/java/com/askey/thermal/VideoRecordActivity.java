package com.askey.thermal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
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

import static com.askey.thermal.Utils.EXTRA_VIDEO_FAIL;
import static com.askey.thermal.Utils.EXTRA_VIDEO_RECORD;
import static com.askey.thermal.Utils.EXTRA_VIDEO_RESET;
import static com.askey.thermal.Utils.EXTRA_VIDEO_RUN;
import static com.askey.thermal.Utils.EXTRA_VIDEO_SUCCESS;
import static com.askey.thermal.Utils.Fail;
import static com.askey.thermal.Utils.LOG_TITLE;
import static com.askey.thermal.Utils.NO_SD_CARD;
import static com.askey.thermal.Utils.Success;
import static com.askey.thermal.Utils.TAG;
import static com.askey.thermal.Utils.delayTime;
import static com.askey.thermal.Utils.errorMessage;
import static com.askey.thermal.Utils.fCamera;
import static com.askey.thermal.Utils.firstCamera;
import static com.askey.thermal.Utils.firstFile;
import static com.askey.thermal.Utils.firstFilePath;
import static com.askey.thermal.Utils.getCalendarTime;
import static com.askey.thermal.Utils.getFail;
import static com.askey.thermal.Utils.getIsRun;
import static com.askey.thermal.Utils.getPath;
import static com.askey.thermal.Utils.getReset;
import static com.askey.thermal.Utils.getSDPath;
import static com.askey.thermal.Utils.getSdCard;
import static com.askey.thermal.Utils.getSuccess;
import static com.askey.thermal.Utils.isCameraOne;
import static com.askey.thermal.Utils.isError;
import static com.askey.thermal.Utils.isLastCamera;
import static com.askey.thermal.Utils.isReady;
import static com.askey.thermal.Utils.isRecord;
import static com.askey.thermal.Utils.isRun;
import static com.askey.thermal.Utils.logName;
import static com.askey.thermal.Utils.sCamera;
import static com.askey.thermal.Utils.sdData;
import static com.askey.thermal.Utils.secondCamera;
import static com.askey.thermal.Utils.secondFile;
import static com.askey.thermal.Utils.secondFilePath;
import static com.askey.thermal.Utils.tCamera;
import static com.askey.thermal.Utils.thirdCamera;
import static com.askey.thermal.Utils.thirdFile;
import static com.askey.thermal.Utils.thirdFilePath;
import static com.askey.thermal.Utils.videoLogList;

@SuppressLint("SetTextI18n")
public class VideoRecordActivity extends Activity {

    //-------------------------------------------------------------------------------
    public static final boolean Open_f_Camera = true, Open_s_Camera = true, Open_t_Camera = true;
    //-------------------------------------------------------------------------------
    // 啟用測試後顯示預覽畫面preview設置為 true
    public static boolean preview = false;
    // 使用SD Card儲存SD_Mode設置為 true
    public static boolean SD_Mode = true;
    // 使用錯誤重啟autoRestart設置為 true
    public static boolean autoRestart = true;
    // 使用自動停止錄影autoStopRecord設置為 true
    public static boolean autoStopRecord = true;
    public static boolean extraRecordStatus = false, onRestart = false;
    private final int delayMillis = 3000;
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    private static String codeDate0, codeDate1, codeDate2;
    private Size mPreviewSize = null;
    private TextureView mTextureView0, mTextureView1, mTextureView2;
    private CameraDevice mCameraDevice0, mCameraDevice1, mCameraDevice2;
    private CameraCaptureSession mPreviewSession0, mPreviewSession1, mPreviewSession2;
    private CameraDevice.StateCallback mStateCallback0, mStateCallback1, mStateCallback2;
    private MediaRecorder mMediaRecorder0, mMediaRecorder1, mMediaRecorder2;
    private Handler mainHandler, resetHandler;
    private Handler recordHandler0, stopRecordHandler0;
    private Handler recordHandler1, stopRecordHandler1;
    private Handler recordHandler2, stopRecordHandler2;
    private Handler backgroundHandler0, backgroundHandler1, backgroundHandler2;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float mLaptime = 0.0f;

    private void setRecord() {
        isRecord = true;
        if (extraRecordStatus) {
            isRun = onRun;
            Success = onSuccess;
            Fail = onFail;
        } else {
            onReset = 0;
            isRun = 0;
            Success = 0;
            Fail = 0;
        }
        firstFile = "";
        secondFile = "";
        thirdFile = "";
        firstFilePath.clear();
        secondFilePath.clear();
        thirdFilePath.clear();
        extraRecordStatus = true;
    }

    private void isRecordStart(boolean auto) {
        if (!isError && getSdCard) {
            if (isReady) {
                if (!isRecord) {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Start record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Start record", mLog.v));
                    setRecord();
                    takeRecord();
                } else {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Stop record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Stop record", mLog.v));
                    new Handler().post(() -> stopRecord(false));
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

        return permission(CAMERA) || permission(STORAGE);
    }

    @TargetApi(23)
    @SuppressLint("NewApi")
    private void showPermission() {
        videoLogList.add(new mLogMsg("#showPermission", mLog.v));
        // We don't have permission so prompt the user
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermissions(permissions.toArray(new String[0]), 0);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可授權
                setStart();
            } else {
                // 沒有權限
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
            //adb shell setprop persist.logd.logpersistd.size 1024
            //adb shell setprop persist.logd.logpersistd logcatd
            //adb shell setprop persist.our.camera.fps
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
        Log.d(TAG, "onConfigurationChanged: E");
        // 接收新裝置設定時不處理
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRun = 0;
        videoLogList = new ArrayList<>();
        //setProp();
        if (checkPermission()) {
            showPermission();
        } else {
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


    private CameraDevice.StateCallback setCallback(String cameraId) {
        try {
            CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
                public void onOpened(CameraDevice camera) {
                    Log.e(TAG, "onOpened Camera " + cameraId);
                    switch (cameraId) {
                        case firstCamera:
                            fCamera = true;
                            mCameraDevice0 = camera;
                            break;
                        case secondCamera:
                            sCamera = true;
                            mCameraDevice1 = camera;
                            break;
                        case thirdCamera:
                            tCamera = true;
                            mCameraDevice2 = camera;
                            break;
                    }
                    takePreview(cameraId);
                    videoLogList.add(new mLogMsg("Camera " + cameraId + " is opened.", mLog.i));
                    if (isLastCamera(cameraId) && !isReady) {
                        isReady = true;
                        new Handler().postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), 500);
                    }
                }

                public void onDisconnected(CameraDevice camera) {
                    Log.e(TAG, "onDisconnected Camera " + cameraId);
                    try {
                        camera.close();
                        videoLogList.add(new mLogMsg("Camera " + cameraId + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("closeCameraDevices " + cameraId + " close error.", true, e);
                    }
                }

                public void onError(CameraDevice camera, int error) {
                    Log.e(TAG, "onError Camera " + cameraId);
                    switch (cameraId) {
                        case firstCamera:
                            fCamera = false;
                            break;
                        case secondCamera:
                            sCamera = false;
                            break;
                        case thirdCamera:
                            tCamera = false;
                            break;
                    }
                    try {
                        camera.close();
                        videoLogList.add(new mLogMsg("Camera " + cameraId + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    errorMessage("Camera " + cameraId + " is error. <============ Crash here", true, null);
                }
            };
            return callback;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("CameraDevice.StateCallback " + cameraId + " error. <============ Crash here", true, e);
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
        videoLogList.add(new mLogMsg(msg, mLog.e));
        errorMessage = msg;
        new Handler().post(() -> stopRecordAndSaveLog(false));
        if (reset) {
            new Handler().postDelayed(this::restartApp, delayMillis);
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        if (isRecord)
            new Handler().post(() -> stopRecord(true));
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
        intent.putExtra(EXTRA_VIDEO_RESET, onReset);
        intent.putExtra(EXTRA_VIDEO_RECORD, extraRecordStatus);
        context.startActivity(intent);
    }

    @SuppressLint("HandlerLeak")
    private void initial() {
        getSdCard = !getSDPath().equals("");
        firstFilePath = new ArrayList<>();
        secondFilePath = new ArrayList<>();
        thirdFilePath = new ArrayList<>();
        // findViewById
        videoLogList.add(new mLogMsg("Initial now.", mLog.v));
        HandlerThread thread0 = new HandlerThread("CameraPreview0");
        thread0.start();
        HandlerThread thread1 = new HandlerThread("CameraPreview1");
        thread1.start();
        HandlerThread thread2 = new HandlerThread("CameraPreview2");
        thread2.start();
        backgroundHandler0 = new Handler(thread0.getLooper());
        backgroundHandler1 = new Handler(thread1.getLooper());
        backgroundHandler2 = new Handler(thread2.getLooper());
        mainHandler = new Handler(getMainLooper());
        recordHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(firstCamera);
                    videoLogList.add(new mLogMsg("startRecord " + firstCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new mLogMsg("startRecord " + firstCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        recordHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(secondCamera);
                    videoLogList.add(new mLogMsg("startRecord " + secondCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new mLogMsg("startRecord " + secondCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        recordHandler2 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(thirdCamera);
                    videoLogList.add(new mLogMsg("startRecord " + thirdCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new mLogMsg("startRecord " + thirdCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        stopRecordHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), firstCamera);
                        videoLogList.add(new mLogMsg("stopRecord " + firstCamera + ".", mLog.w));
                    } catch (Exception e) {
                        videoLogList.add(new mLogMsg("stopRecord " + firstCamera + " is error.", mLog.w));
                        e.printStackTrace();
                    }
            }
        };
        stopRecordHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), secondCamera);
                        videoLogList.add(new mLogMsg("stopRecord " + secondCamera + ".", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        videoLogList.add(new mLogMsg("stopRecord " + secondCamera + " is error.", mLog.w));
                    }
            }
        };
        stopRecordHandler2 = new Handler() {
            public void handleMessage(Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), thirdCamera);
                        videoLogList.add(new mLogMsg("stopRecord " + thirdCamera + ".", mLog.w));
                    } catch (Exception e) {
                        videoLogList.add(new mLogMsg("stopRecord " + thirdCamera + " is error.", mLog.w));
                        e.printStackTrace();
                    }
            }
        };
        codeDate0 = getCalendarTime();
        codeDate1 = getCalendarTime();
        codeDate2 = getCalendarTime();
        if (Open_f_Camera) {
            try {
                mStateCallback0 = setCallback(firstCamera);
                mTextureView0 = findViewById(R.id.surfaceView0);
                mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
                videoLogList.add(new mLogMsg("setCallback " + firstCamera + ".", mLog.w));
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("setCallback " + firstCamera + " is error.", mLog.w));
            }
        }
        if (Open_s_Camera) {
            try {
                mStateCallback1 = setCallback(secondCamera);
                mTextureView1 = findViewById(R.id.surfaceView1);
                mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));
                videoLogList.add(new mLogMsg("setCallback " + secondCamera + ".", mLog.w));
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("setCallback " + secondCamera + " is error.", mLog.w));
            }
        }
        if (Open_t_Camera) {
            try {
                mStateCallback2 = setCallback(thirdCamera);
                mTextureView2 = findViewById(R.id.surfaceView2);
                mTextureView2.setSurfaceTextureListener(new mSurfaceTextureListener(thirdCamera));
                videoLogList.add(new mLogMsg("setCallback " + thirdCamera + ".", mLog.w));
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("setCallback " + thirdCamera + " is error.", mLog.w));
            }
        }
        checkSdCardFromFileList();
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new mLogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
        });
        findViewById(R.id.record).setOnClickListener((View v) -> isRecordStart(false));
        ((TextView) findViewById(R.id.record_status)).setText(getSDPath().equals("") ? "Error" : "Ready");
        videoLogList.add(new mLogMsg("#initial complete", mLog.v));
        onRun = getIntent().getIntExtra(EXTRA_VIDEO_RUN, 0);
        onFail = getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        onReset = getIntent().getIntExtra(EXTRA_VIDEO_RESET, 0);
        onSuccess = getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);
        if (onReset != 0)
            videoLogList.add(new mLogMsg("#noReset:" + onReset, mLog.v));
        extraRecordStatus = getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        resetHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (extraRecordStatus) {
                    isRecordStart(true);
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
        if (getSdCard)
            if (real)
                list.add("CheckFile -> video_success/fail:(" + getSuccess() + "/" + getFail() + ") app_reset:(" + getReset() + ")");
            else
                list.add("App Version:" + this.getString(R.string.app_name));
        else list.add(NO_SD_CARD);

        if (!errorMessage.equals(""))
            list.add(errorMessage);
        else {
            if (Open_f_Camera && !fCamera)
                list.add("Camera Access error, Please check camera " + firstCamera + ". <============ Crash here");
            if (Open_s_Camera && !sCamera)
                list.add("Camera Access error, Please check camera " + secondCamera + ". <============ Crash here");
            if (Open_t_Camera && !tCamera)
                list.add("Camera Access error, Please check camera " + thirdCamera + ". <============ Crash here");
        }
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
            videoLogList.add(new mLogMsg("#takeRecord FrameRate: default", mLog.v));
            if (Open_f_Camera)
                recordHandler0.obtainMessage().sendToTarget();
            if (Open_s_Camera)
                recordHandler1.obtainMessage().sendToTarget();
            if (Open_t_Camera)
                recordHandler2.obtainMessage().sendToTarget();
            saveLog(getApplicationContext(), false, false);
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
        closeStateCallback(firstCamera);
        closeStateCallback(secondCamera);
        closeStateCallback(thirdCamera);
        closePreviewSession(firstCamera);
        closePreviewSession(secondCamera);
        closePreviewSession(thirdCamera);
        closeMediaRecorder(firstCamera);
        closeMediaRecorder(secondCamera);
        closeMediaRecorder(thirdCamera);
        new Handler().post(() -> stopRecordAndSaveLog(false));
    }

    private void openCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            videoLogList.add(new mLogMsg("#Open Camera" + cameraId + ".", mLog.w));
            Log.e(TAG, "camera ID: " + cameraId);
            if (isCameraOne(cameraId))
                mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                        .getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.e(TAG, isRun + " camera " + cameraId + " is open");
            switch (cameraId) {
                case firstCamera:
                    manager.openCamera(cameraId, mStateCallback0, mainHandler);
                    break;
                case secondCamera:
                    manager.openCamera(cameraId, mStateCallback1, mainHandler);
                    break;
                case thirdCamera:
                    manager.openCamera(cameraId, mStateCallback2, mainHandler);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("Camera Access error, Please check camera " + cameraId + ". <============ Crash here", false, e);
        }
    }

    private String getCodeDate(String cameraId) {
        String d = "";
        switch (cameraId) {
            case firstCamera:
                d = codeDate0;
                break;
            case secondCamera:
                d = codeDate1;
                break;
            case thirdCamera:
                d = codeDate2;
                break;
        }
        return d;
    }

    private void stopRecord(String date, String cameraId) {
        try {
            if (date.equals(getCodeDate(cameraId))) {
                isRun++;
                videoLogList.add(new mLogMsg("#stopRecord " + cameraId, mLog.v));
                Log.e(TAG, isRun + " stopRecord " + cameraId);
                if (isCameraOne(cameraId))
                    runOnUiThread(() -> {
                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        ((TextView) findViewById(R.id.record_status)).setText("Stop");
                    });
                switch (cameraId) {
                    case firstCamera:
                        codeDate0 = getCalendarTime();
                        break;
                    case secondCamera:
                        codeDate1 = getCalendarTime();
                        break;
                    case thirdCamera:
                        codeDate2 = getCalendarTime();
                        break;
                }
                closeMediaRecorder(cameraId);
                checkAndClear(cameraId);
                startRecord(cameraId);
                if (isError || !getSdCard) {
                    isRun = 0;
                    isRecord = false;
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("Camera " + cameraId + " stopRecord error. <============ Crash here", true, e);
        }
    }

    private void stopRecord(boolean reset) {
        try {
            if (!isError && getSdCard) {
                ArrayList<String> cameras = new ArrayList<>();
                if (Open_f_Camera)
                    cameras.add(firstCamera);
                if (Open_s_Camera)
                    cameras.add(secondCamera);
                if (Open_t_Camera)
                    cameras.add(thirdCamera);
                for (String cameraId : cameras) {
                    videoLogList.add(new mLogMsg("#stopRecord " + cameraId, mLog.v));
                    Log.e(TAG, isRun + " stopRecord " + cameraId);
                    if (isCameraOne(cameraId))
                        runOnUiThread(() -> {
                            if (mTimer != null) {
                                mTimer.cancel();
                                mTimer = null;
                            }
                            ((TextView) findViewById(R.id.record_status)).setText("Stop");
                        });
                    switch (cameraId) {
                        case firstCamera:
                            codeDate0 = getCalendarTime();
                            break;
                        case secondCamera:
                            codeDate1 = getCalendarTime();
                            break;
                        case thirdCamera:
                            codeDate2 = getCalendarTime();
                            break;
                    }
                    closeMediaRecorder(cameraId);
                    checkAndClear(cameraId);
                }
            }
            isRun = 0;
            isRecord = false;
            videoLogList.add(new mLogMsg("#Complete"));
            videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
            if (!reset)
                extraRecordStatus = false;
            saveLog(getApplicationContext(), false, false);
            if (preview) {
                takePreview();
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
                } catch (Exception ignored) {
                    if (null != videoLogList)
                        videoLogList.add(new mLogMsg("CheckFile error.", mLog.e));
                }
                if (frameRate != 0) Success++;
                else Fail++;
            } else {
                Fail++;
            }
            if (null != videoLogList)
                videoLogList.add(new mLogMsg("CheckFile:(" + path.split("/")[3] +
                        ") video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        ") app_reset:(" + getReset() + ")", mLog.i));
        } catch (Exception ignored) {
            if (null != videoLogList)
                videoLogList.add(new mLogMsg("CheckFile error.", mLog.e));
            Fail++;
        }
    }

    private void closeStateCallback(String cameraId) {
        try {
            switch (cameraId) {
                case firstCamera:
                    if (mStateCallback0 != null) {
                        mStateCallback0.onDisconnected(mCameraDevice0);
                    }
                    break;
                case secondCamera:
                    if (mStateCallback1 != null) {
                        mStateCallback1.onDisconnected(mCameraDevice1);
                    }
                    break;
                case thirdCamera:
                    if (mStateCallback2 != null) {
                        mStateCallback2.onDisconnected(mCameraDevice2);
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closeStateCallback" + cameraId + " is error.", true, e);
        }
    }

    private void closePreviewSession(String cameraId) {
        try {
            switch (cameraId) {
                case firstCamera:
                    if (mPreviewSession0 != null) {
                        mPreviewSession0.close();
                        mPreviewSession0 = null;
                    }
                    break;
                case secondCamera:
                    if (mPreviewSession1 != null) {
                        mPreviewSession1.close();
                        mPreviewSession1 = null;
                    }
                    break;
                case thirdCamera:
                    if (mPreviewSession2 != null) {
                        mPreviewSession2.close();
                        mPreviewSession2 = null;
                    }
                    break;
            }
        } catch (Exception e) {
            errorMessage("closePreviewSession" + cameraId + " is error.", false, e);
        }
    }

    private void closeMediaRecorder(String cameraId) {
        try {
            switch (cameraId) {
                case firstCamera:
                    if (mMediaRecorder0 != null) {
                        mMediaRecorder0.stop();
                        mMediaRecorder0.release();
                        videoLogList.add(new mLogMsg("Record " + cameraId + " finish."));
                    }
                    break;
                case secondCamera:
                    if (mMediaRecorder1 != null) {
                        mMediaRecorder1.stop();
                        mMediaRecorder1.release();
                        videoLogList.add(new mLogMsg("Record " + cameraId + " finish."));
                    }
                    break;
                case thirdCamera:
                    if (mMediaRecorder2 != null) {
                        mMediaRecorder2.stop();
                        mMediaRecorder2.release();
                        videoLogList.add(new mLogMsg("Record " + cameraId + " finish."));
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("mMediaRecorder " + cameraId + " is error.", false, e);
        }
    }

    private void startRecord(String cameraId) {
        if (!isError) {
            Log.e(TAG, isRun + " startRecord " + cameraId);
            try {
                if (isCameraOne(cameraId)) {
                    checkSdCardFromFileList();
                    runOnUiThread(() -> {
                        if (mTimer == null) {
                            onRun = getIsRun();
                            onFail = getFail();
                            onSuccess = getSuccess();
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
                    switch (cameraId) {
                        case firstCamera:
                            codeDate0 = getCalendarTime();
                            texture = mTextureView0.getSurfaceTexture();
                            break;
                        case secondCamera:
                            codeDate1 = getCalendarTime();
                            texture = mTextureView1.getSurfaceTexture();
                            break;
                        case thirdCamera:
                            codeDate2 = getCalendarTime();
                            texture = mTextureView2.getSurfaceTexture();
                            break;
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
                CameraDevice mCameraDevice = null;
                CaptureRequest.Builder mPreviewBuilder;
                Surface recorderSurface = null;
                Handler backgroundHandler = null;
                switch (cameraId) {
                    case firstCamera:
                        mCameraDevice = mCameraDevice0;
                        backgroundHandler = backgroundHandler0;
                        mMediaRecorder0 = setUpMediaRecorder(firstCamera);
                        if (mMediaRecorder0 == null)
                            return;
                        recorderSurface = mMediaRecorder0.getSurface();
                        break;
                    case secondCamera:
                        mCameraDevice = mCameraDevice1;
                        backgroundHandler = backgroundHandler1;
                        mMediaRecorder1 = setUpMediaRecorder(secondCamera);
                        if (mMediaRecorder1 == null)
                            return;
                        recorderSurface = mMediaRecorder1.getSurface();
                        break;
                    case thirdCamera:
                        mCameraDevice = mCameraDevice2;
                        backgroundHandler = backgroundHandler2;
                        mMediaRecorder2 = setUpMediaRecorder(thirdCamera);
                        if (mMediaRecorder2 == null)
                            return;
                        recorderSurface = mMediaRecorder2.getSurface();
                        break;
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
                                        if (isCameraOne(cameraId))
                                            mTimer.schedule(timerTask, 100, 100);
                                        switch (cameraId) {
                                            case firstCamera:
                                                mPreviewSession0 = session;
                                                //updatePreview(mPreviewBuilder, session, backgroundHandler0);
                                                if (mMediaRecorder0 != null)
                                                    mMediaRecorder0.start();
                                                Message msg0 = stopRecordHandler0.obtainMessage();
                                                msg0.obj = getCodeDate(cameraId);
                                                if (autoStopRecord)
                                                    stopRecordHandler0.sendMessageDelayed(msg0, delayTime);
                                                break;
                                            case secondCamera:
                                                mPreviewSession1 = session;
                                                //updatePreview(mPreviewBuilder, session, backgroundHandler1);
                                                if (mMediaRecorder1 != null)
                                                    mMediaRecorder1.start();
                                                Message msg1 = stopRecordHandler1.obtainMessage();
                                                msg1.obj = getCodeDate(cameraId);
                                                if (autoStopRecord)
                                                    stopRecordHandler1.sendMessageDelayed(msg1, delayTime + 250);
                                                break;
                                            case thirdCamera:
                                                mPreviewSession2 = session;
                                                //updatePreview(mPreviewBuilder, session, backgroundHandler2);
                                                if (mMediaRecorder2 != null)
                                                    mMediaRecorder2.start();
                                                Message msg2 = stopRecordHandler2.obtainMessage();
                                                msg2.obj = getCodeDate(cameraId);
                                                if (autoStopRecord)
                                                    stopRecordHandler2.sendMessageDelayed(msg2, delayTime + 500);
                                                break;
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
                            if (!(file.toString().equals(firstFile) || file.toString().equals(secondFile) || file.toString().equals(thirdFile)))
                                tmp.add(file.toString());
                    }
                    int video = 0;
                    if (Open_f_Camera)
                        video += 2;
                    if (Open_s_Camera)
                        video += 2;
                    if (Open_t_Camera)
                        video += 2;
                    if (tmp.size() >= video) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        for (int i = 0; i < video; i++)
                            delete((String) (list != null ? list[i] : null), SD_Mode);
                        checkSdCardFromFileList();
                    } else {
                        isError = true;
                        videoLogList.add(new mLogMsg("MP4 video file not found. <============ Crash here"));

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

    private void checkAndClear(String cameraId) {
        try {
            if (isRecord)
                switch (cameraId) {
                    case firstCamera:
                        if (Open_f_Camera && firstFilePath != null) {
                            for (String f : firstFilePath) {
                                checkFile(f);
                            }
                            firstFilePath.clear();
                        }
                        break;
                    case secondCamera:
                        if (Open_s_Camera && secondFilePath != null) {
                            for (String s : secondFilePath) {
                                checkFile(s);
                            }
                            secondFilePath.clear();
                        }
                        break;
                    case thirdCamera:
                        if (Open_t_Camera && thirdFilePath != null) {
                            for (String t : thirdFilePath) {
                                checkFile(t);
                            }
                            thirdFilePath.clear();
                        }
                        break;
                }
        } catch (Exception e) {
            videoLogList.add(new mLogMsg("CheckFile " + cameraId + " error.", mLog.e));
        }
    }

    private MediaRecorder setUpMediaRecorder(String cameraId) {
        String file = "";
        MediaRecorder mediaRecorder = null;
        try {
            if (!getSDPath().equals("")) {
                file = getSDPath() + getCalendarTime(cameraId) + ".mp4";
                videoLogList.add(new mLogMsg("Create: " + file.split("/")[3], mLog.w));
                switch (cameraId) {
                    case firstCamera:
                        firstFile = file;
                        firstFilePath.add(file);
                        break;
                    case secondCamera:
                        secondFile = file;
                        secondFilePath.add(file);
                        break;
                    case thirdCamera:
                        thirdFile = file;
                        thirdFilePath.add(file);
                        break;
                }
                //TODO Quality
                /* CamcorderProfile.QUALITY_HIGH:质量等级对应于最高可用分辨率*/// 1080p, 720p
                CamcorderProfile profile_480 = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
                CamcorderProfile profile_720 = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                CamcorderProfile profile_1080 = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
                CamcorderProfile profile = profile_480;
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setVideoEncoder(profile.videoCodec);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate / 4);
                mediaRecorder.setOutputFile(file);
                mediaRecorder.prepare();
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
            if (Open_f_Camera)
                takePreview(firstCamera);
            if (Open_s_Camera)
                takePreview(secondCamera);
            if (Open_t_Camera)
                takePreview(thirdCamera);
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private void takePreview(String cameraId) {
        Log.d(TAG, "takePreview");
        videoLogList.add(new mLogMsg("Preview " + cameraId + " Camera.", mLog.i));
        SurfaceTexture texture = null;
        try {
            switch (cameraId) {
                case firstCamera:
                    texture = mTextureView0.getSurfaceTexture();
                    break;
                case secondCamera:
                    texture = mTextureView1.getSurfaceTexture();
                    break;
                case thirdCamera:
                    texture = mTextureView2.getSurfaceTexture();
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("takePreview " + cameraId + " error. <============ Crash here", true, e);
        }
        if (null != texture) {
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            CaptureRequest.Builder mPreviewBuilder;
            CameraDevice mCameraDevice = null;
            Handler backgroundHandler = null;
            switch (cameraId) {
                case firstCamera:
                    mCameraDevice = mCameraDevice0;
                    backgroundHandler = backgroundHandler0;
                    break;
                case secondCamera:
                    mCameraDevice = mCameraDevice1;
                    backgroundHandler = backgroundHandler1;
                    break;
                case thirdCamera:
                    mCameraDevice = mCameraDevice2;
                    backgroundHandler = backgroundHandler2;
                    break;
            }
            if (!isError) {
                try {
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewBuilder.addTarget(surface);
                    Handler finalBackgroundHandler = backgroundHandler;
                    mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                            new CameraCaptureSession.StateCallback() {
                                public void onConfigured(CameraCaptureSession session) {
                                    switch (cameraId) {
                                        case firstCamera:
                                            mPreviewSession0 = session;
                                            break;
                                        case secondCamera:
                                            mPreviewSession1 = session;
                                            break;
                                        case thirdCamera:
                                            mPreviewSession2 = session;
                                            break;
                                    }
                                    updatePreview(mPreviewBuilder, session, finalBackgroundHandler);
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
        String cameraId;

        public mSurfaceTextureListener(String cameraId) {
            this.cameraId = cameraId;
        }

        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            openCamera(cameraId);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    }
}