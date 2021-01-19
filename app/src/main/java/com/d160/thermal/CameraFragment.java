package com.d160.thermal;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
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
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.d160.view.CustomTextView;
import com.d160.view.HomeListen;
import com.d160.view.mListAdapter;
import com.d160.view.mLog;
import com.d160.view.mLogMsg;

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
import java.util.concurrent.atomic.AtomicReferenceArray;

import static android.os.Looper.getMainLooper;
import static com.d160.thermal.Utils.EXTRA_VIDEO_FAIL;
import static com.d160.thermal.Utils.EXTRA_VIDEO_RECORD;
import static com.d160.thermal.Utils.EXTRA_VIDEO_RESET;
import static com.d160.thermal.Utils.EXTRA_VIDEO_RUN;
import static com.d160.thermal.Utils.EXTRA_VIDEO_SUCCESS;
import static com.d160.thermal.Utils.Fail;
import static com.d160.thermal.Utils.LOG_TITLE;
import static com.d160.thermal.Utils.NO_SD_CARD;
import static com.d160.thermal.Utils.Success;
import static com.d160.thermal.Utils.allCamera;
import static com.d160.thermal.Utils.backgroundHandler;
import static com.d160.thermal.Utils.cameraDevice;
import static com.d160.thermal.Utils.cameraFile;
import static com.d160.thermal.Utils.cameraFilePath;
import static com.d160.thermal.Utils.codeDate;
import static com.d160.thermal.Utils.delete;
import static com.d160.thermal.Utils.errorMessage;
import static com.d160.thermal.Utils.getCalendarTime;
import static com.d160.thermal.Utils.getFail;
import static com.d160.thermal.Utils.getIsRun;
import static com.d160.thermal.Utils.getPath;
import static com.d160.thermal.Utils.getReset;
import static com.d160.thermal.Utils.getSDPath;
import static com.d160.thermal.Utils.getSuccess;
import static com.d160.thermal.Utils.id_textView;
import static com.d160.thermal.Utils.isCameraOne;
import static com.d160.thermal.Utils.isCameraOpened;
import static com.d160.thermal.Utils.isError;
import static com.d160.thermal.Utils.isOpenCamera;
import static com.d160.thermal.Utils.isReady;
import static com.d160.thermal.Utils.isRecord;
import static com.d160.thermal.Utils.isRun;
import static com.d160.thermal.Utils.isSave;
import static com.d160.thermal.Utils.logName;
import static com.d160.thermal.Utils.mediaRecorder;
import static com.d160.thermal.Utils.previewSession;
import static com.d160.thermal.Utils.recordHandler;
import static com.d160.thermal.Utils.sdData;
import static com.d160.thermal.Utils.stateCallback;
import static com.d160.thermal.Utils.stopRecordHandler;
import static com.d160.thermal.Utils.textView;
import static com.d160.thermal.Utils.thread;
import static com.d160.thermal.Utils.threadString;
import static com.d160.thermal.Utils.videoLogList;

public class CameraFragment extends Fragment {

    public static final String TAG = "com.d160.thermal";
    public static final String firstCamera = "0", secondCamera = "1", thirdCamera = "2";
    public static final boolean Open_f_Camera = true, Open_s_Camera = true, Open_t_Camera = false;
    public static final boolean audio = true;
    public static final int delay_3 = 3000, delay_60 = 60600;
    //-------------------------------------------------------------------------------
    //TODO 是否啟用keepScreen
    public static boolean keepScreen = true;
    //TODO 是否啟用preview
    public static boolean preview = false;
    //TODO 是否使用SD_Mode
    public static boolean SD_Mode = true;
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
    private Handler mainHandler, resetHandler;
    private Size mPreviewSize = null;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float mLaptime = 0.0f;

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
        //#onPause -> #onResume -> #onPause -> #onResume..
        return inflater.inflate(R.layout.fragment_camera, container, false);
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
//        extraRecordStatus =  activity.getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        if (onReset != 0)
            videoLogList.add(new mLogMsg("#noReset:" + onReset, mLog.v));
    }

    private void isRecordStart(boolean auto) {
        if (!isError && isSave) {
            if (isReady) {
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
                            int id = Integer.parseInt(CameraId);
                            if (isOpenCamera.get(id)) {
                                new Handler().postDelayed(() -> recordHandler.get(id).obtainMessage().sendToTarget(), delay);
                                delay += delay_3 / 3;
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
            Log.e(TAG, "#onResume");
            videoLogList.add(new mLogMsg("#onResume", mLog.v));
            if (autoPause)
                startBackgroundThread();
        } else {
            showPermission();
        }
    }

    // TODO onPause 螢幕關閉
    public void onPause() {
        super.onPause();
        if (autoPause && !checkPermission(Objects.requireNonNull(getContext()))) {
            Log.e(TAG, "#onPause");
            videoLogList.add(new mLogMsg("#onPause", mLog.v));
            if (isRecord)
                isRecordStart(false);
            closeCamera();
            stopBackgroundThread();
            isReady = false;
            saveLog(getContext(), false, false);
        }
    }

    private void showPermission() {
        videoLogList.add(new mLogMsg("#showPermission", mLog.v));
        // We don't have permission so prompt the user
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        requestPermissions(permissions.toArray(new String[0]), 0);
    }

    private boolean checkPermission(@NonNull Context context) {
        videoLogList.add(new mLogMsg("#checkPermission", mLog.v));
        int CAMERA = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA);
        int AUDIO = ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO);
        int ExSTORAGE = ActivityCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return permission(CAMERA) || permission(AUDIO) || permission(ExSTORAGE);
    }

    private boolean permission(int mp) {
        return mp != PackageManager.PERMISSION_GRANTED;
    }

    @Override
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

    private void openCamera(String CameraId) {
        int id = Integer.parseInt(CameraId);
        if (ContextCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        videoLogList.add(new mLogMsg("#Open Camera" + CameraId + ".", mLog.w));
        Log.e(TAG, "camera ID: " + CameraId);
        Activity activity = getActivity();
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(CameraId)
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                    .getOutputSizes(SurfaceTexture.class)[0];
            Log.e(TAG, "camera " + CameraId + " is open");
            manager.openCamera(CameraId, stateCallback.get(id), mainHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeCamera() {
        for (String CameraId : allCamera) {
            int id = Integer.parseInt(CameraId);
            try {
                if (isOpenCamera.get(id)) {
                    if (null != previewSession.get(id)) {
                        previewSession.get(id).close();
                        previewSession.set(id, null);
                    }
                    if (null != cameraDevice.get(id)) {
                        cameraDevice.get(id).close();
                        cameraDevice.set(id, null);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Interrupted while trying to lock camera closing.", e);
            }
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
            int id = Integer.parseInt(CameraId);
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
                if (textView.get(id).isAvailable())
                    openCamera(CameraId);
                else
                    textView.get(id).setSurfaceTextureListener(new mSurfaceTextureListener(CameraId));
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
        if (!isReady) {
            isReady = true;
            resetHandler = new Handler() {
                public void handleMessage(Message msg) {
                    if (autoPause || extraRecordStatus)
                        isRecordStart(true);
                    saveLog(getContext(), false, false);
                }
            };
            new Handler().postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), delay_3 / 2);
        }
    }

    private void stopBackgroundThread() {
        Objects.requireNonNull(getActivity()).getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (String CameraId : allCamera) {
            int id = Integer.parseInt(CameraId);
            if (isOpenCamera.get(id)) {
                thread.get(id).quitSafely();
                try {
                    thread.get(id).join();
                    thread.set(id, null);
                    backgroundHandler.set(id, null);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        getActivity().unregisterReceiver(mBroadcastReceiver);
    }

    private void takePreview(String CameraId) {
        int id = Integer.parseInt(CameraId);
        Log.e(TAG, "takePreview " + CameraId);
        videoLogList.add(new mLogMsg("Preview " + CameraId + " Camera.", mLog.i));
        try {
            SurfaceTexture texture = null;
            try {
                texture = textView.get(id).getSurfaceTexture();
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
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                // When the session is ready, we start displaying the preview.
                                previewSession.set(id, session);
                                updatePreview(mPreviewBuilder, previewSession.get(id), backgroundHandler.get(id));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            showToast("Camera " + CameraId + " Failed");
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
    private void startRecord(String CameraId) {
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
                            mLaptime = 0.0f;
                            mTimer = new Timer(true);
                            ((TextView) getActivity().findViewById(R.id.record_timer)).setText("00");
                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Recording");
                        }
                    });
                    CameraManager manager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
                    mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(CameraId)
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                            .getOutputSizes(SurfaceTexture.class)[0];
                }
                try {
                    closePreviewSession(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    errorMessage("closePreviewSession error.", true, e);
                }
                SurfaceTexture texture = null;
                try {
                    codeDate.set(id, getCalendarTime());
                    texture = textView.get(id).getSurfaceTexture();
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("getSurfaceTexture" + CameraId + " is error."));
                }
                if (null == texture) {
                    Log.e(TAG, isRun + " texture is null, return");
                    return;
                }
                texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
                Surface surface = new Surface(texture);
                List<Surface> surfaces = new ArrayList<>();
                CaptureRequest.Builder mPreviewBuilder;
                CameraDevice mCameraDevice;
                Surface recorderSurface;
                Handler backgroundHandler;
                mCameraDevice = cameraDevice.get(id);
                backgroundHandler = Utils.backgroundHandler.get(id);
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
                                            if (isCameraOne(CameraId))
                                                mTimer.schedule(timerTask, 100, 100);
                                            previewSession.set(id, session);
                                            updatePreview(mPreviewBuilder, session, Utils.backgroundHandler.get(id));
                                            if (null != mediaRecorder.get(id)) {
                                                mediaRecorder.get(id).start();
                                                Message msg = stopRecordHandler.get(id).obtainMessage();
                                                msg.obj = codeDate.get(id);
                                                if (autoStopRecord)
                                                    stopRecordHandler.get(id).sendMessageDelayed(msg, delay_60 + (id * 600));
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                            errorMessage("Camera " + CameraId + " can't record. <============ Crash here", false, e);
                                        }
                                    }

                                    public void onConfigureFailed(@NonNull CameraCaptureSession Session) {
                                        errorMessage("Camera " + CameraId + " Record onConfigureFailed.", true, null);
                                    }
                                }, backgroundHandler);
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
                new Handler().postDelayed(this::restartApp, delay_3);
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void stopRecord(String date, String CameraId) {
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
//                if (isFinish == 999 || isRun < isFinish - (isCameraOne(cameraId) ? 1 : 0)) {
                startRecord(CameraId);
//                } else {
//                    if (isLastCamera(CameraId))
//                        isRecordStart(true);
//                }
                if (isError || !isSave) {
                    isRun = 0;
//                    isFinish = 0;
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
//                if (!reset) {
//                    Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
//                        if (getFail() + getReset() == 0) {
//                            videoLogList.add(new mLogMsg("#Pass"));
//                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Pass");
//                        } else {
//                            videoLogList.add(new mLogMsg("#Fail"));
//                            ((TextView) getActivity().findViewById(R.id.record_status)).setText("Fail " + getFail() + getReset());
//                        }
//                    });
//                    new Handler().postDelayed(() -> showDialogLog(true), delay_3);
//                }
                for (String CameraId : allCamera) {    // 遍例Camera
                    if (isOpenCamera.get(Integer.parseInt(CameraId))) {  // 檢查OpenCamera
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
                        codeDate.set(Integer.parseInt(CameraId), getCalendarTime());
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
            if (preview) {
                takePreview();
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("stopRecord all camera error. <============ Crash here", true, e);
        }
    }

    private void checkAndClear(String CameraId) {
        int id = Integer.parseInt(CameraId);
        try {
            if (isRecord)
                if (isOpenCamera.get(id) && null != cameraFilePath.get(id)) {
                    for (String f : cameraFilePath.get(id)) {
                        checkFile(f);
                    }
                    cameraFilePath.get(id).clear();
                }
        } catch (Exception e) {
            videoLogList.add(new mLogMsg("CheckFile " + CameraId + " error.", mLog.e));
        }
    }

    private void takePreview() {
        if (!isError && isSave) {
            for (String CameraId : allCamera) {
                if (isOpenCamera.get(Integer.parseInt(CameraId)))
                    takePreview(CameraId);
            }
        } else {
            stopRecordAndSaveLog(false);
            showDialogLog(false);
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

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            for (String CameraId : allCamera) {
                int id = Integer.parseInt(CameraId);
                if (isOpenCamera.get(id)) {
                    closeStateCallback(CameraId);
                    closePreviewSession(CameraId);
                    closeMediaRecorder(CameraId);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("onDestroy error.", false, e);
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
        int id = Integer.parseInt(CameraId);
        textView.set(id, Objects.requireNonNull(getActivity()).findViewById(id_textView.get(id)));
        cameraFilePath.set(id, new ArrayList<>());
        codeDate.set(id, getCalendarTime());
        stateCallback.set(id, setCallback(CameraId));
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

    private CameraDevice.StateCallback setCallback(String CameraId) {
        CameraDevice.StateCallback callback;
        try {
            callback = new CameraDevice.StateCallback() {
                final int id = Integer.parseInt(CameraId);

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
        try {
            if (null != home)
                home.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    private void closeCameraDevice(String CameraId) {
        int id = Integer.parseInt(CameraId);
        try {
            if (null != cameraDevice.get(id)) {
                cameraDevice.get(id).close();
                cameraDevice.set(id, null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            videoLogList.add(new mLogMsg("closeCameraDevice " + CameraId + " is error."));
        }
    }

    private void closeStateCallback(String CameraId) {
        int id = Integer.parseInt(CameraId);
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
        int id = Integer.parseInt(CameraId);
        try {
            if (null != previewSession.get(id)) {
                previewSession.get(id).close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            errorMessage("closePreviewSession" + CameraId + " is error.", false, e);
        }
    }

    private void closeMediaRecorder(String CameraId) {
        int id = Integer.parseInt(CameraId);
        if (null != mediaRecorder.get(id)) {
            try {
                mediaRecorder.get(id).stop();
                mediaRecorder.get(id).release();
                mediaRecorder.set(id, null);
                videoLogList.add(new mLogMsg("Record " + CameraId + " finish."));
            } catch (Exception e) {
                e.printStackTrace();
                videoLogList.add(new mLogMsg("closeMediaRecorder " + CameraId + " is error."));
            }
        }
    }

    private MediaRecorder setUpMediaRecorder(String CameraId) {
        int id = Integer.parseInt(CameraId);
        String file;
        MediaRecorder mediaRecorder = null;
        try {
            if (!getSDPath().equals("")) {
                file = getSDPath() + getCalendarTime(CameraId) + ".mp4";
                videoLogList.add(new mLogMsg("Create: " + file.split("/")[3], mLog.w));
                cameraFile.set(id, file + "");
                cameraFilePath.get(id).add(file);
                CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
                if (audio && isCameraOne(CameraId))
                    videoLogList.add(new mLogMsg("#audio"));
                mediaRecorder = new MediaRecorder();
                if (audio && isCameraOne(CameraId))
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
                mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                if (audio && isCameraOne(CameraId))
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
                mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
                if (audio && isCameraOne(CameraId))
                    mediaRecorder.setAudioEncodingBitRate(96000);
                mediaRecorder.setVideoEncodingBitRate(2000000);
                if (audio && isCameraOne(CameraId)) {
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
                mLaptime += 0.1d;
                //計算にゆらぎがあるので小数点第1位で丸める
                BigDecimal bi = new BigDecimal(mLaptime);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
                if (autoStopRecord && outputValue >= 65) {
                    errorMessage("Application has timed out.", true, null);
                }
                //現在のLapTime
                ((TextView) getActivity().findViewById(R.id.record_timer)).setText(Float.toString(outputValue));
            });
        }
    }

    private class mSurfaceTextureListener implements TextureView.SurfaceTextureListener {
        String CameraId;

        public mSurfaceTextureListener(String CameraId) {
            this.CameraId = CameraId;
        }

        public void onSurfaceTextureAvailable(SurfaceTexture texture, int width, int height) {
            openCamera(CameraId);
        }

        public void onSurfaceTextureSizeChanged(SurfaceTexture texture, int width, int height) {

        }

        public boolean onSurfaceTextureDestroyed(SurfaceTexture texture) {
            return true;
        }

        public void onSurfaceTextureUpdated(SurfaceTexture texture) {
        }
    }
}
