package com.d160.wa034;

import android.annotation.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.pm.*;

import java.text.*;

import android.hardware.*;
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
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static java.lang.System.gc;
import static com.d160.wa034.Utils.*;

public class CameraFragment extends Fragment {

    public static final String TAG = "com.d160.b030";
    public static final String logName = "CDRB030TestLog.ini";
    public static final String LOG_TITLE = "[CDRB030_Log]";
    public static final String firstCamera = "0", secondCamera = "1", thirdCamera = "2";
    public static final int delay_3 = 3000, delay_60 = 60000;
    //-------------------------------------------------------------------------------
    public static final boolean Open_f_Camera = true, Open_s_Camera = false, Open_t_Camera = false;
    public static final boolean Open_Audio = false;
    //TODO 是否啟用keepScreen
    public static boolean keepScreen = true;
    //TODO 是否啟用preview
    public static boolean preview = false;
    //TODO 是否使用SD_Mode
    public static boolean SD_Mode = false;
    //TODO 是否啟用onPause
    public static boolean autoPause = true;
    //TODO 是否啟用重啟功能
    public static boolean autoRestart = true;
    //TODO 是否啟用60s停止錄影
    public static boolean autoStopRecord = true;
    //-------------------------------------------------------------------------------
    public static boolean extraRecordStatus = true, onRestart = false;
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    //-------------------------------------------------------------------------------
    private BroadcastReceiver mBroadcastReceiver;
    private Handler resetHandler;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float value = 0.0f;
    private Camera camera;

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
                File file = new File(getSDPath(), logName);
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
//                                time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
//                                        + " run:" + logs.runTime + " -> ";
                            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            logString.append(dateFormat.format(logs.time)).append(" run:")
                                    .append(logs.runTime).append(" -> ").append(logs.msg).append("\r\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getSDPath(), logName), !reFormat);
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
        //setProp();
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
            new Handler().post(() -> saveLog(getContext(), false, false));
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
        Activity activity = getActivity();
        isSave = !getSDPath().equals("");
        Log.e(TAG, "#initial");
        videoLogList.add(new mLogMsg("#initial", mLog.v));
        checkSdCardFromFileList();
        Objects.requireNonNull(activity).findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new mLogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
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
                        int id = Integer.parseInt(CameraId);
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
                            int id = Integer.parseInt(CameraId);
                            if (isOpenCamera.get(id)) {
                                new Handler().postDelayed(() -> recordHandler.get(id).obtainMessage().sendToTarget(), delay);
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
        if (keepScreen)
            Objects.requireNonNull(getActivity()).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (String CameraId : allCamera) {
            /* this function will error with camera changed. */
            int id = Integer.parseInt(CameraId);
            if (isOpenCamera.get(id)) {
                try {
                    setCamera(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("setCallback " + CameraId + " is error.", mLog.w));
                }
            }
        }
        getActivity().registerReceiver(mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    Log.e(TAG, "Battery:" + intent.getIntExtra("level", 0) + "%");
                    videoLogList.add(new mLogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    saveLog(getContext(), false, false);
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        resetHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (autoPause || extraRecordStatus)
                    isRecordStart(true);
                saveLog(getContext(), false, false);
            }
        };
        new Handler().postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), delay_3 / 2);
    }

    private void stopBackgroundThread() {
        Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    @SuppressLint("SetTextI18n")
    private void startRecord(String CameraId) {
        /* this function will error with camera changed. */
        int id = Integer.parseInt(CameraId);
        if (!isError) {
            Log.e(TAG, "startRecord " + CameraId);
            try {
                if (isCameraOne(CameraId)) {
                    checkSdCardFromFileList();
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        if (mTimer == null) {
                            onRun = getIsRun();
                            onFail = getFail();
                            onSuccess = getSuccess();
                            timerTask = new mTimerTask();
                            value = 0.0f;
                            mTimer = new Timer(true);
                            ((TextView) getActivity().findViewById(R.id.record_timer)).setText("00");
                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Recording");
                        }
                    });
                }
                SurfaceHolder holder = null;
                try {
                    holder = surfaceview.get(id).getHolder();
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("getSurfaceTexture" + CameraId + " is error."));
                }
                if (null == holder) {
                    Log.e(TAG, isRun + " texture is null, return");
                    return;
                }
                holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
                holder.setFixedSize(1080, 1920);
                holder.addCallback(new SurfaceHolder.Callback() {
                    public void surfaceCreated(@NonNull SurfaceHolder holder) {
                        try {
                            camera = Camera.open();
                            camera.setPreviewDisplay(holder);
                            camera.startPreview();
                        } catch (Exception e) {
                            errorMessage("Camera open error. <============ Crash here", true, e);
                            e.printStackTrace();
                        }
                    }

                    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {

                    }

                    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
                        try {
                            camera.stopPreview();
                            //關閉預覽
                            camera.release();
                        } catch (Exception e) {
                            errorMessage("Camera close error. <============ Crash here", true, e);
                            e.printStackTrace();
                        }
                    }
                });
                mediaRecorder.set(id, setUpMediaRecorder(CameraId));
                try {
                    if (isCameraOne(CameraId))
                        mTimer.schedule(timerTask, 100, 100);

                    if (null != mediaRecorder.get(id)) {
                        mediaRecorder.get(id).start();
                        codeDate.set(id, getCalendarTime());
                        Message msg = stopRecordHandler.get(id).obtainMessage();
                        msg.obj = codeDate.get(id);
                        int delay = 0;
                        if (!isCameraOne(CameraId)) {
                            for (String c : allCamera) {
                                if (isOpenCamera.get(Integer.parseInt(c))) {
                                    delay += id * 600;
                                    if (c.equals(CameraId))
                                        break;
                                }
                            }
                        }
                        if (autoStopRecord)
                            stopRecordHandler.get(id).sendMessageDelayed(msg, delay_60 + delay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage("Camera " + CameraId + " can't record. <============ Crash here", false, e);
                }
            } catch (Exception e) {
                e.printStackTrace();
                errorMessage("Camera " + CameraId + " startRecord error. <============ Crash here", true, e);
            }
        } else {
            if (autoRestart) {
                new Handler().postDelayed(this::restartApp, delay_3);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void stopRecord(String date, String CameraId) {
        /* this function will error with camera changed. */
        int id = Integer.parseInt(CameraId);
        try {
            if (date.equals(codeDate.get(id))) {
                isRun++;
                videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                Log.e(TAG, isRun + " stopRecord " + CameraId);
                if (isCameraOne(CameraId))
                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        ((TextView) getActivity().findViewById(R.id.record_status)).setText("Stop");
                    });
                codeDate.set(id, getCalendarTime());
                try {
                    closeMediaRecorder(CameraId);
                    checkAndClear(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("Check file is fail."));
                }
                startRecord(CameraId);
                if (isError || !isSave) {
                    isRun = 0;
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
                for (String CameraId : allCamera) {    // 遍例Camera
                    /* this function will error with camera changed. */
                    int id = Integer.parseInt(CameraId);
                    if (isOpenCamera.get(id)) {  // 檢查OpenCamera
                        videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                        Log.e(TAG, isRun + " stopRecord " + CameraId);
                        if (isCameraOne(CameraId))
                            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                                if (mTimer != null) {
                                    mTimer.cancel();
                                    mTimer = null;
                                }
                                ((TextView) getActivity().findViewById(R.id.record_status)).setText("Stop");
                            });
                        codeDate.set(id, getCalendarTime());
                        closeMediaRecorder(CameraId);
                        checkAndClear(CameraId);
                    }
                }
            }
            isRun = 0;
//            isFinish = 0;
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
        int id = Integer.parseInt(CameraId);
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
        final AlertDialog dialog = new AlertDialog.Builder(getContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
                .setView(view).setCancelable(true).create();

        view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
            videoLogList.add(new mLogMsg("@Log_ok", mLog.v));
            dialog.dismiss();
        });
        ArrayList<String> list = new ArrayList<>();
        if (isSave)
            if (end)
                list.add("CheckFile -> video_success/fail:(" + getSuccess() + "/" + getFail() + ") app_reset:(" + getReset() + ")");
            else
                list.add("App Version:" + this.getString(R.string.app_name));
        else list.add(NO_SD_CARD);

        if (!errorMessage.equals(""))
            list.add(errorMessage);
        else {
            for (String CameraId : allCamera) {
                /* this function will error with camera changed. */
                int id = Integer.parseInt(CameraId);
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
            ((ListView) view.findViewById(R.id.dialog_listview)).setSelection(items.size() - 1);
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
            for (String CameraId : allCamera) {
                int id = Integer.parseInt(CameraId);
                if (isOpenCamera.get(id)) {
                    closeMediaRecorder(CameraId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("onDestroy error.", false, e);
        }
        try {
            if (null != home)
                home.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        new Handler().post(() -> stopRecordAndSaveLog(false));
    }

    private void stopRecordAndSaveLog(boolean kill) {
        if (isRecord)
            new Handler().post(() -> stopRecord(true));
        saveLog(getContext(), false, kill);
    }

    @SuppressLint("HandlerLeak")
    private void setCamera(String CameraId) {
        /* this function will error with camera changed. */
        int id = Integer.parseInt(CameraId);
        surfaceview.set(id, Objects.requireNonNull(getActivity()).findViewById(id_surfaceView.get(id)));
        cameraFilePath.set(id, new ArrayList<>());
        codeDate.set(id, getCalendarTime());
        videoLogList.add(new mLogMsg("setCallback " + CameraId + ".", mLog.w));
        recordHandler.set(id, new Handler() {
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
        stopRecordHandler.set(id, new Handler() {
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
        new Handler().post(() -> stopRecordAndSaveLog(false));
        if (reset) {
            new Handler().postDelayed(this::restartApp, delay_3);
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

    private void closeMediaRecorder(String CameraId) {
        /* this function will error with camera changed. */
        int id = Integer.parseInt(CameraId);
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
        int id = Integer.parseInt(CameraId);
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
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediaRecorder.setVideoEncodingBitRate(2000000);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                mediaRecorder.setOutputFile(file);
                mediaRecorder.setPreviewDisplay(
                        surfaceview.get(id).getHolder().getSurface());
                mediaRecorder.prepare();
            } else {
                errorMessage("MediaRecorder error. " + NO_SD_CARD + " <============ Crash here", false, null);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
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
                Log.e(TAG, "Size:"+ gigaAvailable + " gb");
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
            home = new HomeListen(getContext());
            home.setOnHomeBtnPressListener(new HomeListen.OnHomeBtnPressListener() {
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

    private class mTimerTask extends TimerTask {
        @SuppressLint("SetTextI18n")
        public void run() {
            // mHandlerを通じてUI Threadへ処理をキューイング
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                //実行間隔分を加算処理
                value += 0.1d;
                //計算にゆらぎがあるので小数点第1位で丸める
                BigDecimal bi = new BigDecimal(value);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
                if (autoStopRecord && outputValue >= 65) {
                    errorMessage("Application has timed out.", true, null);
                }
                //現在のLapTime
                ((TextView) getActivity().findViewById(R.id.record_timer)).setText(Float.toString(outputValue));
            });
        }
    }
}
