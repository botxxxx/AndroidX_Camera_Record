package com.askey.record;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;

import static com.askey.record.VideoRecordActivity.SD_Mode;
import static com.askey.record.VideoRecordActivity.onReset;
import static com.askey.record.VideoRecordActivity.saveLog;

public class Utils {
    public static final double[] DFRAME_RATE = {16, 27.5},
            NEW_DFRAME_RATE = {14, 28};
    public static final String[] FRAME_RATE = {"16fps", "27.5fps"},
            NEW_FRAME_RATE = {"14fps", "28fps"};
    public static final String[] FPS = {"140", "280"};
    public static final String FRAMESKIP = "persist.our.camera.fps";
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    public static final String EXTRA_VIDEO_COPY = "RestartActivity.copy";
    public static final String EXTRA_VIDEO_PATH = "RestartActivity.path";
    public static final String EXTRA_VIDEO_PASTE = "RestartActivity.paste";
    public static final String EXTRA_VIDEO_REMOVE = "RestartActivity.remove";
    public static final String EXTRA_VIDEO_VERSION = "RestartActivity.version";
    public static final String EXTRA_VIDEO_REFORMAT = "RestartActivity.reformat";
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final String configName = "VideoRecordConfig.ini";
    public static final String logName = "VideoRecordLog.ini";
    public static final String CONFIG_TITLE = "[Video_Record_Config]";
    public static final String LOG_TITLE = "[Video_Record_Log]";
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final double sdData = 1;
    public static final boolean defaultProp = false;
    public static int isRun = 0, Success = 0, Fail = 0;
    public static String TAG = "VideoRecordActivity";
    public static String firstCamera = "0";
    public static String secondCamera = "1";
    public static String lastfirstCamera = "0";
    public static String lastsecondCamera = "1";
    public static String firstFile = "";
    public static String secondFile = "";
    public static ArrayList<String> firstFilePath, secondFilePath;
    public static ArrayList<LogMsg> videoLogList = null;
    public static int isFinish = 999, delayTime = 60500, isFrame = 0, isQuality = 0;
    public static boolean isReady = false, isRecord = false, isError = false, isNew = false;
    public static boolean fCamera = true, sCamera = true, getSdCard = false;
    public static String errorMessage = "";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    //TODO Default Path
    public static String getPath() {
        String path = "/sdcard/NORMAL/";
        return path;
    }

    public static String getSDPath() {
        String path = "";
        if (SD_Mode) {
            try {
                long start = System.currentTimeMillis();
                long end = start + 10000;
                Runtime run = Runtime.getRuntime();
                String cmd = "ls /storage";
                Process pr = run.exec(cmd);
                BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
                String line;
                while ((line = buf.readLine()) != null) {
                    if (!line.equals("self") && !line.equals("emulated") && !line.equals("enterprise") && !line.contains("sdcard")) {
                        path = "/storage/" + line + "/";
                        break;
                    }
                    if (System.currentTimeMillis() > end) {
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            path = getPath();
        }
        return path;
    }

    public static int getFail() {
        return Fail;
    }

    public static int getIsRun() {
        return isRun;
    }

    public static int getSuccess() {
        return Success;
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

    public static boolean isBoolean(String s) {
        try {
            boolean t = Boolean.valueOf(s);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void setTestTime(int min) {
        if (min == 999)
            videoLogList.add(new LogMsg("setRecord time: unlimited times.", mLog.d));
        else {
            videoLogList.add(new LogMsg("setRecord time: " + min + " min.", mLog.d));
        }
        isFinish = min;
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
                    videoLogList.add(new LogMsg("Find the config file.", mLog.d));
                    videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                }
                checkConfigFile(context, new File(getPath(), configName), first);
            }
        } else {
            getSdCard = false;
        }
    }

    public static void checkLogFile(Context context, File file, ArrayList list) {
        String input = readConfigFile(context, file);
        if (input.length() > 0) {
            String[] read = input.split("\r\n");
            for (String s : read)
                list.add(s);
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
                String code = "numberOfRuns = ", prop = "setProperty = ";
                for (String s : read)
                    if (s.indexOf(title) != -1) {
                        target++;
                        t = s.indexOf(title) + title.length();
                        title = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.indexOf(first) != -1) {
                        target++;
                        t = s.indexOf(first) + first.length();
                        first = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.indexOf(second) != -1) {
                        target++;
                        t = s.indexOf(second) + second.length();
                        second = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.indexOf(code) != -1) {
                        target++;
                        t = s.indexOf(code) + code.length();
                        code = s.substring(t);
                        break;
                    }
                for (String s : read)
                    if (s.indexOf(prop) != -1) {
                        target++;
                        t = s.indexOf(prop) + prop.length();
                        prop = s.substring(t);
                        break;
                    }

                if (target == 5) {
                    reformat = false;
                    if (title.equals(context.getString(R.string.app_name))) {
                        update = false;
                    } else {
                        videoLogList.add(new LogMsg("Config is updated.", mLog.e));
                        reformat = true;
                    }
                    if (!first.equals(second)) {
                        if ((first.equals("1") && second.equals("2")) || (first.equals("2") && second.equals("1"))) {
                            videoLogList.add(new LogMsg("Inner and External can't be used at the same time.", mLog.e));
                            reformat = true;
                        } else {
                            if (isCameraID(context, first.split("\n")[0], second.split("\n")[0])) {
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
                        if (min < 0) {
                            videoLogList.add(new LogMsg("The test time must be a positive number.", mLog.e));
                            reformat = true;
                        }
                        setTestTime(min);
                    } else {
                        videoLogList.add(new LogMsg("Unknown Record Times.", mLog.e));
                        reformat = true;
                    }
                    if (isBoolean(prop)) {
                        boolean getProp = Boolean.valueOf(prop);
                        if (isNew != getProp)
                            isPropChange = true;
                        isNew = getProp;

                    } else {
                        videoLogList.add(new LogMsg("Unknown setProperty.", mLog.e));
                        reformat = true;
                    }
                }
            }
            if (update) {
                String logString = LOG_TITLE + context.getString(R.string.app_name) + "\r\n";
                videoLogList.add(new LogMsg("Reformat the Log file.", mLog.e));
                for (LogMsg logs : videoLogList) {
                    String time = logs.time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                            + " run:" + logs.runTime + " -> ";
                    logString += (time + logs.msg + "\r\n");
                }
                try {
                    FileOutputStream output = new FileOutputStream(new File(getPath(), logName), false);
                    output.write(logString.getBytes());
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

    public static boolean isCameraID(Context context, String f, String b) {
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
        int isFinish = 999;

        if (!reset) {
            if (isInteger(editText_3.getText().toString(), false)) {
                isFinish = Integer.parseInt(editText_3.getText().toString());
            } else {
                isFinish = 999;
            }
        }

        //toast(context, "Ready to write.", mLog.w);
        writeConfigFile(context, file, (
                !reset ? new Config(context, editText_1.getText().toString(),
                        editText_2.getText().toString(), isFinish, isNew) : new Config(context)).config());
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
            FileInputStream input = new FileInputStream(file);
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                bytes.write(buffer, 0, length);
            }
            tmp += bytes.toString();
            bytes.close();
            input.close();
        } catch (Exception e) {
            e.printStackTrace();
            isError = true;
            getSdCard = !getSDPath().equals("");
            videoLogList.add(new LogMsg("Read failed. " + NO_SD_CARD + ". <============ Crash here", mLog.e));
            new Handler().post(() -> saveLog(context, false, false));
            errorMessage = "Read failed." + NO_SD_CARD + "<============ Crash here";
            videoLogList.add(new LogMsg("Read failed.", mLog.e));
            tmp += ("App Version:" + context.getString(R.string.app_name) + "\r\n");
            tmp += (NO_SD_CARD);
            return tmp;
        }
        return tmp;
    }

    public static void writeConfigFile(Context context, File file, String[] str) {
        if (getSdCard) {
            String tmp = "";
            for (String s : str)
                tmp += s;
            try {
                FileOutputStream output = new FileOutputStream(file);
                output.write(tmp.getBytes());
                output.close();
            } catch (Exception e) {
                e.printStackTrace();
                isError = true;
                getSdCard = !getSDPath().equals("");
                videoLogList.add(new LogMsg("Write failed. " + NO_SD_CARD + ". <============ Crash here", mLog.e));
                new Handler().post(() -> saveLog(context, false, false));
                errorMessage = "Write failed. " + NO_SD_CARD + "<============ Crash here";
            }
        } else {
            videoLogList.add(new LogMsg(NO_SD_CARD, mLog.e));
        }
    }

    @SuppressLint("DefaultLocale")
    public static String getCalendarTime() {
        String d, h, i, s;
        Calendar calendar = Calendar.getInstance();
        d = String.format("%02d", calendar.get(Calendar.DATE));
        h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        i = String.format("%02d", calendar.get(Calendar.MINUTE));
        s = String.format("%02d", calendar.get(Calendar.SECOND));

        return d + h + i + s + "";
    }

    @SuppressLint("DefaultLocale")
    public static String getCalendarTime(boolean isCameraOne) {
        String y, m, d, h, i, s;
        Calendar calendar = Calendar.getInstance();
        y = String.format("%02d", calendar.get(Calendar.YEAR));
        m = String.format("%02d", (calendar.get(Calendar.MONTH) - 1));
        d = String.format("%02d", calendar.get(Calendar.DATE));
        h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        i = String.format("%02d", calendar.get(Calendar.MINUTE));
        s = String.format("%02d", calendar.get(Calendar.SECOND));

        return "v" + y + m + d + h + i + s + (isCameraOne ? "f" : "s");
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

}
