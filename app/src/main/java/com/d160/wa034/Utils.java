package com.d160.wa034;

import android.Manifest;
import android.annotation.*;
import android.content.Context;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.media.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.view.*;

import com.d160.view.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static android.os.Environment.*;
import static com.d160.wa034.CameraFragment.*;

@SuppressLint("StaticFieldLeak")
public class Utils {
    //-------------------------------------------------------------------------------
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    //-------------------------------------------------------------------------------
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final int defaultRun = 480;
    public static final double sdData = 3;
    public static long isFinish = defaultRun, isRun = 0, Success = 0, Fail = 0;
    public static ArrayList<mLogMsg> videoLogList = null;
    public static boolean isInitReady = false, isCameraReady = true, isRecord = false,
            isError = false, isSave = false;
    public static String errorMessage = "";
    //-------------------------------------------------------------------------------
    public static List<String> allCamera = Arrays.asList(firstCamera);
    public static AtomicReferenceArray<Boolean> isOpenCamera = new AtomicReferenceArray<>(new Boolean[]{Open_f_Camera});
    public static AtomicIntegerArray view_id = new AtomicIntegerArray(new int[]{R.id.view0});
    public static AtomicReferenceArray<String> threadString = new AtomicReferenceArray<>(new String[]{"CameraPreview0", "CameraPreview1"});
    public static AtomicReferenceArray<String> permission = new AtomicReferenceArray<>(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    public static AtomicReferenceArray<String> cameraFile = new AtomicReferenceArray<>(new String[]{"", ""});
    public static AtomicReferenceArray<Boolean> isCameraOpened = new AtomicReferenceArray<>(new Boolean[]{false, false});
    public static AtomicReferenceArray<ArrayList<String>> cameraFilePath = new AtomicReferenceArray<ArrayList<String>>(new ArrayList[2]);
    public static AtomicReferenceArray<String> codeDate = new AtomicReferenceArray<>(new String[2]);
    public static AtomicReferenceArray<TextureView> textureView = new AtomicReferenceArray<>(new TextureView[2]);
    public static AtomicReferenceArray<CameraDevice> cameraDevice = new AtomicReferenceArray<>(new CameraDevice[2]);
    public static AtomicReferenceArray<CameraCaptureSession> previewSession = new AtomicReferenceArray<>(new CameraCaptureSession[2]);
    public static AtomicReferenceArray<CameraDevice.StateCallback> stateCallback = new AtomicReferenceArray<>(new CameraDevice.StateCallback[2]);
    public static AtomicReferenceArray<MediaRecorder> mediaRecorder = new AtomicReferenceArray<>(new MediaRecorder[2]);
    public static AtomicReferenceArray<HandlerThread> thread = new AtomicReferenceArray<>(new HandlerThread[2]);
    public static AtomicReferenceArray<Handler> backgroundHandler = new AtomicReferenceArray<>(new Handler[2]);
    public static AtomicReferenceArray<Handler> recordHandler = new AtomicReferenceArray<>(new Handler[2]);
    public static AtomicReferenceArray<Handler> stopRecordHandler = new AtomicReferenceArray<>(new Handler[2]);
    //-------------------------------------------------------------------------------

    public static String getLogPath() {
        return getPath() + "DCIM/";
    }

    //TODO Default Path
    public static String getPath() {
        return getStorageDirectory().getPath() + "/emulated/0/";
    }

    public static String getSDPath() {
        String path = "";
        if (SD_Mode) {
            try {
                long start = (System.currentTimeMillis() / 1000) % 60;
                long end = start + 10;
                Runtime run = Runtime.getRuntime();
                String cmd = "ls /storage";
                Process pr = run.exec(cmd);
                InputStreamReader input = new InputStreamReader(pr.getInputStream());
                BufferedReader buf = new BufferedReader(input);
                String line;
                while ((line = buf.readLine()) != null) {
                    if (!line.equals("self") && !line.equals("emulated") && !line.equals("enterprise") && !line.contains("sdcard")) {
                        path = "/storage/" + line + "/";
                        break;
                    }
                    if ((System.currentTimeMillis() / 1000) % 60 > end) {
                        videoLogList.add(new mLogMsg("getSDPath time out.", mLog.e));
                        break;
                    }
                }
                buf.close();
                input.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            path = getPath();
        }
        return path;
    }

    public static long getIsRun() {
        return isRun;
    }

    public static long getSuccess() {
        return Success;
    }

    public static long getFail() {
        return Fail;
    }

    public static long getReset() {
        return onReset;
    }

    public static boolean isInteger(String s, boolean zero) {
        try {
            if (Integer.parseInt(s) <= (zero ? 0 : -1)) {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void setTestTime(int min) {
        if (min == 999) {
            Log.e(TAG, "setRecord time: 999.");
            videoLogList.add(new mLogMsg("setRecord time: 999 times.", mLog.d));
        } else {
            Log.e(TAG, "setRecord time: " + min + " min.");
            videoLogList.add(new mLogMsg("setRecord time: " + min + " min.", mLog.d));
        }
        isFinish = min;
    }

    public static void checkConfigFile(Context context) {
        videoLogList.add(new mLogMsg("#checkConfigFile", mLog.v));
        if (!getPath().equals("")) {
            isSave = true;
            File file = new File(getLogPath(), configName);
            if (!file.exists()) {
                try {
                    if (file.createNewFile())
                        videoLogList.add(new mLogMsg("Create the config.", mLog.w));
                    else
                        videoLogList.add(new mLogMsg("Failed to create the config.", mLog.w));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                writeConfigFile(context, file, new Config(context).config());
            } else {
                if (!isCameraReady) {
                    videoLogList.add(new mLogMsg("Find the config file.", mLog.e));
                    videoLogList.add(new mLogMsg("#------------------------------", mLog.v));
                }
                checkConfigFile(context, new File(getLogPath(), configName));
            }
        } else {
            isSave = false;
        }
    }

    @SuppressLint("SimpleDateFormat")
    public static void checkConfigFile(Context context, File file) {
        try {
            String input = readConfigFile(context, file);
            boolean reformat = true, update = true;
            if (input.length() > 0) {
                String[] read = input.split("\r\n");
                int target = 0, t;
                String title = CONFIG_TITLE;
                String code = "numberOfRuns = ";
                for (String s : read)
                    if (s.contains(title)) {
                        target++;
                        t = s.indexOf(title) + title.length();
                        title = s.substring(t);
                    } else if (s.contains(code)) {
                        target++;
                        t = s.indexOf(code) + code.length();
                        code = s.substring(t);
                    }
                if (target == 2) {
                    reformat = false;
                    if (title.equals(context.getString(R.string.app_name))) {
                        update = false;
                    } else {
                        Log.e(TAG, "Config is updated.");
                        videoLogList.add(new mLogMsg("Config is updated.", mLog.e));
                        reformat = true;
                    }
                    if (isInteger(code.split("\n")[0], true)) {
                        int min = Integer.parseInt(code.split("\n")[0]);
                        if (min <= 0) {
                            Log.e(TAG, "The test time must be a positive number.");
                            videoLogList.add(new mLogMsg("The test time must be a positive number.", mLog.e));
                            reformat = true;
                        } else {
                            setTestTime(min);
                        }
                    } else {
                        Log.e(TAG, "Unknown Record Times.");
                        videoLogList.add(new mLogMsg("Unknown Record Times.", mLog.e));
                        reformat = true;
                    }
                } else {
                    Log.e(TAG, "target != 2 & Config is reset.");
                }
            }
            if (update) {
                StringBuilder logString = new StringBuilder(LOG_TITLE + context.getString(R.string.app_name) + "\r\n");
                videoLogList.add(new mLogMsg("Reformat the Log file.", mLog.e));
                for (mLogMsg logs : videoLogList) {
                    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    logString.append(dateFormat.format(logs.date)).append(" run:")
                            .append(logs.run).append(" -> ").append(logs.msg).append("\r\n");
                }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getLogPath(), logName), false);
                    output.write(logString.toString().getBytes());
                    output.close();
                    videoLogList.clear();
                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new mLogMsg("Write LOG failed. <============ error here", mLog.e));
                }
            }
            if (reformat) {
                reformatConfigFile(context, file);
            }
        } catch (Exception e) {
            e.printStackTrace();
            reformatConfigFile(context, file);
        }
    }

    public static void reformatConfigFile(Context context, File file) {
        writeConfigFile(context, file, new Config(context).config());
        videoLogList.add(new mLogMsg("Reformat the Config file.", mLog.e));
    }

    public static String readConfigFile(Context context, File file) {
        String tmp = "";
        try {
            byte[] buffer = new byte[100];
            int length;
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            while ((length = bis.read(buffer)) != -1) {
                bytes.write(buffer, 0, length);
            }
            tmp += bytes.toString();
            bytes.close();
            bis.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new mLogMsg("Read failed. <============ Crash here", mLog.e));
            saveLog(context, false, false);
            errorMessage = "Read failed. <============ Crash here";
            videoLogList.add(new mLogMsg("Read failed.", mLog.e));
            tmp += ("App Version:" + context.getString(R.string.app_name) + "\r\n");
            tmp += ("Read failed. <============ Crash here");
            return tmp;
        }
        return tmp;
    }

    public static void writeConfigFile(Context context, File file, String[] str) {
        StringBuilder tmp = new StringBuilder();
        for (String s : str)
            tmp.append(s);
        try {
            FileOutputStream output = new FileOutputStream(file);
            output.write(tmp.toString().getBytes());
            output.close();
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            videoLogList.add(new mLogMsg("Write failed. <============ Crash here", mLog.e));
            saveLog(context, false, false);
            errorMessage = "Write failed. <============ Crash here";
        }
    }

    public static void updateConfigFile(Context context, File file, int min){
        Log.e(TAG, "setConfig time: " + min + " min.");
//        videoLogList.add(new mLogMsg("set Config file. "+isRun+" ", mLog.e));
        writeConfigFile(context, file, new Config(context, min).config());
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    public static String getCalendarTime() {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat("HHmmss").format(calendar.getTime()) + "";
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    public static String getCalendarTime(String cameraId) {
        Calendar calendar = Calendar.getInstance();
        String cm = "c";
        switch (cameraId) {
            case firstCamera:
                cm = "f";
                break;
            case secondCamera:
                cm = "s";
                break;
            case thirdCamera:
                cm = "t";
                break;
        }
        return "v" + new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime()) + cm;
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static boolean isCameraOne(String cameraId) {
//        if (isOpenCamera.get(0))
            return cameraId.equals(allCamera.get(0));
//        else if (isOpenCamera.get(1))
//            return cameraId.equals(allCamera.get(1));
//        else
//            return cameraId.equals(allCamera.get(2));
    }

    public static boolean isLastCamera(String cameraId) {
//        if (isOpenCamera.get(2))
//            return cameraId.equals(allCamera.get(2));
//        else if (isOpenCamera.get(1))
//            return cameraId.equals(allCamera.get(1));
//        else
            return cameraId.equals(allCamera.get(0));
    }

    public static void delete(String path, boolean SdMode) {
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
}
