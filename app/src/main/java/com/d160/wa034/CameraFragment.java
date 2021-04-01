package com.d160.wa034;

import android.annotation.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;

import java.text.*;

import android.graphics.*;
import android.hardware.camera2.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.app.*;
import androidx.fragment.app.*;

import com.d160.view.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.*;

import static android.os.Looper.getMainLooper;
import static java.lang.System.gc;
import static com.d160.wa034.Utils.*;

public class CameraFragment extends Fragment implements mFragmentBackHandler {

    public static final String TAG = "com.d160.b030";
    public static final String configName = "BurnInTestConfig.ini";
    public static final String logName = "BurnInTestLog.ini";
    public static final String CONFIG_TITLE = "[BurnIn_Config]";
    public static final String LOG_TITLE = "[BurnIn_Log]";
    public static final String firstCamera = "0", secondCamera = "1", thirdCamera = "2";
    public static final int delay_3 = 3000, delay_60 = 60000;
    public static MediaPlayer mp;
    //-------------------------------------------------------------------------------
    public static final boolean Open_f_Camera = true, Open_s_Camera = false, Open_t_Camera = false;
    public static final boolean Open_Audio = false;
    //TODO 是否啟用keepScreen
    public static boolean keepScreen = true;
    //TODO 是否啟用preview
    public static boolean preview = false;
    //TODO 是否使用SD_Mode
    public static boolean SD_Mode = true;
    //TODO 是否啟用onPause
    public static boolean autoPause = false;
    //TODO 是否啟用重啟功能
    public static boolean autoRestart = true;
    //TODO 是否啟用60s停止錄影
    public static boolean autoStopRecord = true;
    //-------------------------------------------------------------------------------
    public static boolean extraRecordStatus = true, onRestart = false;
    public static long onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    //-------------------------------------------------------------------------------
    private BroadcastReceiver batteryReceiver = null;
    private Handler mainHandler;
    private Size mPreviewSize = null;
    private mHomeListen home;
    private Timer recordTimer = null, alarmTimer = null;
    private long recordValue = 0, alarmValue = 0;
    private mTimerTask recordTask = null;
    private mAlarmTask alarmTask = null;

    public static CameraFragment newInstance() {
        return new CameraFragment();
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
            if (null != videoLogList) {
                videoLogList.add(new mLogMsg("CheckFile:(" + path.split("/")[3] +
                        ") video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        ") app_reset:(" + getReset() + ")", mLog.i));
                Log.e(TAG, "CheckFile:(video_frameRate:(" + frameRate + ") video_success/fail:(" + getSuccess() + "/" + getFail() +
                        ") app_reset:(" + getReset() + ")");
            }
        } catch (Exception ignored) {
            if (null != videoLogList)
                videoLogList.add(new mLogMsg("CheckFile error.", mLog.e));
            Fail++;
        }
    }

    private static int getFrameRate(File file) {
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

    @SuppressLint("SimpleDateFormat")
    public static void saveLog(Context context, boolean reFormat, boolean kill) {
        if (null != videoLogList)
            if (!getSDPath().equals("")) {
                String version = context.getString(R.string.app_name);
                StringBuilder logString = new StringBuilder();
                assert videoLogList != null;
                File file = new File(getLogPath(), logName);
                DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                if (!file.exists()) {
                    logString = new StringBuilder(LOG_TITLE + version + "\r\n");
                    try {
                        boolean create = file.createNewFile();
                        if (null != videoLogList) {
                            if (!create)
                                videoLogList.add(new mLogMsg("Create file failed.", mLog.w));
                            else
                                videoLogList.add(new mLogMsg("Create the log file.", mLog.w));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        if (null != videoLogList)
                            videoLogList.add(new mLogMsg("Create file failed.", mLog.w));
                    }
                }
                if (null != videoLogList)
                    try {
                        for (mLogMsg logs : videoLogList) {
                            // ex: 2020-03-25 19:46:55 run:0 -> logs.msg
                            logString.append(dateFormat.format(logs.date))
                                    .append(" run:").append(logs.run)
                                    .append(" -> ").append(logs.msg)
                                    .append("\r\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                try {
                    FileOutputStream output = new FileOutputStream(file, !reFormat);
                    output.write(logString.toString().getBytes());
                    output.close();
                    videoLogList.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                isError = true;
                isSave = !getSDPath().equals("");
            }
        if (kill)
            android.os.Process.killProcess(android.os.Process.myPid());
    }

    // TODO onCreateView
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        isRun = 0;
        videoLogList = new ArrayList<>();
        //#onCreateView -> #onViewCreated -> #onActivityCreated -> #onResume
        setProp();
        return inflater.inflate(R.layout.fragment_camera, container, false);
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
            new Handler(getMainLooper()).post(() -> saveLog(getContext(), false, false));
        }
    }

    // TODO onViewCreated
    @SuppressLint("HandlerLeak")
    public void onViewCreated(@NonNull final View view, Bundle savedInstanceState) {
        Log.e(TAG, "#onViewCreated");
        videoLogList.add(new mLogMsg("#onViewCreated", mLog.v));
        if (checkPermission(Objects.requireNonNull(getContext()))) {
            showPermission();
        } else {
            initial();
            if (!autoPause)
                startBackgroundThread();
        }
    }

    private void initial() {
        setHomeListener();
        checkConfigFile(getContext());
        Activity activity = getActivity();
        isSave = !getSDPath().equals("");
        Log.e(TAG, "#initial");
        videoLogList.add(new mLogMsg("#initial", mLog.v));
        checkSdCardFromFileList();
        Objects.requireNonNull(activity).findViewById(R.id.cancel).setOnClickListener((View v) -> {
            isCancel();
        });
        activity.findViewById(R.id.record).setOnClickListener((View v) -> isRecordStart(false));
        ((TextView) activity.findViewById(R.id.record_status)).setText(getSDPath().equals("") ? "Error" : "Ready");
        videoLogList.add(new mLogMsg("#initial complete", mLog.v));
        onRun = activity.getIntent().getIntExtra(EXTRA_VIDEO_RUN, 0);
        onFail = activity.getIntent().getIntExtra(EXTRA_VIDEO_FAIL, 0);
        onReset = activity.getIntent().getIntExtra(EXTRA_VIDEO_RESET, 0);
        onSuccess = activity.getIntent().getIntExtra(EXTRA_VIDEO_SUCCESS, 0);
        isInitReady = true;
//        extraRecordStatus =  activity.getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);

        if (onReset != 0)
            videoLogList.add(new mLogMsg("#noReset:" + onReset, mLog.v));
    }

    private void isCancel() {
        videoLogList.add(new mLogMsg("@cancel", mLog.v));
        stopRecordAndSaveLog(true);
    }

    private void isRecordStart(boolean auto) {
        if (!isError && isSave) {
            if (isCameraReady) {
                if (!isRecord) {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Start record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Start record", mLog.v));
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
                    for (String CameraId : allCamera) {
                        /* this function will error with camera changed. */
                        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                        cameraFile.set(id, "");
                        if (isOpenCamera.get(id))
                            cameraFilePath.get(id).clear();
                    }
                    extraRecordStatus = true;
                    if (!isError && isSave) {
                        videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
                        videoLogList.add(new mLogMsg("#takeRecord FrameRate: default", mLog.v));
                        int delay = 0;
                        for (String CameraId : allCamera) {
                            /* this function will error with camera changed. */
                            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                            if (isOpenCamera.get(id)) {
                                new Handler(getMainLooper()).postDelayed(() -> recordHandler.get(id).obtainMessage().sendToTarget(), delay);
                                delay += 500;
                            }
                        }
                        saveLog(getContext(), false, false);
                    } else {
                        stopRecordAndSaveLog(false);
                        showDialogLog(false);
                    }
                } else {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Stop record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Stop record", mLog.v));
                    new Handler(getMainLooper()).post(() -> stopRecord(!auto));
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

    public boolean onBackPressed() {
        videoLogList.add(new mLogMsg("@back", mLog.v));
        stopRecordAndSaveLog(true);
        return false;
    }


    // TODO onResume 螢幕開啟
    public void onResume() {
        super.onResume();
        if (!checkPermission(Objects.requireNonNull(getContext()))) {
            if (autoPause && isInitReady) {
                Log.e(TAG, "#onResume");
                videoLogList.add(new mLogMsg("#onResume", mLog.v));
                startBackgroundThread();
            }
        } else {
            showPermission();
        }
    }

    // TODO onPause 螢幕關閉
    public void onPause() {
        if (autoPause && !checkPermission(Objects.requireNonNull(getContext())) && isInitReady) {
            Log.e(TAG, "#onPause");
            videoLogList.add(new mLogMsg("#onPause", mLog.v));
            if (isRecord)
                isRecordStart(true);
            stopBackgroundThread();
            saveLog(getContext(), false, false);
        }
        super.onPause();
    }

    private void showPermission() {
        videoLogList.add(new mLogMsg("#showPermission", mLog.v));
        // We don't have permission so prompt the user
        List<String> permissions = new ArrayList<>();
        permissions.add(permission.get(0));
        permissions.add(permission.get(1));
        permissions.add(permission.get(2));
        requestPermissions(permissions.toArray(new String[0]), 0);
    }

    private boolean checkPermission(@NonNull Context context) {
        videoLogList.add(new mLogMsg("#checkPermission", mLog.v));
        return permission(context, permission.get(0)) || permission(context, permission.get(1)) || permission(context, permission.get(2));
    }

    private boolean permission(@NonNull Context context, String mp) {
        return ActivityCompat.checkSelfPermission(context, mp) != PackageManager.PERMISSION_GRANTED;
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 許可授權
                initial();
                startBackgroundThread();
            } else {
                // 沒有權限
                showPermission();
                videoLogList.add(new mLogMsg("#no permissions!", mLog.e));
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @SuppressLint("HandlerLeak")
    private void startBackgroundThread() {
        mainHandler = new Handler(getMainLooper());
        if (keepScreen)
            Objects.requireNonNull(getActivity()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (String CameraId : allCamera) {
            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
            if (isOpenCamera.get(id)) {
                thread.set(id, new HandlerThread(threadString.get(id)));
                thread.get(id).start();
                backgroundHandler.set(id, new Handler(thread.get(id).getLooper()));
                try {
                    setCamera(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("setCallback " + CameraId + " is error.", mLog.w));
                }
                if (textureView.get(id).isAvailable())
                    openCamera(CameraId);
                else
                    textureView.get(id).setSurfaceTextureListener(new mSurfaceTextureListener(CameraId));
            }
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction("start");
        filter.addAction("close");
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        getActivity().registerReceiver(batteryReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    Log.e(TAG, "Battery:" + intent.getIntExtra("level", 0) + "%");
                    videoLogList.add(new mLogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    saveLog(getContext(), false, false);
                }
                String str = intent.getAction();
                if (str.equals("start")) {
                    isRecordStart(false);
                }
                if (str.equals("close")) {
                    isCancel();
                }
            }
        }, filter);
        new Handler(getMainLooper()).postDelayed(() -> {
            if (autoPause || extraRecordStatus)
                isRecordStart(true);
            saveLog(getContext(), false, false);
        }, delay_3);
    }

    private void stopBackgroundThread() {
        Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (batteryReceiver != null)
            getActivity().unregisterReceiver(batteryReceiver);
        for (String CameraId : allCamera) {
            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
            thread.get(id).quitSafely();
            try {
                thread.get(id).join();
                thread.set(id, null);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            backgroundHandler.get(id).removeCallbacks(thread.get(id));
            backgroundHandler.set(id, null);
        }
    }

    @SuppressLint("SetTextI18n")
    private void startRecord(String CameraId) {
        /* this function will error with camera changed. */
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        if (!isError) {
            Log.e(TAG, "startRecord " + CameraId);
            try {
                if (isCameraOne(CameraId)) {
                    checkSdCardFromFileList();
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        checkConfigFile(getContext());
                        if (recordTimer == null) {
                            onRun = getIsRun();
                            onFail = getFail();
                            onSuccess = getSuccess();
                            recordValue = 0;
                            recordTask = new mTimerTask(recordValue);
                            recordTimer = new Timer(true);
                            ((TextView) getActivity().findViewById(R.id.record_timer)).setText("0");
                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Recording");

                        }
                        if (isFinish != 999) {
                            if (alarmTimer == null) {
                                alarmValue = (isFinish - onRun) * 60;
                                alarmTask = new mAlarmTask(alarmValue);
                                alarmTimer = new Timer(true);
                            } else {
                                ((TextView) getActivity().findViewById(R.id.record_alarm)).setText("");
                            }
                        } else {
                            ((TextView) getActivity().findViewById(R.id.record_alarm)).setText("Loop testing..");
                        }
                    });
                    CameraManager manager = (CameraManager) Objects.requireNonNull(getActivity())
                            .getSystemService(Context.CAMERA_SERVICE);
                    mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(CameraId)
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                            .getOutputSizes(SurfaceTexture.class)[0];
                }
                try {
                    closePreviewSession(CameraId);
                } catch (Exception e) {
                    errorMessage("closePreviewSession error.", true, e);
                }
                SurfaceTexture texture = null;
                try {
                    texture = textureView.get(id).getSurfaceTexture();
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("getSurfaceTexture" + CameraId + " is error."));
                }
                if (null == texture) {
                    Log.e(TAG, "Run " + isRun + " texture is null, return");
                    return;
                }
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);
                List<Surface> surfaces = new ArrayList<>();
                CaptureRequest.Builder mPreviewBuilder;
                CameraDevice mCameraDevice;
                Surface recorderSurface;
                mCameraDevice = cameraDevice.get(id);
                mediaRecorder.set(id, setUpMediaRecorder(CameraId));
                recorderSurface = mediaRecorder.get(id).getSurface();
                surfaces.add(surface);
                surfaces.add(recorderSurface);
                if (mCameraDevice != null) {
                    mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                    mPreviewBuilder.addTarget(surface);
                    mPreviewBuilder.addTarget(recorderSurface);
                    try {
                        mCameraDevice.createCaptureSession(surfaces,
                                new CameraCaptureSession.StateCallback() {
                                    public void onConfigured(@NonNull CameraCaptureSession session) {
                                        try {
                                            if (isCameraOne(CameraId)) {
                                                recordTimer.schedule(recordTask, 1000, 1000);
                                                if (isFinish != 999)
                                                    alarmTimer.schedule(alarmTask, 1000, 1000);
                                            }

                                            previewSession.set(id, session);
                                            updatePreview(mPreviewBuilder, session, backgroundHandler.get(id));
                                            if (null != mediaRecorder.get(id)) {
                                                try {
                                                    mp = MediaPlayer.create(getContext(), R.raw.v_0189);
                                                    mp.setOnCompletionListener(MediaPlayer::release);
                                                    mp.start();
                                                }catch (Exception e){
                                                    videoLogList.add(new mLogMsg("MediaPlayer error", mLog.v));
                                                }
                                                mediaRecorder.get(id).start();
                                                codeDate.set(id, getCalendarTime());
                                                Message msg = stopRecordHandler.get(id).obtainMessage();
                                                msg.obj = codeDate.get(id);
                                                int delay = 0;
                                                if (!isCameraOne(CameraId)) {
                                                    for (String c : allCamera) {
                                                        delay += id * 600;
                                                        if (c.equals(CameraId))
                                                            break;
                                                    }
                                                }
                                                if (autoStopRecord)
                                                    stopRecordHandler.get(id).sendMessageDelayed(msg, delay_60 + delay);
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            errorMessage("Camera " + CameraId + " can't record. <============ Crash here", false, e);
                                        }
                                    }

                                    public void onConfigureFailed(@NonNull CameraCaptureSession Session) {
                                        errorMessage("Camera " + CameraId + " Record onConfigureFailed.", true, null);
                                    }
                                }, backgroundHandler.get(id));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("Camera " + CameraId + " CameraCaptureSession.StateCallback() error. <============ Crash here", true, e);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage("Camera " + CameraId + " startRecord error. <============ Crash here", true, e);
            }
        } else {
            if (autoRestart) {
                new Handler(getMainLooper()).postDelayed(this::restartApp, delay_3);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void stopRecord(String date, String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (date.equals(codeDate.get(id))) {
                isRun++;
                videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                Log.e(TAG, isRun + " stopRecord " + CameraId);
                if (isCameraOne(CameraId))
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        if (recordTimer != null) {
                            recordTimer.cancel();
                            recordTimer = null;
                        }
                        if (isFinish != 999)
                            if (alarmTimer != null) {
                                alarmTimer.cancel();
                                alarmTimer = null;
                            }
                        ((TextView) getActivity().findViewById(R.id.record_status)).setText("Stop");
                    });
                codeDate.set(id, getCalendarTime());
                try {
                    closeMediaRecorder(CameraId);
                    checkAndClear(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage("Check file is fail. <============ Crash here", true, e);
                }
                if (isFinish == 999 || isRun < isFinish) {
                    startRecord(CameraId);
                } else {
                    isRecordStart(true);
                }
                if (isError || !isSave) {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    ((TextView) Objects.requireNonNull(getActivity()).findViewById(R.id.record_status)).setText("Error");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("Camera " + CameraId + " stopRecord error. <============ Crash here", true, e);
        }
    }

    @SuppressLint("SetTextI18n")
    private void stopRecord(boolean reset) {
        try {
            if (!isError && isSave) {    // noError, isSave
                if (!reset) {
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        if (getFail() + getReset() == 0) {
                            videoLogList.add(new mLogMsg("#Pass"));
                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Pass");
                        } else {
                            videoLogList.add(new mLogMsg("#Fail"));
                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Fail " + getFail() + getReset());
                        }
                    });
                    new Handler(getMainLooper()).postDelayed(() -> showDialogLog(true), delay_3 / 3);
                }
                for (String CameraId : allCamera) {    // 遍例Camera
                    /* this function will error with camera changed. */
                    int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                    if (isOpenCamera.get(id)) {  // 檢查OpenCamera
                        videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                        Log.e(TAG, isRun + " stopRecord " + CameraId);
                        if (isCameraOne(CameraId))
                            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                                if (recordTimer != null) {
                                    recordTimer.cancel();
                                    recordTimer = null;
                                }
                                if (isFinish != 999)
                                    if (alarmTimer != null) {
                                        alarmTimer.cancel();
                                        alarmTimer = null;
                                    }
                                ((TextView) getActivity().findViewById(R.id.record_status)).setText("Stop");
                            });
                        codeDate.set(id, getCalendarTime());
                        try {
                            closeMediaRecorder(CameraId);
                            checkAndClear(CameraId);
                        } catch (Exception e) {
                            e.printStackTrace();
                            errorMessage("Check file is fail. <============ Crash here", true, e);
                        }
                    }
                }
            }
            isRun = 0;
            isFinish = 0;
            isRecord = false;
            videoLogList.add(new mLogMsg("#Complete"));
            videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
            saveLog(getContext(), false, false);
            if (!reset) {
                extraRecordStatus = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("stopRecord all camera error. <============ Crash here", true, e);
        }
    }

    private void checkAndClear(String CameraId) {
        /* this function will error with camera changed. */
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (isRecord)
                if (isOpenCamera.get(id) && null != cameraFilePath.get(id)) {
                    for (String f : cameraFilePath.get(id)) {
                        checkFile(f);
                    }
                    cameraFilePath.get(id).clear();
                }
            gc();
        } catch (Exception e) {
            videoLogList.add(new mLogMsg("CheckFile " + CameraId + " error.", mLog.e));
        }
    }

    private void showDialogLog(boolean end) {
        videoLogList.add(new mLogMsg("#Log_show", mLog.v));
        View view = LayoutInflater.from(getContext()).inflate(R.layout.layout_getlog, null);
        final AlertDialog dialog = new AlertDialog.Builder(getContext(), R.style.Theme_AppCompat_NoActionBar)
                .setView(view).setCancelable(true).create();

        view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
            videoLogList.add(new mLogMsg("@Log_ok", mLog.v));
            dialog.dismiss();
        });
        ArrayList<String> list = new ArrayList<>();
        if (isSave)
            if (end) {
                if (getFail() + getReset() == 0) {
                    list.add("#Pass");
                } else {
                    list.add("#Fail");
                }
                list.add("CheckFile -> video_success/fail:(" + getSuccess() + "/" + getFail() + ") app_reset:(" + getReset() + ")");
            } else
                list.add("App Version:" + this.getString(R.string.app_name));
        else list.add(NO_SD_CARD);

        if (!errorMessage.equals(""))
            list.add(errorMessage);
        else {
            for (String CameraId : allCamera) {
                /* this function will error with camera changed. */
                int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                if (isOpenCamera.get(id) && !isCameraOpened.get(id))
                    list.add("Camera Access error, Please check camera " + CameraId + ". <============ Crash here");
            }
        }
        if (list.size() > 0) {
            ArrayList<View> items = new ArrayList<>();
            for (String s : list) {
                @SuppressLint("InflateParams") View item_layout = LayoutInflater.from(getContext()).inflate(R.layout.style_text_item, null);
                ((CustomTextView) item_layout.findViewById(R.id.customTextView)).setText(s);
                items.add(item_layout);
            }
            ((ListView) view.findViewById(R.id.dialog_listview)).setAdapter(new mListAdapter(items));
//            ((ListView) view.findViewById(R.id.dialog_listview)).setSelection(items.size() - 1);
        }
        dialog.show();
    }

    boolean checkFile(String file, AtomicReferenceArray<String> list) {
        if (file.equals(list.get(0)))
            return false;
        else if (file.equals(list.get(1)))
            return false;
        return !file.equals(list.get(2));
    }

    public void onDestroy() {
        super.onDestroy();
        try {
            closeCamera();
            stopBackgroundThread();
            for (String CameraId : allCamera) {
                closeStateCallback(CameraId);
                closePreviewSession(CameraId);
                closeMediaRecorder(CameraId);
            }
            stopBackgroundThread();
            if (null != home)
                home.stop();
            saveLog(getContext(), false, false);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("onDestroy error.", false, e);
        }
    }

    @SuppressLint("MissingPermission")
    private void openCamera(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        videoLogList.add(new mLogMsg("#Open Camera" + CameraId + ".", mLog.w));
        Log.e(TAG, "camera ID: " + CameraId);
        try {
            CameraManager manager = (CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
            mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(CameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                    .getOutputSizes(SurfaceTexture.class)[0];
            Log.e(TAG, "camera " + CameraId + " is open");
            manager.openCamera(CameraId, stateCallback.get(id), mainHandler);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (isLastCamera(CameraId))
                isCameraReady = true;
        }
    }

    private void closeCamera() {
        isCameraReady = false;
        Log.e(TAG, "camera is close");
        for (String CameraId : allCamera) {
            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
            try {
                if (null != cameraDevice.get(id)) {
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        try {
                            cameraDevice.get(id).close();
                            cameraDevice.set(id, null);
                            Thread.sleep(300); //TODO 防止 Camera error, 不過會慢很多
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (Exception e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            }
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        if (isRecord)
            new Handler(getMainLooper()).post(() -> stopRecord(true));
        saveLog(getContext(), false, kill);
    }

    @SuppressLint("HandlerLeak")
    private void setCamera(String CameraId) {
        /* this function will error with camera changed. */
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        textureView.set(id, Objects.requireNonNull(getActivity()).findViewById(view_id.get(id)));
        cameraFilePath.set(id, new ArrayList<>());
        codeDate.set(id, getCalendarTime());
        stateCallback.set(id, setCallback(CameraId));
        videoLogList.add(new mLogMsg("setCallback " + CameraId + ".", mLog.w));
        recordHandler.set(id, new Handler(getMainLooper()) {
            public void handleMessage(Message msg) {
                try {
                    startRecord(CameraId);
                    videoLogList.add(new mLogMsg("startRecord " + CameraId + ".", mLog.w));
                } catch (Exception e) {
                    videoLogList.add(new mLogMsg("startRecord " + CameraId + " is error.", mLog.w));
                    e.printStackTrace();
                }
            }
        });
        stopRecordHandler.set(id, new Handler(getMainLooper()) {
            public void handleMessage(Message msg) {
                if (isRecord)
                    try {
                        stopRecord(msg.obj.toString(), CameraId);
                        videoLogList.add(new mLogMsg("stopRecord " + CameraId + ".", mLog.w));
                    } catch (Exception e) {
                        videoLogList.add(new mLogMsg("stopRecord " + CameraId + " is error.", mLog.w));
                        e.printStackTrace();
                    }
            }
        });
    }

    @SuppressLint("SetTextI18n")
    private void errorMessage(String msg, boolean reset, Exception e) {
        if (e != null)
            Log.e(TAG, e.toString());
        isSave = !getSDPath().equals("");
        isError = true;
        isRecord = false;
        Objects.requireNonNull(getActivity()).runOnUiThread(() ->
                ((TextView) getActivity().findViewById(R.id.record_status)).setText("Error"));
        if (null != videoLogList)
            videoLogList.add(new mLogMsg(msg, mLog.e));
        errorMessage = msg;
        new Handler(getMainLooper()).post(() -> stopRecordAndSaveLog(false));
        if (reset) {
            new Handler(getMainLooper()).postDelayed(this::restartApp, delay_3);
        }
    }

    private void restartApp() {
        onRestart = true;
        onReset++;
        Context context = getContext();
        Intent intent = RestartActivity.createIntent(Objects.requireNonNull(context));
        intent.putExtra(EXTRA_VIDEO_RUN, onRun);
        intent.putExtra(EXTRA_VIDEO_FAIL, onFail);
        intent.putExtra(EXTRA_VIDEO_RESET, onReset);
        intent.putExtra(EXTRA_VIDEO_SUCCESS, onSuccess);
        intent.putExtra(EXTRA_VIDEO_RECORD, extraRecordStatus);
        context.startActivity(intent);
    }

    private void closeStateCallback(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (null != stateCallback.get(id)) {
                stateCallback.get(id).onDisconnected(cameraDevice.get(id));
                stateCallback.set(id, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closeStateCallback" + CameraId + " is error.", true, e);
        }
    }

    private void closePreviewSession(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (null != previewSession.get(id)) {
                previewSession.get(id).close();
                previewSession.set(id, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closePreviewSession" + CameraId + " is error.", false, e);
        }
    }

    private void closeMediaRecorder(String CameraId) {
        /* this function will error with camera changed. */
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        if (null != mediaRecorder.get(id)) {
            try {
                try {
                    mediaRecorder.get(id).stop();
                    mediaRecorder.get(id).release();
                } catch (RuntimeException stopException) {
                    // handle cleanup here
                    videoLogList.add(new mLogMsg("stop MediaRecorder " + CameraId + " is error."));
                }
                mediaRecorder.set(id, null);
                videoLogList.add(new mLogMsg("Record " + CameraId + " finish."));
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("stop MediaRecorder " + CameraId + " is error."));
            }
        }
    }

    private MediaRecorder setUpMediaRecorder(String CameraId) {
        /* this function will error with camera changed. */
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        int kbps = 1000;
        String file;
        MediaRecorder mediaRecorder = null;
        try {
            if (!getSDPath().equals("")) {
                String name = getCalendarTime(CameraId) + ".mp4";
                file = getSDPath() + name;
                videoLogList.add(new mLogMsg("Create: " + name, mLog.w));
                cameraFile.set(id, file + "");
                cameraFilePath.get(id).add(file);
                mediaRecorder = new MediaRecorder();
                if (Open_Audio) videoLogList.add(new mLogMsg("#audio"));
                if (Open_Audio) mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
//                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                if (Open_Audio) mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                if (Open_Audio) mediaRecorder.setAudioEncodingBitRate(96 * kbps);
                mediaRecorder.setVideoEncodingBitRate(2000 * kbps);
                if (Open_Audio) {
                    mediaRecorder.setAudioChannels(2);
                    mediaRecorder.setAudioSamplingRate(44100);
                }
                mediaRecorder.setVideoFrameRate(50);
                mediaRecorder.setOutputFile(file);
                mediaRecorder.prepare();
            } else {
                errorMessage("MediaRecorder error. " + NO_SD_CARD + " <============ Crash here", false, null);
                return null;
            }
        } catch (Exception e) {
            errorMessage("MediaRecorder " + CameraId + " error. <============ Crash here", false, e);
        }
        return mediaRecorder;
    }

    private void showToast(final String text) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
        }
    }

    private void checkSdCardFromFileList() {
        isSave = !getSDPath().equals("");
        if (isSave) {
            try {
                StatFs stat = new StatFs(getSDPath());
                long sdAvailSize = stat.getAvailableBlocksLong() * stat.getBlockSizeLong();
                double gigaAvailable = (sdAvailSize >> 30);
                Log.e(TAG, "Size:" + gigaAvailable + " gb");
                if (gigaAvailable < sdData) {
                    videoLogList.add(new mLogMsg("SD Card is Full."));
                    ArrayList<String> tmp = new ArrayList<>();
                    File[] fileList = new File(getSDPath()).listFiles();
                    for (File file : fileList) {
                        if (!file.isDirectory() && Utils.getFileExtension(file.toString()).equals("mp4"))
                            if (checkFile(file.toString(), cameraFile))
                                tmp.add(file.toString());
                    }
                    if (tmp.size() >= 6) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        for (int i = 0; i < 6; i++)
                            delete((String) (list != null ? list[i] : null), SD_Mode);
                        checkSdCardFromFileList();
                    } else {
                        isError = true;
                        videoLogList.add(new mLogMsg("MP4 video file not found. <============ Crash here"));
                        errorMessage("error: At least " + sdData + "gb memory needs to be available to record, please check the SD Card free space.", false, null);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (!getSDPath().equals("")) {
                    errorMessage("error: At least " + sdData + "gb memory needs to be available to record, please check the SD Card free space.", false, null);
                } else {
                    errorMessage(NO_SD_CARD, false, null);
                }
            }
        } else {
            errorMessage(NO_SD_CARD, false, null);
        }
    }

    private void setHomeListener() {
        try {
            home = new mHomeListen(getContext());
            home.setOnHomeBtnPressListener(new mHomeListen.OnHomeBtnPressListener() {
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

    private CameraDevice.StateCallback setCallback(String CameraId) {
        CameraDevice.StateCallback callback;
        try {
            callback = new CameraDevice.StateCallback() {
                final int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;

                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Log.e(TAG, "onOpened Camera " + CameraId);
                    try {
                        isCameraOpened.set(id, true);
                        Utils.cameraDevice.set(id, cameraDevice);
                        takePreview(CameraId);
                        videoLogList.add(new mLogMsg("Camera " + CameraId + " is opened.", mLog.i));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("closeCameraDevices " + CameraId + " close error.", true, e);
                    }
                }

                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Log.e(TAG, "onDisconnected Camera " + CameraId);
                    try {
                        isCameraOpened.set(id, false);
                        Utils.cameraDevice.get(id).close();
                        videoLogList.add(new mLogMsg("Camera " + CameraId + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("closeCameraDevices " + CameraId + " close error.", true, e);
                    }
                }

                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    Log.e(TAG, "onError Camera " + CameraId);
                    try {
                        isCameraOpened.set(id, false);
                        Utils.cameraDevice.get(id).close();
                        videoLogList.add(new mLogMsg("Camera " + CameraId + " is disconnected.", mLog.w));
                    } catch (Exception e) {
                        e.printStackTrace();
                        errorMessage("closeCameraDevices " + CameraId + " close error.", true, e);
                    }
                    errorMessage("Camera " + CameraId + " is error. <============ Crash here", true, null);
                }
            };
            return callback;
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closeCameraDevices " + CameraId + " close error.", true, e);
        }
        return null;
    }

    private void takePreview(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        Log.e(TAG, "takePreview " + CameraId);
        videoLogList.add(new mLogMsg("Preview " + CameraId + " Camera.", mLog.i));
        try {
            SurfaceTexture texture = null;
            try {
                texture = textureView.get(id).getSurfaceTexture();
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage("takePreview " + CameraId + " error. <============ Crash here", true, e);
            }
            assert texture != null;
            // We configure the size of default buffer to be the size of camera preview we want.
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            CaptureRequest.Builder mPreviewBuilder;
            // We set up a CaptureRequest.Builder with the output Surface.
            mPreviewBuilder = cameraDevice.get(id).createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);
            // Here, we create a CameraCaptureSession for camera preview.
            cameraDevice.get(id).createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                // When the session is ready, we start displaying the preview.
                                previewSession.set(id, session);
                                updatePreview(mPreviewBuilder, previewSession.get(id), backgroundHandler.get(id));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            Objects.requireNonNull(getActivity()).runOnUiThread(() ->
                                    errorMessage("Preview " + CameraId + " onConfigureFailed", true, null));
                        }
                    }, backgroundHandler.get(id));
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void updatePreview(CaptureRequest.Builder mPreviewBuilder, CameraCaptureSession mPreviewSession, Handler backgroundHandler) {
        try {
            mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("setCaptureRequest error.", true, e);
        }
    }

    @SuppressLint("SetTextI18n")
    private class mTimerTask extends TimerTask {
        long value;

        public mTimerTask(long value) {
            this.value = value;
        }

        public void run() {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                try {
                    value += 1;
                    ((TextView) getActivity().findViewById(R.id.record_timer)).setText(value + "");
                    if (autoStopRecord && value >= 65) {
                        errorMessage("Application has timed out.", true, null);
                    }
                } catch (Exception ignored) {
                }
            });
        }
    }

    @SuppressLint("SetTextI18n")
    private class mAlarmTask extends TimerTask {
        long value;

        public mAlarmTask(long value) {
            this.value = value;
        }

        @SuppressLint("DefaultLocale")
        public void run() {
            try {
                value -= 1;
                long uptime = value;
                long hour = TimeUnit.SECONDS.toHours(uptime);
                uptime -= TimeUnit.HOURS.toSeconds(hour);
                long min = TimeUnit.SECONDS.toMinutes(uptime);
                uptime -= TimeUnit.MINUTES.toSeconds(min);
                long sec = uptime;
                Objects.requireNonNull(getActivity()).runOnUiThread(() ->
                        ((TextView) getActivity().findViewById(R.id.record_alarm)).setText(String.format("%02d:%02d:%02d", hour, min, sec)));
            } catch (Exception ignored) {
            }
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
