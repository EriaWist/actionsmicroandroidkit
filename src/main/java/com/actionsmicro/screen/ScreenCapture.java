package com.actionsmicro.screen;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by Actions on 2018/3/9.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenCapture implements DisplayManager.DisplayListener {

    public static final String RESULT_CODE_KEY = "actions.service.result.code.key";
    public static final String RESULT_INTENT_KEY = "actions.service.result.intent.key";
    public static final String VIRTUAL_DISPLAY_NAME = "SCREENCAST_VIRTUAL";

    private Surface mSurface;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private MediaProjectionManager mMediaProjectionManager;
    private MediaCodec mMediaCodec;
    private MediaFormat mMediaFormat;
    private DisplayManager mDisplayManager;

    private int width = 1280;
    private int height = 720;
    private int screenDensity;

    private Intent resultIntent;
    private int resultCode;
    private boolean restart = false;

    private DisplayManager.DisplayListener displayListener;
    public void setDisplayListener(DisplayManager.DisplayListener displayListener) {
        this.displayListener = displayListener;
    }

    public void setMediaCallback(MediaCodec.Callback mediaCallback) {
        this.mediaCallback = mediaCallback;
    }

    public interface DataCallback {
        void dataBufferAvailable(byte[] outData, int width, int height);
    }
    public interface MediaFormatI {
        MediaFormat getMediaFormat();
    }
    private MediaFormatI mediaFormatI;
    private MediaCodec.Callback mediaCallback;
    private DataCallback dataCallback;
    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }
    private VirtualDisplay.Callback virtualDisplayCallback;
    public void setVirtualCallback(VirtualDisplay.Callback virtualDisplayCallback) {
        this.virtualDisplayCallback = virtualDisplayCallback;
    }

    public ScreenCapture(Context context, Intent intent) {
        this.resultCode = intent.getIntExtra(RESULT_CODE_KEY, -10001);
        this.resultIntent = intent.getParcelableExtra(RESULT_INTENT_KEY);

        init(context);

    }

    public ScreenCapture(Context context, Intent intent, MediaFormatI mediaFormatI) {
        this.resultCode = intent.getIntExtra(RESULT_CODE_KEY, -10001);
        this.resultIntent = intent.getParcelableExtra(RESULT_INTENT_KEY);
        this.mediaFormatI = mediaFormatI;

        init(context);

    }

    public ScreenCapture(Context context, Intent intent, int width, int height) {
        this.resultCode = intent.getIntExtra(RESULT_CODE_KEY, -10001);
        this.resultIntent = intent.getParcelableExtra(RESULT_INTENT_KEY);
        this.width = width;
        this.height = height;

        init(context);

    }

    public ScreenCapture(Context context, Intent intent, int width, int height, MediaFormatI mediaFormatI) {
        this.resultCode = intent.getIntExtra(RESULT_CODE_KEY, -10001);
        this.resultIntent = intent.getParcelableExtra(RESULT_INTENT_KEY);
        this.mediaFormatI = mediaFormatI;
        this.width = width;
        this.height = height;

        init(context);

    }


    private void init(final Context context) {
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

        try {
            mMediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mMediaCodec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec mediaCodec, int i) {
                if (mediaCallback != null) {
                    mediaCallback.onInputBufferAvailable(mediaCodec, i);
                }
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec mediaCodec, int index, @NonNull MediaCodec.BufferInfo bufferInfo) {
                if (mediaCallback != null) {
                    mediaCallback.onOutputBufferAvailable(mediaCodec, index,bufferInfo);
                }
                ByteBuffer encodedData = mediaCodec.getOutputBuffer(index);
                encodedData.position(bufferInfo.offset);
                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                byte[] outData = new byte[bufferInfo.size];
                encodedData.get(outData);
                //int nalType = ((int) outData[4]) & 0x1f;
                if (dataCallback != null) {
                    dataCallback.dataBufferAvailable(outData, width, height);
                }
                mediaCodec.releaseOutputBuffer(index, System.nanoTime());
            }

            @Override
            public void onError(@NonNull MediaCodec mediaCodec, @NonNull MediaCodec.CodecException e) {
                if (mediaCallback != null) {
                    mediaCallback.onError(mediaCodec, e);
                }
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec mediaCodec, @NonNull MediaFormat mediaFormat) {
                if (mediaCallback != null) {
                    mediaCallback.onOutputFormatChanged(mediaCodec, mediaFormat);
                }
            }
        });
        mMediaFormat = getMediaFormat();
        try {
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IllegalStateException e) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
            mMediaFormat = getMediaFormat();
            mMediaCodec.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        }
        mSurface = mMediaCodec.createInputSurface();
        try {
            mMediaCodec.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, resultIntent);

        mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, width, height, screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
                new VirtualDisplay.Callback() {
                    @Override
                    public void onPaused() {
                        if (virtualDisplayCallback != null && !restart) {
                            virtualDisplayCallback.onPaused();
                        }
                        super.onPaused();
                    }

                    @Override
                    public void onResumed() {
                        if (virtualDisplayCallback != null && !restart) {
                            virtualDisplayCallback.onResumed();
                        }
                        super.onResumed();
                    }

                    @Override
                    public void onStopped() {
                        super.onStopped();
                        releaseResource();
                        if (restart) {
                            restart = false;
                            init(context);
                        } else {
                            if (virtualDisplayCallback != null) {
                                virtualDisplayCallback.onStopped();
                            }
                        }
                    }
                }, null);
        mDisplayManager.registerDisplayListener(this, null);
    }

    public void restart(int width, int height) {
        restart = true;
        this.width = width;
        this.height = height;
        stopScreenCapture();
    }

    public synchronized void stopScreenCapture() {
        mDisplayManager.unregisterDisplayListener(this);
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
        tearDownMediaProjection();
    }

    private MediaFormat getMediaFormat() {
        if (mediaFormatI != null) {
            return mediaFormatI.getMediaFormat();
        } else {
            MediaFormat mediaFormat = getMediaFormat(960000 * 4, 24, 1);
            return mediaFormat;
        }
    }

    private MediaFormat getMediaFormat(int bitRate, int framerate, int iFrameInterval) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setFloat(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        return mediaFormat;
    }

    private void tearDownMediaProjection() {
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }
    private void releaseResource() {
        mMediaCodec.stop();
        mMediaCodec.release();
        mMediaCodec = null;
    }

    @Override
    public void onDisplayAdded(int i) {
        if (displayListener != null) {
            displayListener.onDisplayAdded(i);
        }
    }

    @Override
    public void onDisplayRemoved(int i) {
        if (displayListener != null) {
            displayListener.onDisplayRemoved(i);
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {
        android.view.Display display = mDisplayManager.getDisplay(displayId);
        if (displayListener != null) {
            displayListener.onDisplayChanged(displayId);
        }
        if (VIRTUAL_DISPLAY_NAME.equals(display.getName()) && display.getState() == android.view.Display.STATE_OFF) {
            stopScreenCapture();
            releaseResource();
        }
    }
    public void startFloatingSignal(Context context) {
        Intent signalFloatingWindow = new Intent(context, FloatSignalWindow.class);
        context.startService(signalFloatingWindow);
    }
    public void stopFloatingSignal(Context context) {
        Intent signalFloatingWindow = new Intent(context, FloatSignalWindow.class);
        context.stopService(signalFloatingWindow);
    }
}
