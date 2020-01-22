package com.askey.record;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.os.SystemProperties;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.askey.widget.CustomPageTransformer;
import com.askey.widget.CustomTextView;
import com.askey.widget.LogMsg;
import com.askey.widget.PropertyUtils;
import com.askey.widget.VerticalViewPager;
import com.askey.widget.mListAdapter;
import com.askey.widget.mLog;
import com.askey.widget.mPagerAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_FINISH;
import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_FINISHa;
import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_START;
import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_STARTa;
import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_TEST;
import static com.askey.record.Utils.COMMAND_VIDEO_RECORD_TESTa;
import static com.askey.record.Utils.FRAMESKIP;
import static com.askey.record.Utils.TAG;
import static com.askey.record.Utils.config;
import static com.askey.record.Utils.failed;
import static com.askey.record.Utils.fileName;
import static com.askey.record.Utils.getCalendarTime;
import static com.askey.record.Utils.getFrameRate;
import static com.askey.record.Utils.getSDCardPath;
import static com.askey.record.Utils.getVideo;
import static com.askey.record.Utils.isInteger;
import static com.askey.record.Utils.isRun;
import static com.askey.record.Utils.logName;
import static com.askey.record.Utils.readConfigFile;
import static com.askey.record.Utils.successful;
import static com.askey.record.Utils.writeConfigFile;

public class VideoRecordActivity extends Activity {

    public static String firstCamera = "0";
    public static String secondCamera = "1";
    public static String lastfirstCamera = "0";
    public static String lastsecondCamera = "1";
    private int isFinish = 1, delayTime = 600000, isFrame = 0, isQuality = 0;
    private boolean isReady = false, isRecord = false, isLoop = false, isError = false;
    private String filePath = "/sdcard/";
    private ArrayList<String> firstFilePath, secondFilePath;
    private ArrayList<LogMsg> videoLogList;
    private Size mPreviewSize;
    private TextureView mTextureView0, mTextureView1;
    private CameraDevice mCameraDevice0, mCameraDevice1;
    private CameraCaptureSession mPreviewSession0, mPreviewSession1;
    private CaptureRequest.Builder mPreviewBuilder0, mPreviewBuilder1;
    private ListView mListView;
    private MediaRecorder mMediaRecorder0, mMediaRecorder1;
    private MediaPlayer mMediaPlayer;
    private Handler mainHandler, backgroundHandler, soundHandler, demoHandler;
    private Runnable sound = new Runnable() {

        public void run() {
            if (isLoop && isRun < isFinish) {
                playMusic(R.raw.scanner_beep);
                soundHandler.postDelayed(this, 10000);
            }
        }
    };
    private CameraDevice.StateCallback mStateCallback0 = new CameraDevice.StateCallback() {

        public void onOpened(CameraDevice camera) {
            if (!isReady) {
                // 打开摄像头
                Log.e(TAG, "onOpened");
                toast("Camera " + firstCamera + " is opened.", mLog.i);
            }
            mCameraDevice0 = camera;
            // 开启预览
            takePreview(firstCamera);
        }

        public void onDisconnected(CameraDevice camera) {
            camera.close();
            // 关闭摄像头
            Log.e(TAG, "onDisconnected");
            toast("Camera " + firstCamera + " is disconnected.", mLog.w);
            if (null != mCameraDevice0) {
                mCameraDevice0.close();
                mCameraDevice0 = null;
            }
        }

        public void onError(CameraDevice camera, int error) {
            onDisconnected(camera);
            // 前鏡頭開啟失敗
            Log.e(TAG, "onError");
            toast("Open Camera " + firstCamera + " error.", mLog.e);
            isError = true;
        }
    };
    private CameraDevice.StateCallback mStateCallback1 = new CameraDevice.StateCallback() {

        public void onOpened(CameraDevice camera) {
            if (!isReady) {
                // 打开摄像头
                Log.e(TAG, "onOpened");
                toast("Camera " + secondCamera + " is opened.", mLog.i);
            }
            mCameraDevice1 = camera;
            // 开启预览
            takePreview(secondCamera);
            if (!isReady) {
                isReady = true;
                demoHandler.obtainMessage().sendToTarget(); // playDEMO
                if (isError) {
                    isFinish = 0;
                    isLoop = true;
                    runOnUiThread(() -> stopRecord(false));
                }
            }
        }

        public void onDisconnected(CameraDevice camera) {
            camera.close();
            // 关闭摄像头
            Log.e(TAG, "onDisconnected");
            toast("Camera " + secondCamera + " is disconnected.", mLog.w);
            if (null != mCameraDevice1) {
                mCameraDevice1.close();
                mCameraDevice1 = null;
            }
        }

        public void onError(CameraDevice camera, int error) {
            onDisconnected(camera);
            // 前鏡頭開啟失敗
            Log.e(TAG, "onError");
            toast("Open Camera " + secondCamera + " error.", mLog.e);
        }
    };
    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(COMMAND_VIDEO_RECORD_TEST) || action.equals(COMMAND_VIDEO_RECORD_TESTa)) {
                if (isReady) {
                    if (!isLoop) {
                        // ReCheckConfig
                        checkConfigFile(new File(filePath, fileName), false);
                        isFinish = 0;
                        // isLoop = true;
                        firstFilePath.clear();
                        secondFilePath.clear();
                        soundHandler.post(sound);
                        takeRecord(5000, false);
                    } else {
                        isFinish = 0;
                        runOnUiThread(() -> stopRecord(true));
                    }
                } else toast("Not Ready to Record.");
            }
            if (action.equals(COMMAND_VIDEO_RECORD_START) || action.equals(COMMAND_VIDEO_RECORD_STARTa)) {
                runLoop();
            }
            if (action.equals(COMMAND_VIDEO_RECORD_FINISH) || action.equals(COMMAND_VIDEO_RECORD_FINISHa)) {
                Log.d("VideoRecord", "finish");
                isFinish = 0;
                isLoop = true;
                runOnUiThread(() -> stopRecord(false));
            }
        }
    };

    private void fullScreenCall() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    public static int getRunTime() {
        return isRun;
    }

    public static int getSuccessful() {
        return successful;
    }

    public static int getFailed() {
        return failed;
    }

    private void runLoop() {
        if (isReady)
            if (!isLoop) {
                // ReCheckConfig
                checkConfigFile(new File(filePath, fileName), false);
                isRun = 0;
                isLoop = true;
                firstFilePath.clear();
                secondFilePath.clear();
                soundHandler.post(sound);
                takeRecord(delayTime, false);
            } else {
                isFinish = 0;
                runOnUiThread(() -> stopRecord(true));
            }
    }

    protected void onResume() {
        super.onResume();
        videoLogList = new ArrayList();
        if (checkPermission()) {
            showPermission();
        } else {
            if (checkConfigFile(true)) {
                //TODO SETPROP
                // -> adb shell su 0 getprop persist.our.camera.frameskip
//                OurSystemProperties.set(FRAMESKIP, "0"); //*lib(com.our.sdk).jar
//                SystemProperties.set(FRAMESKIP, "0"); //*lib(layoutlib).jar
                PropertyUtils.set(FRAMESKIP, "0"); //*reflection invoke
//                CommandUtil.executed("setprop "+FRAMESKIP+" 0"); //*not work
                setStart();
            } else finish();
        }
    }

    private void setTestTime(int min) {
        if (min > 0) {
            isFinish = min;
            toast("setRecord time: " + min + "0 min.");
        } else {
            toast("The test time must be a positive number.", mLog.e);
        }
    }

    private boolean checkConfigFile(boolean first) {
        videoLogList.add(new LogMsg("#checkConfigFile", mLog.v));
        if (!"".equals(getSDCardPath())) {
            File file = new File(getSDCardPath(), fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toast("Create the config file.", mLog.w);
                writeConfigFile(file, config);
            } else {
                if (!isRecord) toast("Find the config file.", mLog.d);
                checkConfigFile(new File(getSDCardPath(), fileName), first);
            }
            return true;
        } else {
            toast("Please check the SD card.", mLog.e);
        }
        return false;
    }

    private void checkConfigFile(File file, boolean firstOne) {
        String input = readConfigFile(file);
        if (input.length() > 0) {
            List<String> read = Arrays.asList(input.split("\r\n"));
            boolean target = false;
            int t;
            String code = "total_test_minute = ";
            String first = "firstCameraID = ", second = "secondCameraID = ";
            for (String s : read)
                if (s.indexOf(code) != -1) {
                    target = true;
                    t = s.indexOf(code) + code.length();
                    code = s.substring(t);
                    break;
                }
            for (String s : read)
                if (s.indexOf(first) != -1) {
                    target = true;
                    t = s.indexOf(first) + first.length();
                    first = s.substring(t);
                    toast("firstCameraID: " + first);
                    break;
                }
            for (String s : read)
                if (s.indexOf(second) != -1) {
                    target = true;
                    t = s.indexOf(second) + second.length();
                    second = s.substring(t);
                    toast("secondCameraID: " + second);
                    break;
                }

            if (target) {
                boolean reformat = false;
                if (!first.equals(second)) {
                    if (isCameraID(first.split("\n")[0], second.split("\n")[0])) {
                        lastfirstCamera = firstOne ? first : firstCamera;
                        lastsecondCamera = firstOne ? second : secondCamera;
                        firstCamera = first;
                        secondCamera = second;
                    } else reformat = true;
                } else {
                    toast("Cannot use the same Camera ID.", mLog.e);
                    reformat = true;
                }
                if (isInteger(code.split("\n")[0], true)) {
                    int min = Integer.parseInt(code.split("\n")[0]);
                    setTestTime(min);
                } else reformat = true;
                if (reformat) {
                    reformatConfigFile(file, config);
                }
            } else reformatConfigFile(file, config);
        } else reformatConfigFile(file, config);
    }

    private boolean isCameraID(String f, String b) {
        try {
            if (Integer.parseInt(f) <= -1) {
                toast("The Camera ID must be a positive number.", mLog.e);
                return false;
            } else {
                boolean cameraID;
                switch (Integer.parseInt(f)) {
                    case 0:
                    case 1:
                    case 2:
                        cameraID = true;
                        break;
                    default:
                        cameraID = false;
                        break;
                }
                if (!cameraID) {
                    toast("The Camera ID is unknown.", mLog.e);
                    return false;
                }
            }
            if (Integer.parseInt(b) <= -1) {
                toast("The Camera ID must be a positive number.", mLog.e);
                return false;
            } else {
                boolean cameraID;
                switch (Integer.parseInt(b)) {
                    case 0:
                    case 1:
                    case 2:
                        cameraID = true;
                        break;
                    default:
                        cameraID = false;
                        break;
                }
                if (!cameraID) {
                    toast("The Camera ID is unknown.", mLog.e);
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    private void reformatConfigFile(File file, String[] message) {
        firstCamera = "0";
        secondCamera = "1";
        isFinish = 1;
        toast("Config file error.", mLog.e);
        writeConfigFile(file, message);
        toast("Reformat the file.", mLog.e);
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
                    toast("No permissions!");
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
        fullScreenCall();
        initial();
    }

    private void initial() {
        ArrayList<View> items_frame = new ArrayList();
        ArrayList<View> items_quality = new ArrayList();
        for (String frame : new ArrayList<>(Arrays.asList(new String[]{"27.5fps", "13.7fps"}))) {
            View vi = LayoutInflater.from(this).inflate(R.layout.style_vertical_item, null);
            CustomTextView item = vi.findViewById(R.id.customTextView);
            item.setText(frame);
            items_frame.add(vi);
        }
        for (String quality : new ArrayList<>(Arrays.asList(new String[]{"1080p", "720p"}))) {
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
        mListView = findViewById(R.id.list);
        mListView.setEnabled(false);
        toast("Initial now.", mLog.v);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
        mainHandler = new Handler(getMainLooper());
        soundHandler = new Handler();
        mTextureView0 = findViewById(R.id.surfaceView0);
        mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
        mTextureView1 = findViewById(R.id.surfaceView1);
        mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        findViewById(R.id.surfaceBackground).setOnClickListener((View v) -> fullScreenCall());
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            Log.d("VideoRecord", "finish");
            isFinish = 0;
            isLoop = true;
            runOnUiThread(() -> stopRecord(false));
        });
        findViewById(R.id.volume_down).setOnClickListener((View v) ->
                runOnUiThread(() -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)));
        findViewById(R.id.record).setOnClickListener((View v) ->
                runOnUiThread(() -> runLoop()));
        findViewById(R.id.volume_up).setOnClickListener((View v) ->
                runOnUiThread(() -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)));
        filePath = getSDCardPath();
        firstFilePath = new ArrayList();
        secondFilePath = new ArrayList();
        IntentFilter filter = new IntentFilter();
        filter.addAction(COMMAND_VIDEO_RECORD_TEST);
        filter.addAction(COMMAND_VIDEO_RECORD_START);
        filter.addAction(COMMAND_VIDEO_RECORD_FINISH);
        filter.addAction(COMMAND_VIDEO_RECORD_TESTa);
        filter.addAction(COMMAND_VIDEO_RECORD_STARTa);
        filter.addAction(COMMAND_VIDEO_RECORD_FINISHa);
        registerReceiver(myReceiver, filter);
        videoLogList.add(new LogMsg("#initial complete", mLog.v));
        mListView.setAdapter(new mListAdapter(this, videoLogList));
        // show selection DEMO
        demoHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Runnable r = () -> playMusic(R.raw.scanner_beep);
                new Handler().post(r);
                new Handler().postDelayed(() -> pager_Frame.setCurrentItem(1), 100);
                new Handler().postDelayed(() -> pager_Quality.setCurrentItem(1), 300);
                new Handler().postDelayed(() -> pager_Frame.setCurrentItem(0), 500);
                new Handler().postDelayed(() -> pager_Quality.setCurrentItem(0), 700);
                new Handler().postDelayed(r, 900);
                new Handler().postDelayed(r, 1200);
                new Handler().postDelayed(() -> checkSdCardFromFileList(getSDCardPath()), 1500);
                new Handler().postDelayed(() -> {
                    if (isError) {
                        isFinish = 0;
                        isLoop = true;
                        runOnUiThread(() -> stopRecord(false));
                    }
                }, 1500);
            }
        };
    }

    private void takeRecord(int delayMillis, boolean preview) {
        videoLogList.add(new LogMsg("#takeRecord(" + delayMillis + ")", mLog.v));
        //TODO SETPROP
        int delay = 0;
        String getFrameSkip = PropertyUtils.get(FRAMESKIP);
        if (null != getFrameSkip) {
            if (isInteger(getFrameSkip, false)) {
                //if frameskip is chehe or lastcamera != cameraid, delay 5s to change camera devices
                if ((Integer.parseInt(getFrameSkip) != isFrame) || lastfirstCamera != firstCamera || lastsecondCamera != secondCamera) {
                    SystemProperties.set(FRAMESKIP, isFrame == 1 ? "1" : "0");
                    videoLogList.add(new LogMsg("getFrameSkip:" + PropertyUtils.get(FRAMESKIP), mLog.e));
                    runOnUiThread(() -> setAdapter());
                    mStateCallback0.onDisconnected(mCameraDevice0);
                    mStateCallback1.onDisconnected(mCameraDevice1);
                    new Handler().post(() -> openCamera(firstCamera));
                    new Handler().post(() -> openCamera(secondCamera));
                    delay = 2000;
                }
            } else {
                toast("getFrameSkip error, fs(" + getFrameSkip + ") is not integer.", mLog.e);
                runOnUiThread(() -> setAdapter());
            }
        } else {
            toast("getFrameSkip error, fs == null.", mLog.e);
            runOnUiThread(() -> setAdapter());
        }

        new Handler().postDelayed(() -> new Thread(() -> startRecord(firstCamera)).start(), delay);
        new Handler().postDelayed(() -> new Thread(() -> startRecord(secondCamera)).start(), delay);
        new Handler().postDelayed(() -> runOnUiThread(() -> setAdapter()), delay);
        new Handler().postDelayed(() -> saveLog(), delay);
        new Handler().postDelayed(() -> stopRecord(preview), delay + delayMillis);
    }

    public boolean onKeyDown(int keyCode, KeyEvent event) {
        new Handler().post(() -> saveLog());
        return false;
    }

    private void saveLog() {
        String logString = "[VIDEO_RECORD_LOG]\r\n";
        if (!"".equals(getSDCardPath())) {
            File file = new File(getSDCardPath(), logName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    toast("Create the log file.", mLog.w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logString = "";
                //toast("Find the log file.", mLog.d);
            }

            for (LogMsg logs : videoLogList) {
                String time = logs.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + " run:" + logs.runTime + " -> ";
                logString += (time + logs.msg + "\r\n");
            }
            try {
                FileOutputStream output = new FileOutputStream(new File(filePath, logName), true);
                output.write(logString.getBytes());
                output.close();

                videoLogList.clear();
            } catch (IOException e) {
                toast("write failed.", mLog.e);
            }
        } else {
            toast("Please check the SD card.", mLog.e);
        }
    }

    protected void onDestroy() {
        isFinish = 0;
        isLoop = true;
        if (mMediaRecorder0 != null) {
            mMediaRecorder0.stop();
            mMediaRecorder0.reset();
            mMediaRecorder0 = null;
            toast("Record " + firstCamera + " finish.");
        }
        if (mMediaRecorder1 != null) {
            mMediaRecorder1.stop();
            mMediaRecorder1.reset();
            mMediaRecorder1 = null;
            toast("Record " + secondCamera + " finish.");
        }
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                toast("MediaPlay is Stop.");
            }
        }
        new Handler().post(() -> saveLog());
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }


    private void playMusic(int resources) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying())
                mMediaPlayer.stop();
        }
        AssetFileDescriptor file = this.getResources().openRawResourceFd(resources);
        if (file != null) {
            try {
                // Initialize the media player
                mMediaPlayer = new MediaPlayer();
                try {
                    mMediaPlayer.setDataSource(file.getFileDescriptor(), file.getStartOffset(), file.getLength());
                } finally {
                    file.close();
                }
                mMediaPlayer.prepareAsync();
                mMediaPlayer.setOnPreparedListener((MediaPlayer mps) -> {
                    // Start playing music
                    mps.start();
                });
            } catch (IOException e) {
                videoLogList.add(new LogMsg("#playMusic.error", mLog.e));
                runOnUiThread(() -> setAdapter());
                mMediaPlayer.release();
                e.printStackTrace();
            }
        } else {
            toast("playMusic not find.");
            runOnUiThread(() -> setAdapter());
        }
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
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mPreviewSize = map.getOutputSizes(SurfaceTexture.class)[0];
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            Log.e(TAG, "camera open");
            manager.openCamera(cameraId, isCameraOne(cameraId) ? mStateCallback0 : mStateCallback1, mainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera X");
    }

    private void stopRecord(boolean preview) {
        videoLogList.add(new LogMsg("#stopRecord", mLog.v));
        Log.d(TAG, "stopRecord");
        isRecord = false;
        if (mMediaRecorder0 != null) {
            mMediaRecorder0.stop();
            mMediaRecorder0.reset();   // You can reuse the object by going back to setAudioSource() step
            //mMediaRecorder.release(); // Now the object cannot be reused
            mMediaRecorder0 = null;
            toast("Record " + firstCamera + " finish.");
        }
        if (mMediaRecorder1 != null) {
            mMediaRecorder1.stop();
            mMediaRecorder1.reset();
            mMediaRecorder1 = null;
            toast("Record " + secondCamera + " finish.");
        }
        if (!preview) {
            if (!isLoop) {
                takePreview(firstCamera);
                takePreview(secondCamera);
                if (mMediaPlayer != null) {
                    if (mMediaPlayer.isPlaying()) {
                        mMediaPlayer.stop();
                        toast("MediaPlay is Stop.");
                    }
                }
            } else {
//                checkSdCardFromArrayList(filePath);
                deleteALL();
                isRun++;
                new Handler().post(() -> saveLog());
                if (firstFilePath.size() > 1) {
                    String first = firstFilePath.get(firstFilePath.size() - 1);
                    firstFilePath.clear();
                    firstFilePath.add(first);
                }
                if (secondFilePath.size() > 1) {
                    String second = secondFilePath.get(secondFilePath.size() - 1);
                    secondFilePath.clear();
                    secondFilePath.add(second);
                }

                runOnUiThread(() -> setAdapter());
                if (isRun < isFinish) {
                    takeRecord(delayTime, false);
                } else {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                            toast("MediaPlay is Stop.");
                        }
                    }
                    for (String f : firstFilePath)
                        fileCheck(f);
                    for (String s : secondFilePath)
                        fileCheck(s);
                    toast("Record is completed.");
                    takePreview();
                    toast("#finish");
                    new Handler().post(() -> saveLog());
                    isLoop = false;
                    finish();
                }
            }
        } else {
            if (mMediaPlayer != null) {
                if (mMediaPlayer.isPlaying()) {
                    mMediaPlayer.stop();
                    toast("MediaPlay is Stop.");
                }
            }
            for (String f : firstFilePath)
                fileCheck(f);
            for (String s : secondFilePath)
                fileCheck(s);
            toast("Record is completed.");
            takePreview();
            new Handler().post(() -> saveLog());
            isLoop = false;
        }
    }

    private void fileCheck(String path) {
        File video = new File(path);
        int framerate = 0, duration = 0;
        String convertMinutes = "00", convertSeconds = "00";
        if (video.exists()) {
            framerate = getFrameRate(path, mMediaPlayer);
            duration = getVideo(this, path);
            convertMinutes = String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(duration) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)));
            convertSeconds = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
            if (duration > 10 && framerate > 10)
                successful++;
            else
                failed++;
        } else {
            failed++;
        }
        videoLogList.add(new LogMsg("CheckFile: " + path.split("/")[3] + " framerate:" + framerate +
                " duration:" + convertMinutes + ":" + convertSeconds +
                " success:" + getSuccessful() + " fail:" + getFailed(), mLog.i));
        runOnUiThread(() -> setAdapter());
    }

    private void closePreviewSession(String cameraId) {
        if (isCameraOne(cameraId) && mPreviewSession0 != null) {
            mPreviewSession0.close();
            mPreviewSession0 = null;
        }
        if (cameraId.equals(secondCamera) && mPreviewSession1 != null) {
            mPreviewSession1.close();
            mPreviewSession1 = null;
        }
    }

    private void startRecord(String cameraId) {
        Log.d(TAG, "startRecord");
        try {
            closePreviewSession(cameraId);
            SurfaceTexture texture = isCameraOne(cameraId) ? mTextureView0.getSurfaceTexture() : mTextureView1.getSurfaceTexture();
            if (null == texture) {
                Log.e(TAG, "texture is null, return");
                return;
            }
            // 创建预览需要的CaptureRequest.Builder
            // TEMPLATE_PREVIEW：創建預覽的請求
            // TEMPLATE_STILL_CAPTURE：創建一個適合於靜態圖像捕獲的請求，圖像質量優先於幀速率。
            // TEMPLATE_RECORD：創建視頻錄製的請求
            // TEMPLATE_VIDEO_SNAPSHOT：創建視視頻錄製時截屏的請求
            // TEMPLATE_ZERO_SHUTTER_LAG：創建一個適用於零快門延遲的請求。在不影響預覽幀率的情況下最大化圖像質量。
            // TEMPLATE_MANUAL：創建一個基本捕獲請求，這種請求中所有的自動控制都是禁用的(自動曝光，自動白平衡、自動焦點)。
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            List<Surface> surfaces = new ArrayList<>();
            CameraCaptureSession mPreviewSession;
            CameraDevice mCameraDevice;
            CaptureRequest.Builder mPreviewBuilder;
            Surface recorderSurface;
            if (isCameraOne(cameraId)) {
                mPreviewSession = mPreviewSession0;
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
                mPreviewSession = mPreviewSession1;
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

            isRecord = true;
            // Start a capture session
            // Once the session starts, we can update the UI and start recording
            final CaptureRequest.Builder mPreviewBuilders = mPreviewBuilder;
            final CameraCaptureSession[] mPreviewSessions = {mPreviewSession};
            mCameraDevice.createCaptureSession(surfaces,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            // 当摄像头已经准备好时，开始显示预览
                            mPreviewSessions[0] = session;
                            setCaptureRequest(mPreviewBuilders, mPreviewSessions[0]);
                            new Thread(() -> (cameraId.equals(firstCamera) ? mMediaRecorder0 : mMediaRecorder1).start()).start();
                            toast("Camera " + cameraId + " Is Recording Now.");
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            toast("Camera " + cameraId + " Record onConfigureFailed.", mLog.e);
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void delete(String path, boolean check) {
        if (path != "") {
            File video = new File(path);
            if (video.exists()) {
                if (check) {
                    fileCheck(path);
                }
                videoLogList.add(new LogMsg("Delete: " + path.split("/")[3], mLog.w));
                runOnUiThread(() -> setAdapter());
                video.delete();
            } else {
                failed++;
                toast("Video not find.", mLog.e);
            }
        }
    }

    private void deleteALL() {
        for (int f = 0; f < firstFilePath.size() - 1; f++)
            delete(firstFilePath.get(f), true);
        for (int s = 0; s < secondFilePath.size() - 1; s++)
            delete(secondFilePath.get(s), true);
    }

    private void checkSdCardFromFileList(String filePath) {
        StatFs stat = new StatFs(filePath);
        long sdAvailSize = stat.getAvailableBlocksLong()
                * stat.getBlockSizeLong();
        double gigaAvailable = (sdAvailSize / 1073741824);
        // toast("SD Free Space:" + gigaAvailable);
        if (gigaAvailable < 3.5) { //TODO need 3.5Gb
            toast("SD Card(" + gigaAvailable + "gb) is Full.");
//            runOnUiThread(() -> stopRecord(false));
            ArrayList<String> tmp = new ArrayList();
            File[] fileList = new File(filePath).listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                File file = fileList[i];
                if (!fileList[i].isDirectory()) {
                    if (Utils.getFileExtension(file.toString()).equals("mp4"))
                        tmp.add(file.toString());
                }
            }
            if (tmp.size() >= 2) {
                runOnUiThread(() -> setAdapter());
                Object[] tmps = tmp.toArray();
                Arrays.sort(tmps);
                delete((String) tmps[0], false);
                delete((String) tmps[1], false);
                runOnUiThread(() -> setAdapter());
                new Handler().post(() -> saveLog());
                checkSdCardFromFileList(filePath);
            } else {
                videoLogList.add(new LogMsg("#error: At least 3.5Gb memory needs to be available to record, please check the SD Card free space.", mLog.e));
                new Handler().post(() -> saveLog());
                new Handler().post(() -> finish());
            }
        }
    }

    private void checkSdCardFromArrayList(String filePath) {
        StatFs stat = new StatFs(filePath);
        long sdAvailSize = stat.getAvailableBlocksLong()
                * stat.getBlockSizeLong();
        int gigaAvailable = (int) (sdAvailSize / 1073741824);
        // toast("SD Free Space:" + gigaAvailable);
        if (gigaAvailable < 3) {
            toast("SD Card(" + gigaAvailable + "gb) is Full.");
            runOnUiThread(() -> stopRecord(false));
            ArrayList<String> tmp = new ArrayList();
            delete(firstFilePath.get(0), false);
            delete(secondFilePath.get(0), false);
            for (int i = 1; i < firstFilePath.size(); i++)
                tmp.add(firstFilePath.get(i));
            firstFilePath.clear();
            firstFilePath = tmp;
            tmp = new ArrayList();
            for (int i = 1; i < secondFilePath.size(); i++)
                tmp.add(secondFilePath.get(i));
            secondFilePath.clear();
            secondFilePath = tmp;
            checkSdCardFromArrayList(filePath);
        }
    }

    private MediaRecorder setUpMediaRecorder(String cameraId) {
        /*TODO CamcorderProfile.QUALITY_HIGH:质量等级对应于最高可用分辨率*/// 1080p, 720p
        CamcorderProfile profile_720 = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        CamcorderProfile profile_1080 = CamcorderProfile.get(CamcorderProfile.QUALITY_1080P);
        String file = filePath + getCalendarTime(isCameraOne(cameraId)) + ".mp4";
        runOnUiThread(() -> videoLogList.add(new LogMsg("Create: " + file.split("/")[3], mLog.w)));
        (isCameraOne(cameraId) ? firstFilePath : secondFilePath).add(file);
        // Step 1: Unlock and set camera to MediaRecorder
        MediaRecorder mediaRecorder = new MediaRecorder();
        // Step 2: Set sources
        if (isCameraOne(cameraId))
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        if (isCameraOne(cameraId))
            mediaRecorder.setAudioEncoder(profile_720.audioCodec);
        mediaRecorder.setVideoEncoder(profile_720.videoCodec);
        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        CamcorderProfile profile = isQuality == 1 ? profile_720 : profile_1080;
        mediaRecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        profile = profile_720;
        /*设置编码比特率*/
        if (isCameraOne(cameraId))
            mediaRecorder.setAudioEncodingBitRate(profile.audioBitRate);
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        if (isCameraOne(cameraId)) {
            mediaRecorder.setAudioChannels(profile.audioChannels);
            mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        }
        /*设置要捕获的视频的帧速率*/ // default is 24.6
        mediaRecorder.setVideoFrameRate(27); // 1 -> 12fps, 10 -> 16fps
        // Step 4: Set output file
        mediaRecorder.setOutputFile(file);
        // Step 5: Prepare configured MediaRecorder
        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mediaRecorder;
    }

    private void takePreview() {
        takePreview(firstCamera);
        takePreview(secondCamera);
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                toast("MediaPlay is Stop.");
            }
        }
    }

    protected void takePreview(String cameraId) {
        Log.d(TAG, "takePreview");
        videoLogList.add(new LogMsg("Preview " + cameraId + " Camera.", mLog.i));

        SurfaceTexture texture = isCameraOne(cameraId) ? mTextureView0.getSurfaceTexture() : mTextureView1.getSurfaceTexture();
        if (null == texture) {
            Log.e(TAG, "texture is null, return");
            return;
        }
        texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
        Surface surface = new Surface(texture);

        CaptureRequest.Builder mPreviewBuilder;
        CameraCaptureSession mPreviewSession;
        CameraDevice mCameraDevice;
        if (isCameraOne(cameraId)) {
            mPreviewSession = mPreviewSession0;
            mCameraDevice = mCameraDevice0;
        } else {
            mPreviewSession = mPreviewSession1;
            mCameraDevice = mCameraDevice1;
        }
        try {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            mPreviewBuilder.addTarget(surface);

            final CaptureRequest.Builder mPreviewBuilders = mPreviewBuilder;
            final CameraCaptureSession[] mPreviewSessions = {mPreviewSession};

            // CaptureRequest.CONTROL_MODE

            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            // 当摄像头已经准备好时，开始显示预览
                            mPreviewSessions[0] = session;
                            setCaptureRequest(mPreviewBuilders, mPreviewSessions[0]);
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            videoLogList.add(new LogMsg("Preview " + cameraId + " onConfigureFailed", mLog.e));
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    protected void setCaptureRequest(CaptureRequest.Builder mPreviewBuilder, CameraCaptureSession mPreviewSession) {
        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        try {
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void toast(String t, mLog type) {
        videoLogList.add(new LogMsg(t, type));
        runOnUiThread(() -> Toast.makeText(this, t + "", Toast.LENGTH_SHORT).show());
    }

    private void toast(String t) {
        videoLogList.add(new LogMsg(t, mLog.i));
        runOnUiThread(() -> Toast.makeText(this, t + "", Toast.LENGTH_SHORT).show());
    }

    private void setAdapter() {
        if (videoLogList.size() != 0) {
            mListView.setAdapter(new mListAdapter(this, videoLogList));
            mListView.setSelection(videoLogList.size());
        }
    }

    private boolean isCameraOne(String cameraId) {
        return cameraId.equals(firstCamera);
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
                    isFrame = position;
                    break;
                case 1:
                    isQuality = position;
                    break;
                default:
                    break;
            }
        }

        public void onPageScrollStateChanged(int state) {

        }
    }
}