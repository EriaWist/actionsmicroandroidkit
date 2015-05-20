package com.actionsmicro.airplay.airtunes;

import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AndroidAacEldEncoder implements IAacEldEncoder {

	private MediaCodec decoder;

	@Override
	public void init() {
		MediaCodec mediaCodec = MediaCodec.createByCodecName("OMX.google.aac.decoder");//MediaCodec.createDecoderByType("audio/mp4a-latm");
    	MediaFormat mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 0, 0);
    	byte[] bytes = new byte[]{(byte) 0xF8, (byte) 0xE8, 0x50, 0x00};
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        mediaFormat.setByteBuffer("csd-0", bb);
    	mediaCodec.configure(mediaFormat, null, null, 0);
    	decoder = mediaCodec;
	}

	@Override
	public void start() {
		decoder.start();
	}

	@Override
	public ByteBuffer[] getInputBuffers() {
		return decoder.getInputBuffers();
	}

	@Override
	public int dequeueInputBuffer(int timeoutUs) {
		return decoder.dequeueInputBuffer(timeoutUs);
	}

	@Override
	public void queueInputBuffer(int index, int offset, int size,
			long presentationTimeUs, int flags) {
		decoder.queueInputBuffer(index, offset, size, presentationTimeUs, flags);
	}

	@Override
	public ByteBuffer[] getOutputBuffers() {
		return decoder.getOutputBuffers();
	}

	@Override
	public int dequeueOutputBuffer(BufferInfo info, int timeoutUs) {
		return decoder.dequeueOutputBuffer(info, timeoutUs);
	}

	@Override
	public void releaseOutputBuffer(int outputBufferIndex, boolean render) {
		decoder.releaseOutputBuffer(outputBufferIndex, render);
	}

	@Override
	public MediaFormat getOutputFormat() {
		return decoder.getOutputFormat();
	}

	@Override
	public void stop() {
		decoder.stop();		
	}

	@Override
	public void release() {
		decoder.release();
		decoder = null;
	}

}
