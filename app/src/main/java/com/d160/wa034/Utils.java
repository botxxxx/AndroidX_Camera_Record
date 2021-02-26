package com.d160.wa034;

import android.Manifest;
import android.annotation.*;
import android.media.*;
import android.os.Handler;
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
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final double sdData = 3;
    public static int isRun = 0, Success = 0, Fail = 0;
    public static ArrayList<mLogMsg> videoLogList = null;
    public static boolean isInitReady = false, isCameraReady = true, isRecord = false,
            isError = false, isSave = false;
    public static String errorMessage = "";
    //-------------------------------------------------------------------------------
    public static List<String> allCamera = Arrays.asList(firstCamera, secondCamera, thirdCamera);
    public static AtomicReferenceArray<Boolean> isOpenCamera = new AtomicReferenceArray<>(new Boolean[]{Open_f_Camera, Open_s_Camera, Open_t_Camera});
    public static AtomicIntegerArray id_surfaceView = new AtomicIntegerArray(new int[]{R.id.surfaceView0});
    public static AtomicReferenceArray<String> permission = new AtomicReferenceArray<>(new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE});
    public static AtomicReferenceArray<String> cameraFile = new AtomicReferenceArray<>(new String[]{"", "", ""});
    public static AtomicReferenceArray<Boolean> isCameraOpened = new AtomicReferenceArray<>(new Boolean[]{false, false, false});
    public static AtomicReferenceArray<ArrayList<String>> cameraFilePath = new AtomicReferenceArray<ArrayList<String>>(new ArrayList[3]);
    public static AtomicReferenceArray<String> codeDate = new AtomicReferenceArray<>(new String[3]);
    public static AtomicReferenceArray<SurfaceView> surfaceView = new AtomicReferenceArray<>(new SurfaceView[3]);
    public static AtomicReferenceArray<MediaRecorder> mediaRecorder = new AtomicReferenceArray<>(new MediaRecorder[3]);
    public static AtomicReferenceArray<Handler> recordHandler = new AtomicReferenceArray<>(new Handler[3]);
    public static AtomicReferenceArray<Handler> stopRecordHandler = new AtomicReferenceArray<>(new Handler[3]);
    //-------------------------------------------------------------------------------

    public static String getLogPath() {
        return getStorageDirectory().getPath()+ "/emulated/0/";
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
        if (isOpenCamera.get(0))
            return cameraId.equals(allCamera.get(0));
        else if (isOpenCamera.get(1))
            return cameraId.equals(allCamera.get(1));
        else
            return cameraId.equals(allCamera.get(2));
    }

    public static boolean isLastCamera(String cameraId) {
        if (isOpenCamera.get(2))
            return cameraId.equals(allCamera.get(2));
        else if (isOpenCamera.get(1))
            return cameraId.equals(allCamera.get(1));
        else
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
