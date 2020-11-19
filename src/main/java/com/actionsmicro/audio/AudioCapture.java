package com.actionsmicro.audio;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioPlaybackCaptureConfiguration;
import android.media.AudioRecord;
import android.media.AudioTimestamp;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.os.Process;
import android.util.Log;

import com.actionsmicro.airplay.airtunes.IAacEldEncoder;
import com.actionsmicro.airplay.airtunes.NativeAacEldEncoder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.Q)
public class AudioCapture {
    private static final String TAG = "AudioCapture";
    private static final int SAMPLING_RATE = 44100;
    private static final int AUDIO_SOURCE = MediaRecorder.AudioSource.MIC;
    private static final int CHANNEL_IN_CONFIG = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNEL_COUNT = 2;
    private final int BUFFER_SIZE = CHANNEL_COUNT * 2 * 480;//AudioRecord.getMinBufferSize(SAMPLING_RATE, CHANNEL_IN_CONFIG, AUDIO_FORMAT);
    private final AudioRecord mAudioRecorder;
    private final AudioDataCallback mAudioDataCallBack;
    private final boolean DUMP_AUDIO = false;
    private static final boolean DEBUG_LOG = false;

    public interface AudioDataCallback {
        void onAudioDataAvailable(ByteBuffer dataBuffer, int size);
    }

    public AudioCapture(MediaProjection mediaProjection, AudioDataCallback audioDataCallback) {
        mAudioDataCallBack = audioDataCallback;
        AudioPlaybackCaptureConfiguration config = new AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .build();
        mAudioRecorder = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLING_RATE)
                        .setChannelMask(CHANNEL_IN_CONFIG)
                        .build())
                .setAudioPlaybackCaptureConfig(config)
                .build();
    }

    public void stopRecording() {
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.getState() && AudioRecord.RECORDSTATE_RECORDING == mAudioRecorder.getRecordingState()) {
            synchronized (mAudioRecorder) {
                mAudioRecorder.stop();
            }
            Log.d(TAG, "Recording done…");
        }
    }

    public void startRecording() {
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.getState() && AudioRecord.RECORDSTATE_STOPPED == mAudioRecorder.getRecordingState()) {
            mAudioRecorder.startRecording();
            Log.d(TAG, "Start recording");
            new Thread() {
                FileOutputStream testPCMFile = null;
                FileOutputStream testAACFile = null;
                FileOutputStream testRawAACFile = null;

                @Override
                public void run() {
                    if (DUMP_AUDIO) {
                        try {
                            testPCMFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/pcm.raw");
                            testAACFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/adtsAAC.aac");
                            testRawAACFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/rawAAC.aac");
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    try {
                        final byte[] pcmBuffer = new byte[BUFFER_SIZE];
                        final AudioTimestamp audioTimestamp = new AudioTimestamp();
                        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);

                        Looper.prepare();
                        final AudioRecord audioRecorder = mAudioRecorder;
                        final ByteBuffer aacBuffer = ByteBuffer.allocate(512 * 1024);
                        IAacEldEncoder codec = new NativeAacEldEncoder();
                        codec.setCallback(new IAacEldEncoder.Callback() {
                            private byte[] adtsHeader = new byte[7];
                            private int seq = 0;
                            private int needSize = BUFFER_SIZE;

                            @Override
                            public void onInputBufferAvailable(@NonNull IAacEldEncoder codec, int index) {
                                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                                inputBuffer.clear();
                                int ret = audioRecorder.read(pcmBuffer, 0, needSize, AudioRecord.READ_NON_BLOCKING);
                                debugLog("onInputBufferAvailable audioRecorder.read: " + needSize);
                                if (ret < 0) {
                                    Log.e(TAG, "onInputBufferAvailable audioRecorder.read failed: " + ret);
                                } else if (ret > 0) {
                                    audioRecorder.getTimestamp(audioTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC);
                                    debugLog("onInputBufferAvailable AudioRecord timestamp position: " + audioTimestamp.framePosition + ", delta: " + (System.nanoTime() - audioTimestamp.nanoTime) / 1000000);
                                    inputBuffer.put(pcmBuffer, 0, ret);
                                    if (testPCMFile != null) {
                                        try {
                                            testPCMFile.write(pcmBuffer, 0, ret);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    debugLog("onInputBufferAvailable AudioRecord has nothing to read");
                                }

                                inputBuffer.rewind();
                                needSize -= ret;
                                if (needSize == 0) {
                                    codec.queueInputBuffer(index, 0, ret, System.nanoTime() / 1000, 0);
                                    needSize = BUFFER_SIZE;
                                } else {
                                    codec.queueInputBuffer(index, 0, ret, System.nanoTime() / 1000, MediaCodec.BUFFER_FLAG_PARTIAL_FRAME);
                                }
                            }

                            @Override
                            public void onOutputBufferAvailable(@NonNull IAacEldEncoder codec, int index, @NonNull MediaCodec.BufferInfo info) {
                                debugLog("onOutputBufferAvailable: " + index + ", offset: " + info.offset + ", size: " + info.size + ", flags: " + info.flags + " ,seq: " + seq);
                                seq++;
                                ByteBuffer encodedData = codec.getOutputBuffer(index);
                                encodedData.position(info.offset);
                                aacBuffer.position(0);
                                encodedData.get(aacBuffer.array(), 0, info.size);
                                if (info.size > 0) {
                                    if (testAACFile != null) {
                                        try {
                                            writeAdtsHeader(testAACFile, info.size);
                                            testAACFile.write(aacBuffer.array(), 0, info.size);
                                            testRawAACFile.write(aacBuffer.array(), 0, info.size);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }

                                    if (info.size > 0) {
                                        if (mAudioDataCallBack != null) {
                                            mAudioDataCallBack.onAudioDataAvailable(aacBuffer, info.size);
                                        }
                                    }
                                }
                                aacBuffer.rewind();
                                codec.releaseOutputBuffer(index, false);
                            }

                            @Override
                            public void onError(@NonNull IAacEldEncoder codec, @NonNull MediaCodec.CodecException e) {
                                Log.e(TAG, "MediaCodec.onError: " + e);
                            }

                            private void writeAdtsHeader(FileOutputStream finalTestEncodedFile, int packetLen) throws IOException {
                                packetLen += 7;
                                int profile = 2;  //AAC LC
                                //39=MediaCodecInfo.CodecProfileLevel.AACObjectELD;
                                int freqIdx = 4;  //44.1KHz
                                int chanCfg = 2;  //CPE

                                // fill in ADTS data
                                adtsHeader[0] = (byte) 0xFF;
                                adtsHeader[1] = (byte) 0xF9;
                                adtsHeader[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
                                adtsHeader[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
                                adtsHeader[4] = (byte) ((packetLen & 0x7FF) >> 3);
                                adtsHeader[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
                                adtsHeader[6] = (byte) 0xFC;
                                finalTestEncodedFile.write(adtsHeader);
                            }

                            @Override
                            public void onOutputFormatChanged(@NonNull IAacEldEncoder codec, @NonNull MediaFormat format) {
                                debugLog("MediaCodec.onOutputFormatChanged: " + format);
                            }
                        });
                        audioRecorder.startRecording();
                        codec.start();
                        Looper.loop();
                        codec.stop();
                        audioRecorder.stop();
                        audioRecorder.release();
                        if (testPCMFile != null) {
                            testPCMFile.close();
                        }

                        if (testAACFile != null) {
                            testAACFile.close();
                        }

                        if (testRawAACFile != null) {
                            testRawAACFile.close();
                        }
                        debugLog("Thread exit");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }

            }.start();
        }

    }

    public void release() {
        stopRecording();
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.getState()) {
            mAudioRecorder.release();
            Log.d(TAG, "Release recorder");
        }
    }

    private void debugLog(String msg) {
        if (DEBUG_LOG) {
            Log.d(TAG, msg);
        }
    }
}
