package com.d160.qtr;

import android.*;
import android.annotation.*;
import android.content.*;
import android.hardware.camera2.*;
import android.media.*;
import android.os.*;
import android.util.*;
import android.view.*;
import android.widget.*;

import com.d160.view.*;

import java.io.*;
import java.lang.Process;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static android.os.Environment.*;
import static com.d160.qtr.CameraActivity.*;

public class Utils {
    //-------------------------------------------------------------------------------
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final int defaultRun = 999;
    public static final double sdData = 3;
    public static int isFinish = defaultRun, isRun = 0, Success = 0, Fail = 0;
    public static ArrayList<mLogMsg> videoLogList = null;
    public static boolean isCameraReady = false, isRecord = false,
            isError = false, isSave = false;
    public static String errorMessage = "";
    //-------------------------------------------------------------------------------
    public static List<String> allCamera = Arrays.asList(firstCamera, secondCamera);
    public static AtomicIntegerArray id_textView = new AtomicIntegerArray(new int[]{R.id.textureView0, R.id.textureView1});
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
    //TODO Default Path
    @SuppressLint("NewApi")
    public static String getPath() {
        return getStorageDirectory().getPath() + "/emulated/0/DCIM/";
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

    public static int getIsRun() {
        return isRun;
    }

    public static int getSuccess() {
        return Success;
    }

    public static int getFail() {
        return Fail;
    }

    public static int getReset() {
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
            videoLogList.add(new mLogMsg("setRecord time: unlimited times.", mLog.d));
        }else {
            Log.e(TAG, "setRecord time: " + min + " min.");
            videoLogList.add(new mLogMsg("setRecord time: " + min + " min.", mLog.d));
        }
        isFinish = min == 999 ? min : min * 2;
    }

    public static void checkConfigFile(Context context, boolean first) {
        videoLogList.add(new mLogMsg("#checkConfigFile", mLog.v));
        if (!getPath().equals("")) {
            isSave = true;
            File file = new File(getPath(), configName);
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
                checkConfigFile(context, new File(getPath(), configName), first);
            }
        } else {
            isSave = false;
        }
    }

    public static boolean checkConfigFile(Context context, File file, boolean firstOne) {
        try {
            String input = readConfigFile(context, file);
            boolean reformat = true, isCameraChange = false, update = true;
            if (input.length() > 0) {
                String[] read = input.split("\r\n");
                int target = 0, t;
                String title = CONFIG_TITLE;
                String first = "firstCameraID = ", second = "secondCameraID = ";
                String code = "numberOfRuns = ";
                for (String s : read)
                    if (s.contains(title)) {
                        target++;
                        t = s.indexOf(title) + title.length();
                        title = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.contains(first)) {
                        target++;
                        t = s.indexOf(first) + first.length();
                        first = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.contains(second)) {
                        target++;
                        t = s.indexOf(second) + second.length();
                        second = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.contains(code)) {
                        target++;
                        t = s.indexOf(code) + code.length();
                        code = s.substring(t);
                        break;
                    }
                if (target == 4) {
                    reformat = false;
                    if (title.equals(context.getString(R.string.app_name))) {
                        update = false;
                    } else {
                        Log.e(TAG, "Config is updated.");
                        videoLogList.add(new mLogMsg("Config is updated.", mLog.e));
                        reformat = true;
                    }
                    if (!first.equals(second)) {
                        boolean cdr9020 = (first.equals("1") && second.equals("2")) || (first.equals("2") && second.equals("1"));
                        if (cdr9020) {
                            Log.e(TAG, "Inner and External can't be used at the same time.");
                            videoLogList.add(new mLogMsg("Inner and External can't be used at the same time.", mLog.e));
                            reformat = true;
                        } else {
                            if (isCameraID(first.split("\n")[0], second.split("\n")[0])) {
                                lastFirstCamera = firstOne ? first : firstCamera;
                                lastSecondCamera = firstOne ? second : secondCamera;
                                allCamera.set(0, firstCamera = first);
                                allCamera.set(1, secondCamera = second);
                                if (!firstOne)
                                    if (!lastFirstCamera.equals(firstCamera) || !lastSecondCamera.equals(secondCamera)) {
                                        Log.e(TAG, "isCameraChange:"+lastFirstCamera+"-"+firstCamera+","+lastSecondCamera+"-"+secondCamera);
                                        isCameraChange = true;
                                    }
                            } else {
                                Log.e(TAG, "Unknown Camera ID.");
                                videoLogList.add(new mLogMsg("Unknown Camera ID.", mLog.e));
                                reformat = true;
                            }
                        }
                    } else {
                        Log.e(TAG, "Cannot use the same Camera ID.");
                        videoLogList.add(new mLogMsg("Cannot use the same Camera ID.", mLog.e));
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
                } else{
                    Log.e(TAG, "target != 4 & Config is reset.");
                }
            }
            if (update) {
                StringBuilder logString = new StringBuilder(LOG_TITLE + context.getString(R.string.app_name) + "\r\n");
                videoLogList.add(new mLogMsg("Reformat the Log file.", mLog.e));
                for (mLogMsg logs : videoLogList) {
                    @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    logString.append(dateFormat.format(logs.time)).append(" run:")
                            .append(logs.runTime).append(" -> ").append(logs.msg).append("\r\n");
                }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getPath(), logName), false);
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
                return true;
            } else return isCameraChange;
        } catch (Exception e) {
            e.printStackTrace();
            reformatConfigFile(context, file);
            return true;
        }
    }

    public static boolean isCameraID(String f, String b) {
        try {
            if (Integer.parseInt(f) <= -1) {
                videoLogList.add(new mLogMsg("The Camera ID must be a positive number.", mLog.e));
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
                    videoLogList.add(new mLogMsg("The Camera ID is unknown.", mLog.e));
                    return false;
                }
            }
            if (Integer.parseInt(b) <= -1) {
                videoLogList.add(new mLogMsg("The Camera ID must be a positive number.", mLog.e));
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
                    videoLogList.add(new mLogMsg("The Camera ID is unknown.", mLog.e));
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setConfigFile(Context context, File file, View view, boolean reset) {
        EditText editText_1 = view.findViewById(R.id.dialog_editText_1);
        EditText editText_2 = view.findViewById(R.id.dialog_editText_2);
        EditText editText_3 = view.findViewById(R.id.dialog_editText_3);
        int isFinish = defaultRun;

        if (!reset) {
            if (isInteger(editText_3.getText().toString(), false)) {
                isFinish = Integer.parseInt(editText_3.getText().toString());
            }
        }

        writeConfigFile(context, file, (
                !reset ? new Config(context, editText_1.getText().toString(),
                        editText_2.getText().toString(), isFinish) : new Config(context)).config());
        //toast(context, "Write file is completed.", mLog.i);
    }

    public static void reformatConfigFile(Context context, File file) {
        //toast(context, "Config file error.", mLog.e);
        lastFirstCamera = "0";
        lastSecondCamera = "1";
        allCamera.set(0, firstCamera = lastFirstCamera);
        allCamera.set(1, secondCamera = lastSecondCamera);
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
        if (isSave) {
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
        } else {
            videoLogList.add(new mLogMsg(NO_SD_CARD, mLog.e));
        }
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
        if (cameraId.equals(allCamera.get(0)))
            cm = "f";
        if (cameraId.equals(allCamera.get(1)))
            cm = "s";
        return "v" + new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime()) + cm;
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static boolean isCameraOne(String cameraId) {
        return cameraId.equals(allCamera.get(0));
    }

    public static boolean isLastCamera(String cameraId) {
        return cameraId.equals(allCamera.get(1));
    }
}
