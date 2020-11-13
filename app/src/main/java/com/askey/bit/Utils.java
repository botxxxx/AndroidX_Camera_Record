package com.askey.bit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

import static com.askey.bit.VideoRecordActivity.SD_Mode;
import static com.askey.bit.VideoRecordActivity.onReset;
import static com.askey.bit.VideoRecordActivity.saveLog;

public class Utils {
    public static final String TAG = "CDR9020_BIT";
    //-------------------------------------------------------------------------------
    public static final boolean defaultProp = false;
    public static final int defaultRun = 480;
    public static int isFinish = 999, delayTime = 60500, isQuality = 0;
    //-------------------------------------------------------------------------------
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_WIFI_FAIL = "RestartActivity.wifi.fail";
    public static final String EXTRA_VIDEO_BT_FAIL = "RestartActivity.bt.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    public static final String EXTRA_VIDEO_WIFI_SUCCESS = "RestartActivity.wifi.success";
    public static final String EXTRA_VIDEO_BT_SUCCESS = "RestartActivity.bt.success";
    //-------------------------------------------------------------------------------
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final String configName = "BurnInTestConfig.ini";
    public static final String logName = "BurnInTestLog.ini";
    public static final String CONFIG_TITLE = "[BurnIn_Test_Config]";
    public static final String LOG_TITLE = "[BurnIn_Test_Log]";
    public static final double sdData = 1;
    public static int isRun = 0, Success = 0, Fail = 0;
    public static int wifiSuccess = 0, wifiFail = 0;
    public static int btSuccess = 0, btFail = 0;
    public static String firstCamera = "0";
    public static String secondCamera = "1";
    public static String lastfirstCamera = "0";
    public static String lastsecondCamera = "1";
    public static String firstFile = "";
    public static String secondFile = "";
    public static ArrayList<LogMsg> videoLogList = null;
    public static boolean isReady = false, isRecord = false, isError = false;
    public static boolean fCamera = false, sCamera = false, getSdCard = false;
    public static String errorMessage = "";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static String getLogPath(){
        return "/data/misc/logd/";
    }

    //TODO Default Path
    public static String getPath() {
        return "/storage/emulated/0/DCIM/";
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
                        videoLogList.add(new LogMsg("getSDPath time out.", mLog.e));
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

    public static int getWifiSuccess() {
        return wifiSuccess;
    }

    public static int getWifiFail() {
        return wifiFail;
    }

    public static int getBtSuccess() {
        return btSuccess;
    }

    public static int getBtFail() {
        return btFail;
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
        Log.e(TAG, "setRecord time: " + min + " min.");
        videoLogList.add(new LogMsg("setRecord time: " + min + " min.", mLog.e));
        isFinish = min == 999 ? min : min * 2;
    }

    public static void checkConfigFile(Context context, boolean first) {
        videoLogList.add(new LogMsg("#checkConfigFile", mLog.v));
        if (!getPath().equals("")) {
            getSdCard = true;
            File file = new File(getPath(), configName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                videoLogList.add(new LogMsg("Create the config file.", mLog.w));
                writeConfigFile(context, file, new Config(context).config());
            } else {
                if (!isReady) {
                    videoLogList.add(new LogMsg("Find the config file.", mLog.e));
                    videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                }
                checkConfigFile(context, new File(getPath(), configName), first);
            }
        } else {
            getSdCard = false;
        }
    }

    public static boolean[] checkConfigFile(Context context, File file, boolean firstOne) {
        try {
            String input = readConfigFile(context, file);
            boolean reformat = true, isCameraChange = false, isPropChange = false, update = true;
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
                        videoLogList.add(new LogMsg("Config is updated.", mLog.e));
                        reformat = true;
                    }
                    if (!first.equals(second)) {
                        boolean cdr9020 = (first.equals("1") && second.equals("2")) || (first.equals("2") && second.equals("1"));
                        if (cdr9020) {
                            videoLogList.add(new LogMsg("Inner and External can't be used at the same time.", mLog.e));
                            reformat = true;
                        } else {
                            if (isCameraID(first.split("\n")[0], second.split("\n")[0])) {
                                lastfirstCamera = firstOne ? first : firstCamera;
                                lastsecondCamera = firstOne ? second : secondCamera;
                                firstCamera = first;
                                secondCamera = second;
                                if (!firstOne)
                                    if (!lastfirstCamera.equals(firstCamera) || !lastsecondCamera.equals(secondCamera))
                                        isCameraChange = true;
                            } else {
                                videoLogList.add(new LogMsg("Unknown Camera ID.", mLog.e));
                                reformat = true;
                            }
                        }
                    } else {
                        videoLogList.add(new LogMsg("Cannot use the same Camera ID.", mLog.e));
                        reformat = true;
                    }
                    if (isInteger(code.split("\n")[0], true)) {
                        int min = Integer.parseInt(code.split("\n")[0]);
                        if (min <= 0) {
                            videoLogList.add(new LogMsg("The test time must be a positive number.", mLog.e));
                            reformat = true;
                        } else {
                            setTestTime(min);
                        }
                    } else {
                        videoLogList.add(new LogMsg("Unknown Record Times.", mLog.e));
                        reformat = true;
                    }
                }
            }
            if (update) {
                StringBuilder logString = new StringBuilder(LOG_TITLE + context.getString(R.string.app_name) + "\r\n");
                videoLogList.add(new LogMsg("Reformat the Log file.", mLog.e));
                for (LogMsg logs : videoLogList) {
                    String time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + " run:" + logs.runTime + " -> ";
                    logString.append(time).append(logs.msg).append("\r\n");
                }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getPath(), logName), false);
                    output.write(logString.toString().getBytes());
                    output.close();
                    videoLogList.clear();

                } catch (Exception e) {
                    e.printStackTrace();
                    videoLogList.add(new LogMsg("Write LOG failed. <============ error here", mLog.e));
                }
            }
            if (reformat) {
                reformatConfigFile(context, file);
                return new boolean[]{true, true};
            } else return new boolean[]{isCameraChange, isPropChange};
        } catch (Exception e) {
            e.printStackTrace();
            reformatConfigFile(context, file);
            return new boolean[]{true, true};
        }
    }

    public static boolean isCameraID(String f, String b) {
        try {
            if (Integer.parseInt(f) <= -1) {
                videoLogList.add(new LogMsg("The Camera ID must be a positive number.", mLog.e));
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
                    videoLogList.add(new LogMsg("The Camera ID is unknown.", mLog.e));
                    return false;
                }
            }
            if (Integer.parseInt(b) <= -1) {
                videoLogList.add(new LogMsg("The Camera ID must be a positive number.", mLog.e));
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
                    videoLogList.add(new LogMsg("The Camera ID is unknown.", mLog.e));
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
            } else {
                isFinish = defaultRun;
            }
        }

        writeConfigFile(context, file, (
                !reset ? new Config(context, editText_1.getText().toString(),
                        editText_2.getText().toString(), isFinish, false) : new Config(context)).config());
        //toast(context, "Write file is completed.", mLog.i);
    }

    public static void reformatConfigFile(Context context, File file) {
        //toast(context, "Config file error.", mLog.e);
        writeConfigFile(context, file, new Config(context).config());
        videoLogList.add(new LogMsg("Reformat the Config file.", mLog.e));
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
            getSdCard = !getSDPath().equals("");
            videoLogList.add(new LogMsg("Read failed. <============ Crash here", mLog.e));
            saveLog(context, false, false);
            errorMessage = "Read failed. <============ Crash here";
            videoLogList.add(new LogMsg("Read failed.", mLog.e));
            tmp += ("App Version:" + context.getString(R.string.app_name) + "\r\n");
            tmp += ("Read failed. <============ Crash here");
            return tmp;
        }
        return tmp;
    }

    public static void writeConfigFile(Context context, File file, String[] str) {
        if (getSdCard) {
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
                getSdCard = !getSDPath().equals("");
                videoLogList.add(new LogMsg("Write failed. <============ Crash here", mLog.e));
                saveLog(context, false, false);
                errorMessage = "Write failed. <============ Crash here";
            }
        } else {
            videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
        }
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    public static String getCalendarTime() {
        Calendar calendar = Calendar.getInstance();
        return new SimpleDateFormat("HHmmss").format(calendar.getTime()) + "";
    }

    @SuppressLint({"DefaultLocale", "SimpleDateFormat"})
    public static String getCalendarTime(boolean isCameraOne) {
        Calendar calendar = Calendar.getInstance();
        return "v" + new SimpleDateFormat("yyyyMMddHHmmss").format(calendar.getTime()) + (isCameraOne ? "f" : "s");
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static boolean isCameraOne(String cameraId) {
        return cameraId.equals(firstCamera);
    }
}
