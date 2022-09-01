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
    private final AudioRecord mAudioRecorder2;
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
        mAudioRecorder2 = new AudioRecord.Builder()
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AUDIO_FORMAT)
                        .setSampleRate(SAMPLING_RATE)
                        .setChannelMask(CHANNEL_IN_CONFIG)
                        .build())
                .setAudioSource(AUDIO_SOURCE)
                .build();
    }

    public void stopRecording(AudioRecord audioRecorder) {
        if (AudioRecord.STATE_INITIALIZED == audioRecorder.getState() && AudioRecord.RECORDSTATE_RECORDING == audioRecorder.getRecordingState()) {
            synchronized (audioRecorder) {
                audioRecorder.stop();
            }
            Log.d(TAG, "Recording done…");
        }
    }

    public void startRecording() {
        if (AudioRecord.STATE_INITIALIZED == mAudioRecorder.getState() && AudioRecord.RECORDSTATE_STOPPED == mAudioRecorder.getRecordingState()) {
            mAudioRecorder.startRecording();
            mAudioRecorder2.startRecording();
            Log.d(TAG, "Start recording");
            new Thread() {
                FileOutputStream testPCMFile = null;
                FileOutputStream testPCMFile2 = null;
                FileOutputStream testPCMFile3 = null;
                FileOutputStream testAACFile = null;
                FileOutputStream testRawAACFile = null;

                @Override
                public void run() {
                    if (DUMP_AUDIO) {
                        try {
                            testPCMFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/pcm.raw");
                            testPCMFile2 = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/pcm2.raw");
                            testPCMFile3 = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/pcm3.raw");
//                            testAACFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/adtsAAC.aac");
//                            testRawAACFile = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/Download/rawAAC.aac");
                        } catch (FileNotFoundException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    try {
                        final byte[] pcmBuffer = new byte[BUFFER_SIZE];
                        final byte[] pcmBuffer2 = new byte[BUFFER_SIZE];
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
                            int pos1 = 0;
                            int pos2 = 0;

                            //   p1      p1n
                            //       p2       p2n
                            //   cur     com
                            @Override
                            public void onInputBufferAvailable(@NonNull IAacEldEncoder codec, int index) {
                                ByteBuffer inputBuffer = codec.getInputBuffer(index);
                                inputBuffer.clear();
                                long start = System.currentTimeMillis();
                                int ret = audioRecorder.read(pcmBuffer, pos1, BUFFER_SIZE - pos1, AudioRecord.READ_NON_BLOCKING);
                                int ret2 = mAudioRecorder2.read(pcmBuffer2, pos2, BUFFER_SIZE - pos2, AudioRecord.READ_NON_BLOCKING);
                                Log.d("dddd", "elapse " + (System.currentTimeMillis() - start) + " need " + needSize + " ret " + ret + " ret2 " + ret2);
                                int len = 0;
                                debugLog("onInputBufferAvailable audioRecorder.read: " + needSize);
                                if (ret < 0 || ret2 < 0) {
                                    Log.e("dddd", "onInputBufferAvailable audioRecorder.read failed: " + ret);
                                } else if (ret > 0 || ret2 >0) {

                                    audioRecorder.getTimestamp(audioTimestamp, AudioTimestamp.TIMEBASE_MONOTONIC);
                                    debugLog("onInputBufferAvailable AudioRecord timestamp position: " + audioTimestamp.framePosition + ", delta: " + (System.nanoTime() - audioTimestamp.nanoTime) / 1000000);
                                    int left = Math.min(pos1,pos2);
                                    if (testPCMFile != null && ret >0) {
                                        try {
                                            testPCMFile.write(pcmBuffer, pos1, ret);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (testPCMFile2 != null && ret2 >0) {
                                        try {
                                            testPCMFile2.write(pcmBuffer2, pos2, ret2);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    pos1 += ret;
                                    pos2 += ret2;
                                    int right = Math.min(pos1,pos2);
                                    // mixed follow
                                    // https://gist.github.com/mpuz/78e9e875df646698243affe1870dda58
                                    len = right - left;
                                    byte[] pcmBuffer3 = new byte[len];

                                    for (int i = left; i < right; i++) {
                                        float samplef1 = pcmBuffer[i] / 128.0f * 0.3f;      //     2^7=128
                                        float samplef2 = pcmBuffer2[i] / 128.0f * 2.0f;
                                        float mixed = samplef1 + samplef2;
                                        // mixed *= 0.8;
                                        // hard clipping
                                        if (mixed > 1.0f) mixed = 1.0f;

                                        if (mixed < -1.0f) mixed = -1.0f;

                                        byte outputSample = (byte)(mixed * 128.0f);
                                        pcmBuffer3[i-left] = outputSample;
                                    }
//                                    inputBuffer.put(pcmBuffer, 0, ret);
                                    inputBuffer.put(pcmBuffer3, 0, len);
                                    if (testPCMFile3 != null) {
                                        try {
                                            testPCMFile3.write(pcmBuffer3, 0, len);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                } else {
                                    debugLog("onInputBufferAvailable AudioRecord has nothing to read");
                                }
                                inputBuffer.rewind();
                                if (pos1 == BUFFER_SIZE && pos2 == BUFFER_SIZE) {
                                    codec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, 0);
                                    pos1 = 0;
                                    pos2 = 0;
                                } else {
                                    codec.queueInputBuffer(index, 0, len, System.nanoTime() / 1000, MediaCodec.BUFFER_FLAG_PARTIAL_FRAME);
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
                        mAudioRecorder2.startRecording();
                        Log.e("eeee","start");
                        codec.start();
                        Looper.loop();
                        codec.stop();
                        Log.e("eeee","stop");
                        audioRecorder.stop();
                        audioRecorder.release();
                        mAudioRecorder2.stop();
                        mAudioRecorder2.release();
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
        stopRecording(mAudioRecorder);
        stopRecording(mAudioRecorder2);
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
