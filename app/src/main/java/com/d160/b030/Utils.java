package com.d160.b030;

import android.annotation.*;
import android.hardware.camera2.*;
import android.media.*;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.*;

import com.d160.b030.R;
import com.d160.view.*;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import static com.d160.b030.CameraFragment.*;

@SuppressLint("StaticFieldLeak")
public class Utils {
    //-------------------------------------------------------------------------------
    public static final String EXTRA_VIDEO_RUN = "RestartActivity.run";
    public static final String EXTRA_VIDEO_FAIL = "RestartActivity.fail";
    public static final String EXTRA_VIDEO_RESET = "RestartActivity.reset";
    public static final String EXTRA_VIDEO_RECORD = "RestartActivity.record";
    public static final String EXTRA_VIDEO_SUCCESS = "RestartActivity.success";
    public static final String NO_SD_CARD = "SD card is not available!";
    public static final String logName = "ThermalTestLog.ini";
    public static final String LOG_TITLE = "[CDR9030_Thermal_Test]";
    public static final double sdData = 3;
    public static int isRun = 0, Success = 0, Fail = 0;
    public static ArrayList<mLogMsg> videoLogList = null;
    public static boolean isInitReady = false, isCameraReady = false, isRecord = false,
            isError = false, isSave = false;
    public static String errorMessage = "";
    //-------------------------------------------------------------------------------
    public static List<String> allCamera = Arrays.asList(firstCamera, secondCamera, thirdCamera);
    public static AtomicReferenceArray<Boolean> isOpenCamera = new AtomicReferenceArray<>(new Boolean[]{Open_f_Camera, Open_s_Camera, Open_t_Camera});
    public static AtomicIntegerArray id_textView = new AtomicIntegerArray(new int[]{R.id.textureView0});
    public static AtomicReferenceArray<String> threadString = new AtomicReferenceArray<>(new String[]{"CameraPreview0", "CameraPreview1", "CameraPreview2"});
    public static AtomicReferenceArray<String> cameraFile = new AtomicReferenceArray<>(new String[]{"", "", ""});
    public static AtomicReferenceArray<Boolean> isCameraOpened = new AtomicReferenceArray<>(new Boolean[]{false, false, false});
    public static AtomicReferenceArray<ArrayList<String>> cameraFilePath = new AtomicReferenceArray<>(new ArrayList[3]);
    public static AtomicReferenceArray<String> codeDate = new AtomicReferenceArray<>(new String[3]);
    public static AtomicReferenceArray<TextureView> textView = new AtomicReferenceArray<>(new TextureView[3]);
    public static AtomicReferenceArray<CameraDevice> cameraDevice = new AtomicReferenceArray<>(new CameraDevice[3]);
    public static AtomicReferenceArray<CameraCaptureSession> previewSession = new AtomicReferenceArray<>(new CameraCaptureSession[3]);
    public static AtomicReferenceArray<CameraDevice.StateCallback> stateCallback = new AtomicReferenceArray<>(new CameraDevice.StateCallback[3]);
    public static AtomicReferenceArray<MediaRecorder> mediaRecorder = new AtomicReferenceArray<>(new MediaRecorder[3]);
    public static AtomicReferenceArray<HandlerThread> thread = new AtomicReferenceArray<>(new HandlerThread[3]);
    public static AtomicReferenceArray<Handler> backgroundHandler = new AtomicReferenceArray<>(new Handler[3]);
    public static AtomicReferenceArray<Handler> recordHandler = new AtomicReferenceArray<>(new Handler[3]);
    public static AtomicReferenceArray<Handler> stopRecordHandler = new AtomicReferenceArray<>(new Handler[3]);
    //-------------------------------------------------------------------------------

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
