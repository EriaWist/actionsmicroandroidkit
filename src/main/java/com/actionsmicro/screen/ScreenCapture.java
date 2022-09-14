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
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

import com.actionsmicro.audio.AudioCapture;
import com.actionsmicro.utils.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

/**
 * Created by Actions on 2018/3/9.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class ScreenCapture implements DisplayManager.DisplayListener {

    public static final String RESULT_CODE_KEY = "actions.service.result.code.key";
    public static final String RESULT_INTENT_KEY = "actions.service.result.intent.key";
    public static final String VIRTUAL_DISPLAY_NAME = "SCREENCAST_VIRTUAL";
    private static final String TAG = "ScreenCapture";
    private boolean needCaptureAudio = false;

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
    private MediaProjection.Callback mProjectionCallback;
    private AudioCapture mAudioCapture;

    public void setDisplayListener(DisplayManager.DisplayListener displayListener) {
        this.displayListener = displayListener;
    }

    public void setMediaCallback(MediaCodec.Callback mediaCallback) {
        this.mediaCallback = mediaCallback;
    }

    public void setAudioDataCallback(AudioCapture.AudioDataCallback audioDataCallback) {
        this.audioDataCallback = audioDataCallback;
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
    private AudioCapture.AudioDataCallback audioDataCallback;
    private AudioCapture.RecorderStatusCallback micStatusListener;

    public void setDataCallback(DataCallback dataCallback) {
        this.dataCallback = dataCallback;
    }

    public void setMicStatusListener(AudioCapture.RecorderStatusCallback statusListener){
        micStatusListener = statusListener;
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

    public ScreenCapture(Context context, Intent intent, int width, int height, MediaFormatI mediaFormatI, boolean needCaptureAudio) {
        this.resultCode = intent.getIntExtra(RESULT_CODE_KEY, -10001);
        this.resultIntent = intent.getParcelableExtra(RESULT_INTENT_KEY);
        this.mediaFormatI = mediaFormatI;
        this.width = width;
        this.height = height;
        this.needCaptureAudio = needCaptureAudio;
        init(context);
    }

    public void initMediaCodec() {
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
                    mediaCallback.onOutputBufferAvailable(mediaCodec, index, bufferInfo);
                }
                ByteBuffer encodedData = mediaCodec.getOutputBuffer(index);
                encodedData.position(bufferInfo.offset);
                encodedData.limit(bufferInfo.offset + bufferInfo.size);
                byte[] outData = new byte[bufferInfo.size];
                encodedData.get(outData);
                if (outData.length >= 5) {
                    int nalType = ((int) outData[4]) & 0x1f;
                    if (nalType == 7) {
                        restart = false;
                    }
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

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            if (mProjectionCallback != null && mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mProjectionCallback);
            }
        }
    }

    private void init(final Context context) {
        Log.d(TAG, "init");
        mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
        mMediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        screenDensity = metrics.densityDpi;

        initMediaCodec();

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            if (mProjectionCallback != null && mMediaProjection != null) {
                mMediaProjection.unregisterCallback(mProjectionCallback);
            }
        }

        mMediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, resultIntent);

        if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, width, height, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
                    null, null);
            mProjectionCallback = new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    Log.d(TAG, "onStop");
                    super.onStop();
                    releaseResource();
                    if (virtualDisplayCallback != null) {
                        virtualDisplayCallback.onStopped();
                    }
                }
            };
            mMediaProjection.registerCallback(mProjectionCallback, null);
        } else {
            mVirtualDisplay = mMediaProjection.createVirtualDisplay(VIRTUAL_DISPLAY_NAME, width, height, screenDensity,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC, mSurface,
                    new VirtualDisplay.Callback() {
                        @Override
                        public void onPaused() {
                            Log.d(TAG, "onPaused");
                            if (virtualDisplayCallback != null) {
                                virtualDisplayCallback.onPaused();
                            }
                            super.onPaused();
                        }

                        @Override
                        public void onResumed() {
                            Log.d(TAG, "onResumed");
                            if (virtualDisplayCallback != null) {
                                virtualDisplayCallback.onResumed();
                            }
                            super.onResumed();
                        }

                        @Override
                        public void onStopped() {
                            Log.d(TAG, "onStopped");
                            super.onStopped();
                            releaseResource();

                            if (virtualDisplayCallback != null) {
                                virtualDisplayCallback.onStopped();
                            }
                        }
                    }, null);
        }

        // check if needed
        mDisplayManager.registerDisplayListener(this, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && needCaptureAudio) {
            mAudioCapture = new AudioCapture(mMediaProjection, new AudioCapture.AudioDataCallback() {
                @Override
                public void onAudioDataAvailable(ByteBuffer dataBuffer, int size) {
                    if (audioDataCallback != null) {
                        audioDataCallback.onAudioDataAvailable(dataBuffer, size);
                    }
                }
            }, new AudioCapture.RecorderStatusCallback() {
                @Override
                public void onStatusChange(boolean status) {
                    if(micStatusListener != null){
                        micStatusListener.onStatusChange(status);
                    }
                }
            });
            mAudioCapture.startRecording();
        }
    }

    public void restart(int width, int height) {
        Log.d(TAG, "restart " + width + " " + height);
        if (width == this.width && height == this.height) {
            return;
        }
        this.width = width;
        this.height = height;
        restart = true;

        if (mVirtualDisplay != null) {
            mMediaProjection.unregisterCallback(mProjectionCallback);
            mDisplayManager.unregisterDisplayListener(this);
            mVirtualDisplay.setSurface(null);
            releaseResource();
            initMediaCodec();
            mVirtualDisplay.resize(width, height, screenDensity);
            mVirtualDisplay.setSurface(mSurface);
            mDisplayManager.registerDisplayListener(this, null);
            mMediaProjection.registerCallback(mProjectionCallback, null);
        }
    }

    public synchronized void stopScreenCapture() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mAudioCapture != null) {
            mAudioCapture.release();
            mAudioCapture = null;
        }
        tearDownMediaProjection();
        mDisplayManager.unregisterDisplayListener(this);

    }

    private MediaFormat getMediaFormat() {
        MediaFormat mediaFormat;
        if (mediaFormatI != null) {
            mediaFormat = mediaFormatI.getMediaFormat();
        } else {
            mediaFormat = getMediaFormat(960000 * 4, 24, 1);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mediaFormat.setFloat(MediaFormat.KEY_MAX_FPS_TO_ENCODER, 60);
        }
        return mediaFormat;
    }

    private MediaFormat getMediaFormat(int bitRate, int framerate, int iFrameInterval) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setFloat(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        return mediaFormat;
    }

    private MediaFormat getMediaFormat2(int bitRate, int framerate, int iFrameInterval) {
        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", height, width);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        mediaFormat.setFloat(MediaFormat.KEY_FRAME_RATE, framerate);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval);
        return mediaFormat;
    }

    private void tearDownMediaProjection() {
        Log.d(TAG, "tearDownMediaProjection");
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private void releaseResource() {
        Log.d(TAG, "releaseResource");
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
    }

    @Override
    public void onDisplayAdded(int displayId) {
        if (displayListener != null) {
            displayListener.onDisplayAdded(displayId);
        }
    }

    @Override
    public void onDisplayRemoved(int displayId) {
        if (displayListener != null) {
            displayListener.onDisplayRemoved(displayId);
        }
    }

    @Override
    public void onDisplayChanged(int displayId) {
        Display display = mDisplayManager.getDisplay(displayId);
        if (restart) {
            Log.d(TAG, "restarting, ignore weird state change : name " + display.getName() + " state = " + display.getState());
            return;
        }
        if (displayListener != null) {
            displayListener.onDisplayChanged(displayId);
        }
        if (VIRTUAL_DISPLAY_NAME.equals(display.getName())) {
            if (display.getState() == android.view.Display.STATE_ON) {
                if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)) {
                    if (virtualDisplayCallback != null) {
                        virtualDisplayCallback.onResumed();
                    }
                }
            } else if (display.getState() == android.view.Display.STATE_OFF) {
                Log.d(TAG, "display off");
                stopScreenCapture();
                releaseResource();
            }

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

    public void enableMicRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mAudioCapture != null) {
                mAudioCapture.enableMicRecording();
            }
        }
    }

    public void disableMicRecording(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (mAudioCapture != null) {
                mAudioCapture.disableMicRecording();
            }
        }
    }

}