package vavi.apps.shairport;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodec.BufferInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AacEldDecoder {
	private MediaCodec decocder;
	private BufferInfo bufferInfo;

	public AacEldDecoder() {
		try {
			decocder = MediaCodec.createByCodecName("OMX.google.aac.decoder");//MediaCodec.createDecoderByType("audio/mp4a-latm");
			MediaFormat mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 44100, 2);
	//    	mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectELD);
	//    	mediaFormat.setInteger(MediaFormat.KEY_IS_ADTS, 1);
			byte[] bytes = new byte[]{(byte) 0xF8, (byte)0xE8, 0x50, 0x00};
			ByteBuffer bb = ByteBuffer.wrap(bytes);
			mediaFormat.setByteBuffer("csd-0", bb);
			decocder.configure(mediaFormat, null, null, 0);
			decocder.start();

	//    	int decInBufIdx = decocder.dequeueInputBuffer(10000);
	//		if (decInBufIdx >= 0) {
	//			decocder.getInputBuffers()[decInBufIdx].position(0);
	//			decocder.getInputBuffers()[decInBufIdx].put(bytes, 0, bytes.length);
	//			decocder.getInputBuffers()[decInBufIdx].rewind();
	//			decocder.queueInputBuffer(decInBufIdx, 0, bytes.length, 0, MediaCodec.BUFFER_FLAG_CODEC_CONFIG);
	//		}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public byte[] decode(byte[] au, int length) { 
		try {
			if (au != null) {
				int decInBufIdx = decocder.dequeueInputBuffer(10000); 
				if (decInBufIdx >= 0) {
					Log.e("AacEldDecoder", "dequeueInputBuffer:"+decInBufIdx);
					decocder.getInputBuffers()[decInBufIdx].position(0); 
					decocder.getInputBuffers()[decInBufIdx].put(au, 0, length); 
					decocder.getInputBuffers()[decInBufIdx].rewind();
					decocder.queueInputBuffer(decInBufIdx, 0, length, 0, 0);
				} 
			}
			byte[] pcmbuf = null; int pcmbufPollCnt = 0;
			while (pcmbuf == null && pcmbufPollCnt < 100) {
				int decBufIdx = decocder.dequeueOutputBuffer(bufferInfo, 100); 
				if (decBufIdx >= 0) {
					Log.e("AacEldDecoder", "dequeueOutputBuffer:"+decBufIdx);
					pcmbuf = new byte[bufferInfo.size]; 
					decocder.getOutputBuffers()[decBufIdx].get(pcmbuf, 0, bufferInfo.size); 
					decocder.getOutputBuffers()[decBufIdx].position(0); 
					decocder.releaseOutputBuffer(decBufIdx, false);
				}
				++pcmbufPollCnt; 
			}
			return pcmbuf;
		} catch (Exception e) {
			Log.e("AacEldDecoder", "AacEldDecoder:", e);
			return null; 
		}
	}
	public void release() {
		decocder.release();
	}

}
