package com.askey.record;

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
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.SystemProperties;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.askey.widget.CustomPageTransformer;
import com.askey.widget.CustomTextView;
import com.askey.widget.HomeListen;
import com.askey.widget.LogMsg;
import com.askey.widget.PropertyUtils;
import com.askey.widget.VerticalViewPager;
import com.askey.widget.mListAdapter;
import com.askey.widget.mLog;
import com.askey.widget.mPagerAdapter;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static com.askey.record.Utils.DFRAME_RATE;
import static com.askey.record.Utils.EXTRA_VIDEO_COPY;
import static com.askey.record.Utils.EXTRA_VIDEO_FAIL;
import static com.askey.record.Utils.EXTRA_VIDEO_PASTE;
import static com.askey.record.Utils.EXTRA_VIDEO_RECORD;
import static com.askey.record.Utils.EXTRA_VIDEO_REFORMAT;
import static com.askey.record.Utils.EXTRA_VIDEO_REMOVE;
import static com.askey.record.Utils.EXTRA_VIDEO_RESET;
import static com.askey.record.Utils.EXTRA_VIDEO_RUN;
import static com.askey.record.Utils.EXTRA_VIDEO_SUCCESS;
import static com.askey.record.Utils.EXTRA_VIDEO_VERSION;
import static com.askey.record.Utils.FRAMESKIP;
import static com.askey.record.Utils.FRAME_RATE;
import static com.askey.record.Utils.NEW_DFRAME_RATE;
import static com.askey.record.Utils.NEW_FRAME_RATE;
import static com.askey.record.Utils.NO_SD_CARD;
import static com.askey.record.Utils.TAG;
import static com.askey.record.Utils.checkConfigFile;
import static com.askey.record.Utils.configName;
import static com.askey.record.Utils.delayTime;
import static com.askey.record.Utils.errorMessage;
import static com.askey.record.Utils.fCamera;
import static com.askey.record.Utils.Fail;
import static com.askey.record.Utils.firstCamera;
import static com.askey.record.Utils.firstFile;
import static com.askey.record.Utils.firstFilePath;
import static com.askey.record.Utils.getCalendarTime;
import static com.askey.record.Utils.getFail;
import static com.askey.record.Utils.getFrameRate;
import static com.askey.record.Utils.getIsRun;
import static com.askey.record.Utils.getPath;
import static com.askey.record.Utils.getReset;
import static com.askey.record.Utils.getSDPath;
import static com.askey.record.Utils.getSdCard;
import static com.askey.record.Utils.getSuccess;
import static com.askey.record.Utils.isError;
import static com.askey.record.Utils.isFinish;
import static com.askey.record.Utils.isFrame;
import static com.askey.record.Utils.isInteger;
import static com.askey.record.Utils.isNew;
import static com.askey.record.Utils.isQuality;
import static com.askey.record.Utils.isReady;
import static com.askey.record.Utils.isRecord;
import static com.askey.record.Utils.isRun;
import static com.askey.record.Utils.lastfirstCamera;
import static com.askey.record.Utils.lastsecondCamera;
import static com.askey.record.Utils.logName;
import static com.askey.record.Utils.readConfigFile;
import static com.askey.record.Utils.reformatConfigFile;
import static com.askey.record.Utils.sCamera;
import static com.askey.record.Utils.secondCamera;
import static com.askey.record.Utils.secondFile;
import static com.askey.record.Utils.secondFilePath;
import static com.askey.record.Utils.setConfigFile;
import static com.askey.record.Utils.Success;
import static com.askey.record.Utils.videoLogList;
import static com.askey.record.restartActivity.EXTRA_MAIN_PID;

public class VideoRecordActivity extends Activity {
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    public static boolean extraRecordStatus = false, onRestart = false, onDebug = true;
    private static String codeDate0, codeDate1, resetDate;
    private Size mPreviewSize;
    private TextureView mTextureView0, mTextureView1;
    private CameraDevice mCameraDevice0, mCameraDevice1;
    private CameraCaptureSession mPreviewSession0, mPreviewSession1;
    private CameraDevice.StateCallback mStateCallback0, mStateCallback1;
    private CaptureRequest.Builder mPreviewBuilder0, mPreviewBuilder1;
    private MediaRecorder mMediaRecorder0, mMediaRecorder1;
    private Handler mainHandler, demoHandler;
    private Handler recordHandler0, recordHandler1, stopRecordHandler0, stopRecordHandler1;
    private Handler backgroundHandler0, backgroundHandler1;
    private HandlerThread thread0, thread1;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float mLaptime = 0.0f;

    private void getSetting(Context context, EditText editText1, EditText editText2, EditText editText3, TextView editText4) {
        String input = readConfigFile(context, new File(getPath(), configName));
        if (input.length() > 0) {
            String[] read = input.split("\r\n");
            int t;
            String first = "firstCameraID = ", second = "secondCameraID = ";
            String code = "numberOfRuns = ", prop = "setProperty = ";
            for (String s : read)
                if (s.indexOf(first) != -1) {
                    t = s.indexOf(first) + first.length();
                    first = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.indexOf(second) != -1) {
                    t = s.indexOf(second) + second.length();
                    second = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.indexOf(code) != -1) {
                    t = s.indexOf(code) + code.length();
                    code = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.indexOf(prop) != -1) {
                    t = s.indexOf(prop) + prop.length();
                    prop = s.substring(t);
                    break;
                }
            editText1.setText(first);
            editText2.setText(second);
            editText3.setText(code);
            editText4.setText(prop);
        } else {
            videoLogList.add(new LogMsg("Error reading config file."));
            reformatConfigFile(context, new File(getPath(), configName));
        }
    }

    private void setRecord() {
        isRecord = true;
        checkConfigFile(VideoRecordActivity.this, new File(getPath(), configName), false);
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
        firstFilePath.clear();
        secondFilePath.clear();
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
            showDialogLog();
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
        List<String> permissions = new ArrayList();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermissions(permissions.toArray(new String[permissions.size()]), 0);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 0:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 許可授權
                    setStart();
                } else {
                    // 沒有權限
                    showPermission();
                    videoLogList.add(new LogMsg("#no permissions!", mLog.e));
                    videoLogList.add(new LogMsg("No permissions!"));
                }
                break;
            default:
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
        // do nothing
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        isRun = 0;
        videoLogList = new ArrayList();
        if (checkPermission()) {
            showPermission();
        } else {
            checkConfigFile(this, true);
            //TODO SETPROP
            // -> adb shell su 0 getprop persist.our.camera.frameskip
            if (isNew) {
                try {
                    SystemProperties.set(FRAMESKIP, "1");
                } catch (Exception e) {
                    e.getStackTrace();
                    isError = true;
                    videoLogList.add(new LogMsg("SystemProperties error.", mLog.e));
                    new Handler().post(() -> saveLog(this, false, false));
                    errorMessage = "SystemProperties error. Please check your BuildVersion is 0302.";
                }
            } //*reflection invoke
            setStart();

        }
    }

    private void setHomeListener() {
        home = new HomeListen(this);
        home.setOnHomeBtnPressListener(new HomeListen.OnHomeBtnPressLitener() {
            public void onHomeBtnPress() {
                videoLogList.add(new LogMsg("@home", mLog.v));
                stopRecordAndSaveLog(true);
            }

            public void onHomeBtnLongPress() {
                videoLogList.add(new LogMsg("@home", mLog.v));
                stopRecordAndSaveLog(true);
            }
        });
        home.start();
    }

    private void setCallback(int callback) {
        try {
            if (callback == 0)
                mStateCallback0 = new CameraDevice.StateCallback() {

                    public void onOpened(CameraDevice camera) {
                        fCamera = true;
                        if (!isReady) {
                            // 打开摄像头
                            Log.e(TAG, "onOpened");
                            videoLogList.add(new LogMsg("Camera " + firstCamera + " is opened.", mLog.i));
                        }
                        mCameraDevice0 = camera;
                        // 开启预览
                        takePreview(firstCamera);
                    }

                    public void onDisconnected(CameraDevice camera) {
                        try {
                            fCamera = false;
                            camera.close();
                            // 关闭摄像头
                            Log.e(TAG, "onDisconnected");
                            videoLogList.add(new LogMsg("Camera " + firstCamera + " is disconnected.", mLog.w));
                            if (null != mCameraDevice0) {
                                mCameraDevice0.close();
                                mCameraDevice0 = null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            isError = true;
                            videoLogList.add(new LogMsg("Camera " + firstCamera + " close error.", mLog.w));
                            new Handler().post(() -> stopRecordAndSaveLog(false));
                            errorMessage = "Camera " + firstCamera + " close error.";
                            ((TextView) findViewById(R.id.record_status)).setText("Error");
                        }
                        if (isError) {
                            final String dates = resetDate + "";
                            final boolean records = extraRecordStatus;
                            new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                        }
                    }

                    public void onError(CameraDevice camera, int error) {
                        isError = true;
                        onDisconnected(camera);
                        // 前鏡頭開啟失敗
                        Log.e(TAG, "onError");
                        closePreviewSession(firstCamera);
                        videoLogList.add(new LogMsg("Open Camera " + firstCamera + " error. <============ Crash here", mLog.e));
                        new Handler().post(() -> stopRecordAndSaveLog(false));
                        errorMessage = "Open Camera " + firstCamera + " error. <============ Crash here";
                        ((TextView) findViewById(R.id.record_status)).setText("Error");
                    }
                };
            if (callback == 1)
                mStateCallback1 = new CameraDevice.StateCallback() {

                    public void onOpened(CameraDevice camera) {
                        sCamera = true;
                        if (!isReady) {
                            // 打开摄像头
                            Log.e(TAG, "onOpened");
                            videoLogList.add(new LogMsg("Camera " + secondCamera + " is opened.", mLog.i));
                        }
                        mCameraDevice1 = camera;
                        // 开启预览
                        takePreview(secondCamera);
                        if (!isReady) {
                            isReady = true;
                            new Handler().postDelayed(() -> demoHandler.obtainMessage().sendToTarget(), 500);
                        }
                    }

                    public void onDisconnected(CameraDevice camera) {
                        try {
                            sCamera = false;
                            camera.close();
                            // 关闭摄像头
                            Log.e(TAG, "onDisconnected");
                            videoLogList.add(new LogMsg("Camera " + secondCamera + " is disconnected.", mLog.w));
                            if (null != mCameraDevice1) {
                                mCameraDevice1.close();
                                mCameraDevice1 = null;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            isError = true;
                            videoLogList.add(new LogMsg("Camera " + secondCamera + " close error.", mLog.w));
                            new Handler().post(() -> stopRecordAndSaveLog(false));
                            errorMessage = "Camera " + secondCamera + " close error.";
                            ((TextView) findViewById(R.id.record_status)).setText("Error");
                        }
                        if (isError) {
                            final String dates = resetDate + "";
                            final boolean records = extraRecordStatus;
                            new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                        }
                    }

                    public void onError(CameraDevice camera, int error) {
                        isError = true;
                        onDisconnected(camera);
                        // 前鏡頭開啟失敗
                        Log.e(TAG, "onError");
                        closePreviewSession(secondCamera);
                        videoLogList.add(new LogMsg("Open Camera " + secondCamera + " error. <============ Crash here", mLog.e));
                        new Handler().post(() -> stopRecordAndSaveLog(false));
                        errorMessage = "Open Camera " + secondCamera + " error. <============ Crash here";
                        ((TextView) findViewById(R.id.record_status)).setText("Error");
                    }
                };
        } catch (Exception e) {
            e.printStackTrace();
            getSdCard = !getSDPath().equals("");
            isError = true;
            videoLogList.add(new LogMsg("CameraDevice.StateCallback " + callback + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "CameraDevice.StateCallback " + callback + " error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (onDebug) {
                final String dates = resetDate + "";
                final boolean records = extraRecordStatus;
                new Handler().postDelayed(() -> restartApp(dates, records), 3000);
            }
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        isFinish = 0;
        if (isRecord) new Handler().post(() -> {
            new Handler().post(() -> stopRecord(false));
        });
        new Handler().post(() -> saveLog(getApplicationContext(), false, kill));
    }

    private void restartApp(String date, boolean record) {
        if (date.equals(resetDate)) {
            try {
                home.stop();
            } catch (Exception e) {

            }
            onRestart = true;
            onReset++;
            Context context = getApplicationContext();
            Intent intent = restartActivity.createIntent(context);
            intent.putExtra(EXTRA_VIDEO_RUN, onRun);
            intent.putExtra(EXTRA_VIDEO_FAIL, onFail);
            intent.putExtra(EXTRA_VIDEO_RESET, onReset);
            intent.putExtra(EXTRA_VIDEO_SUCCESS, onSuccess);
            intent.putExtra(EXTRA_VIDEO_RECORD, record);
            context.startActivity(intent);
        }
    }

    @SuppressLint("HandlerLeak")
    private void initial() {
        getSdCard = !getSDPath().equals("");
        ArrayList<View> items_frame = new ArrayList();
        ArrayList<View> items_quality = new ArrayList();
        for (String frame : new ArrayList<>(Arrays.asList( // or "3.9fps", "3.4fps", "1.7fps", "0.8fps"
                isNew ? NEW_FRAME_RATE : FRAME_RATE))) {
            View vi = LayoutInflater.from(this).inflate(R.layout.style_vertical_item, null);
            CustomTextView item = vi.findViewById(R.id.customTextView);
            item.setText(frame);
            items_frame.add(vi);
        }
        for (String quality : new ArrayList<>(Arrays.asList("1080p", "720p"))) {
            View vi = LayoutInflater.from(this).inflate(R.layout.style_vertical_item, null);
            CustomTextView item = vi.findViewById(R.id.customTextView);
            item.setText(quality);
            items_quality.add(vi);
        }
        VerticalViewPager pager_Frame = findViewById(R.id.pager1);
        pager_Frame.addOnPageChangeListener(new mOnPageChangeListener(0));
        pager_Frame.setAdapter(new mPagerAdapter(items_frame));
        pager_Frame.setPageTransformer(true, new CustomPageTransformer());

        VerticalViewPager pager_Quality = findViewById(R.id.pager2);
        pager_Quality.addOnPageChangeListener(new mOnPageChangeListener(1));
        pager_Quality.setAdapter(new mPagerAdapter(items_quality));
        pager_Quality.setPageTransformer(true, new CustomPageTransformer());

        // TODO findViewById
        setLoading(false);
        videoLogList.add(new LogMsg("Initial now.", mLog.v));
        thread0 = new HandlerThread("CameraPreview0");
        thread0.start();
        thread1 = new HandlerThread("CameraPreview1");
        thread1.start();
        backgroundHandler0 = new Handler(thread0.getLooper());
        backgroundHandler1 = new Handler(thread1.getLooper());
        mainHandler = new Handler(getMainLooper());
        recordHandler0 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                startRecord(firstCamera);
            }
        };
        recordHandler1 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                startRecord(secondCamera);
            }
        };
        stopRecordHandler0 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                stopRecord(false, msg.obj.toString(), msg.arg1 + "");
            }
        };
        stopRecordHandler1 = new Handler() {
            public void handleMessage(android.os.Message msg) {
                stopRecord(false, msg.obj.toString(), msg.arg1 + "");
            }
        };

        codeDate0 = getCalendarTime();
        codeDate1 = getCalendarTime();
        resetDate = getCalendarTime();
        setCallback(0);
        setCallback(1);
        mTextureView0 = findViewById(R.id.surfaceView0);
        mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
        mTextureView1 = findViewById(R.id.surfaceView1);
        mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));

        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new LogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
        });
        findViewById(R.id.record).setOnClickListener((View v) -> {
            isRecordStart(false);
        });
        findViewById(R.id.setting).setOnClickListener((View v) -> {
            if (getSdCard && !isError) {
                videoLogList.add(new LogMsg("@dialog_setting", mLog.v));
                View view = LayoutInflater.from(this).inflate(R.layout.layout_setting, null);
                final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
                view.findViewById(R.id.dialog_button_1).setOnClickListener((View vs) -> { // reset
                    videoLogList.add(new LogMsg("@setting_reset", mLog.v));
                    setConfigFile(this, new File(getPath(), configName), view, true);
                    getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                            view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
                    setSetting();
                });
                view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // cancel
                    videoLogList.add(new LogMsg("@setting_cancel", mLog.v));
                    dialog.dismiss();
                });
                view.findViewById(R.id.dialog_button_3).setOnClickListener((View vs) -> { // ok
                    videoLogList.add(new LogMsg("@setting_ok", mLog.v));
                    setConfigFile(this, new File(getPath(), configName), view, false);
                    setSetting();
                    dialog.dismiss();
                });
                getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                        view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
                dialog.show();
            } else {
                showDialogLog();
            }
        });
        findViewById(R.id.loadingView).setVisibility(View.INVISIBLE);
        ((TextView) findViewById(R.id.record_status)).setText(getSDPath().equals("") ? "Error" : "Ready");
        firstFilePath = new ArrayList();
        secondFilePath = new ArrayList();
        videoLogList.add(new LogMsg("#initial complete", mLog.v));
        onRun = getIntent().getIntExtra(EXTRA_VIDEO_RUN, 0);
        onFail = getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        onReset = getIntent().getIntExtra(EXTRA_VIDEO_RESET, 0);
        onSuccess = getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);
        if (onReset != 0)
            videoLogList.add(new LogMsg("#noReset:" + onReset, mLog.v));
        extraRecordStatus = getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        // show DEMO
        demoHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                this.post(() -> checkSdCardFromFileList());
                if (!extraRecordStatus) {
                    this.post(() -> saveLog(getApplicationContext(), false, false));
                } else {
                    isRecordStart(true);
                }
            }
        };
        this.registerReceiver(new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    videoLogList.add(new LogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    new Handler().post(() -> saveLog(getApplicationContext(), false, false));
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
            if (check[1]) {
                try {
                    SystemProperties.set(FRAMESKIP, "1");
                    if (isNew) {
                        String getFrameSkip = PropertyUtils.get(FRAMESKIP);
                        if (null != getFrameSkip) {
                            if (isInteger(getFrameSkip, false)) {
                                //if frameskip is chehe or lastcamera != cameraid, delay 3s to change camera devices
                                SystemProperties.set(FRAMESKIP, String.valueOf(isFrame == 0 ? 1 : 0));
                                videoLogList.add(new LogMsg("getFrameSkip:" + PropertyUtils.get(FRAMESKIP), mLog.e));
                            } else {
                                videoLogList.add(new LogMsg("getFrameSkip error, fs(" + getFrameSkip + ") is not integer.", mLog.e));
                            }
                        } else {
                            videoLogList.add(new LogMsg("getFrameSkip error, fs == null.", mLog.e));
                        }
                    }
                    ArrayList<View> new_frame = new ArrayList();
                    for (String frame : new ArrayList<>(Arrays.asList( // or "3.9fps", "3.4fps", "1.7fps", "0.8fps"
                            isNew ? NEW_FRAME_RATE : FRAME_RATE))) {
                        View vi = LayoutInflater.from(this).inflate(R.layout.style_vertical_item, null);
                        CustomTextView item = vi.findViewById(R.id.customTextView);
                        item.setText(frame);
                        new_frame.add(vi);
                    }
                    ((VerticalViewPager) findViewById(R.id.pager1)).setAdapter(new mPagerAdapter(new_frame));
                } catch (Exception e) {
                    e.getStackTrace();
                    isError = true;
                    videoLogList.add(new LogMsg("SystemProperties error.", mLog.e));
                    new Handler().post(() -> saveLog(this, false, false));
                    errorMessage = "SystemProperties error. Please check your BuildVersion is 0302.";
                }
            }
        } else {
            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
        }
    }

    private void showDialogLog() {
        if (!isRecord) {
            videoLogList.add(new LogMsg("#dialog_log", mLog.v));
            View view = LayoutInflater.from(this).inflate(R.layout.layout_getlog, null);
            final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).setView(view).setCancelable(true).create();

            view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
                videoLogList.add(new LogMsg("@log_ok", mLog.v));
                dialog.dismiss();
            });
            ArrayList<String> list = new ArrayList();
            list.add("App Version:" + this.getString(R.string.app_name));
            if (getSdCard)
                list.add("Please check file at your SD card");
            else list.add(NO_SD_CARD);
            if (!fCamera) {
                list.add("Camera Access error, Please check camera " + firstCamera + ". <============ Crash here");
                if (firstCamera.equals("2"))
                    list.add("You can try Reboot device to walk up external camera.");
            }
            if (!sCamera) {
                list.add("Camera Access error, Please check camera " + secondCamera + ". <============ Crash here");
                if (secondCamera.equals("2"))
                    list.add("You can try Reboot device to walk up external camera.");
            }
            if (!errorMessage.equals(""))
                list.add(errorMessage);
            if (list.size() > 0) {
                ArrayList<View> items = new ArrayList();
                for (String s : list) {
                    View item_layout = LayoutInflater.from(this).inflate(R.layout.style_text_item, null);
                    ((CustomTextView) item_layout.findViewById(R.id.customTextView)).setText(s);
                    items.add(item_layout);
                }
                ((ListView) view.findViewById(R.id.dialog_listview)).setAdapter(new mListAdapter(items));
                ((ListView) view.findViewById(R.id.dialog_listview)).setSelection(items.size() - 1);
            }
            dialog.show();
        }
    }

    private void setLoading(boolean visible) {
        runOnUiThread(() -> findViewById(R.id.loadingView).setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
    }

    private void takeRecord() {
        if (!isError && getSdCard) {
            videoLogList.add(new LogMsg("#------------------------------", mLog.v));
            videoLogList.add(new LogMsg("#takeRecord FrameRate:" +
                    (isNew ? NEW_FRAME_RATE : FRAME_RATE)[isFrame], mLog.v));
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
                delay = 3000;
            }

            recordHandler0.obtainMessage().sendToTarget();
            recordHandler1.obtainMessage().sendToTarget();
            new Handler().postDelayed(() -> saveLog(getApplicationContext(), false, false), delay);
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog();
        }
    }

    public static void saveLog(Context context, boolean reFormat, boolean kill) {
        if (!getSDPath().equals("")) {
            String version = context.getString(R.string.app_name);
            Intent intent = new Intent();
            intent.setClassName(context.getPackageName(), saveLogService.class.getName());
            intent.putExtra(EXTRA_VIDEO_VERSION, version);
            intent.putExtra(EXTRA_VIDEO_REFORMAT, reFormat);
            if (kill)
                intent.putExtra(EXTRA_MAIN_PID, android.os.Process.myPid());
            context.startService(intent);
        } else {
            isError = true;
            getSdCard = !getSDPath().equals("");
            if (kill)
                android.os.Process.killProcess(android.os.Process.myPid());
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        isFinish = 0;
        if (mStateCallback0 != null) {
            mStateCallback0.onDisconnected(mCameraDevice0);
            mStateCallback0 = null;
        }
        if (mStateCallback1 != null) {
            mStateCallback1.onDisconnected(mCameraDevice1);
            mStateCallback1 = null;
        }
        closePreviewSession(firstCamera);
        closePreviewSession(secondCamera);
        if (mMediaRecorder0 != null) {
            mMediaRecorder0.stop();
            mMediaRecorder0.release();
            videoLogList.add(new LogMsg("Record " + firstCamera + " finish."));
        }
        if (mMediaRecorder1 != null) {
            mMediaRecorder1.stop();
            mMediaRecorder1.release();
            videoLogList.add(new LogMsg("Record " + secondCamera + " finish."));
        }
        new Handler().post(() -> stopRecordAndSaveLog(false));
    }

    private void openCamera(String cameraId) {
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera E");
        try {
            /** cameraId
             * 0 = CameraCharacteristics.LENS_FACING_FRONT
             * 1 = CameraCharacteristics.LENS_FACING_BACK
             * 2 = CameraCharacteristics.LENS_FACING_EXTERNAL
             */
            Log.e(TAG, "camera ID: " + cameraId);
            Log.e(TAG, "number of camera: " + manager.getCameraIdList().length);
            if (isCameraOne(cameraId))
                mPreviewSize = manager.getCameraCharacteristics(cameraId)
                        .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        .getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.e(TAG, "camera open");
            manager.openCamera(cameraId, isCameraOne(cameraId) ? mStateCallback0 : mStateCallback1, mainHandler);
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("Camera Access error, Please check camera " + cameraId + ". <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "Camera Access error, Please check camera " + cameraId + ". <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (cameraId.equals("2"))
                errorMessage += "\nYou can try Reboot device to walk up external camera.";
        }
        Log.e(TAG, "openCamera X");
    }

    private String getCodeDate(String CameraID) {
        return (isCameraOne(CameraID)) ? codeDate0 : codeDate1;
    }

    private void stopRecord(boolean preview, String date, String cameraID) {
        try {
            if (date.equals(getCodeDate(cameraID))) {
                if (isCameraOne(cameraID))
                    codeDate0 = getCalendarTime();
                else
                    codeDate1 = getCalendarTime();
                if (isCameraOne(cameraID)) {
                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }
                    ((TextView) findViewById(R.id.record_status)).setText("Stop");
                    videoLogList.add(new LogMsg("#stopRecord", mLog.v));
                    Log.d(TAG, "stopRecord");
                } else
                    moveFile(getPath() + logName, getSDPath() + logName, false);

                if (isCameraOne(cameraID)) {
                    if (mMediaRecorder0 != null) {
                        mMediaRecorder0.stop();
                        mMediaRecorder0.release();
                        videoLogList.add(new LogMsg("Record " + firstCamera + " finish."));
                    } else {
                        videoLogList.add(new LogMsg("mMediaRecorder " + firstCamera + " is null."));
                    }
                } else {
                    if (mMediaRecorder1 != null) {
                        mMediaRecorder1.stop();
                        mMediaRecorder1.release();
                        videoLogList.add(new LogMsg("Record " + secondCamera + " finish."));
                    } else {
                        videoLogList.add(new LogMsg("mMediaRecorder " + secondCamera + " is null."));
                    }
                }
                checkAndClear(cameraID);

                if (isFinish == 999 || isRun <= isFinish) {
                    startRecord(cameraID);
                } else {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    extraRecordStatus = false;
                    videoLogList.add(new LogMsg("#completed"));
                    end(preview);
                }
                if (isError || !getSdCard) {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    extraRecordStatus = false;
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("Camera " + cameraID + " stopRecord error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "Camera " + cameraID + " stopRecord error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (onDebug) {
                final String dates = resetDate + "";
                final boolean records = extraRecordStatus;
                new Handler().postDelayed(() -> restartApp(dates, records), 3000);
            }
        }
    }

    private void stopRecord(boolean preview) {
        try {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            codeDate0 = getCalendarTime();
            codeDate1 = getCalendarTime();
            moveFile(getPath() + logName, getSDPath() + logName, false);
            if (!isError && getSdCard) {
                ((TextView) findViewById(R.id.record_status)).setText("Stop");
                if (isRecord) {
                    videoLogList.add(new LogMsg("#stopRecord", mLog.v));
                    Log.d(TAG, "stopRecord");
                    try {
                        if (mMediaRecorder0 != null) {
                            mMediaRecorder0.stop();
                            mMediaRecorder0.release();
                            videoLogList.add(new LogMsg("Record " + firstCamera + " finish."));
                        }
                    } catch (Exception e) {
                        videoLogList.add(new LogMsg("mMediaRecorder0 is error."));
                    }
                    try {
                        if (mMediaRecorder1 != null) {
                            mMediaRecorder1.stop();
                            mMediaRecorder1.release();
                            videoLogList.add(new LogMsg("Record " + secondCamera + " finish."));
                        }
                    } catch (Exception e) {
                        videoLogList.add(new LogMsg("mMediaRecorder1 is error."));
                    }
                    try {
                        checkAndClear();
                    } catch (Exception e) {
                        videoLogList.add(new LogMsg("Check file is fail."));
                    }
                    if (isFinish == 999 || isRun <= isFinish) {
                        takeRecord();
                    } else {
                        isRecord = false;
                        extraRecordStatus = false;
                        videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                        videoLogList.add(new LogMsg("#completed"));
                        isRun = 0;
                        isFinish = 0;
                        end(preview);
                    }
                } else {
                    isRun = 0;
                    isFinish = 0;
                    end(preview);
                }
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
            if (onDebug) {
                final String dates = resetDate + "";
                final boolean records = extraRecordStatus;
                new Handler().postDelayed(() -> restartApp(dates, records), 3000);
            }
        }
    }

    private void end(boolean preview) {
        new Handler().post(() -> saveLog(getApplicationContext(), false, false));
        if (preview) {
            takePreview();
        }
    }

    private void moveFile(String video, String pathname, boolean remove) {
        Context context = getApplicationContext();
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), copyFileService.class.getName());
        intent.putExtra(EXTRA_VIDEO_COPY, video);
        intent.putExtra(EXTRA_VIDEO_PASTE, pathname);
        intent.putExtra(EXTRA_VIDEO_REMOVE, remove);
        context.startService(intent);
    }

    @SuppressLint("DefaultLocale")
    private void fileCheck(String path) {
        try {
            File video = new File(path);
            int frameRate = 0;

            if (video.exists()) {
                try {
                    frameRate = getFrameRate(path);

                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
                    new Handler().post(() -> saveLog(getApplicationContext(), false, false));
                    errorMessage = "CheckFile error.";
                }

                boolean check = false;
                double[] range = isNew ? NEW_DFRAME_RATE : DFRAME_RATE;
                if (frameRate >= range[isFrame]) {
                    if (frameRate <= range[isFrame] + 3) {
                        check = true;
                    }
                } else if (frameRate < range[isFrame]) {
                    if (frameRate >= range[isFrame] - 3) {
                        check = true;
                    }
                }
                if (check)
                    Success++;
                else
                    Fail++;
            } else {
                Fail++;
            }
            videoLogList.add(new LogMsg("CheckFile: " + path.split("/")[3] + " frameRate:" + frameRate +
                    " success:" + getSuccess() + " fail:" + getFail() + " reset:" + getReset(), mLog.i));
            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new LogMsg("CheckFile error.", mLog.e));
            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
        }
    }

    private void closePreviewSession(String cameraId) {
        if (isCameraOne(cameraId) && mPreviewSession0 != null) {
            mPreviewSession0.close();
            mPreviewSession0 = null;
        }
        if (!isCameraOne(cameraId) && mPreviewSession1 != null) {
            mPreviewSession1.close();
            mPreviewSession1 = null;
        }
    }

    private void startRecord(String cameraId) {
        if (!isError) {
            Log.d(TAG, "startRecord");
            try {
                if (isCameraOne(cameraId))
                    codeDate0 = getCalendarTime();
                else
                    codeDate1 = getCalendarTime();

                if (isCameraOne(cameraId)) {
                    checkSdCardFromFileList();
                    runOnUiThread(() -> {
                        if (mTimer == null) {
                            isRun++;
                            onFail = getFail();
                            onRun = getIsRun();
                            onSuccess = getSuccess();
                            //タイマーの初期化処理
                            timerTask = new mTimerTask();
                            mLaptime = 0.0f;
                            mTimer = new Timer(true);
                            mTimer.schedule(timerTask, 100, 100);
                            ((TextView) findViewById(R.id.record_timer)).setText("00");
                            ((TextView) findViewById(R.id.record_status)).setText("Recording");
                        }
                    });
                }
                closePreviewSession(cameraId);
                SurfaceTexture texture = isCameraOne(cameraId) ? mTextureView0.getSurfaceTexture() : mTextureView1.getSurfaceTexture();
                if (null == texture) {
                    Log.e(TAG, "texture is null, return");
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
                    mPreviewBuilder0 = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    surfaces.add(surface);
                    mPreviewBuilder0.addTarget(surface);
                    recorderSurface = mMediaRecorder0.getSurface();
                    surfaces.add(recorderSurface);
                    mPreviewBuilder0.addTarget(recorderSurface);
                    mPreviewBuilder = mPreviewBuilder0;
                } else {
                    backgroundHandler = backgroundHandler1;
                    mCameraDevice = mCameraDevice1;
                    mMediaRecorder1 = setUpMediaRecorder(secondCamera);
                    mPreviewBuilder1 = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    surfaces.add(surface);
                    mPreviewBuilder1.addTarget(surface);
                    recorderSurface = mMediaRecorder1.getSurface();
                    surfaces.add(recorderSurface);
                    mPreviewBuilder1.addTarget(recorderSurface);
                    mPreviewBuilder = mPreviewBuilder1;
                }
                // Start a capture session
                // Once the session starts, we can update the UI and start recording
                if (mPreviewBuilder != null) {

                    try {
                        mCameraDevice.createCaptureSession(surfaces,
                                new CameraCaptureSession.StateCallback() {

                                    public void onConfigured(CameraCaptureSession session) {
                                        // 当摄像头已经准备好时，开始显示预览
                                        if (isCameraOne(cameraId)) {
                                            mPreviewSession0 = session;
                                        } else {
                                            mPreviewSession1 = session;
                                        }
                                        updatePreview(mPreviewBuilder, session, backgroundHandler);

                                        if (isCameraOne(cameraId)) {
                                            Message msg = stopRecordHandler0.obtainMessage();
                                            msg.arg1 = Integer.parseInt(cameraId);
                                            msg.obj = getCodeDate(cameraId);
                                            stopRecordHandler0.sendMessageDelayed(msg, delayTime);
                                            if (mMediaRecorder0 != null)
                                                mMediaRecorder0.start();
                                        } else {
                                            Message msg = stopRecordHandler1.obtainMessage();
                                            msg.arg1 = Integer.parseInt(cameraId);
                                            msg.obj = getCodeDate(cameraId);
                                            stopRecordHandler1.sendMessageDelayed(msg, delayTime);
                                            if (mMediaRecorder1 != null)
                                                mMediaRecorder1.start();
                                        }

                                    }


                                    public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                                        videoLogList.add(new LogMsg("Camera " + cameraId + " Record onConfigureFailed.", mLog.e));
                                        new Handler().post(() -> saveLog(getApplicationContext(), false, false));
                                    }
                                }, backgroundHandler);
                    } catch (Exception e) {
                        e.printStackTrace();
                        isError = true;
                        videoLogList.add(new LogMsg("Camera " + cameraId + " CameraCaptureSession.StateCallback() error. <============ Crash here", mLog.e));
                        new Handler().post(() -> stopRecordAndSaveLog(false));
                        errorMessage = "Camera " + cameraId + " startRecord error. <============ Crash here";
                        ((TextView) findViewById(R.id.record_status)).setText("Error");
                        if (onDebug) {
                            final String dates = resetDate + "";
                            final boolean records = extraRecordStatus;
                            new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                        }
                    }
                } else {
                    isError = true;
                    videoLogList.add(new LogMsg("Camera " + cameraId + " mPreviewBuilder is null. <============ Crash here", mLog.e));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Camera " + cameraId + " mPreviewBuilder is null. <============ Crash here";
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                    if (onDebug) {
                        final String dates = resetDate + "";
                        final boolean records = extraRecordStatus;
                        new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                videoLogList.add(new LogMsg("Camera " + cameraId + " startRecord error. <============ Crash here", mLog.e));
                new Handler().post(() -> stopRecordAndSaveLog(false));
                errorMessage = "Camera " + cameraId + " startRecord error. <============ Crash here";
                ((TextView) findViewById(R.id.record_status)).setText("Error");
                if (onDebug) {
                    final String dates = resetDate + "";
                    final boolean records = extraRecordStatus;
                    new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                }
            }
        }
    }

    private void checkSdCardFromFileList() {
        Context context = getApplicationContext();
        Intent intent = new Intent();
        intent.setClassName(context.getPackageName(), checkSdCardService.class.getName());
        context.startService(intent);
    }

    private void checkAndClear(String cameraID) {
        if (isCameraOne(cameraID)) {
            try {
                for (String f : firstFilePath)
                    fileCheck(f);
            } catch (Exception e) {
                videoLogList.add(new LogMsg("CheckFile " + cameraID + " error.", mLog.e));
            } finally {
                firstFilePath.clear();
            }
        }
        if (!isCameraOne(cameraID)) {
            try {
                for (String s : secondFilePath)
                    fileCheck(s);
            } catch (Exception e) {
                videoLogList.add(new LogMsg("CheckFile " + cameraID + " error.", mLog.e));
            } finally {
                secondFilePath.clear();
            }
        }
    }

    private void checkAndClear() {
        for (String f : firstFilePath)
            fileCheck(f);
        for (String s : secondFilePath)
            fileCheck(s);
        firstFilePath.clear();
        secondFilePath.clear();
    }

    private MediaRecorder setUpMediaRecorder(String cameraId) {
        MediaRecorder mediaRecorder = null;
        try {
            /*TODO CamcorderProfile.QUALITY_HIGH:质量等级对应于最高可用分辨率*/// 1080p, 720p
            CamcorderProfile profile_720 = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
            CamcorderProfile profile_1080 = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
            String file = "";
            if (!getSDPath().equals("")) {
                file = getSDPath() + getCalendarTime(isCameraOne(cameraId)) + ".mp4";
                videoLogList.add(new LogMsg("Create: " + file.split("/")[3], mLog.w));

                (isCameraOne(cameraId) ? firstFilePath : secondFilePath).add(file);
                if (isCameraOne(cameraId))
                    firstFile = file + "";
                else
                    secondFile = file + "";
                // Step 1: Unlock and set camera to MediaRecorder
                CamcorderProfile profile = isQuality == 1 ? profile_720 : profile_1080;
                mediaRecorder = new MediaRecorder();
                // Step 2: Set sources
                if (isCameraOne(cameraId))
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                if (isCameraOne(cameraId))
                    mediaRecorder.setAudioEncoder(profile.audioCodec);
                mediaRecorder.setVideoEncoder(profile.videoCodec);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                if (isCameraOne(cameraId))
                    mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
                mediaRecorder.setVideoEncodingBitRate((int) (profile.videoBitRate / 3.3));
                if (isCameraOne(cameraId)) {
                    mediaRecorder.setAudioChannels(profile.audioChannels);
                    mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
                }
                /*设置要捕获的视频的帧速率*/ // default is 24.6
                mediaRecorder.setVideoFrameRate(!isNew ? isFrame == 0 ? 10 : 28 : 27); // 1 -> 12fps, 10 -> 16fps
                // Step 4: Set output file
                mediaRecorder.setOutputFile(file);
                // Step 5: Prepare configured MediaRecorder
                mediaRecorder.prepare();
            } else {
                getSdCard = !getSDPath().equals("");
                isError = true;
                videoLogList.add(new LogMsg("MediaRecorder error. " + NO_SD_CARD + " <============ Crash here", mLog.e));
                new Handler().post(() -> stopRecordAndSaveLog(false));
                errorMessage = "MediaRecorder error. " + NO_SD_CARD + " <============ Crash here";
                ((TextView) findViewById(R.id.record_status)).setText("Error");
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            getSdCard = !getSDPath().equals("");
            isError = true;
            videoLogList.add(new LogMsg("MediaRecorder " + cameraId + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "MediaRecorder " + cameraId + " error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (getSdCard) {
                if (onDebug) {
                    final String dates = resetDate + "";
                    final boolean records = extraRecordStatus;
                    new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                }
            } else {
                videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
            }
        }
        return mediaRecorder;
    }

    private void takePreview() {
        takePreview(firstCamera);
        takePreview(secondCamera);
    }

    private void takePreview(String cameraId) {
        Log.d(TAG, "takePreview");
        videoLogList.add(new LogMsg("Preview " + cameraId + " Camera.", mLog.i));
        SurfaceTexture texture = null;
        try {
            texture = isCameraOne(cameraId) ? mTextureView0.getSurfaceTexture() : mTextureView1.getSurfaceTexture();
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new LogMsg("takePreview " + cameraId + " error. <============ Crash here", mLog.e));
            new Handler().post(() -> stopRecordAndSaveLog(false));
            errorMessage = "takePreview " + cameraId + " error. <============ Crash here";
            ((TextView) findViewById(R.id.record_status)).setText("Error");
            if (onDebug) {
                final String dates = resetDate + "";
                final boolean records = extraRecordStatus;
                new Handler().postDelayed(() -> restartApp(dates, records), 3000);
            }
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
                    mCameraDevice.createCaptureSession(Arrays.asList(surface),
                            new CameraCaptureSession.StateCallback() {

                                public void onConfigured(CameraCaptureSession session) {
                                    // 当摄像头已经准备好时，开始显示预览
                                    if (isCameraOne(cameraId)) {
                                        mPreviewSession0 = session;
                                    } else {
                                        mPreviewSession1 = session;
                                    }
                                    updatePreview(mPreviewBuilder, session, backgroundHandler);
                                }


                                public void onConfigureFailed(CameraCaptureSession session) {
                                    videoLogList.add(new LogMsg("Preview " + cameraId + " onConfigureFailed", mLog.e));
                                }
                            }, backgroundHandler);
                } catch (Exception e) {
                    e.printStackTrace();
                    isError = true;
                    videoLogList.add(new LogMsg("Preview " + cameraId + " error.", mLog.e));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Preview " + cameraId + " error.";
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
                    if (onDebug) {
                        final String dates = resetDate + "";
                        final boolean records = extraRecordStatus;
                        new Handler().postDelayed(() -> restartApp(dates, records), 3000);
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
            if (onDebug) {
                final String dates = resetDate + "";
                final boolean records = extraRecordStatus;
                new Handler().postDelayed(() -> restartApp(dates, records), 3000);
            }
        }
    }

    private boolean isCameraOne(String cameraId) {
        return cameraId.equals(firstCamera);
    }

    private class mTimerTask extends TimerTask {


        public void run() {
            // mHandlerを通じてUI Threadへ処理をキューイング
            runOnUiThread(() -> {
                //実行間隔分を加算処理
                mLaptime += 0.1d;
                if (mLaptime >= 65) {
                    isError = true;
                    videoLogList.add(new LogMsg("Application has timed out.", mLog.w));
                    new Handler().post(() -> stopRecordAndSaveLog(false));
                    errorMessage = "Application has timed out.";
                    new Handler().post(() -> ((TextView) findViewById(R.id.record_status)).setText("Error"));
                    if (onDebug) {
                        final String dates = resetDate + "";
                        final boolean records = extraRecordStatus;
                        new Handler().postDelayed(() -> restartApp(dates, records), 3000);
                    }
                }
                //計算にゆらぎがあるので小数点第1位で丸める
                BigDecimal bi = new BigDecimal(mLaptime);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
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

    private class mOnPageChangeListener implements VerticalViewPager.OnPageChangeListener {
        int pos;

        public mOnPageChangeListener(int pos) {
            this.pos = pos;
        }

        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        public void onPageSelected(int position) {
            switch (pos) {
                case 0:
                    if (!isRecord) {
                        isFrame = position;
                        if (isNew) {
                            setLoading(true);
                            new Handler().post(() -> {
                                String getFrameSkip = PropertyUtils.get(FRAMESKIP);
                                if (null != getFrameSkip) {
                                    if (isInteger(getFrameSkip, false)) {
                                        //if frameskip is chehe or lastcamera != cameraid, delay 3s to change camera devices
                                        try {
                                            SystemProperties.set(FRAMESKIP, String.valueOf(isFrame == 0 ? 1 : 0));
                                        } catch (Exception e) {
                                            e.getStackTrace();
                                            isError = true;
                                            videoLogList.add(new LogMsg("SystemProperties error.", mLog.e));
                                            new Handler().post(() -> saveLog(getApplicationContext(), false, false));
                                            errorMessage = "SystemProperties error. Please check your BuildVersion is 0302.";
                                        }
                                        videoLogList.add(new LogMsg("getFrameSkip:" + PropertyUtils.get(FRAMESKIP), mLog.e));
                                        mStateCallback0.onDisconnected(mCameraDevice0);
                                        mStateCallback1.onDisconnected(mCameraDevice1);
                                        new Handler().post(() -> openCamera(firstCamera));
                                        new Handler().post(() -> openCamera(secondCamera));
                                    } else {
                                        videoLogList.add(new LogMsg("getFrameSkip error, fs(" + getFrameSkip + ") is not integer.", mLog.e));
                                    }
                                } else {
                                    videoLogList.add(new LogMsg("getFrameSkip error, fs == null.", mLog.e));
                                }
                                setLoading(false);
                            });
                        }
                    } else {
                        ((VerticalViewPager) findViewById(R.id.pager1)).setCurrentItem(isFrame);
                    }
                    break;
                case 1:
                    if (!isRecord) {
                        isQuality = position;
                    } else {
                        ((VerticalViewPager) findViewById(R.id.pager2)).setCurrentItem(isQuality);
                    }
                    break;
                default:
                    break;
            }
        }

        public void onPageScrollStateChanged(int state) {

        }
    }
}