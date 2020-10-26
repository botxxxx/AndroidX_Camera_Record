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
import android.hardware.camera2.CameraAccessException;
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
import com.askey.widget.LogMsg;
import com.askey.widget.mListAdapter;
import com.askey.widget.mLog;

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
import static com.askey.thermal.Utils.NEW_FRAME_RATE;
import static com.askey.thermal.Utils.NO_SD_CARD;
import static com.askey.thermal.Utils.Success;
import static com.askey.thermal.Utils.TAG;
import static com.askey.thermal.Utils.delayTime;
import static com.askey.thermal.Utils.errorMessage;
import static com.askey.thermal.Utils.fCamera;
import static com.askey.thermal.Utils.firstCamera;
import static com.askey.thermal.Utils.firstFile;
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
import static com.askey.thermal.Utils.isFinish;
import static com.askey.thermal.Utils.isFrame;
import static com.askey.thermal.Utils.isLastCamera;
import static com.askey.thermal.Utils.isReady;
import static com.askey.thermal.Utils.isRecord;
import static com.askey.thermal.Utils.isRun;
import static com.askey.thermal.Utils.logName;
import static com.askey.thermal.Utils.sCamera;
import static com.askey.thermal.Utils.sdData;
import static com.askey.thermal.Utils.secondCamera;
import static com.askey.thermal.Utils.secondFile;
import static com.askey.thermal.Utils.tCamera;
import static com.askey.thermal.Utils.thirdCamera;
import static com.askey.thermal.Utils.thirdFile;
import static com.askey.thermal.Utils.videoLogList;

@SuppressLint("SetTextI18n")
public class VideoRecordActivity extends Activity {

    //-------------------------------------------------------------------------------
    public static final boolean Open_f_Camera = true, Open_s_Camera = true, Open_t_Camera = true;
    //-------------------------------------------------------------------------------
    private final int delayMillis = 3000;
    // 使用SD Card儲存 SD_Mode 設置為 true
    public static boolean SD_Mode = true;
    // 使用錯誤重啟 autoRestart 設置為 true
    public static boolean autoRestart = true;
    // 使用每分鐘停止錄影 autoStopRecord 設置為 true
    public static boolean autoStopRecord = true;
    public static boolean extraRecordStatus = false, onRestart = false;
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
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
        extraRecordStatus = true;
    }

    private void isRecordStart(boolean auto) {
        if (!isError && getSdCard) {
            if (isReady) {
                if (!isRecord) {
                    if (!auto)
                        videoLogList.add(new LogMsg("@Start record", mLog.v));
                    else
                        videoLogList.add(new LogMsg("#Start record", mLog.v));
                    setRecord();
                    takeRecord();
                } else {
                    if (!auto)
                        videoLogList.add(new LogMsg("@Stop record", mLog.v));
                    else
                        videoLogList.add(new LogMsg("#Stop record", mLog.v));
                    isFinish = 0;
                    new Handler().post(() -> stopRecord(true));
                }
            } else {
                videoLogList.add(new LogMsg("#Camera is not ready.", mLog.v));
            }
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private boolean checkPermission() {
        videoLogList.add(new LogMsg("#checkPermission", mLog.v));
        int CAMERA = checkSelfPermission(Manifest.permission.CAMERA);
        int STORAGE = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);

        return permission(CAMERA) || permission(STORAGE);
    }

    @TargetApi(23)
    @SuppressLint("NewApi")
    private void showPermission() {
        videoLogList.add(new LogMsg("#showPermission", mLog.v));
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
                videoLogList.add(new LogMsg("#no permissions!", mLog.e));
                videoLogList.add(new LogMsg("No permissions!"));
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean permission(int mp) {
        return mp != PackageManager.PERMISSION_GRANTED;
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
        if (checkPermission()) {
            showPermission();
        } else {
            setStart();
        }
    }

    public void onBackPressed() {
        videoLogList.add(new LogMsg("@back", mLog.v));
        stopRecordAndSaveLog(true);
    }

    private void setHomeListener() {
        home = new HomeListen(this);
        home.setOnHomeBtnPressListener(new HomeListen.OnHomeBtnPressLitener() {
            public void onHomeBtnPress() {
                videoLogList.add(new LogMsg("@home", mLog.v));
                stopRecordAndSaveLog(true);
            }

            public void onHomeBtnLongPress() {
                videoLogList.add(new LogMsg("@recent", mLog.v));
                stopRecordAndSaveLog(true);
            }
        });
        home.start();
    }

    private CameraDevice.StateCallback setCallback(String cameraId) {
        try {
            CameraDevice.StateCallback callback = new CameraDevice.StateCallback() {
                public void onOpened(CameraDevice camera) {
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
                    if (!isReady) {
                        Log.e(TAG, "onOpened");
                        videoLogList.add(new LogMsg("Camera " + cameraId + " is opened.", mLog.i));
                        if (isLastCamera(cameraId)) {
                            isReady = true;
                            new Handler().postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), 500);
                        }
                    }
                }

                public void onDisconnected(CameraDevice camera) {
                    try {
                        switch (cameraId) {
                            case firstCamera:
                                fCamera = false;
                                mCameraDevice0.close();
                                break;
                            case secondCamera:
                                sCamera = false;
                                mCameraDevice1.close();
                                break;
                            case thirdCamera:
                                tCamera = false;
                                mCameraDevice2.close();
                                break;
                        }
                        Log.e(TAG, "onDisconnected");
                        videoLogList.add(new LogMsg("Camera " + cameraId + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        isError = true;
                        videoLogList.add(new LogMsg("Camera " + cameraId + " close error.", mLog.w));
                        new Handler().post(() -> stopRecordAndSaveLog(false));
                        errorMessage = "Camera " + cameraId + " close error.";
                        ((TextView) findViewById(R.id.record_status)).setText("Error");
                    }
                    if (autoRestart && isError) {
                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                    }
                }

                public void onError(CameraDevice camera, int error) {
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
                    isError = true;
                    onDisconnected(camera);
                    Log.e(TAG, "onError");
                    closePreviewSession(cameraId);
                    videoLogList.add(new LogMsg("Camera " + cameraId + " is error. <============ Crash here", mLog.e));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Camera " + cameraId + " is error. <============ Crash here";
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                }
            };
            return callback;
        } catch (Exception e) {
            e.printStackTrace();
            getSdCard = !getSDPath().equals("");
            isError = true;
            videoLogList.add(new LogMsg("CameraDevice.StateCallback " + cameraId + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "CameraDevice.StateCallback " + cameraId + " error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
            return null;
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        isFinish = 0;
        if (isRecord) stopRecord(false);
        saveLog(getApplicationContext(), false, kill);
    }

    private void restartApp(boolean record) {
        try {
            home.stop();
        } catch (Exception ignored) {
        }
        onRestart = true;
        onReset++;
        Context context = getApplicationContext();
        Intent intent = restartActivity.createIntent(context);
        intent.putExtra(EXTRA_VIDEO_RUN, onRun);
        intent.putExtra(EXTRA_VIDEO_FAIL, onFail);
        intent.putExtra(EXTRA_VIDEO_SUCCESS, onSuccess);
        intent.putExtra(EXTRA_VIDEO_RESET, onReset);
        intent.putExtra(EXTRA_VIDEO_RECORD, record);
        context.startActivity(intent);
    }

    @SuppressLint("HandlerLeak")
    private void initial() {
        getSdCard = !getSDPath().equals("");
        // findViewById
        videoLogList.add(new LogMsg("Initial now.", mLog.v));
        HandlerThread thread0 = new HandlerThread("CameraPreview0");
        thread0.start();
        HandlerThread thread1 = new HandlerThread("CameraPreview1");
        thread1.start();
        HandlerThread thread2 = new HandlerThread("CameraPreview2");
        thread2.start();
        resetHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (!extraRecordStatus) {
                    saveLog(getApplicationContext(), false, false);
                } else {
                    isRecordStart(true);
                }
            }
        };
        backgroundHandler0 = new Handler(thread0.getLooper());
        backgroundHandler1 = new Handler(thread1.getLooper());
        backgroundHandler2 = new Handler(thread2.getLooper());
        mainHandler = new Handler(getMainLooper());
        recordHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(firstCamera);
                    videoLogList.add(new LogMsg("startRecord " + firstCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("startRecord " + firstCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        recordHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(secondCamera);
                    videoLogList.add(new LogMsg("startRecord " + secondCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("startRecord " + secondCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        recordHandler2 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    startRecord(thirdCamera);
                    videoLogList.add(new LogMsg("startRecord " + thirdCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("startRecord " + thirdCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        stopRecordHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    stopRecord(firstCamera);
                    videoLogList.add(new LogMsg("stopRecord " + firstCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("stopRecord " + firstCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        stopRecordHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    stopRecord(secondCamera);
                    videoLogList.add(new LogMsg("stopRecord " + secondCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("stopRecord " + secondCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        stopRecordHandler2 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    stopRecord(thirdCamera);
                    videoLogList.add(new LogMsg("stopRecord " + thirdCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("stopRecord " + thirdCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        Handler previewHandler0 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    mStateCallback0 = setCallback(firstCamera);
                    videoLogList.add(new LogMsg("mStateCallback " + firstCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mStateCallback " + firstCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        Handler previewHandler1 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    mStateCallback1 = setCallback(secondCamera);
                    videoLogList.add(new LogMsg("mStateCallback " + secondCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mStateCallback " + secondCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        Handler previewHandler2 = new Handler() {
            public void handleMessage(Message msg) {
                try {
                    mStateCallback2 = setCallback(thirdCamera);
                    videoLogList.add(new LogMsg("mStateCallback " + thirdCamera + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mStateCallback " + thirdCamera + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        };
        if (Open_f_Camera) {
            previewHandler0.obtainMessage().sendToTarget();
            mTextureView0 = findViewById(R.id.surfaceView0);
            mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
        }
        if (Open_s_Camera) {
            previewHandler1.obtainMessage().sendToTarget();
            mTextureView1 = findViewById(R.id.surfaceView1);
            mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));
        }
        if (Open_t_Camera) {
            previewHandler2.obtainMessage().sendToTarget();
            mTextureView2 = findViewById(R.id.surfaceView2);
            mTextureView2.setSurfaceTextureListener(new mSurfaceTextureListener(thirdCamera));
        }
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new LogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
        });
        findViewById(R.id.record).setOnClickListener((View v) -> isRecordStart(false));
        ((TextView) findViewById(R.id.record_status)).setText(getSDPath().equals("") ? "Error" : "Ready");
        videoLogList.add(new LogMsg("#initial complete", mLog.v));
        onRun = getIntent().getIntExtra(EXTRA_VIDEO_RUN, 0);
        onFail = getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        onReset = getIntent().getIntExtra(EXTRA_VIDEO_RESET, 0);
        onSuccess = getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);
        if (onReset != 0)
            videoLogList.add(new LogMsg("#noReset:" + onReset, mLog.v));
        extraRecordStatus = getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        this.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    videoLogList.add(new LogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    saveLog(getApplicationContext(), false, false);
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void showDialogLog(boolean real) {
        if (!isRecord || real) {
            videoLogList.add(new LogMsg("#dialog_log", mLog.v));
            View view = LayoutInflater.from(this).inflate(R.layout.layout_getlog, null);
            final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                    .setView(view).setCancelable(true).create();
            view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
                videoLogList.add(new LogMsg("@log_ok", mLog.v));
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
                if (!fCamera)
                    list.add("Camera Access error, Please check camera " + firstCamera + ". <============ Crash here");
                if (!sCamera)
                    list.add("Camera Access error, Please check camera " + secondCamera + ". <============ Crash here");
                if (!tCamera)
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
    }

    private void takeRecord() {
        if (!isError && getSdCard) {
            videoLogList.add(new LogMsg("#------------------------------", mLog.v));
            videoLogList.add(new LogMsg("#takeRecord FrameRate:" + NEW_FRAME_RATE[isFrame], mLog.v));
            if (Open_f_Camera)
                recordHandler0.obtainMessage().sendToTarget();
            if (Open_s_Camera)
                recordHandler1.obtainMessage().sendToTarget();
            if (Open_t_Camera)
                recordHandler2.obtainMessage().sendToTarget();
            new Handler().postDelayed(() -> saveLog(getApplicationContext(), false, false), 3000);
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    public static void saveLog(Context context, boolean reFormat, boolean kill) {
        if (!getSDPath().equals("")) {
            String version = context.getString(R.string.app_name);
            StringBuilder logString;
            assert videoLogList != null;
            File file = new File(getPath(), logName);
            if (!file.exists()) {
                logString = new StringBuilder(LOG_TITLE + version + "\r\n");
                try {
                    file.createNewFile();
                    videoLogList.add(new LogMsg("Create the log file.", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("Create file failed.", mLog.w));
                    e.printStackTrace();
                }
            } else {
                logString = new StringBuilder();
            }

            for (LogMsg logs : videoLogList) {
                String time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                        + " run:" + logs.runTime + " -> ";
                logString.append(time).append(logs.msg).append("\r\n");
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
        Log.e(TAG, "openCamera E");
        try {
            Log.e(TAG, "camera ID: " + cameraId);
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.e(TAG, "camera open");
            switch (cameraId) {
                case firstCamera:
                    if (mStateCallback0 != null)
                        manager.openCamera(cameraId, mStateCallback0, mainHandler);
                    else
                        videoLogList.add(new LogMsg("mStateCallback0 is null. <============ Crash here", mLog.e));
                    break;
                case secondCamera:
                    if (mStateCallback1 != null)
                        manager.openCamera(cameraId, mStateCallback1, mainHandler);
                    else
                        videoLogList.add(new LogMsg("mStateCallback1 is null. <============ Crash here", mLog.e));
                    break;
                case thirdCamera:
                    if (mStateCallback2 != null)
                        manager.openCamera(cameraId, mStateCallback2, mainHandler);
                    else
                        videoLogList.add(new LogMsg("mStateCallback2 is null. <============ Crash here", mLog.e));
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("Camera Access error, Please check camera " + cameraId + ". <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "Camera Access error, Please check camera " + cameraId + ". <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
        }
        Log.e(TAG, "openCamera X");
    }

    private void stopRecord(String cameraID) {
        try {
            isRun++;
            if (isCameraOne(cameraID)) {
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
                ((TextView) findViewById(R.id.record_status)).setText("Stop");
                videoLogList.add(new LogMsg("#stopRecord", mLog.v));
                Log.d(TAG, "stopRecord");
            }
            try {
                switch (cameraID) {
                    case firstCamera:
                        if (mMediaRecorder0 != null) {
                            mMediaRecorder0.stop();
                            mMediaRecorder0.release();
                            videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                        }
                        break;
                    case secondCamera:
                        if (mMediaRecorder1 != null) {
                            mMediaRecorder1.stop();
                            mMediaRecorder1.release();
                            videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                        }
                        break;
                    case thirdCamera:
                        if (mMediaRecorder2 != null) {
                            mMediaRecorder2.stop();
                            mMediaRecorder2.release();
                            videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                        }
                        break;
                }

            } catch (Exception e) {
                videoLogList.add(new LogMsg("mMediaRecorder" + cameraID + " is error."));
            }
            try {
                checkAndClear(cameraID);
            } catch (Exception e) {
                videoLogList.add(new LogMsg("Check file is fail."));
            }
            if (isFinish == 999 || isRun <= isFinish) {
                startRecord(cameraID);
            } else {
                showDialogLog(true);
                isRecordStart(true);
            }
            if (isError || !getSdCard) {
                isRun = 0;
                isFinish = 0;
                isRecord = false;
                extraRecordStatus = false;
                ((TextView) findViewById(R.id.record_status)).setText("Error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("Camera " + cameraID + " stopRecord error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "Camera " + cameraID + " stopRecord error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
    }

    private void stopRecord(boolean preview) {
        try {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (!isError && getSdCard) {
                ((TextView) findViewById(R.id.record_status)).setText("Stop");
                if (isRecord) {
                    videoLogList.add(new LogMsg("#stopRecord", mLog.v));
                    Log.d(TAG, "stopRecord");
                    for (String cameraID : new String[]{firstCamera, secondCamera, thirdCamera}) {
                        try {
                            switch (cameraID) {
                                case firstCamera:
                                    if (mMediaRecorder0 != null) {
                                        mMediaRecorder0.stop();
                                        mMediaRecorder0.release();
                                        videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                                    }
                                    break;
                                case secondCamera:
                                    if (mMediaRecorder1 != null) {
                                        mMediaRecorder1.stop();
                                        mMediaRecorder1.release();
                                        videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                                    }
                                    break;
                                case thirdCamera:
                                    if (mMediaRecorder2 != null) {
                                        mMediaRecorder2.stop();
                                        mMediaRecorder2.release();
                                        videoLogList.add(new LogMsg("Record " + cameraID + " finish."));
                                    }
                                    break;
                            }
                            checkAndClear(cameraID);
                        } catch (Exception e) {
                            videoLogList.add(new LogMsg("mMediaRecorder" + cameraID + " is error."));
                        }
                    }
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    extraRecordStatus = false;
                    videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                    videoLogList.add(new LogMsg("#Complete"));
                    ((TextView) findViewById(R.id.record_status)).setText("Complete");
                } else {
                    isRun = 0;
                    isFinish = 0;
                }
                end(preview);
            } else {
                ((TextView) findViewById(R.id.record_status)).setText("Error");
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("stopRecord all camera error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "stopRecord all camera error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
    }

    private void end(boolean preview) {
        saveLog(getApplicationContext(), false, false);
        if (preview) {
            takePreview();
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
                } catch (Exception ignored) {
                    videoLogList.add(new LogMsg("getFrameRate failed on MediaExtractor.<============ Crash here", mLog.e));
                    return 0;
                }
                int numTracks = extractor.getTrackCount();
                for (int i = 0; i < numTracks; i++) {
                    MediaFormat format = extractor.getTrackFormat(i);
                    if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                        try {
                            frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                        } catch (Exception ignored) {
                            videoLogList.add(new LogMsg("getFrameRate failed on MediaExtractor.<============ Crash here", mLog.e));
                        }
                    }
                }
                extractor.release();
                fis.close();
            } catch (Exception e) {
                videoLogList.add(new LogMsg("getFrameRate failed.<============ Crash here", mLog.e));
            }
        } else {
            videoLogList.add(new LogMsg("getFrameRate failed " + NO_SD_CARD + ".", mLog.e));
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
                        videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
                }
                if (frameRate != 0) Success++;
                else Fail++;
            } else {
                Fail++;
            }
            if (null != videoLogList)
                videoLogList.add(new LogMsg("CheckFile:(" + path.split("/")[3] +
                        ") video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        ") app_reset:(" + getReset() + ")", mLog.i));
        } catch (Exception ignored) {
            if (null != videoLogList)
                videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
            Fail++;
        }
    }

    private void closeStateCallback(String cameraId) {
        switch (cameraId) {
            case firstCamera:
                if (mStateCallback0 != null) {
                    mStateCallback0.onDisconnected(mCameraDevice0);
                    mStateCallback0 = null;
                }
                break;
            case secondCamera:
                if (mStateCallback1 != null) {
                    mStateCallback1.onDisconnected(mCameraDevice1);
                    mStateCallback1 = null;
                }
                break;
            case thirdCamera:
                if (mStateCallback2 != null) {
                    mStateCallback2.onDisconnected(mCameraDevice2);
                    mStateCallback2 = null;
                }
                break;
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
            videoLogList.add(new LogMsg("mPreviewSession" + cameraId + " is error."));
        }
    }

    private void closeMediaRecorder(String cameraId) {
        switch (cameraId) {
            case firstCamera:
                try {
                    if (mMediaRecorder0 != null) {
                        mMediaRecorder0.stop();
                        mMediaRecorder0.release();
                        videoLogList.add(new LogMsg("Record " + cameraId + " finish."));
                    }
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mMediaRecorder" + cameraId + " is error."));
                }
                break;
            case secondCamera:
                try {
                    if (mMediaRecorder1 != null) {
                        mMediaRecorder1.stop();
                        mMediaRecorder1.release();
                        videoLogList.add(new LogMsg("Record " + cameraId + " finish."));
                    }
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mMediaRecorder" + cameraId + " is error."));
                }
                break;
            case thirdCamera:
                try {
                    if (mMediaRecorder2 != null) {
                        mMediaRecorder2.stop();
                        mMediaRecorder2.release();
                        videoLogList.add(new LogMsg("Record " + cameraId + " finish."));
                    }
                } catch (Exception e) {
                    videoLogList.add(new LogMsg("mMediaRecorder" + cameraId + " is error."));
                }
                break;
        }
    }

    private void startRecord(String cameraId) {
        if (!isError) {
            Log.d(TAG, "startRecord");
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
                    videoLogList.add(new LogMsg("mPreviewSession" + cameraId + " is error."));
                }
                if (null == texture) {
                    Log.e(TAG, "texture is null, return");
                    return;
                }
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);
                List<Surface> surfaces = new ArrayList<>();
                CaptureRequest.Builder mPreviewBuilder;
                CameraDevice mCameraDevice = null;
                Surface recorderSurface = null;
                Handler backgroundHandler = null;
                switch (cameraId) {
                    case firstCamera:
                        backgroundHandler = backgroundHandler0;
                        mCameraDevice = mCameraDevice0;
                        mMediaRecorder0 = setUpMediaRecorder(firstCamera);
                        recorderSurface = Objects.requireNonNull(mMediaRecorder0).getSurface();
                        break;
                    case secondCamera:
                        backgroundHandler = backgroundHandler1;
                        mCameraDevice = mCameraDevice1;
                        mMediaRecorder1 = setUpMediaRecorder(secondCamera);
                        recorderSurface = Objects.requireNonNull(mMediaRecorder1).getSurface();
                        break;
                    case thirdCamera:
                        backgroundHandler = backgroundHandler2;
                        mCameraDevice = mCameraDevice2;
                        mMediaRecorder2 = setUpMediaRecorder(thirdCamera);
                        recorderSurface = Objects.requireNonNull(mMediaRecorder2).getSurface();
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
                                                updatePreview(mPreviewBuilder, mPreviewSession0, backgroundHandler0);
                                                if (mMediaRecorder0 != null)
                                                    mMediaRecorder0.start();
                                                if (autoStopRecord)
                                                    stopRecordHandler0.postDelayed(null, delayTime);
                                                break;
                                            case secondCamera:
                                                mPreviewSession1 = session;
                                                updatePreview(mPreviewBuilder, mPreviewSession1, backgroundHandler1);
                                                if (mMediaRecorder1 != null)
                                                    mMediaRecorder1.start();
                                                if (autoStopRecord)
                                                    stopRecordHandler1.postDelayed(null, delayTime);
                                                break;
                                            case thirdCamera:
                                                mPreviewSession2 = session;
                                                updatePreview(mPreviewBuilder, mPreviewSession2, backgroundHandler2);
                                                if (mMediaRecorder2 != null)
                                                    mMediaRecorder2.start();
                                                if (autoStopRecord)
                                                    stopRecordHandler2.postDelayed(null, delayTime);
                                                break;
                                        }
                                    } catch (Exception e) {
                                        isError = true;
                                        videoLogList.add(new LogMsg("Camera " + cameraId + " can't record. <============ Crash here", mLog.e));
                                        saveLog(getApplicationContext(), false, false);
                                        errorMessage = "Camera " + cameraId + " can't record. <============ Crash here";
                                        ((TextView) findViewById(R.id.record_status)).setText("Error");
                                    }
                                }

                                public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                                    isError = true;
                                    videoLogList.add(new LogMsg("Camera " + cameraId + " Record onConfigureFailed.", mLog.e));
                                    saveLog(getApplicationContext(), false, false);
                                    errorMessage = "Camera " + cameraId + " onConfigureFailed error.";
                                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                                    if (autoRestart) {
                                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                                    }
                                }
                            }, backgroundHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                    isError = true;
                    videoLogList.add(new LogMsg("Camera " + cameraId + " CameraCaptureSession.StateCallback() error. <============ Crash here", mLog.e));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Camera " + cameraId + " startRecord error. <============ Crash here";
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                    if (autoRestart) {
                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                videoLogList.add(new LogMsg("Camera " + cameraId + " startRecord error. <============ Crash here", mLog.e));
                new Handler().post(() -> stopRecordAndSaveLog(false));
                errorMessage = "Camera " + cameraId + " startRecord error. <============ Crash here";
                ((TextView) findViewById(R.id.record_status)).setText("Error");
                if (autoRestart) {
                    new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                }
            }
        } else {
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
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
                    videoLogList.add(new LogMsg("SD Card is Full."));
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
                        videoLogList.add(new LogMsg("MP4 video file not found. <============ Crash here"));

                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                getSdCard = !getSDPath().equals("");
                if (getSdCard) {
                    saveLog(getApplicationContext(), false, false);
                    errorMessage = "error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.";
                    videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
                } else {
                    videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
                    saveLog(getApplicationContext(), false, false);
                    errorMessage = NO_SD_CARD;
                }
            }
        } else {
            isError = true;
            videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
            saveLog(getApplicationContext(), false, false);
            errorMessage = NO_SD_CARD;
        }
    }

    private void delete(String path, boolean fromSDcard) {
        try {
            if (!path.equals("")) {
                File video = new File(path);
                if (video.exists()) {
                    if (fromSDcard)
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[3], mLog.w));
                    else
                        videoLogList.add(new LogMsg("Delete: " + path.split("/")[5], mLog.w));
                    video.delete();
                } else {
                    videoLogList.add(new LogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            getSdCard = !getSDPath().equals("");
            videoLogList.add(new LogMsg("#delete " + path + " error. <============ Crash here", mLog.e));
            saveLog(getApplicationContext(), false, false);
            errorMessage = "Delete file error. <============ Crash here";
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
    }

    private void checkAndClear(String cameraID) {
        try {
            if (isRecord)
                switch (cameraID) {
                    case firstCamera:
                        checkFile(firstFile);
                        break;
                    case secondCamera:
                        checkFile(secondFile);
                        break;
                    case thirdCamera:
                        checkFile(thirdFile);
                        break;
                }
        } catch (Exception e) {
            videoLogList.add(new LogMsg("CheckFile " + cameraID + " error.", mLog.e));
        }
    }

    private MediaRecorder setUpMediaRecorder(String cameraId) {
        MediaRecorder mediaRecorder = null;
        try {
            CamcorderProfile profile_1080 = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            String file = "";
            if (!getSDPath().equals("")) {
                file = getSDPath() + getCalendarTime(cameraId) + ".mp4";
                videoLogList.add(new LogMsg("Create: " + file.split("/")[3], mLog.w));
                switch (cameraId) {
                    case firstCamera:
                        firstFile = file;
                        break;
                    case secondCamera:
                        secondFile = file;
                        break;
                    case thirdCamera:
                        thirdFile = file;
                        break;
                }
                mediaRecorder = new MediaRecorder();
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setVideoEncoder(profile_1080.videoCodec);
                mediaRecorder.setVideoSize(profile_1080.videoFrameWidth, profile_1080.videoFrameHeight);
                mediaRecorder.setVideoEncodingBitRate((int) (profile_1080.videoBitRate / 3.3));
                mediaRecorder.setOutputFile(file);
                mediaRecorder.prepare();
            } else {
                getSdCard = !getSDPath().equals("");
                isError = true;
                videoLogList.add(new LogMsg("MediaRecorder error. " + NO_SD_CARD + " <============ Crash here", mLog.e));
                new Handler().post(() -> stopRecordAndSaveLog(false));
                errorMessage = "MediaRecorder error. " + NO_SD_CARD + " <============ Crash here";
                ((TextView) findViewById(R.id.record_status)).setText("Error");
                if (autoRestart) {
                    new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                }
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            getSdCard = !getSDPath().equals("");
            isError = true;
            videoLogList.add(new LogMsg("MediaRecorder " + cameraId + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
        return mediaRecorder;
    }

    private void takePreview() {
        if (Open_f_Camera)
            takePreview(firstCamera);
        if (Open_s_Camera)
            takePreview(secondCamera);
        if (Open_t_Camera)
            takePreview(thirdCamera);
    }

    private void takePreview(String cameraId) {
        Log.d(TAG, "takePreview");
        videoLogList.add(new LogMsg("Preview " + cameraId + " Camera.", mLog.i));
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
            isError = true;
            videoLogList.add(new LogMsg("takePreview " + cameraId + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "takePreview " + cameraId + " error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
        if (null != texture) {
            if (mPreviewSize == null) {
                CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(cameraId)
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                            .getOutputSizes(SurfaceTexture.class)[0];
                    texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }
            Surface surface = new Surface(texture);
            CaptureRequest.Builder mPreviewBuilder;
            CameraDevice mCameraDevice = null;
            Handler backgroundHandler = null;
            if (!isError) {
                try {
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
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                    mPreviewBuilder.addTarget(surface);
                    // CaptureRequest.CONTROL_MODE
                    mCameraDevice.createCaptureSession(Collections.singletonList(surface),
                            new CameraCaptureSession.StateCallback() {
                                public void onConfigured(CameraCaptureSession session) {
                                    switch (cameraId) {
                                        case firstCamera:
                                            mPreviewSession0 = session;
                                            updatePreview(mPreviewBuilder, mPreviewSession0, backgroundHandler0);
                                            break;
                                        case secondCamera:
                                            mPreviewSession1 = session;
                                            updatePreview(mPreviewBuilder, mPreviewSession1, backgroundHandler1);
                                            break;
                                        case thirdCamera:
                                            mPreviewSession2 = session;
                                            updatePreview(mPreviewBuilder, mPreviewSession2, backgroundHandler2);
                                            break;
                                    }
                                }

                                public void onConfigureFailed(CameraCaptureSession session) {
                                    isError = true;
                                    videoLogList.add(new LogMsg("Preview " + cameraId + " onConfigureFailed", mLog.e));
                                    saveLog(getApplicationContext(), false, false);
                                    errorMessage = "Camera " + cameraId + " onConfigureFailed error.";
                                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                                    if (autoRestart) {
                                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                                    }
                                }
                            }, backgroundHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                    isError = true;
                    videoLogList.add(new LogMsg("Preview " + cameraId + " error.", mLog.e));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Preview " + cameraId + " error.";
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                    if (autoRestart) {
                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                    }
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
            isError = true;
            videoLogList.add(new LogMsg("setCaptureRequest error.", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "setCaptureRequest error.";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (autoRestart) {
                new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
            }
        }
    }

    private class mTimerTask extends TimerTask {
        public void run() {
            runOnUiThread(() -> {
                mLaptime += 0.1d;
                BigDecimal bi = new BigDecimal(mLaptime);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
                if (autoStopRecord && outputValue >= 65) {
                    isError = true;
                    videoLogList.add(new LogMsg("Application has timed out.", mLog.w));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Application has timed out.";
                    new Handler().post(() -> ((TextView) findViewById(R.id.record_status)).setText("Error"));
                    if (autoRestart) {
                        new Handler().postDelayed(() -> restartApp(extraRecordStatus), delayMillis);
                    }
                }
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
}