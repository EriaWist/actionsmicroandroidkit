package com.actionsmicro.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.provider.Settings;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;

public class TetheringUtil {
    // ref: https://en.wikipedia.org/wiki/MIUI 10.2.3
    public static final String MIUI_10_RELEASE_DATE = "2018.06.19";
    private final static String[] NONTETHERING_DEVICE_LIST = new String[]{"Nexus 7"};

    private static String TAG = TetheringUtil.class.getSimpleName();

    public static final int TETHERING_SETTING_ACTIVITY_CODE = 1001;
    public static final int HOTSPOT_SETTING_ACTIVITY_CODE = 1002;

    private Activity activity;
    private ConnectivityManager mConnectivityManager;

    public TetheringUtil(Activity activity) {
        this.activity = activity;
        mConnectivityManager = (ConnectivityManager) this.activity.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public void launchTetherSettings() {
        launchTetherSettings(TETHERING_SETTING_ACTIVITY_CODE);
    }

    public void launchTetherSettings(int requestCode) {
        PackageManager pm = activity.getPackageManager();
        try {
            Intent tetherSettings = new Intent();

            if (Build.MODEL.toLowerCase().contains("nexus")) {
                tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
            } else {
                if (Build.MANUFACTURER.equalsIgnoreCase("lge") || Build.MANUFACTURER.equalsIgnoreCase("lg")) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                        tetherSettings.setClassName("com.android.settings", "com.android.settings.Settings$UsbSettingsActivity");
                    } else {
                        tetherSettings.setClassName("com.android.settings", "com.android.settings.Settings$TetherNetworkSettingsActivity");
                    }
                } else if (Build.MANUFACTURER.equalsIgnoreCase("LENOVO") && Build.HARDWARE.equalsIgnoreCase("mt6589")) {
                    // for lenovo a830
                    tetherSettings.setClassName("com.android.settings", "com.android.settings.Settings$UsbSettingsActivity");
                } else if (Build.MANUFACTURER.equalsIgnoreCase("YuLong")) {
                    tetherSettings.setClassName("com.android.settings", "com.android.settings.deviceinfo.UsbSettings");
                } else if (Build.MANUFACTURER.equalsIgnoreCase("OPPO")) {
                    tetherSettings = new Intent("android.settings.OPPO_TETHER_SETTINGS");
                    if (tetherSettings.resolveActivity(pm) == null) {
                        tetherSettings = new Intent();
                        tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                    }
                } else if (Build.MANUFACTURER.equalsIgnoreCase("xiaomi")) {
                    tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                    String miUIVersion = getMiUIVersion();
                    if (!miUIVersion.isEmpty() && miUIVersion.compareTo(MIUI_10_RELEASE_DATE) >= 0) {
                        tetherSettings = new Intent();
                        tetherSettings.setClassName("com.android.settings", "com.android.settings.Settings$WirelessSettingsActivity");
                    }
                } else if (Build.MANUFACTURER.equalsIgnoreCase("HUAWEI")) {
                    tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        Intent tetherIntent = new Intent();
                        tetherIntent.setClassName("com.android.settings", "com.android.settings.Settings$TetherSettingsActivity");
                        if (tetherIntent.resolveActivity(pm) != null) {
                            tetherSettings = tetherIntent;
                        }
                    }
                } else {
                    tetherSettings.setClassName("com.android.settings", "com.android.settings.TetherSettings");
                }
            }

            activity.startActivityForResult(tetherSettings, requestCode);
            return;
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
        try {
            activity.startActivityForResult(new Intent(Settings.ACTION_SETTINGS), requestCode);
            return;
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
        }
    }

    public boolean setUsbTetheringLaunchSettingIfFailed(boolean enable) {
        int code = -1;
        Method[] wmMethods = mConnectivityManager.getClass().getMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("setUsbTethering")) {
                try {
                    Log.d(TAG, "setUsbTethering enable = " + enable);
                    code = (Integer) method.invoke(mConnectivityManager, enable);
                } catch (IllegalAccessException e) {
                    Log.d(TAG, "IllegalAccessException");
                    e.printStackTrace();
                    return false;
                } catch (InvocationTargetException e) {
                    Log.d(TAG, "InvocationTargetException");
                    launchTetherSettings();
                    e.printStackTrace();
                    return false;
                }
            }
        }

        if (code == 0) {
            Log.d(TAG, "setUsbTethering successfully!");
            return true;
        } else {
            Log.d(TAG, "setUsbTethering failed!");
            return false;
        }
    }

    public boolean setUsbTethering(boolean enable) {
        int code = -1;
        Method[] wmMethods = mConnectivityManager.getClass().getMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("setUsbTethering")) {
                try {
                    Log.d(TAG, "setUsbTethering enable = " + enable);
                    code = (Integer) method.invoke(mConnectivityManager, enable);
                } catch (IllegalAccessException e) {
                    Log.d(TAG, "IllegalAccessException");
                    e.printStackTrace();
                    return false;
                } catch (InvocationTargetException e) {
                    Log.d(TAG, "InvocationTargetException");
                    //launchTetherSettings();
                    e.printStackTrace();
                    return false;
                }
            }
        }

        if (code == 0) {
            Log.d(TAG, "setUsbTethering successfully!");
            return true;
        } else {
            Log.d(TAG, "setUsbTethering failed!");
            return false;
        }
    }

    public boolean isTetheringSupported() {
        boolean supported = false;
        Method[] wmMethods = mConnectivityManager.getClass().getMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("isTetheringSupported")) {
                try {
                    supported = (Boolean) method.invoke(mConnectivityManager);
                    Log.d(TAG, "is Tether  supported: " + (supported ? "yes" : "no"));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                        supported = true;
                    }
                } catch (Exception e) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        supported = isTetheringModel();
                    }
                    e.printStackTrace();
                }
            }
        }
        return supported;
    }

    @TargetApi(24)
    private boolean isTetheringModel() {
        String model = Build.MODEL;
        for (String s : NONTETHERING_DEVICE_LIST) {
            if (model.equals(s)) {
                return false;
            }
        }
        return true;
    }

    public static String[] getTetheredIfaces(Context ctx) {
        String[] tetheredIfaces = null;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        Method[] wmMethods = cm.getClass().getMethods();
        for (Method method : wmMethods) {
            if (method.getName().equals("getTetheredIfaces")) {
                try {
                    tetheredIfaces = (String[]) method.invoke(cm);
                    for (String t : tetheredIfaces) {
                        Log.d(TAG, "Tethered iface = " + t);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return tetheredIfaces;
    }

    public static boolean isUsbTethered(Context ctx) {
        boolean ret = false;
        String[] ifaces = getTetheredIfaces(ctx);
        if (null != ifaces) {
            for (String iface : ifaces) {
                if (iface.toLowerCase().startsWith("rndis") || iface.toLowerCase().startsWith("usb")) {
                    ret = true;
                    break;
                }
            }
        }
        return ret;
    }

    private String getMiUIVersion() {
        String miui = "";
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            String miuicode = (String) get.invoke(c, "ro.miui.version.code_time");
            if(miuicode != null && !miuicode.isEmpty()){
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd");
                miui = sdf.format(Long.parseLong(miuicode)*1000L);
            }

        } catch (InvocationTargetException ite) {
        } catch (IllegalAccessException iae) {
        } catch (NoSuchMethodException nsme) {
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        return miui;

    }
}