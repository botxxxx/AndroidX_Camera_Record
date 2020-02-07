package com.askey.record;

import android.content.Context;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

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
    public static final String[] config = {
            "[VIDEO_RECORD_TESTING]\r\n",
            "#CameraID(0:BACK, 1:FRONT, 2:EXTERNAL)\r\n",
            "firstCameraID = 0\r\n",
            "secondCameraID = 1\r\n", "\r\n",
            "#Camera Device total minute: one day has minutes(*10) = 144\r\n",
            "total_test_minute = 1\r\n", "\r\n",
            "#Play video path(can't change video path)\r\n",
            "video1_path = /sdcard/(ddhhmmss)f.mp4\r\n",
            "video2_path = /sdcard/(ddhhmmss)b.mp4\r\n", "\r\n",
            "#Start application\r\n",
            "adb shell am start -n com.askey.record/.VideoRecordActivity\r\n", "\r\n",
            "#Start test record(no audio with 5s)\r\n",
            "adb shell am broadcast -a com.askey.record.t\r\n", "\r\n",
            "#Start/Stop record(default is 10 min)\r\n",
            "adb shell am broadcast -a com.askey.record.s\r\n", "\r\n",
            "#Finish applcation\r\n",
            "adb shell am broadcast -a com.askey.record.f\r\n", "\r\n",
            "#At least 3.5Gb memory needs to be available to record, \r\n",
            "#Please check the SD card.\r\n",
//            "#Frame rate switch will delay 3s to restart the camera device. \r\n", "\r\n"
    };
    public static int isRun = 0, successful = 0, failed = 0;
    public static String TAG = "VideoRecord";

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
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

    public static String readConfigFile(File file) {
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
//            Log.e(TAG, " read failed: \" + e.toString()");
//            toast("read failed.", mLog.e);
        }
        return tmp;
    }

    public static void writeConfigFile(File file, String[] str) {
        String tmp = "";
        for (String s : str)
            tmp += s;
        try {
            FileOutputStream output = new FileOutputStream(file);
            output.write(tmp.getBytes());
            output.close();
        } catch (IOException e) {
//            Log.e(TAG, " write failed: \" + e.toString()");
//            toast("write failed.", mLog.e);
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
