package com.actionsmicro.airplay.airtunes;

import android.media.MediaCodec;
import android.media.MediaFormat;

import java.nio.ByteBuffer;

import androidx.annotation.NonNull;

public interface IAacEldEncoder {

    ByteBuffer getInputBuffer(int index);

    void queueInputBuffer(int index, int offset, int size, long presentationTimestamp, int flags);

    ByteBuffer getOutputBuffer(int index);

    void releaseOutputBuffer(int index, boolean render);

    void setCallback(IAacEldEncoder.Callback callback);

    void start();

    void stop();

    abstract class Callback {
        /**
         * Called when an input buffer becomes available.
         *
         * @param codec The MediaCodec object.
         * @param index The index of the available input buffer.
         */
        public abstract void onInputBufferAvailable(@NonNull IAacEldEncoder codec, int index);

        /**
         * Called when an output buffer becomes available.
         *
         * @param codec The MediaCodec object.
         * @param index The index of the available output buffer.
         * @param info Info regarding the available output buffer {@link MediaCodec.BufferInfo}.
         */

        public abstract void onOutputBufferAvailable(
                @NonNull IAacEldEncoder codec, int index, @NonNull MediaCodec.BufferInfo info);

        /**
         * Called when the MediaCodec encountered an error
         *
         * @param codec The MediaCodec object.
         * @param e The {@link MediaCodec.CodecException} object describing the error.
         */
        public abstract void onError(@NonNull IAacEldEncoder codec, @NonNull MediaCodec.CodecException e);

        /**
         * Called when the output format has changed
         *
         * @param codec The MediaCodec object.
         * @param format The new output format.
         */
        public abstract void onOutputFormatChanged(
                @NonNull IAacEldEncoder codec, @NonNull MediaFormat format);
    }
}
