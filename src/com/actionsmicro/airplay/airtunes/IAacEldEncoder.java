package com.actionsmicro.airplay.airtunes;

import java.nio.ByteBuffer;

import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;

public interface IAacEldEncoder {
	public void init();
	public void start();
	public ByteBuffer[] getInputBuffers();
	public int dequeueInputBuffer(int timeoutUs);
	public void queueInputBuffer(int bufferIndex, int offset, int size, long presentationTimeUs, int flags);
	public ByteBuffer[] getOutputBuffers();
	public int dequeueOutputBuffer(BufferInfo bufferInfo, int timeoutUs);
	public void releaseOutputBuffer(int outputBufferIndex, boolean render);
	public MediaFormat getOutputFormat();
	public void stop();
	public void release();
}
