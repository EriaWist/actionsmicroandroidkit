package com.actionsmicro.utils;

import com.actionsmicro.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    // DUMP_LOG is used for some case that logcat will lose logs : ex: ezcast wire
    private static final boolean DUMP_LOG = false;

    public static void dumpLog(String message) {
        FileOutputStream fop = null;
        File file;
        Date date = new Date(System.currentTimeMillis());
        String content = sdf.format(date) + ":" + message + "\n";

        try {
            file = new File("/sdcard/ezcastLog");
            Log.i("Write File:", file + "");
            if (!file.exists()) {
                file.createNewFile();
            }
            fop = new FileOutputStream(file, true);

            byte[] contentInBytes = content.getBytes();

            fop.write(contentInBytes);
            fop.flush();
            fop.close();
        } catch (IOException e) {
            Log.i("Write E:", e + "");
            e.printStackTrace();
        } finally {
            try {
                if (fop != null) {
                    fop.close();
                }
            } catch (IOException e) {
                Log.i("Write IOException", e + "");
                e.printStackTrace();
            }
        }
    }

    public static void d(String tag, String string) {
        if (BuildConfig.DEBUG) {
            android.util.Log.d(tag, string);
            if (DUMP_LOG) {
                dumpLog(tag + " :" + string);
            }
        }
    }

    public static void i(String tag, String string) {
        if (BuildConfig.DEBUG) {
            android.util.Log.i(tag, string);
        }
    }

    public static void e(String tag, String string) {
        android.util.Log.e(tag, string);
    }

    public static void e(String tag, String string, Throwable e) {
        android.util.Log.e(tag, string, e);
    }

    public static void v(String tag, String string) {
        if (BuildConfig.DEBUG) {
            android.util.Log.v(tag, string);
        }
    }

    public static void w(String tag, String string) {
        android.util.Log.w(tag, string);
    }

}