package com.actionsmicro.airplay.mirror;

import java.io.IOException;
import java.nio.ByteBuffer;

import com.actionsmicro.utils.Log;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AvcEncoder {
	public interface ParameterSetsListener {

		void avcParametersSetsEstablished(byte[] sps, byte[] pps);

	}

	public interface EncodedFrameListener {

		void frameReceived(byte[] outData, int index, int length, MediaCodec.BufferInfo bufferInfo);

	}

	private EncodedFrameListener frameListener;
	public EncodedFrameListener getFrameListener() {
		return frameListener;
	}
	public void setFrameListener(EncodedFrameListener frameListener) {
		this.frameListener = frameListener;
	}

	private MediaCodec mediaCodec;
	
	private byte[] sps;
	private byte[] pps;
	private ParameterSetsListener parameterSetsListener;

	private int height;

	private int width;
 
	public ParameterSetsListener getParameterSetsListener() {
		return parameterSetsListener;
	}
	public void setParameterSetsListener(ParameterSetsListener parameterSetsListener) {
		this.parameterSetsListener = parameterSetsListener;
	}
	public AvcEncoder(int width, int height) {
		this.width = width;
		this.height = height;
		mediaCodec = MediaCodec.createEncoderByType("video/avc");
		MediaFormat mediaFormat = getMediaFormat();
		mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		mediaCodec.start();
	}
	public MediaFormat getMediaFormat() {
		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 6000000);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 25);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
		return mediaFormat;
	}
 	public void close() throws IOException {
		mediaCodec.stop();
		mediaCodec.release();
	}
 	public static byte[] NV21toYUV420Planar(byte[] input, byte[] output, int widthXheight) {
 	    final int frameSize = widthXheight;
 	    final int qFrameSize = frameSize/4;
 	 
 	    System.arraycopy(input, 0, output, 0, frameSize); // Y
 	 
 	    byte v, u;
 	 
 	    for (int i = 0; i < qFrameSize; i++) {
 	        v = input[frameSize + i*2];
 	        u = input[frameSize + i*2 + 1];
 	 
 	        output[frameSize + i + qFrameSize] = v;
 	        output[frameSize + i] = u;
 	    }
 	 
 	    return output;
 	}
 	private static byte[] yuvBuffer = new byte[1024*1024];
	public synchronized void offerEncoder(byte[] input, long presentationTimeStamp) {
		try {
			ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
			ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
			int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				int framesize = input.length*2/3;
				NV21toYUV420Planar(input, yuvBuffer, framesize);
	            inputBuffer.put(yuvBuffer, 0, input.length);
				mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, presentationTimeStamp, 0);
			}
 
			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
			while (outputBufferIndex >= 0) {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				onOutputBufferReady(bufferInfo, outputBuffer);
				mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
				outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
				
			}
			if (MediaCodec.INFO_OUTPUT_FORMAT_CHANGED == outputBufferIndex) {
				onOutputForamtChanged(mediaCodec.getOutputFormat());
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
 
	}
	protected void onOutputForamtChanged(MediaFormat outputFormat) {
		// TODO Auto-generated method stub
		
	}
	protected void onOutputBufferReady(MediaCodec.BufferInfo bufferInfo,
			ByteBuffer outputBuffer) {
		byte[] outData = new byte[bufferInfo.size];
		outputBuffer.get(outData);
		Log.d("AVC", "bufferInfo.flags:" + bufferInfo.flags);
		if (sps != null && pps != null) {
			frameListener.frameReceived(outData, 0, outData.length, bufferInfo);
		} else {
			ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
			if (spsPpsBuffer.getInt() == 0x00000001) {
				System.out.println("parsing sps/pps");
			} else {
				System.out.println("something is amiss?");
			}
			int ppsIndex = 0;
			while(!(spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x00 && spsPpsBuffer.get() == 0x01)) {
 
			}
			ppsIndex = spsPpsBuffer.position();
			sps = new byte[ppsIndex - 8];
			System.arraycopy(outData, 4, sps, 0, sps.length);
			pps = new byte[outData.length - ppsIndex];
			System.arraycopy(outData, ppsIndex, pps, 0, pps.length);
			if (null != parameterSetsListener) {
				parameterSetsListener.avcParametersSetsEstablished(sps, pps);
			}
		}
	}
	public int getHeight() {
		return height;
	}
	public int getWidth() {
		return width;
	}
}
