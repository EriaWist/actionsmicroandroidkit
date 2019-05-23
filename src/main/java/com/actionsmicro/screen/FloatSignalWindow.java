package com.actionsmicro.screen;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.AnimationDrawable;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.actionsmicro.R;

public class FloatSignalWindow extends Service {

    private IBinder binder = new FloatSignalWindowBinder();
    private WindowManager windowManager;
    private View view;

    private AnimationDrawable signalDrawable;

    public class FloatSignalWindowBinder extends Binder {
        public FloatSignalWindow getService() {
            return FloatSignalWindow.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        view = LayoutInflater.from(this).inflate(R.layout.floating_signal_dialog, null);
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params = new WindowManager.LayoutParams(
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.WRAP_CONTENT,
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                    PixelFormat.TRANSLUCENT);
        }

        signalDrawable = (AnimationDrawable) view.findViewById(R.id.float_signal_animation).getBackground();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(view, params);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        signalDrawable.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        signalDrawable.stop();
        if (view != null) {
            windowManager.removeView(view);
        }
    }
}
