package com.d160.bit;

import android.*;
import android.annotation.*;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.*;
import android.graphics.*;
import android.hardware.camera2.*;
import android.media.*;
import android.net.wifi.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.core.app.*;
import androidx.core.content.*;

import com.d160.view.*;

import java.io.*;
import java.math.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.*;

import static com.d160.bit.Utils.*;
import static java.lang.System.gc;

@SuppressLint("SetTextI18n")
public class CameraActivity extends Activity {
    public static final String TAG = "com.d160.bit";
    public static final String configName = "BurnInTestConfig.ini";
    public static final String logName = "BurnInTestLog.ini";
    public static final String CONFIG_TITLE = "[BurnIn_Test_Config]";
    public static final String LOG_TITLE = "[BurnIn_Test_Log]";
    public static final int delay_3 = 3000, delay_60 = 60600;
    //-------------------------------------------------------------------------------
    public static final boolean Open_Audio = false;
    public static String firstCamera = "0", secondCamera = "1";
    public static String lastFirstCamera = "0", lastSecondCamera = "1";
    //TODO 是否啟用keepScreen
    public static boolean keepScreen = true;
    //TODO 是否啟用preview
    public static boolean preview = false;
    //TODO 是否使用SD_Mode
    public static boolean SD_Mode = true;
    //TODO 是否啟用重啟功能
    public static boolean autoRestart = true;
    //TODO 是否啟用60s停止錄影
    public static boolean autoStopRecord = true;
    //TODO 是否啟用BurnIn測試
    public static boolean burnInTest = true;
    //-------------------------------------------------------------------------------
    public static boolean extraRecordStatus = true, onRestart = false;
    public static int onRun = 0, onSuccess = 0, onFail = 0, onReset = 0;
    public static int onWifiSuccess = 0, onWifiFail = 0, onBtSuccess = 0, onBtFail = 0;
    //-------------------------------------------------------------------------------
    private BroadcastReceiver mBroadcastReceiver;
    private Handler mainHandler, resetHandler;
    private Size mPreviewSize = null;
    private HomeListen home;
    private mTimerTask timerTask = null;
    private Timer mTimer = null;
    private float value = 0.0f;
    private WifiManager wifiManager;
    private BluetoothAdapter mbtAdapter;

    public static void saveLog(Context context, boolean reFormat, boolean kill) {
        if (null != videoLogList) {
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
                        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                        logString.append(dateFormat.format(logs.time)).append(" run:")
                                .append(logs.runTime).append(" -> ").append(logs.msg).append("\r\n");
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
        }
        if (kill)
            android.os.Process.killProcess(android.os.Process.myPid());
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

    private boolean checkFile(String file, AtomicReferenceArray<String> list) {
        if (file.equals(list.get(0)))
            return false;
        else return (file.equals(list.get(1)));
    }

    private void setRecord() {
        isRecord = true;
        checkConfigFile(CameraActivity.this, new File(getPath(), configName), false);
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
        for (String CameraId : allCamera) {
            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
            cameraFile.set(id, "");
            cameraFilePath.get(id).clear();
        }
        extraRecordStatus = true;
    }

    private void isRecordStart(boolean auto) {
        if (!isError && isSave) {
            if (isCameraReady) {
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
                    int delay = 0;
                    for (String CameraId : allCamera) {
                        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                        new Handler(getMainLooper()).postDelayed(() -> recordHandler.get(id).obtainMessage().sendToTarget(), delay);
                        delay += 500;
                    }
                    saveLog(this, false, false);
                } else {
                    if (!auto)
                        videoLogList.add(new mLogMsg("@Stop record", mLog.v));
                    else
                        videoLogList.add(new mLogMsg("#Stop record", mLog.v));
                    new Handler(getMainLooper()).post(() -> stopRecord(!auto));
                }
            } else {
                Log.e(TAG, "isCameraReady is not ready");
                showDialogLog(false);
                videoLogList.add(new mLogMsg("#Camera is not ready.", mLog.v));
            }
        } else {
            Log.e(TAG, "isSave && !isError");
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private boolean checkPermission() {
        videoLogList.add(new mLogMsg("#checkPermission", mLog.v));
        return permission(permission.get(0)) || permission(permission.get(1)) || permission(permission.get(2)) || permission(permission.get(3)) || permission(permission.get(4));
    }

    @TargetApi(23)
    @SuppressLint("NewApi")
    private void showPermission() {
        videoLogList.add(new mLogMsg("#showPermission", mLog.v));
        // We don't have permission so prompt the user
        List<String> permissions = new ArrayList<>();
        for (int i = 0; i < permission.length(); i++) {
            permissions.add(permission.get(i));
        }
        requestPermissions(permissions.toArray(new String[0]), 0);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initial();
                startBackgroundThread();
            } else {
                showPermission();
                videoLogList.add(new mLogMsg("#no permissions!", mLog.e));
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean permission(String mp) {
        return ActivityCompat.checkSelfPermission(this, mp) != PackageManager.PERMISSION_GRANTED;
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
            new Handler(getMainLooper()).post(() -> saveLog(this, false, false));
        }
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
        //#onCreateView -> #onViewCreated -> #onActivityCreated -> #onResume
        setProp();
    }

    protected void onResume() {
        super.onResume();
        if (checkPermission()) {
            showPermission();
        } else {
            initial();
            startBackgroundThread();
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

    @SuppressLint("SetTextI18n")
    private void errorMessage(String msg, boolean reset, Exception e) {
        if (e != null)
            Log.e(TAG, e.toString());
        isSave = !getSDPath().equals("");
        isError = true;
        isRecord = false;
        runOnUiThread(() -> ((TextView) findViewById(R.id.record_status)).setText("Error"));
        if (null != videoLogList)
            videoLogList.add(new mLogMsg(msg, mLog.e));
        errorMessage = msg;
        new Handler(getMainLooper()).post(() -> stopRecordAndSaveLog(false));
        if (reset) {
            new Handler(getMainLooper()).postDelayed(this::restartApp, delay_3);
        }
    }

    private void stopRecordAndSaveLog(boolean kill) {
        if (isRecord)
            new Handler(getMainLooper()).post(() -> stopRecord(true));
        saveLog(this, false, kill);
    }

    @SuppressLint("HandlerLeak")
    private void setCamera(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        textView.set(id, this.findViewById(id_textView.get(id)));
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

    private void restartApp() {
        try {
            home.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        onRestart = true;
        onReset++;
        Context context = getApplicationContext();
        Intent intent = RestartActivity.createIntent(context);
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
        setContentView(R.layout.activity_video_record);
        setHomeListener();
        checkConfigFile(this, true);
        isSave = !getSDPath().equals("");
        Log.e(TAG, "#initial");
        videoLogList.add(new mLogMsg("#initial", mLog.v));
        checkSdCardFromFileList();
        try {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        } catch (Exception e) {
            errorMessage("wifiManager error.", false, e);
        }
        try {
            mbtAdapter = BluetoothAdapter.getDefaultAdapter();
        } catch (Exception e) {
            errorMessage("bluetoothAdapter error.", false, e);
        }
        findViewById(R.id.cancel).setOnClickListener((View v) -> {
            videoLogList.add(new mLogMsg("@cancel", mLog.v));
            stopRecordAndSaveLog(true);
        });
        findViewById(R.id.record).setOnClickListener((View v) -> isRecordStart(false));
        findViewById(R.id.setting).setOnClickListener((View v) -> {
            if (isSave && !isError) {
                videoLogList.add(new mLogMsg("@setting_show", mLog.v));
                View view = LayoutInflater.from(this).inflate(R.layout.layout_setting, null);
                final AlertDialog dialog = new AlertDialog.Builder(this).setView(view).setCancelable(false).create();
                view.findViewById(R.id.dialog_button_1).setOnClickListener((View vs) -> { // reset
                    videoLogList.add(new mLogMsg("@setting_reset", mLog.v));
                    setConfigFile(this, new File(getPath(), configName), view, true);
                    getSetting(this, view.findViewById(R.id.dialog_editText_1),
                            view.findViewById(R.id.dialog_editText_2),
                            view.findViewById(R.id.dialog_editText_3));
                    setSetting(true);
                    dialog.dismiss();
                });
                view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // cancel
                    videoLogList.add(new mLogMsg("@setting_cancel", mLog.v));
                    dialog.dismiss();
                });
                view.findViewById(R.id.dialog_button_3).setOnClickListener((View vs) -> { // ok
                    videoLogList.add(new mLogMsg("@setting_ok", mLog.v));
                    if (!isRecord) {
                        setConfigFile(this, new File(getPath(), configName), view, false);
                        setSetting(false);
                    } else {
                        Toast.makeText(this, "you are recording..", Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                });
                getSetting(this, view.findViewById(R.id.dialog_editText_1), view.findViewById(R.id.dialog_editText_2),
                        view.findViewById(R.id.dialog_editText_3));
                dialog.show();
            } else {
                Log.e(TAG, "isSave && !isError");
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
//        extraRecordStatus = getIntent().getBooleanExtra(EXTRA_VIDEO_RECORD, false);
        if (onReset != 0)
            videoLogList.add(new mLogMsg("#noReset:" + onReset, mLog.v));
    }

    private void getSetting(Context context, EditText editText1, EditText editText2, EditText editText3) {
        String input = readConfigFile(context, new File(getPath(), configName));
        if (input.length() > 0) {
            String[] read = input.split("\r\n");
            int t;
            String first = "firstCameraID = ", second = "secondCameraID = ";
            String code = "numberOfRuns = ";
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
            editText1.setText(first);
            editText2.setText(second);
            editText3.setText(code);
        } else {
            videoLogList.add(new mLogMsg("Error reading config file."));
            reformatConfigFile(context, new File(getPath(), configName));
        }
    }

    private void setSetting(boolean reset) {
        if (!isRecord) {
            boolean check = checkConfigFile(this, new File(getPath(), configName), false);
            Log.e(TAG, "setSetting check:" + check);
            if (check) {
                runOnUiThread(() -> {
                    try {
                        closeCamera();
                        stopBackgroundThread();
                        Thread.sleep(1500);
                        startBackgroundThread();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
            }
        } else {
            saveLog(getApplicationContext(), false, false);
        }
        if (reset) {
            Log.e(TAG, "@ResetApp");
            videoLogList.add(new mLogMsg("@ResetApp", mLog.v));
            restartApp();
        }
    }

    private void showDialogLog(boolean end) {
        videoLogList.add(new mLogMsg("#Log_show", mLog.v));
        View view = LayoutInflater.from(this).inflate(R.layout.layout_getlog, null);
        final AlertDialog dialog = new AlertDialog.Builder(this, R.style.Theme_AppCompat_NoActionBar)
                .setView(view).setCancelable(true).create();

        view.findViewById(R.id.dialog_button_2).setOnClickListener((View vs) -> { // ok
            videoLogList.add(new mLogMsg("@Log_ok", mLog.v));
            dialog.dismiss();
        });
        ArrayList<String> list = new ArrayList<>();
        String bit = ") wifi_success/fail:(" + getWifiSuccess() + "/" + getWifiFail() +
                ") bt_success/fail:(" + getBtSuccess() + "/" + getBtFail();
        if (isSave)
            if (end)
                list.add("CheckFile -> video_success/fail:(" + getSuccess() + "/" + getFail() +
                        (burnInTest ? bit : "") + ") app_reset:(" + getReset() + ")");
            else
                list.add("App Version:" + this.getString(R.string.app_name));
        else list.add(NO_SD_CARD);

        if (!errorMessage.equals(""))
            list.add(errorMessage);
        else {
            for (String CameraId : allCamera) {
                int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                if (!isCameraOpened.get(id))
                    list.add("Camera Access error, Please check camera " + CameraId + ". <============ Crash here");
            }
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

    protected void onDestroy() {
        super.onDestroy();
        try {
            for (String CameraId : allCamera) {
                closeStateCallback(CameraId);
                closePreviewSession(CameraId);
                closeMediaRecorder(CameraId);
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
        new Handler(getMainLooper()).post(() -> stopRecordAndSaveLog(false));
    }

    private void openCamera(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        videoLogList.add(new mLogMsg("#Open Camera" + CameraId + ".", mLog.w));
        Log.e(TAG, "camera ID: " + CameraId);
        CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
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
                    runOnUiThread(() -> {
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

    @SuppressLint("HandlerLeak")
    private void startBackgroundThread() {
        mainHandler = new Handler(getMainLooper());
        if (keepScreen)
            this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else
            this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        for (String CameraId : allCamera) {
            int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
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
        registerReceiver(mBroadcastReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), Intent.ACTION_BATTERY_CHANGED)) {  //Battery
                    Log.e(TAG, "Battery:" + intent.getIntExtra("level", 0) + "%");
                    videoLogList.add(new mLogMsg("Battery:" + intent.getIntExtra("level", 0) + "%", mLog.e));
                    saveLog(CameraActivity.this, false, false);
                }
            }
        }, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        resetHandler = new Handler(getMainLooper()) {
            public void handleMessage(Message msg) {
                if (extraRecordStatus)
                    isRecordStart(true);
                saveLog(CameraActivity.this, false, false);
            }
        };
        new Handler(getMainLooper()).postDelayed(() -> resetHandler.obtainMessage().sendToTarget(), delay_3);
    }

    private void stopBackgroundThread() {
        this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        this.unregisterReceiver(mBroadcastReceiver);
    }

    @SuppressLint("SetTextI18n")
    private void stopRecord(String date, String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (date.equals(codeDate.get(id))) {
                isRun++;
                videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                Log.e(TAG, "Run " + isRun + " stopRecord " + CameraId);
                if (isCameraOne(CameraId))
                    runOnUiThread(() -> {
                        if (mTimer != null) {
                            mTimer.cancel();
                            mTimer = null;
                        }
                        ((TextView) findViewById(R.id.record_status)).setText("Stop");
                    });
                codeDate.set(id, getCalendarTime());
                try {
                    closeMediaRecorder(CameraId);
                    checkAndClear(CameraId);
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("Check file is fail."));
                }
                if (isFinish == 999 || isRun < isFinish - (isCameraOne(CameraId) ? 1 : 0)) {
                    startRecord(CameraId);
                } else {
                    if (isLastCamera(CameraId))
                        isRecordStart(true);
                }
                if (isError || !isSave) {
                    isRun = 0;
                    isFinish = 0;
                    isRecord = false;
                    ((TextView) findViewById(R.id.record_status)).setText("Error");
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
                    runOnUiThread(() -> {
                        if (getFail() + getReset() == 0) {
                            videoLogList.add(new mLogMsg("#Pass"));
                            ((TextView) findViewById(R.id.record_status)).setText("Pass");
                        } else {
                            videoLogList.add(new mLogMsg("#Fail"));
                            ((TextView) findViewById(R.id.record_status)).setText("Fail " + getFail() + getReset());
                        }
                    });
                    new Handler(getMainLooper()).postDelayed(() -> showDialogLog(true), delay_3 / 3);
                }
                for (String CameraId : allCamera) {    // 遍例Camera
                    int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
                    videoLogList.add(new mLogMsg("#stopRecord " + CameraId, mLog.v));
                    Log.e(TAG, "Run " + isRun + " stopRecord " + CameraId);
                    if (isCameraOne(CameraId))
                        runOnUiThread(() -> {
                            if (mTimer != null) {
                                mTimer.cancel();
                                mTimer = null;
                            }
                            ((TextView) findViewById(R.id.record_status)).setText("Stop");
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
            isRun = 0;
            isFinish = 0;
            isRecord = false;
            videoLogList.add(new mLogMsg("#Complete"));
            videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
            saveLog(this, false, false);
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
                videoLogList.add(new mLogMsg("closeMediaRecorder " + CameraId + " is error."));
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private void startRecord(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        if (!isError) {
            Log.e(TAG, isRun + " startRecord " + CameraId);
            try {
                if (isCameraOne(CameraId)) {
                    checkSdCardFromFileList();
                    runOnUiThread(() -> {
                        if (mTimer == null) {
                            onRun = getIsRun();
                            onFail = getFail();
                            onSuccess = getSuccess();
                            timerTask = new mTimerTask();
                            value = 0.0f;
                            mTimer = new Timer(true);
                            ((TextView) findViewById(R.id.record_timer)).setText("00");
                            ((TextView) findViewById(R.id.record_status)).setText("Recording");
                        }
                    });
                    CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                    mPreviewSize = Objects.requireNonNull(manager.getCameraCharacteristics(CameraId)
                            .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP))
                            .getOutputSizes(SurfaceTexture.class)[0];
                    if (burnInTest) {
                        onWifiFail = getWifiFail();
                        onWifiSuccess = getWifiSuccess();
                        onBtFail = getBtFail();
                        onBtSuccess = getBtSuccess();
                        wifiEnableOrDisable();
                    }
                } else {
                    if (burnInTest)
                        btEnableOrDisable();
                }
                try {
                    closePreviewSession(CameraId);
                } catch (Exception e) {
                    errorMessage("closePreviewSession error.", true, e);
                }
                SurfaceTexture texture = null;
                try {
                    texture = textView.get(id).getSurfaceTexture();
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
                                            if (isCameraOne(CameraId))
                                                mTimer.schedule(timerTask, 100, 100);
                                            previewSession.set(id, session);
                                            updatePreview(mPreviewBuilder, session, backgroundHandler.get(id));
                                            if (null != mediaRecorder.get(id)) {
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
                    assert fileList != null;
                    for (File file : fileList) {
                        if (!file.isDirectory() && Utils.getFileExtension(file.toString()).equals("mp4"))
                            if (checkFile(file.toString(), cameraFile))
                                tmp.add(file.toString());
                    }
                    if (tmp.size() >= 6) {
                        Object[] list = tmp.toArray();
                        Arrays.sort(list);
                        for (int i = 0; i < 6; i++)
                            delete((String) list[i], SD_Mode);
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

    private void delete(String path, boolean SdMode) {
        File video = new File(path);
        try {
            if (!path.equals("")) {
                if (video.exists()) {
                    if (SdMode)
                        videoLogList.add(new mLogMsg("Delete: " + path.split("/")[3], mLog.w));
                    else
                        videoLogList.add(new mLogMsg("Delete: " + path.split("/")[5], mLog.w));
                    boolean del = video.delete();
                    if (!del)
                        videoLogList.add(new mLogMsg("Video delete failed.", mLog.e));
                } else {
                    videoLogList.add(new mLogMsg("Video not find.", mLog.e));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkAndClear(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
        try {
            if (isRecord)
                if (null != cameraFilePath.get(id)) {
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
                mediaRecorder.setVideoSource(MediaRecorder.VideoSource.DEFAULT);
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
//                mediaRecorder.setPreviewDisplay(surfaceView.get(id).getHolder().getSurface());
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

    private void takePreview() {
        if (isSave && !isError) {
            for (String CameraId : allCamera) {
                takePreview(CameraId);
            }
        } else {
            Log.e(TAG, "isSave && !isError");
            stopRecordAndSaveLog(false);
            showDialogLog(false);
        }
    }

    private void takePreview(String CameraId) {
        int id = CameraId.equals(allCamera.get(0)) ? 0 : 1;
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
            texture.setDefaultBufferSize(mPreviewSize.getWidth(), mPreviewSize.getHeight());
            Surface surface = new Surface(texture);
            CaptureRequest.Builder mPreviewBuilder;
            mPreviewBuilder = cameraDevice.get(id).createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surface);
            cameraDevice.get(id).createCaptureSession(Collections.singletonList(surface),
                    new CameraCaptureSession.StateCallback() {
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            try {
                                previewSession.set(id, session);
                                updatePreview(mPreviewBuilder, previewSession.get(id), backgroundHandler.get(id));
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }

                        public void onConfigureFailed(
                                @NonNull CameraCaptureSession cameraCaptureSession) {
                            runOnUiThread(() ->
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

    private void wifiEnableOrDisable() {
        try {
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            wifiManager.setWifiEnabled(WifiManager.WIFI_STATE_ENABLED != wifiManager.getWifiState());
            wifiSuccess++;
        } catch (Exception e) {
            wifiFail++;
            errorMessage("Error wifiEnableOrDisable fail.", false, e);
        }
    }

    private void btEnableOrDisable() {
        try {
            mbtAdapter = BluetoothAdapter.getDefaultAdapter();
            if (!mbtAdapter.isEnabled()) {
                mbtAdapter.enable();
            } else {
                mbtAdapter.disable();
            }
            btSuccess++;
        } catch (Exception e) {
            btFail++;
            errorMessage("Error btEnableOrDisable fail.", false, e);
        }
    }

    private class mTimerTask extends TimerTask {
        @SuppressLint("SetTextI18n")
        public void run() {
            runOnUiThread(() -> {
                value += 0.1d;
                BigDecimal bi = new BigDecimal(value);
                float outputValue = bi.setScale(1, BigDecimal.ROUND_HALF_UP).floatValue();
                if (autoStopRecord && outputValue >= 65) {
                    errorMessage("Application has timed out.", true, null);
                }
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