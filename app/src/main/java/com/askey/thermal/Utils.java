package com.askey.thermal;

import android.annotation.SuppressLint;
import android.util.SparseIntArray;
import android.view.Surface;

import com.askey.widget.mLog;
import com.askey.widget.mLogMsg;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import static com.askey.thermal.VideoRecordActivity.Open_f_Camera;
import static com.askey.thermal.VideoRecordActivity.Open_s_Camera;
import static com.askey.thermal.VideoRecordActivity.Open_t_Camera;
import static com.askey.thermal.VideoRecordActivity.SD_Mode;
import static com.askey.thermal.VideoRecordActivity.onReset;

public class Utils {
    public static String TAG = "CDR9030_Thermal";
    //-------------------------------------------------------------------------------
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    public static final String NO_SD_CARD = "SD card is not available!";
    //-------------------------------------------------------------------------------
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final String logName = "ThermalTestLog.ini";
    public static final String LOG_TITLE = "[CDR9030_Thermal_Test]";
    public static final double sdData = 3;
    public static int isRun = 0, Success = 0, Fail = 0;
    public static final String firstCamera = "0";
    public static final String secondCamera = "1";
    public static final String thirdCamera = "2";
    public static String firstFile = "";
    public static String secondFile = "";
    public static String thirdFile = "";
    public static ArrayList<String> firstFilePath, secondFilePath, thirdFilePath;
    public static ArrayList<mLogMsg> videoLogList = null;
    public static boolean isReady = false, isRecord = false, isError = false;
    public static boolean fCamera = false, sCamera = false, tCamera = false, getSdCard = false;
    public static String errorMessage = "";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static String getLogPath() {
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
                        videoLogList.add(new mLogMsg("getSDPath time out.", mLog.d));
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
        if (Open_f_Camera)
            return cameraId.equals(firstCamera);
        else if (Open_s_Camera)
            return cameraId.equals(secondCamera);
        else
            return cameraId.equals(thirdCamera);
    }

    public static boolean isLastCamera(String cameraId) {
        if (Open_t_Camera)
            return cameraId.equals(thirdCamera);
        else if (Open_s_Camera)
            return cameraId.equals(secondCamera);
        else
            return cameraId.equals(firstCamera);
    }
}
