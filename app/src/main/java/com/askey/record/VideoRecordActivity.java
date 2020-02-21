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
import com.askey.widget.mLogListAdapter;
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
import static com.askey.record.Utils.DFRAME_RATE;
import static com.askey.record.Utils.FRAMESKIP;
import static com.askey.record.Utils.FRAME_RATE;
import static com.askey.record.Utils.NEW_DFRAME_RATE;
import static com.askey.record.Utils.NEW_FRAME_RATE;
import static com.askey.record.Utils.TAG;
import static com.askey.record.Utils.checkConfigFile;
import static com.askey.record.Utils.checkLogFile;
import static com.askey.record.Utils.configName;
import static com.askey.record.Utils.delayTime;
import static com.askey.record.Utils.failed;
import static com.askey.record.Utils.firstCamera;
import static com.askey.record.Utils.firstFilePath;
import static com.askey.record.Utils.getCalendarTime;
import static com.askey.record.Utils.getFailed;
import static com.askey.record.Utils.getFrameRate;
import static com.askey.record.Utils.getSDCardPath;
import static com.askey.record.Utils.getSuccessful;
import static com.askey.record.Utils.getVideo;
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
import static com.askey.record.Utils.sdData;
import static com.askey.record.Utils.secondCamera;
import static com.askey.record.Utils.secondFilePath;
import static com.askey.record.Utils.setConfigFile;
import static com.askey.record.Utils.successful;
import static com.askey.record.Utils.toast;
import static com.askey.record.Utils.videoLogList;

public class VideoRecordActivity extends Activity {

    private static String filePath = "/storage/", codeDate;
    private Size mPreviewSize;
    private TextureView mTextureView0, mTextureView1;
    private CameraDevice mCameraDevice0, mCameraDevice1;
    private CameraCaptureSession mPreviewSession0, mPreviewSession1;
    private CameraDevice.StateCallback mStateCallback0, mStateCallback1;
    private CaptureRequest.Builder mPreviewBuilder0, mPreviewBuilder1;
    private MediaRecorder mMediaRecorder0, mMediaRecorder1;
    private ListView mListView;
    private Handler mainHandler, backgroundHandler, soundHandler, demoHandler;
    private MediaPlayer mMediaPlayer;
    private Runnable sound = new Runnable() {

        public void run() {
            if (isRecord)
                if (isFinish == 999 || isRun <= isFinish) {
                    playMusic(R.raw.scanner_beep);
                    soundHandler.postDelayed(this, 10000);
                }
        }
    };

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(COMMAND_VIDEO_RECORD_TEST) || action.equals(COMMAND_VIDEO_RECORD_TESTa)) {
                if (isReady) {
                    if (!isRecord) {
                        setRecord();
                        isFinish = 0;
                        takeRecord(5000, true);
                    } else {
                        isFinish = 0;
                        new stopRecord(true);
                    }
                } else toast(VideoRecordActivity.this, "Not Ready to Record.");
            }
            if (action.equals(COMMAND_VIDEO_RECORD_START) || action.equals(COMMAND_VIDEO_RECORD_STARTa)) {
                isRecordStart();
            }
            if (action.equals(COMMAND_VIDEO_RECORD_FINISH) || action.equals(COMMAND_VIDEO_RECORD_FINISHa)) {
                Log.d("VideoRecord", "finish");
                isFinish = 0;
                new stopRecord(false);
            }
        }
    };

    private void setRecord() {
        isRecord = true;
        checkConfigFile(VideoRecordActivity.this, new File(filePath, configName), false);
        isRun = 0;
        successful = 0;
        failed = 0;
        firstFilePath.clear();
        secondFilePath.clear();
        soundHandler.post(sound);
    }

    private void fullScreenCall() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    private void isRecordStart() {
        if (isReady)
            if (!isRecord) {
                setRecord();
                takeRecord(delayTime, false);
            } else {
                isFinish = 0;
                new stopRecord(true);
            }
    }

    protected void onResume() {
        super.onResume();
        isRun = 0;
        videoLogList = new ArrayList();
        if (checkPermission()) {
            showPermission();
        } else {
            if (checkConfigFile(this, true)) {
                //TODO SETPROP
                // -> adb shell su 0 getprop persist.our.camera.frameskip
//                SystemProperties.set(FRAMESKIP, "0"); //*lib(layoutlib).jar
                if (isNew) PropertyUtils.set(FRAMESKIP, "0"); //*reflection invoke
//                CommandUtil.executed("setprop "+FRAMESKIP+" 0"); //*not work
                setStart();
            } else android.os.Process.killProcess(android.os.Process.myPid());
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
                    toast(VideoRecordActivity.this, "No permissions!");
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
        setHomeListener();
        initial();
    }

    private void setHomeListener() {
        HomeListen home = new HomeListen(this);
        home.setOnHomeBtnPressListener(new HomeListen.OnHomeBtnPressLitener() {
            public void onHomeBtnPress() {
                android.os.Process.killProcess(android.os.Process.myPid());

            }

            public void onHomeBtnLongPress() {
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        });
        home.start();
    }

    public static void getSetting(Context context, EditText editText1, EditText editText2, EditText editText3, TextView editText4) {
        String input = readConfigFile(context, new File(filePath, configName));
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
            toast(context, "Error reading config file.");
            reformatConfigFile(context, new File(filePath, configName));
        }
    }

    private void initial() {
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
        mListView = findViewById(R.id.list);
        mListView.setEnabled(false);
        toast(VideoRecordActivity.this, "Initial now.", mLog.v);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        backgroundHandler = new Handler(thread.getLooper());
        mainHandler = new Handler(getMainLooper());
        soundHandler = new Handler();
        mStateCallback0 = new CameraDevice.StateCallback() {

            public void onOpened(CameraDevice camera) {
                if (!isReady) {
                    // 打开摄像头
                    Log.e(TAG, "onOpened");
                    toast(VideoRecordActivity.this, "Camera " + firstCamera + " is opened.", mLog.i);
                }
                mCameraDevice0 = camera;
                // 开启预览
                takePreview(firstCamera);
            }

            public void onDisconnected(CameraDevice camera) {
                camera.close();
                // 关闭摄像头
                Log.e(TAG, "onDisconnected");
                toast(VideoRecordActivity.this, "Camera " + firstCamera + " is disconnected.", mLog.w);
                if (null != mCameraDevice0) {
                    mCameraDevice0.close();
                    mCameraDevice0 = null;
                }
            }

            public void onError(CameraDevice camera, int error) {
                onDisconnected(camera);
                // 前鏡頭開啟失敗
                Log.e(TAG, "onError");
                toast(VideoRecordActivity.this, "Open Camera " + firstCamera + " error.", mLog.e);
                isError = true;
            }
        };
        mStateCallback1 = new CameraDevice.StateCallback() {

            public void onOpened(CameraDevice camera) {
                if (!isReady) {
                    // 打开摄像头
                    Log.e(TAG, "onOpened");
                    toast(VideoRecordActivity.this, "Camera " + secondCamera + " is opened.", mLog.i);
                }
                mCameraDevice1 = camera;
                // 开启预览
                takePreview(secondCamera);
                if (!isReady) {
                    isReady = true;
                    demoHandler.obtainMessage().sendToTarget(); // playDEMO
                    if (isError) {
                        isFinish = 0;
                        new stopRecord(false);
                    }
                }
            }

            public void onDisconnected(CameraDevice camera) {
                camera.close();
                // 关闭摄像头
                Log.e(TAG, "onDisconnected");
                toast(VideoRecordActivity.this, "Camera " + secondCamera + " is disconnected.", mLog.w);
                if (null != mCameraDevice1) {
                    mCameraDevice1.close();
                    mCameraDevice1 = null;
                }
            }

            public void onError(CameraDevice camera, int error) {
                onDisconnected(camera);
                // 前鏡頭開啟失敗
                Log.e(TAG, "onError");
                toast(VideoRecordActivity.this, "Open Camera " + secondCamera + " error.", mLog.e);
            }
        };
        mTextureView0 = findViewById(R.id.surfaceView0);
        mTextureView0.setSurfaceTextureListener(new mSurfaceTextureListener(firstCamera));
        mTextureView1 = findViewById(R.id.surfaceView1);
        mTextureView1.setSurfaceTextureListener(new mSurfaceTextureListener(secondCamera));
        AudioManager audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        findViewById(R.id.listBackground).setOnClickListener((View v) -> {
            View view = LayoutInflater.from(this).inflate(R.layout.layout_getlog, null);
            final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen).setView(view).setCancelable(true).create();
            view.findViewById(R.id.dialog_button_1).setOnClickListener((View vs) -> { // reset
                videoLogList.add(new LogMsg("#reset LogFile", mLog.v));
                saveLog(true);
                dialog.dismiss();
            });
            view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
                dialog.dismiss();
            });
            ArrayList<String> list = new ArrayList();
            checkLogFile(this, new File(getSDCardPath(), logName), list);
            if (list.size() > 0) {
                ArrayList<View> items = new ArrayList();
                for (String s : list) {
                    View item_layout = LayoutInflater.from(this).inflate(R.layout.style_text_item, null);
                    ((CustomTextView) item_layout.findViewById(R.id.customTextView)).setText(s);
                    items.add(item_layout);
                }
                ((ListView) view.findViewById(R.id.dialog_listview)).setAdapter(new mListAdapter(items));
            }
            dialog.show();
        });
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            Log.d("VideoRecord", "finish");
            new Handler().post(() -> saveLog(false));
            android.os.Process.killProcess(android.os.Process.myPid());
        });
        findViewById(R.id.volume_down).setOnClickListener((View v) ->
                runOnUiThread(() -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)));
        findViewById(R.id.record).setOnClickListener((View v) ->
                runOnUiThread(() -> isRecordStart()));
        findViewById(R.id.volume_up).setOnClickListener((View v) ->
                runOnUiThread(() -> audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)));
        findViewById(R.id.setting).setOnClickListener((View v) -> {
            View view = LayoutInflater.from(this).inflate(R.layout.layout_setting, null);
            final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
            view.findViewById(R.id.dialog_button_1).setOnClickListener((View vs) -> { // reset
                setConfigFile(this, new File(filePath, configName), view, true);
                getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                        view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
                setSetting();
            });
            view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // cancel
                dialog.dismiss();
            });
            view.findViewById(R.id.dialog_button_3).setOnClickListener((View vs) -> { // ok
                setConfigFile(this, new File(filePath, configName), view, false);
                setSetting();
                dialog.dismiss();
            });
            getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                    view.findViewById(R.id.dialog_editText_3), view.findViewById(R.id.dialog_editText_4));
            dialog.show();
        });
        findViewById(R.id.loadingView).setVisibility(View.INVISIBLE);
        codeDate = getCalendarTime();
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
        mListView.setAdapter(new mLogListAdapter(this, videoLogList));
        // show DEMO
        demoHandler = new Handler() {
            public void handleMessage(android.os.Message msg) {
                Runnable r = () -> playMusic(R.raw.scanner_beep);
                this.post(r);
                this.postDelayed(r, 900);
                this.postDelayed(r, 1200);
                this.postDelayed(() -> checkSdCardFromFileList(getSDCardPath()), 1500);
                this.postDelayed(() -> {
                    if (isError) {
                        android.os.Process.killProcess(android.os.Process.myPid());
                    } else {
                        runOnUiThread(() -> setAdapter());
                        saveLog(false);
                    }
                }, 1500);
            }
        };
    }

    private void setSetting() {
        if (!isRecord) {
            boolean[] check = checkConfigFile(this, new File(filePath, configName), false);
            if (check[0]) {
                runOnUiThread(() -> setAdapter());
                new Handler().post(() -> {
                    mStateCallback0.onDisconnected(mCameraDevice0);
                    mStateCallback1.onDisconnected(mCameraDevice1);
                    openCamera(firstCamera);
                    openCamera(secondCamera);
                });
            }
            if (check[1]) {
                PropertyUtils.set(FRAMESKIP, "0");
                if (isNew) {
                    String getFrameSkip = PropertyUtils.get(FRAMESKIP);
                    if (null != getFrameSkip) {
                        if (isInteger(getFrameSkip, false)) {
                            //if frameskip is chehe or lastcamera != cameraid, delay 3s to change camera devices
                            SystemProperties.set(FRAMESKIP, String.valueOf(isFrame));
                            videoLogList.add(new LogMsg("getFrameSkip:" + PropertyUtils.get(FRAMESKIP), mLog.e));
                        } else {
                            toast(VideoRecordActivity.this, "getFrameSkip error, fs(" + getFrameSkip + ") is not integer.", mLog.e);
                        }
                    } else {
                        toast(VideoRecordActivity.this, "getFrameSkip error, fs == null.", mLog.e);
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
            }
        } else {
            toast(this);
            runOnUiThread(() -> setAdapter());
        }
    }

    private void setLoading(boolean visible) {
        runOnUiThread(() -> findViewById(R.id.loadingView).setVisibility(visible ? View.VISIBLE : View.INVISIBLE));
    }

    private void takeRecord(int delayMillis, boolean preview) {
        isRun++;
        videoLogList.add(new LogMsg("#------------------------------", mLog.v));
        videoLogList.add(new LogMsg("#takeRecord FrameRate:" +
                (isNew ? NEW_FRAME_RATE : FRAME_RATE)[isFrame], mLog.v));
        int delay = 0;
        new Handler().post(() -> checkSdCardFromFileList(filePath));
        if (!lastfirstCamera.equals(firstCamera) || !lastsecondCamera.equals(secondCamera)) {
            lastfirstCamera = firstCamera; // String
            lastsecondCamera = secondCamera;
            runOnUiThread(() -> setAdapter());
            new Handler().post(() -> {
                mStateCallback0.onDisconnected(mCameraDevice0);
                mStateCallback1.onDisconnected(mCameraDevice1);
                openCamera(firstCamera);
                openCamera(secondCamera);
            });
            delay = 3000;
        }

        new Handler().postDelayed(() -> new Thread(() -> startRecord(firstCamera)).start(), delay);
        new Handler().postDelayed(() -> new Thread(() -> startRecord(secondCamera)).start(), delay);
        new stopRecord(preview, delay + delayMillis + 2000);
        new Handler().postDelayed(() -> {
            runOnUiThread(() -> setAdapter());
            saveLog(false);
        }, delay);
    }

    private void saveLog(boolean Reformate) {
        String logString = "[VIDEO_RECORD_LOG]\r\n";
        if (!"".equals(getSDCardPath())) {
            File file = new File(getSDCardPath(), logName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                    toast(this, "Create the log file.", mLog.w);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logString = "";
                //toast(VideoRecordActivity.this,"Find the log file.", mLog.d);
            }

            for (LogMsg logs : videoLogList) {
                String time = logs.time.format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        + " run:" + logs.runTime + " -> ";
                logString += (time + logs.msg + "\r\n");
            }
            try {
//                toast(VideoRecordActivity.this, "write failed.", mLog.e);
                FileOutputStream output = new FileOutputStream(new File(filePath, logName), !Reformate);
                output.write(logString.getBytes());
                output.close();

                videoLogList.clear();
            } catch (IOException e) {
//                toast(VideoRecordActivity.this, "write failed.", mLog.e);
            }
        } else {
            toast(VideoRecordActivity.this, "Please check the SD card.", mLog.e);
        }
    }

    protected void onDestroy() {
        isFinish = 0;
        isRecord = false;
        if (mMediaRecorder0 != null) {
            mMediaRecorder0.stop();
            mMediaRecorder0.reset();
            mMediaRecorder0 = null;
            toast(VideoRecordActivity.this, "Record " + firstCamera + " finish.");
        }
        if (mMediaRecorder1 != null) {
            mMediaRecorder1.stop();
            mMediaRecorder1.reset();
            mMediaRecorder1 = null;
            toast(VideoRecordActivity.this, "Record " + secondCamera + " finish.");
        }
        stopMediaPlayer();
        new Handler().post(() -> saveLog(false));
        unregisterReceiver(myReceiver);
        super.onDestroy();
    }


    private void playMusic(int resources) {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                mMediaPlayer = null;
            }
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
            toast(VideoRecordActivity.this, "playMusic not find.");
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

    private String getCodeDate() {
        return codeDate;
    }

    private class stopRecord {
        public stopRecord(boolean preview) {
            final String codeDate = getCodeDate();
            new Handler().post(() -> stopRecord(preview, codeDate));
        }

        public stopRecord(boolean preview, int delay) {
            final String codeDate = getCodeDate();
            new Handler().postDelayed(() -> stopRecord(preview, codeDate), delay);
        }
    }

    private void stopRecord(boolean preview, String date) {
        if (date.equals(getCodeDate())) {
            codeDate = getCalendarTime();
            if (isRecord) {
                videoLogList.add(new LogMsg("#stopRecord", mLog.v));
                Log.d(TAG, "stopRecord");
                if (mMediaRecorder0 != null) {
                    mMediaRecorder0.stop();
                    mMediaRecorder0.reset();   // You can reuse the object by going back to setAudioSource() step
                    //mMediaRecorder.release(); // Now the object cannot be reused
                    mMediaRecorder0 = null;
                    toast(VideoRecordActivity.this, "Record " + firstCamera + " finish.");
                }
                if (mMediaRecorder1 != null) {
                    mMediaRecorder1.stop();
                    mMediaRecorder1.reset();
                    mMediaRecorder1 = null;
                    toast(VideoRecordActivity.this, "Record " + secondCamera + " finish.");
                }
                boolean autoClean = false;
                if (autoClean) {
                    deleteAndLeftTwo();
                } else {
                    checkAndClear();
                }

                new Handler().post(() -> saveLog(false));
                runOnUiThread(() -> setAdapter());

                if (isFinish == 999 || isRun <= isFinish) {
                    takeRecord(delayTime, false);
                } else {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                    toast(VideoRecordActivity.this, "#completed");
                    if (autoClean) {
                        checkAndClear();
                    }
                    end(preview);
                }
            } else {
                isRun = 0;
                isFinish = 0;
                end(preview);
            }
        }
    }

    private void end(boolean preview) {
        new Handler().post(() -> saveLog(false));
        runOnUiThread(() -> setAdapter());
        if (preview) {
            takePreview();
        } else android.os.Process.killProcess(android.os.Process.myPid());
    }

    private void stopMediaPlayer() {
        if (mMediaPlayer != null) {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
                toast(VideoRecordActivity.this, "MediaPlay is Stop.");
            }
        }
    }

    private void fileCheck(String path) {
        File video = new File(path);
        int framerate = 0, duration;
        String convertMinutes = "00", convertSeconds = "00";
        if (video.exists()) {
            framerate = getFrameRate(path, mMediaPlayer);
            duration = getVideo(this, path);
            convertMinutes = String.format("%02d", TimeUnit.MILLISECONDS.toMinutes(duration) -
                    TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(duration)));
            convertSeconds = String.format("%02d", TimeUnit.MILLISECONDS.toSeconds(duration) -
                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration)));
            double[] range = isNew ? NEW_DFRAME_RATE : DFRAME_RATE;
            boolean check = false;
            if (duration != 0) {
                if (framerate >= range[isFrame]) {
                    if (framerate <= range[isFrame] + 3) {
                        check = true;
                    }
                } else if (framerate < range[isFrame]) {
                    if (framerate >= range[isFrame] - 3) {
                        check = true;
                    }
                }
            }
            if (check)
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
                            toast(VideoRecordActivity.this, "Camera " + cameraId + " Is Recording Now.");
                            runOnUiThread(() -> setAdapter());
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession cameraCaptureSession) {
                            toast(VideoRecordActivity.this, "Camera " + cameraId + " Record onConfigureFailed.", mLog.e);
                            runOnUiThread(() -> setAdapter());
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
                toast(VideoRecordActivity.this, "Video not find.", mLog.e);
            }
        }
    }

    private void deleteAndLeftTwo() {
        ArrayList<String> filepath = new ArrayList();
        if (firstFilePath.size() == 3) {
            delete(firstFilePath.get(0), true);
            for (int f = 1; f < firstFilePath.size(); f++) {
                filepath.add(firstFilePath.get(f));
            }
            firstFilePath.clear();
            for (String s : filepath)
                firstFilePath.add(s);
            filepath.clear();
        }
        if (secondFilePath.size() == 3) {
            delete(secondFilePath.get(0), true);
            for (int f = 1; f < secondFilePath.size(); f++) {
                filepath.add(secondFilePath.get(f));
            }
            secondFilePath.clear();
            for (String s : filepath)
                secondFilePath.add(s);
            filepath.clear();
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

    private void checkSdCardFromFileList(String filePath) {
        StatFs stat = new StatFs(filePath);
        long sdAvailSize = stat.getAvailableBlocksLong()
                * stat.getBlockSizeLong();
        double gigaAvailable = (sdAvailSize / 1073741824);
        if (gigaAvailable < sdData) {
            toast(VideoRecordActivity.this, "SD Card(" + gigaAvailable + "gb) is Full.");
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
                Object[] list = tmp.toArray();
                Arrays.sort(list);
                delete((String) list[0], false);
                delete((String) list[1], false);
                new Handler().post(() -> checkSdCardFromFileList(filePath));
            } else {
                videoLogList.add(new LogMsg("#error: At least " + sdData + " memory needs to be available to record, please check the SD Card free space.", mLog.e));
                new Handler().post(() -> saveLog(false));
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }

    private void checkSdCardFromArrayList(String filePath) {
        StatFs stat = new StatFs(filePath);
        long sdAvailSize = stat.getAvailableBlocksLong()
                * stat.getBlockSizeLong();
        int gigaAvailable = (int) (sdAvailSize / 1073741824);
        // toast(VideoRecordActivity.this,"SD Free Space:" + gigaAvailable);
        if (gigaAvailable < 3) {
            toast(VideoRecordActivity.this, "SD Card(" + gigaAvailable + "gb) is Full.");
            new stopRecord(false);
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
        CamcorderProfile profile = isQuality == 1 ? profile_720 : profile_1080;
        MediaRecorder mediaRecorder = new MediaRecorder();
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
        mediaRecorder.setVideoEncodingBitRate(profile.videoBitRate);
        if (isCameraOne(cameraId)) {
            mediaRecorder.setAudioChannels(profile.audioChannels);
            mediaRecorder.setAudioSamplingRate(profile.audioSampleRate);
        }
        /*设置要捕获的视频的帧速率*/ // default is 24.6
        mediaRecorder.setVideoFrameRate(!isNew ? isFrame == 1 ? 10 : 28 : 27); // 1 -> 12fps, 10 -> 16fps
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
            }
        }
    }

    private void takePreview(String cameraId) {
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

    private void setAdapter() {
        if (videoLogList.size() != 0) {
            mListView.setAdapter(new mLogListAdapter(this, videoLogList));
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
                    if (!isRecord) {
                        isFrame = position;
                        if (isNew) {
                            setLoading(true);
                            new Handler().post(() -> {
                                String getFrameSkip = PropertyUtils.get(FRAMESKIP);
                                if (null != getFrameSkip) {
                                    if (isInteger(getFrameSkip, false)) {
                                        //if frameskip is chehe or lastcamera != cameraid, delay 3s to change camera devices
                                        SystemProperties.set(FRAMESKIP, String.valueOf(isFrame));
                                        videoLogList.add(new LogMsg("getFrameSkip:" + PropertyUtils.get(FRAMESKIP), mLog.e));
                                        runOnUiThread(() -> setAdapter());
                                        mStateCallback0.onDisconnected(mCameraDevice0);
                                        mStateCallback1.onDisconnected(mCameraDevice1);
                                        new Handler().post(() -> openCamera(firstCamera));
                                        new Handler().post(() -> openCamera(secondCamera));
                                    } else {
                                        toast(VideoRecordActivity.this, "getFrameSkip error, fs(" + getFrameSkip + ") is not integer.", mLog.e);
                                    }
                                } else {
                                    toast(VideoRecordActivity.this, "getFrameSkip error, fs == null.", mLog.e);
                                }
                                setLoading(false);
                            });
                        }
                    } else {
                        toast(VideoRecordActivity.this);
                        ((VerticalViewPager) findViewById(R.id.pager1)).setCurrentItem(isFrame);
                    }
                    break;
                case 1:
                    if (!isRecord) {
                        isQuality = position;
                    } else {
                        toast(VideoRecordActivity.this);
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