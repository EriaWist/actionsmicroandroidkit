package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class RtspDecoder {
    private static final String TAG = "RtspDecoder";
    private final TextureView surfaceTextureView;
    //处理音视频的编解码的类MediaCodec
    private MediaCodec video_decoder;
    //显示画面的Surface
    private Surface surface;
    // 0: live, 1: playback, 2: local file
    private int state = 0;
    //视频数据
    private BlockingQueue<byte[]> video_data_Queue = new ArrayBlockingQueue<byte[]>(10000);
    //音频数据
    private BlockingQueue<byte[]> audio_data_Queue = new ArrayBlockingQueue<byte[]>(10000);

    private boolean isReady = false;
    private int fps = 0;

    private ByteBuffer[] inputBuffers;
    private ByteBuffer[] outputBuffers;
    private MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private int frameCount = 0;
    private long deltaTime = 0;
    private long counterTime = System.currentTimeMillis();
    private boolean isRuning = false;
    private float surfaceWidth = 1280;
    private float surfaceHeight = 720;

    public RtspDecoder(TextureView surfaceTextureView, int playerState) {
        this.surfaceTextureView = surfaceTextureView;
        this.surface = new Surface(surfaceTextureView.getSurfaceTexture());
        this.state = playerState;
    }

    public void stopRunning() {
        video_data_Queue.clear();
        audio_data_Queue.clear();
    }

    //添加视频数据
    public void setVideoData(byte[] data) {

        try {
            video_data_Queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    //添加音频数据
    public void setAudioData(byte[] data) {
        try {
            audio_data_Queue.put(data);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public int getFPS() {
        return fps;
    }


    public void initial(byte[] sps) throws IOException {
        if (video_decoder != null) {
            video_decoder.stop();
            video_decoder.release();
            video_decoder = null;
        }
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", 1280, 720);
        video_decoder = MediaCodec.createDecoderByType("video/avc");
        if (video_decoder == null) {
            return;
        }

        video_decoder.configure(format, surface, null, 0);
        video_decoder.start();
        inputBuffers = video_decoder.getInputBuffers();
        outputBuffers = video_decoder.getOutputBuffers();
        frameCount = 0;
        deltaTime = 0;
        isRuning = true;
        runDecodeVideoThread();
        updateTransformAccodingToSps(sps);
    }

    /**
     * @description 解码视频流数据
     * @author ldm
     * @time 2016/12/20
     */
    private void runDecodeVideoThread() {

        Thread t = new Thread() {

            @SuppressLint("NewApi")
            public void run() {

                while (isRuning) {

                    int inIndex = -1;
                    try {
                        inIndex = video_decoder.dequeueInputBuffer(-1);
                    } catch (Exception e) {
                        return;
                    }
                    try {

                        if (inIndex >= 0) {
                            ByteBuffer buffer = inputBuffers[inIndex];
                            buffer.clear();

                            if (!video_data_Queue.isEmpty()) {
                                byte[] data;
                                data = video_data_Queue.take();
                                buffer.put(data);
                                if (state == 0) {
                                    video_decoder.queueInputBuffer(inIndex, 0, data.length, 66, 0);
                                } else {
                                    video_decoder.queueInputBuffer(inIndex, 0, data.length, 33, 0);
                                }
                            } else {
                                if (state == 0) {
                                    video_decoder.queueInputBuffer(inIndex, 0, 0, 66, 0);
                                } else {
                                    video_decoder.queueInputBuffer(inIndex, 0, 0, 33, 0);
                                }
                            }
                        } else {
                            video_decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        }

                        int outIndex = video_decoder.dequeueOutputBuffer(info, 0);
                        switch (outIndex) {
                            case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                                outputBuffers = video_decoder.getOutputBuffers();
                                break;
                            case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                                isReady = true;
                                break;
                            case MediaCodec.INFO_TRY_AGAIN_LATER:
                                break;
                            default:

                                video_decoder.releaseOutputBuffer(outIndex, true);
                                frameCount++;
                                deltaTime = System.currentTimeMillis() - counterTime;
                                if (deltaTime > 1000) {
                                    fps = (int) (((float) frameCount / (float) deltaTime) * 1000);
                                    counterTime = System.currentTimeMillis();
                                    frameCount = 0;
                                }
                                break;
                        }

                        //所有流数据解码完成，可以进行关闭等操作
                        if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            Log.e("dddd", "BUFFER_FLAG_END_OF_STREAM");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }
        };

        t.start();
    }


    private void updateTransformAccodingToSps(byte[] spsData) {
        SeqParameterSet sps = SeqParameterSet.read(ByteBuffer.wrap(spsData, 1, spsData.length - 1));
        int codedWidth = (sps.pic_width_in_mbs_minus1 + 1) << 4;
        int codedHeight = H264Utils.getPicHeightInMbs(sps) << 4;

        final int width = sps.frame_cropping_flag ? codedWidth
                - ((sps.frame_crop_right_offset + sps.frame_crop_left_offset) << sps.chroma_format_idc.compWidth[1])
                : codedWidth;
        final int height = sps.frame_cropping_flag ? codedHeight
                - ((sps.frame_crop_bottom_offset + sps.frame_crop_top_offset) << sps.chroma_format_idc.compHeight[1])
                : codedHeight;
        Log.v(TAG, "seqParameterSet width:" + width + ", height:" + height);
        updateTransform(width, height);
    }

    private void updateTransform(int width, int height) {
        final Matrix transform = new Matrix();
        transform.setRectToRect(new RectF(0, 0, width, height), new RectF(0, 0, surfaceWidth, surfaceHeight), Matrix.ScaleToFit.CENTER);
        transform.preScale((float) width / (float) surfaceWidth, (float) height / (float) surfaceHeight);
        Log.v(TAG, "mirror view width:" + surfaceTextureView.getWidth() + ", height:" + surfaceTextureView.getHeight());
        Log.v(TAG, "surfaceWidth:" + surfaceWidth + ", surfaceHeight:" + surfaceHeight);
        surfaceTextureView.setTransform(transform);
    }
}
