package com.actionsmicro.airplay.airtunes;

import android.media.MediaCodec;
import android.os.Handler;
import android.os.Process;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

public class NativeAacEldEncoder implements IAacEldEncoder {
    private static final int SAMPLE_RATE = 44100;
    private static final int NUMBER_OF_CHANNEL = 2;
    private static final int NUMBER_OF_FRAME = 480;
    private static final String TAG = "NativeAacEldEncoder";
    private static final boolean DEBUG_LOG = false;
    private final AacEldEncoder aacEldEncoder;
    private List<ByteBuffer> inputBuffers = new ArrayList<>();
    private List<Buffer> outputBuffers = new ArrayList<>();
    private static final int NUMBER_OF_BUFFER = 2;
    private List<Integer> inputBufferArray = new ArrayList<>();
    private List<Integer> outputBufferArray = new ArrayList<>();
    private IAacEldEncoder.Callback callback;
    private Thread driver;
    private Thread encoderThread;
    private Buffer partialFrameBuffer = new Buffer(ByteBuffer.allocate(NUMBER_OF_CHANNEL*2*NUMBER_OF_FRAME));

    class Buffer {
        public int offset;
        public int index;
        private int size;
        private long presentationTimeUs;
        private int flags;
        ByteBuffer byteBuffer;
        Buffer(ByteBuffer byteBuffer) {
            this.byteBuffer = byteBuffer;
        }
    }
    private final BlockingQueue<Integer> inputBufferPool = new LinkedBlockingDeque<>(NUMBER_OF_BUFFER);
    private final BlockingQueue<Buffer> availableInputBuffer = new LinkedBlockingDeque<>(NUMBER_OF_BUFFER);
    private final BlockingQueue<Integer> outputBufferPool = new LinkedBlockingDeque<>(NUMBER_OF_BUFFER);
    private final BlockingQueue<Buffer> availableOutputBuffer = new LinkedBlockingDeque<>(NUMBER_OF_BUFFER);
    public NativeAacEldEncoder() throws Exception {
        for (int i = 0; i < NUMBER_OF_BUFFER; i++) {
            ByteBuffer inputBuffer = ByteBuffer.allocate(NUMBER_OF_CHANNEL*2*NUMBER_OF_FRAME);
            inputBuffers.add(inputBuffer);
            inputBufferArray.add(i);
            inputBufferPool.add(i);

            ByteBuffer outputBuffer = ByteBuffer.allocate(NUMBER_OF_CHANNEL*2*NUMBER_OF_FRAME);
            outputBufferArray.add(i);
            Buffer buffer = new Buffer(outputBuffer);
            buffer.index = i;
            outputBuffers.add(buffer);
            outputBufferPool.add(i);
        }
        aacEldEncoder = new AacEldEncoder(96000, SAMPLE_RATE);

        debugLog("inputBufferPool size: " + inputBufferPool.size() + ", outputBufferPool size: " + outputBufferPool.size());
    }

    @Override
    public void setCallback(IAacEldEncoder.Callback callback) {
        this.callback = callback;
    }

    private void debugLog(String msg) {
        if (DEBUG_LOG) {
            Log.d(TAG, msg);
        }
    }

    public void start() {
        final Handler handler = new Handler();
        driver = new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                try {
                    while (!isInterrupted()) {
                        final int inputBufferIndex = dequeueInputBuffer(3000);
                        if (inputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                            debugLog("dequeueInputBuffer: " + inputBufferIndex);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    debugLog("onInputBufferAvailable: " + inputBufferIndex);
                                    callback.onInputBufferAvailable(NativeAacEldEncoder.this, inputBufferIndex);
                                }
                            });
                        } else {
                            debugLog("dequeueInputBuffer buffer is not enough!");
                        }

                        final int outputBufferIndex = dequeueOutputBuffer(bufferInfo, 3000);
                        if (outputBufferIndex != MediaCodec.INFO_TRY_AGAIN_LATER) {
                            debugLog("dequeueOutputBuffer: " + outputBufferIndex);
                            final MediaCodec.BufferInfo copy = new MediaCodec.BufferInfo();
                            copy.size = bufferInfo.size;
                            copy.flags = bufferInfo.flags;
                            copy.offset = bufferInfo.offset;
                            copy.presentationTimeUs = bufferInfo.presentationTimeUs;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    debugLog("onOutputBufferAvailable: " + outputBufferIndex);
                                    callback.onOutputBufferAvailable(NativeAacEldEncoder.this, outputBufferIndex, copy);
                                }
                            });
                        } else {
                            debugLog("dequeueOutputBuffer buffer is not enough!");
                        }
                    }
                } catch (InterruptedException e) {

                }
                Log.d(TAG, "Driver Thread exit");
            }
        };
        driver.start();
        encoderThread = new Thread() {
            @Override
            public void run() {
                Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
                try {
                    while (!isInterrupted()) {
                        processInputBuffer(10000);
                    }
                } catch (InterruptedException e) {

                }
                Log.d(TAG,"Encoder Thread exit");
            }
        };
        encoderThread.start();
    }

    public void stop() {
        driver.interrupt();
        encoderThread.interrupt();
        try {
            driver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            encoderThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        inputBufferPool.clear();
        inputBufferPool.addAll(inputBufferArray);

        availableOutputBuffer.clear();
        outputBufferPool.clear();
        outputBufferPool.addAll(outputBufferArray);
    }

    public int dequeueInputBuffer(int timeoutUs) throws InterruptedException {
        Integer bufferIndex = inputBufferPool.poll(timeoutUs, TimeUnit.MICROSECONDS);
        if (bufferIndex != null) {
            return bufferIndex;
        }
        return MediaCodec.INFO_TRY_AGAIN_LATER;
    }

    @Override
    public ByteBuffer getInputBuffer(int index) {
        return inputBuffers.get(index);
    }
    private void processInputBuffer(int timeoutUs) throws InterruptedException {
        Buffer buffer = availableInputBuffer.poll(timeoutUs, TimeUnit.MICROSECONDS);
        if (buffer != null) {
            Integer outputBufferIndex = outputBufferPool.take();
            debugLog("processInputBuffer: " + buffer.index + " to outputBufferIndex: " + outputBufferIndex);
            Buffer outputBuffer = outputBuffers.get(outputBufferIndex);
            outputBuffer.presentationTimeUs = buffer.presentationTimeUs;
            outputBuffer.flags = buffer.flags;
            outputBuffer.size = aacEldEncoder.encode(buffer.byteBuffer.array(), buffer.offset, buffer.size, outputBuffer.byteBuffer.array());
            debugLog("processInputBuffer done output size: " + outputBuffer.size);
            inputBufferPool.add(buffer.index);
            availableOutputBuffer.add(outputBuffer);
        }
    }
    @Override
    public void queueInputBuffer(int bufferIndex, int offset, int size, long presentationTimeUs, int flags) {
        debugLog("queueInputBuffer: " + bufferIndex + ", flags: " + Integer.toHexString(flags));
        if ((flags & MediaCodec.BUFFER_FLAG_PARTIAL_FRAME) != 0) {
            ByteBuffer inputBuffer = inputBuffers.get(bufferIndex);
            partialFrameBuffer.byteBuffer.put(inputBuffer.array(), offset, size);
            partialFrameBuffer.presentationTimeUs = presentationTimeUs;
            partialFrameBuffer.size = partialFrameBuffer.byteBuffer.position();
            inputBufferPool.add(bufferIndex);
        } else {
            ByteBuffer inputBuffer = inputBuffers.get(bufferIndex);
            Buffer inputBufferInfo = new Buffer(inputBuffer);
            if (partialFrameBuffer.size != 0) {
                partialFrameBuffer.byteBuffer.put(inputBuffer.array(), offset, size);
                partialFrameBuffer.size += size;
                inputBuffer.position(0);
                inputBuffer.put(partialFrameBuffer.byteBuffer.array(), 0, partialFrameBuffer.size);

                inputBufferInfo.presentationTimeUs = partialFrameBuffer.presentationTimeUs;
                inputBufferInfo.flags = flags;
                inputBufferInfo.size = partialFrameBuffer.size;
                inputBufferInfo.offset = 0;
                inputBufferInfo.index = bufferIndex;
                partialFrameBuffer.size = 0;
                partialFrameBuffer.byteBuffer.position(0);
            } else {
                inputBufferInfo.presentationTimeUs = presentationTimeUs;
                inputBufferInfo.flags = flags;
                inputBufferInfo.size = size;
                inputBufferInfo.offset = offset;
                inputBufferInfo.index = bufferIndex;
            }
            availableInputBuffer.add(inputBufferInfo);
        }
    }

    public int dequeueOutputBuffer(MediaCodec.BufferInfo bufferInfo, int timeoutUs) throws InterruptedException {
        Buffer outputBuffer = availableOutputBuffer.poll(timeoutUs, TimeUnit.MICROSECONDS);
        if (outputBuffer != null) {
            bufferInfo.presentationTimeUs = outputBuffer.presentationTimeUs;
            bufferInfo.offset = 0;
            bufferInfo.size = outputBuffer.size;
            bufferInfo.flags = outputBuffer.flags;
            return outputBuffer.index;
        }
        return MediaCodec.INFO_TRY_AGAIN_LATER;
    }

    @Override
    public ByteBuffer getOutputBuffer(int index) {
        return outputBuffers.get(index).byteBuffer;
    }

    public void releaseOutputBuffer(int outputBufferIndex, boolean render) {
        outputBufferPool.add(outputBufferIndex);
    }

    public void release() {
        aacEldEncoder.release();
    }

}
