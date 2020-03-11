package com.askey.record;

import android.app.Activity;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;

import static com.askey.record.VideoRecordActivity.onReset;
import static com.askey.record.VideoRecordActivity.saveLog;

public class Utils {

    public static final String[] FRAME_RATE = {"27.5fps", "16fps"},
            NEW_FRAME_RATE = {"27.5fps", "13.7fps"}; // , "9.1fps", "6.8fps", "5.5fps", "4.5fps"
    public static final double[] DFRAME_RATE = {27.5, 16},
            NEW_DFRAME_RATE = {27.5, 13.7}; // , 9.1, 6.8, 5.5, 4.5
    public static final String FRAMESKIP = "persist.our.camera.frameskip";
    public static final String COMMAND_VIDEO_RECORD_TEST = "com.askey.record.t";
    public static final String COMMAND_VIDEO_RECORD_START = "com.askey.record.s";
    public static final String COMMAND_VIDEO_RECORD_FINISH = "com.askey.record.f";
    public static final String COMMAND_VIDEO_RECORD_TESTa = "com.askey.record.T";
    public static final String COMMAND_VIDEO_RECORD_STARTa = "com.askey.record.S";
    public static final String COMMAND_VIDEO_RECORD_FINISHa = "com.askey.record.F";
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_COPY = "RestartActivity.copy";
    public static final String EXTRA_VIDEO_PASTE = "RestartActivity.paste";
    public static final String EXTRA_VIDEO_REMOVE = "RestartActivity.remove";
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final String configName = "VideoRecordConfig.ini";
    public static final String logName = "VideoRecordLog.ini";
    public static final double sdData = 1;
    public static int isRun = 0, successful = 0, failed = 0;
    public static String TAG = "VideoRecord";
    public static String firstCamera = "0";
    public static String secondCamera = "1";
    public static String lastfirstCamera = "0";
    public static String lastsecondCamera = "1";
    public static ArrayList<String> firstFilePath, secondFilePath;
    public static ArrayList<LogMsg> videoLogList = null;
    public static int isFinish = 999, delayTime = 60000, isFrame = 0, isQuality = 0;
    public static boolean isReady = false, isRecord = false, isError = false, isNew = false, getSdCard = false;
    public static boolean fCamera = true, sCamera = true;
    public static String errorMessage = "";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static void toast(Context context, String t, mLog type) {
        videoLogList.add(new LogMsg(t, type));
//        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, t + "", Toast.LENGTH_SHORT).show());
        //new Handler().post(() -> VideoRecordActivity.saveLog(context, false));
    }

    public static void toast(Context context, String t) {
        videoLogList.add(new LogMsg(t, mLog.i));
//        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, t + "", Toast.LENGTH_SHORT).show());
        //new Handler().post(() -> VideoRecordActivity.saveLog(context, false));
    }

    public static void toast(Context context) {
//        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Is Recording Now.", Toast.LENGTH_SHORT).show());
        //new Handler().post(() -> VideoRecordActivity.saveLog(context, false));
    }

    public static int getSuccessful() {
        return successful;
    }

    public static int getFailed() {
        return failed;
    }

    public static int getReset() {
        return onReset;
    }

    public static int getIsRun() {
        return isRun;
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
            if (Boolean.parseBoolean(s)) {
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static void setTestTime(Context context, int min) {
        if (min > 0) {
            isFinish = min;
            if (min != 999)
                toast(context, "setRecord time: " + min + " min.");
            else
                toast(context, "setRecord time: unlimited times.");
        } else {
            toast(context, "The test time must be a positive number.", mLog.e);
        }
    }

    public static boolean checkConfigFile(Context context, boolean first) {
        videoLogList.add(new LogMsg("#checkConfigFile", mLog.v));
        if (!getPath().equals("")) {
            File file = new File(getPath(), configName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                toast(context, "Create the config file.", mLog.w);
                writeConfigFile(context, file, new Configini(context).config());
            } else {
                if (!isReady) {
                    toast(context, "Find the config file.", mLog.d);
                    videoLogList.add(new LogMsg("#------------------------------", mLog.v));
                }
                checkConfigFile(context, new File(getPath(), configName), first);
            }
            return true;
        } else {
            return false;
        }
    }

    public static void checkLogFile(ArrayList list) {
        list.add("Please check file at you SD card");
//        if (input.length() > 0) {
//            String[] read = input.split("\r\n");
//            for (String s : read)
//                list.add(s);
//        }
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
            boolean reformat = true, isCameraChange = false, isPropChange = false, update = false;
            if (input.length() > 0) {
                String[] read = input.split("\r\n");
                int target = 0, t;
                String title = "[VIDEO_RECORD_TESTING]";
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
                    update = false;
                    if (!title.equals(context.getString(R.string.app_name))) {
                        toast(context, "Config is updated.", mLog.e);
                        reformat = true;
                        update = true;
                    }
                    if (!first.equals(second)) {
                        if ((first.equals("1") && second.equals("2")) || (first.equals("2") && second.equals("1"))) {
                            toast(context, "Inner and External can't be used at the same time.", mLog.e);
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
                                toast(context, "Unknown Camera ID.", mLog.e);
                                reformat = true;
                            }
                        }
                    } else {
                        toast(context, "Cannot use the same Camera ID.", mLog.e);
                        reformat = true;
                    }
                    if (isInteger(code.split("\n")[0], true)) {
                        int min = Integer.parseInt(code.split("\n")[0]);
                        setTestTime(context, min);
                    } else {
                        toast(context, "Unknown Record Times.", mLog.e);
                        reformat = true;
                    }
                    if (isBoolean(prop)) {
                        if (!Boolean.parseBoolean(prop)) {
                            if (isNew != Boolean.parseBoolean(prop))
                                isPropChange = true;
                            isNew = Boolean.parseBoolean(prop);
                        }
                    } else {
                        toast(context, "Unknown setProperty.", mLog.e);
                        reformat = true;
                    }
                }
            }
            if (update) {
                if (getSdCard)
                    reformatConfigFile(context, new File(logName));
            }
            if (reformat) {
                if (getSdCard)
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
                toast(context, "The Camera ID must be a positive number.", mLog.e);
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
                    toast(context, "The Camera ID is unknown.", mLog.e);
                    return false;
                }
            }
            if (Integer.parseInt(b) <= -1) {
                toast(context, "The Camera ID must be a positive number.", mLog.e);
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
                    toast(context, "The Camera ID is unknown.", mLog.e);
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
        TextView editText_4 = view.findViewById(R.id.dialog_editText_4);
        int isFinish = 999;
        boolean isNew = false;

        if (!reset) {
            if (isInteger(editText_3.getText().toString(), false)) {
                isFinish = Integer.parseInt(editText_3.getText().toString());
            } else {
                isFinish = 999;
            }
            if (isBoolean(editText_4.getText().toString())) {
                isNew = Boolean.parseBoolean(editText_4.getText().toString());
            } else {
                isNew = false;
            }
        }

        //toast(context, "Ready to write.", mLog.w);
        writeConfigFile(context, file, (
                !reset ? new Configini(context, editText_1.getText().toString(),
                        editText_2.getText().toString(),
                        isFinish, isNew) : new Configini(context)).config());
        //toast(context, "Write file is completed.", mLog.i);
    }

    public static void reformatConfigFile(Context context, File file) {
        //toast(context, "Config file error.", mLog.e);
        writeConfigFile(context, file, new Configini(context).config());
        toast(context, "Reformat the Config file.", mLog.e);
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
            toast(context, "Read failed. "+ NO_SD_CARD + ". <============ Crash here", mLog.e);
            new Handler().post(() -> saveLog(context, false));
            isError = true;
            getSdCard = !getSDPath().equals("");
            errorMessage = "Read failed."+ NO_SD_CARD+"<============ Crash here";
            toast(context, "Read failed.", mLog.e);
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
                toast(context, "Write failed. "+ NO_SD_CARD + ". <============ Crash here", mLog.e);
                new Handler().post(() -> saveLog(context, false));
                isError = true;
                getSdCard = !getSDPath().equals("");
                errorMessage = "Write failed. "+ NO_SD_CARD+"<============ Crash here";
            }
        } else {
            toast(context, NO_SD_CARD, mLog.e);
        }
    }

    public static String getCalendarTime() {
        String d, h, i, s;
        Calendar calendar = Calendar.getInstance();
        d = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
        h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        i = String.format("%02d", calendar.get(Calendar.MINUTE));
        s = String.format("%02d", calendar.get(Calendar.SECOND));

        return d + h + i + s + "";
    }

    public static String getCalendarTime(boolean isCameraOne) {
        String d, h, i, s;
        Calendar calendar = Calendar.getInstance();
        d = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
        h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        i = String.format("%02d", calendar.get(Calendar.MINUTE));
        s = String.format("%02d", calendar.get(Calendar.SECOND));

        return "v" + d + h + i + s + (isCameraOne ? "f" : "s");
    }

    public static String getPath() {
        String path = "/storage/emulated/0/DCIM/";
        return path;
    }

    public static String getSDPath() {
        String path = "";
        try {
            long start = System.currentTimeMillis();
            long end = start + 10000;
            String cmd = "ls /storage";
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = buf.readLine()) != null) {
                if (!line.equals("self") && !line.equals("emulated") && !line.equals("enterprise") && !line.contains("sdcard")) {
                    path = "/storage/" + line + "/";
                    break;
                }
                if(System.currentTimeMillis() > end) {
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static int getVideo(Context context, String path) {
        int duration = 0;
        try {
            MediaPlayer mp = MediaPlayer.create(context, Uri.parse(path));
            duration = mp.getDuration();
            mp.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return duration;
    }

    public static int getFrameRate(String path) {
        int frameRate = 0;
        MediaPlayer mMediaPlayer = new MediaPlayer();
        try {
            MediaExtractor extractor = new MediaExtractor();
            FileInputStream fis = new FileInputStream(new File(path));
            extractor.setDataSource(fis.getFD());
            int numTracks = extractor.getTrackCount();
            for (int i = 0; i < numTracks; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                if (format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                    frameRate = format.getInteger(MediaFormat.KEY_FRAME_RATE);
                }
            }
            extractor.release();
            if (fis != null) fis.close();
        } catch (Exception e) {
            e.printStackTrace();
            mMediaPlayer.release();
        }
        return frameRate;
    }

    public static String getFileExtension(String fullName) {
        String fileName = new File(fullName).getName();
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1);
    }

    public static long dirSize(File dir) {
        if (dir.exists()) {
            long result = 0;
            File[] fileList = dir.listFiles();
            for (int i = 0; i < fileList.length; i++) {
                // Recursive call if it's a directory
                File file = fileList[i];
                if (file.isDirectory()) {
                    result += dirSize(file);
                } else {
                    // Sum the file size in bytes
                    result += fileList[i].length();
                }
            }
            return result; // return the file size
        }
        return 0;
    }

}
