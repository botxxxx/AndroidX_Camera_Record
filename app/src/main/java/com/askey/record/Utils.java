package com.askey.record;

import android.app.Activity;
import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.widget.Toast;

import com.askey.widget.LogMsg;
import com.askey.widget.mLog;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class Utils {

    public static final String FRAMESKIP = "persist.our.camera.frameskip";
    public static final String COMMAND_VIDEO_RECORD_TEST = "com.askey.record.t";
    public static final String COMMAND_VIDEO_RECORD_START = "com.askey.record.s";
    public static final String COMMAND_VIDEO_RECORD_FINISH = "com.askey.record.f";
    public static final String COMMAND_VIDEO_RECORD_TESTa = "com.askey.record.T";
    public static final String COMMAND_VIDEO_RECORD_STARTa = "com.askey.record.S";
    public static final String COMMAND_VIDEO_RECORD_FINISHa = "com.askey.record.F";
    public static final SparseIntArray ORIENTATIONS = new SparseIntArray();
    public static final String fileName = "VideoRecordConfig.ini";
    public static final String logName = "VideoRecordLog.ini";
    public static int isRun = 0, successful = 0, failed = 0;
    public static String TAG = "VideoRecord";
    public static String firstCamera = "0";
    public static String secondCamera = "1";
    public static String lastfirstCamera = "0";
    public static String lastsecondCamera = "1";
    public static ArrayList<String> firstFilePath, secondFilePath;
    public static ArrayList<LogMsg> videoLogList;
    public static int isFinish = 1, delayTime = 600000, isFrame = 0, isQuality = 0;
    public static boolean isReady = false, isRecord = false, isLoop = false, isError = false, isNew = false;

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    public static void toast(Context context, String t, mLog type) {
        videoLogList.add(new LogMsg(t, type));
        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, t + "", Toast.LENGTH_SHORT).show());
    }

    public static void toast(Context context, String t) {
        videoLogList.add(new LogMsg(t, mLog.i));
        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, t + "", Toast.LENGTH_SHORT).show());
    }

    public static int getSuccessful() {
        return successful;
    }

    public static int getFailed() {
        return failed;
    }

    public static int getIsRun() {
        return isRun;
    }

    public static boolean isInteger(String s, boolean zero) {
        try {
            if (Integer.parseInt(s) <= (zero ? 0 : -1)) {
                return false;
            }
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static boolean isBoolean(String s) {
        try {
            if (Boolean.parseBoolean(s)) {
                return true;
            }
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
        return true;
    }

    public static void setTestTime(Context context, int min) {
        if (min > 0) {
            isFinish = min;
            toast(context, "setRecord time: " + min + "0 min.");
        } else {
            toast(context, "The test time must be a positive number.", mLog.e);
        }
    }

    public static boolean checkConfigFile(Context context, boolean first) {
        videoLogList.add(new LogMsg("#checkConfigFile", mLog.v));
        if (!"".equals(getSDCardPath())) {
            File file = new File(getSDCardPath(), fileName);
            if (!file.exists()) {
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                toast(context, "Create the config file.", mLog.w);
                writeConfigFile(context, file, new Configini().config());
            } else {
                if (!isRecord) {
                    toast(context, "Find the config file.", mLog.d);
                    videoLogList.add(new LogMsg("#---------------------------------------------------------------------", mLog.v));
                }
                checkConfigFile(context, new File(getSDCardPath(), fileName), first);
            }
            return true;
        } else {
            toast(context, "Please check the SD card.", mLog.e);
        }
        return false;
    }

    public static void checkConfigFile(Context context, File file, boolean firstOne) {
        String input = readConfigFile(context, file);
        if (input.length() > 0) {
            List<String> read = Arrays.asList(input.split("\r\n"));
            boolean target = false;
            int t;
            String code = "total_test_minute = ";
            String first = "firstCameraID = ", second = "secondCameraID = ";
//            String s = read.get(2);
            for (String s : read)
                if (s.indexOf(first) != -1) {
                    target = true;
                    t = s.indexOf(first) + first.length();
                    first = s.substring(t);
                    toast(context, "firstCameraID: " + first);
                    break;
                }
//            s = read.get(3);
            for (String s : read)
                if (s.indexOf(second) != -1) {
                    target = true;
                    t = s.indexOf(second) + second.length();
                    second = s.substring(t);
                    toast(context, "secondCameraID: " + second);
                    break;
                }
//            s = read.get(6);
            for (String s : read)
                if (s.indexOf(code) != -1) {
                    target = true;
                    t = s.indexOf(code) + code.length();
                    code = s.substring(t);
                    break;
                }
            if (target) {
                boolean reformat = false;
                if (!first.equals(second)) {
                    if (isCameraID(context, first.split("\n")[0], second.split("\n")[0])) {
                        lastfirstCamera = firstOne ? first : firstCamera;
                        lastsecondCamera = firstOne ? second : secondCamera;
                        firstCamera = first;
                        secondCamera = second;
                    } else reformat = true;
                } else {
                    toast(context, "Cannot use the same Camera ID.", mLog.e);
                    reformat = true;
                }
                if (isInteger(code.split("\n")[0], true)) {
                    int min = Integer.parseInt(code.split("\n")[0]);
                    setTestTime(context, min);
                } else reformat = true;
                if (reformat) {
                    reformatConfigFile(context, file, new Configini().config());
                }
            } else reformatConfigFile(context, file, new Configini().config());
        } else reformatConfigFile(context, file, new Configini().config());
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
        } catch (NumberFormatException e) {
            return false;
        } catch (NullPointerException e) {
            return false;
        }
    }

    public static void setConfigFile(Context context, File file, String camera1, String camera2, String fin) {
        firstCamera = camera1;
        secondCamera = camera2;
        if (isInteger(fin, false))
            isFinish = Integer.parseInt(fin);
        else {
            isFinish = 1;
        }

        toast(context, "Ready to write.", mLog.e);
        writeConfigFile(context, file, new Configini(firstCamera, secondCamera, isFinish).config());
        toast(context, "Write file is completed.", mLog.e);
    }

    public static void reformatConfigFile(Context context, File file, String[] message) {
        firstCamera = "0";
        secondCamera = "1";
        isFinish = 1;
        toast(context, "Config file error.", mLog.e);
        writeConfigFile(context, file, message);
        toast(context, "Reformat the file.", mLog.e);
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
        } catch (IOException e) {
            Log.e(TAG, " read failed: \" + e.toString()");
            toast(context, "read failed.", mLog.e);
        }
        return tmp;
    }

    public static void writeConfigFile(Context context, File file, String[] str) {
        String tmp = "";
        for (String s : str)
            tmp += s;
        try {
            FileOutputStream output = new FileOutputStream(file);
            output.write(tmp.getBytes());
            output.close();
        } catch (IOException e) {
            Log.e(TAG, " write failed: \" + e.toString()");
            toast(context, "write failed.", mLog.e);
        }
    }

    public static String getCalendarTime(boolean isCameraOne) {
        String d, h, i, s;
        Calendar calendar = Calendar.getInstance();
        d = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
        h = String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY));
        i = String.format("%02d", calendar.get(Calendar.MINUTE));
        s = String.format("%02d", calendar.get(Calendar.SECOND));

        return "v" + d + h + i + s + (isCameraOne ? "b" : "f");
    }

    public static String getSDCardPath() {
        String path = "";
        try {
            String cmd = "ls /storage";
            Runtime run = Runtime.getRuntime();
            Process pr = run.exec(cmd);
            BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
            String line;
            while ((line = buf.readLine()) != null) {
                if (!line.equals("self") && !line.equals("emulated") && !line.equals("enterprise") && !line.contains("sdcard")) {
                    path = "/storage/" + line + "/";
                    Log.d("Lewis", "sdpath = " + path);
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

    public static int getFrameRate(String path, MediaPlayer mMediaPlayer) {
        int frameRate = 0;

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
        } catch (IOException e) {
            mMediaPlayer.release();
            e.printStackTrace();
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
