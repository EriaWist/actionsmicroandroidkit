package com.actionsmicro.airplay.airtunes;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class NativeAacEldDecoder implements IAacEldEncoder {
	private static final int SMAPLE_RATE = 44100;
	private static final int NUMBER_OF_CHANNEL = 2;
	private static final int NUMBER_OF_FRAME = 480;
	private List<ByteBuffer> inputBuffers = new ArrayList<ByteBuffer>();
	private List<Buffer> outputBuffers = new ArrayList<Buffer>();
	private static final int NUMBER_OF_BUFFER = 2;
	private ByteBuffer[] inputBufferArray = new ByteBuffer[NUMBER_OF_BUFFER];
	private ByteBuffer[] outputBufferArray = new ByteBuffer[NUMBER_OF_BUFFER];
	class Buffer {
		private int size;
		private long presentationTimeUs;
		private int flags;
		ByteBuffer byteBuffer;
		Buffer(ByteBuffer byteBuffer) {
			this.byteBuffer = byteBuffer;
		}
	}
	private final BlockingQueue<ByteBuffer> inputBufferPool = new ArrayBlockingQueue<ByteBuffer>(NUMBER_OF_BUFFER);
	private final BlockingQueue<Buffer> outputBufferPool = new ArrayBlockingQueue<Buffer>(NUMBER_OF_BUFFER);
	private final BlockingQueue<Buffer> availableOutputBuffer = new ArrayBlockingQueue<Buffer>(NUMBER_OF_BUFFER);
	public NativeAacEldDecoder() {
		for (int i = 0; i < NUMBER_OF_BUFFER; i++) {
			ByteBuffer inputBuffer = ByteBuffer.allocate(NUMBER_OF_CHANNEL*2*NUMBER_OF_FRAME);
			inputBuffers.add(inputBuffer);
			inputBufferArray[i] = inputBuffer;
			inputBufferPool.add(inputBuffer);
			
			ByteBuffer outputBuffer = ByteBuffer.allocate(NUMBER_OF_CHANNEL*2*NUMBER_OF_FRAME);
			outputBufferArray[i] = outputBuffer;
			Buffer buffer = new Buffer(outputBuffer);
			outputBuffers.add(buffer);	
			outputBufferPool.add(buffer);
		}
	}
	@Override
	public void init() {
		AacEldDecoder.init(SMAPLE_RATE, NUMBER_OF_CHANNEL, NUMBER_OF_FRAME);
	}

	@Override
	public void start() {

	}

	@Override
	public ByteBuffer[] getInputBuffers() {
		return inputBufferArray;
	}

	@Override
	public int dequeueInputBuffer(int timeoutUs) {
		try {
			ByteBuffer buffer = inputBufferPool.poll(timeoutUs, TimeUnit.MICROSECONDS);
			if (buffer != null) {
				return inputBuffers.indexOf(buffer);
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		return MediaCodec.INFO_TRY_AGAIN_LATER;
	}

	@Override
	public void queueInputBuffer(int bufferIndex, int offset, int size, long presentationTimeUs, int flags) {
		ByteBuffer inputBuffer = inputBufferArray[bufferIndex];
		try {
			Buffer outputBuffer = outputBufferPool.take();
			outputBuffer.presentationTimeUs = presentationTimeUs;
			outputBuffer.flags = flags;
			outputBuffer.size = AacEldDecoder.decode(inputBuffer.array(), offset, size, outputBuffer.byteBuffer.array());
			inputBufferPool.add(inputBuffer);
			availableOutputBuffer.add(outputBuffer);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
	}

	@Override
	public ByteBuffer[] getOutputBuffers() {
		return outputBufferArray;
	}

	@Override
	public int dequeueOutputBuffer(BufferInfo bufferInfo, int timeoutUs) {
		try {
			Buffer outputBuffer = availableOutputBuffer.poll(timeoutUs, TimeUnit.MICROSECONDS);
			if (outputBuffer != null) {
				bufferInfo.presentationTimeUs = outputBuffer.presentationTimeUs;
				bufferInfo.offset = 0;
				bufferInfo.size = outputBuffer.size;
				bufferInfo.flags = outputBuffer.flags;
				return outputBuffers.indexOf(outputBuffer);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return MediaCodec.INFO_TRY_AGAIN_LATER;
	}

	@Override
	public void releaseOutputBuffer(int outputBufferIndex, boolean render) {
		outputBufferPool.add(outputBuffers.get(outputBufferIndex));
	}

	@Override
	public MediaFormat getOutputFormat() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void stop() {
		inputBufferPool.clear();
		inputBufferPool.addAll(inputBuffers);
		
		availableOutputBuffer.clear();
		outputBufferPool.clear();
		outputBufferPool.addAll(outputBuffers);
	}

	@Override
	public void release() {
		AacEldDecoder.release();
	}

}
