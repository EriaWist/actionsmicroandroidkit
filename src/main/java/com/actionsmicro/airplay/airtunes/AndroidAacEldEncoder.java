package com.actionsmicro.airplay.airtunes;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;

import com.actionsmicro.utils.Log;

import java.io.IOException;
import java.nio.ByteBuffer;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import static android.media.MediaCodec.CONFIGURE_FLAG_ENCODE;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AndroidAacEldEncoder implements IAacEldEncoder {
    private final MediaCodec codec;

    public AndroidAacEldEncoder(int sampleRate, int channelCount) throws IOException {
        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaFormat mediaFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channelCount);
//                    mediaFormat.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_AUDIO_AAC);
//                    mediaFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLING_RATE);
//                    mediaFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, CHANNEL_COUNT);
        mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectELD);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 96000);
//                    mediaFormat.setInteger(MediaFormat.KEY_DURATION, 480);

//                    mediaFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BUFFER_SIZE);
//                    byte[] bytes = new byte[]{(byte) 0xF8, (byte)0xE8, 0x50, 0x00};
//                    ByteBuffer bb = ByteBuffer.wrap(bytes);
//                    mediaFormat.setByteBuffer("csd-0", bb);
        String codecName = codecList.findEncoderForFormat(mediaFormat);
        codec = MediaCodec.createByCodecName(codecName);
        codec.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE);

//        MediaFormat mediaFormat = makeAACCodecSpecificData(MediaCodecInfo.CodecProfileLevel.AACObjectLC,44100,2);
//        codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
//        codec.configure(mediaFormat, null, null, CONFIGURE_FLAG_ENCODE);

    }

    @Override
    public ByteBuffer getInputBuffer(int index) {
        return codec.getInputBuffer(index);
    }

    @Override
    public void queueInputBuffer(int index, int offset, int size, long presentationTimestamp, int flags) {
        codec.queueInputBuffer(index, offset, size, presentationTimestamp, flags);
    }

    @Override
    public ByteBuffer getOutputBuffer(int index) {
        return codec.getOutputBuffer(index);
    }

    @Override
    public void releaseOutputBuffer(int index, boolean render) {
        codec.releaseOutputBuffer(index, render);
    }

    @Override
    public void setCallback(final IAacEldEncoder.Callback callback) {
        codec.setCallback(new MediaCodec.Callback() {
            @Override
            public void onInputBufferAvailable(@NonNull MediaCodec codec, int index) {
                callback.onInputBufferAvailable(AndroidAacEldEncoder.this, index);
            }

            @Override
            public void onOutputBufferAvailable(@NonNull MediaCodec codec, int index, @NonNull MediaCodec.BufferInfo info) {
                callback.onOutputBufferAvailable(AndroidAacEldEncoder.this, index, info);
            }

            @Override
            public void onError(@NonNull MediaCodec codec, @NonNull MediaCodec.CodecException e) {
                callback.onError(AndroidAacEldEncoder.this, e);
            }

            @Override
            public void onOutputFormatChanged(@NonNull MediaCodec codec, @NonNull MediaFormat format) {
                callback.onOutputFormatChanged(AndroidAacEldEncoder.this, format);
            }
        });
    }

    @Override
    public void start() {
        codec.start();
    }

    @Override
    public void stop() {
        codec.stop();
    }

    private MediaFormat makeAACCodecSpecificData(int audioProfile, int sampleRate, int channelConfig) {
        MediaFormat format = new MediaFormat();
        format.setString(MediaFormat.KEY_MIME, "audio/mp4a-latm");
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, sampleRate);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, channelConfig);

        int samplingFreq[] = {
                96000, 88200, 64000, 48000, 44100, 32000, 24000, 22050,
                16000, 12000, 11025, 8000
        };

        // Search the Sampling Frequencies
        int sampleIndex = -1;
        for (int i = 0; i < samplingFreq.length; ++i) {
            if (samplingFreq[i] == sampleRate) {
                Log.d("TAG", "kSamplingFreq " + samplingFreq[i] + " i : " + i);
                sampleIndex = i;
            }
        }

        if (sampleIndex == -1) {
            return null;
        }

        ByteBuffer csd = ByteBuffer.allocate(2);
        csd.put((byte) ((audioProfile << 3) | (sampleIndex >> 1)));

        csd.position(1);
        csd.put((byte) ((byte) ((sampleIndex << 7) & 0x80) | (channelConfig << 3)));
        csd.flip();
        format.setByteBuffer("csd-0", csd); // add csd-0

        for (int k = 0; k < csd.capacity(); ++k) {
            Log.e("TAG", "csd : " + csd.array()[k]);
        }

        return format;
    }
}
