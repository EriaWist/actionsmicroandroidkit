package com.actionsmicro.airplay.airtunes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.net.ntp.TimeStamp;

import vavi.apps.shairport.AudioSession;
import vavi.apps.shairport.UDPDelegate;
import vavi.apps.shairport.UDPListener;
import android.annotation.TargetApi;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.actionsmicro.airplay.clock.PlaybackClock;
import com.actionsmicro.airplay.clock.SimplePlaybackClock;
import com.actionsmicro.debug.DumpBinaryFile;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.ThreadUtils;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioPlayer implements vavi.apps.shairport.AudioPlayer {
	private static final int SAMPLE_RATE_IN_HZ = 44100;
	private static final String TAG = "AudioPlayer";
	private static final boolean DEBUG_LOG = false;
	private DatagramSocket sock;
	private DatagramSocket csock;
	private UDPListener udpListener;
	private PureAudioBuffer pureAudioBuffer;
	private Thread decoderThread;
	private IAacEldEncoder decoder;
	private Thread playerThread;
	private boolean decoderThreadShouldStop;
	private boolean playerThreadShouldStop;
	private AudioTrack track;
	private UDPListener controlPortListener;
	private DumpBinaryFile debugFile;
	private PlaybackClock playbackClock;
	protected long rtpTimestamp;
	protected long ntpTimestamp;

	public AudioPlayer(final AudioSession session) {
		this.pureAudioBuffer = new PureAudioBuffer(new PureAudioBuffer.ResendDelegate() {

			@Override
			public void request_resend(int begin, int end) {
				if (end < begin) {
					return;
				}
				Log.i(TAG, "Resend Request: " + begin + "::" + end);
				
				int len = end - begin + 1;
			    byte[] request = new byte[] { (byte) 0x80, (byte) (0x55|0x80), 0x01, 0x00, (byte) ((begin & 0xFF00) >> 8), (byte) (begin & 0xFF), (byte) ((len & 0xFF00) >> 8), (byte) (len & 0xFF)};

			    try {
			    	DatagramPacket temp = new DatagramPacket(request, request.length, session.getAddress(), session.getControlPort());
			    	csock.send(temp);

			    } catch (IOException e) {
			    	e.printStackTrace();
			    }
			}
			
		});
    	initDecoder();
		spawnDecoderThread(session);
		spawnPlayerThread();
		initRTP(session);		
	}
	private void initDecoder() {
		if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR1) {
			decoder = new NativeAacEldDecoder();
		} else {
			decoder = new AndroidAacEldEncoder();
		}
		decoder.init();
    	decoder.start();
	}
	private void spawnDecoderThread(final AudioSession session) {
		decoderThreadShouldStop = false;
		decoderThread = new Thread(new Runnable() {
			private Cipher cipher;
			/**
			 * Initiate the cipher	
			 */
			private void initAES() {
				// Init AES encryption
				try {
					cipher = Cipher.getInstance("AES/CBC/NoPadding");
					cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(session.getAESKEY(), "AES"), new IvParameterSpec(session.getAESIV()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			/**
			 * Decrypt array from input offset with a length of inputlen and puts it in output at outputoffsest
			 * @param array
			 * @param inputOffset
			 * @param inputLen
			 * @param output
			 * @param outputOffset
			 * @return
			 */
			private int decryptAES(byte[] array, int inputOffset, int inputLen, byte[] output, int outputOffset){
				try{
			        return cipher.update(array, inputOffset, inputLen, output, outputOffset);
				}catch(Exception e){
					e.printStackTrace();
				}
				
				return -1;
			}
			@Override
			public void run() {
				// TODO reduce packet buffer
				byte[] packet = new byte[2048];
				final ByteBuffer[] inputBuffers = decoder.getInputBuffers();
				final PureAudioBuffer.BufferInfo bufferInfo = new PureAudioBuffer.BufferInfo();
				try {
					do {
						int bufferIndex = decoder.dequeueInputBuffer(1000);
						if (bufferIndex != -1) {
							do {
								ByteBuffer data = pureAudioBuffer.getNextBuffer(bufferInfo);
								if (data != null && data.position() > 0) {
									int i = 0;
									initAES();
									for (i=0; i+16<=data.position(); i += 16){
										// Decrypt
										decryptAES(data.array(), i, 16, packet, i);
									}							    
									// The rest of the packet is unencrypted
									for (int k = 0; k<(data.position() % 16); k++){
										packet[i+k] = data.array()[i+k];
									}

									inputBuffers[bufferIndex].clear();
									inputBuffers[bufferIndex].put(packet, 0, data.position());
									inputBuffers[bufferIndex].rewind();
									long timestamp = convertRtpTimestampToNtp(bufferInfo.timestamp);
									if (playbackClock.waitUntilTime(timestamp)) {
									}
									decoder.queueInputBuffer(bufferIndex, 0, data.position(), bufferInfo.timestamp, 0);
									break;
								} else {
									Log.w(TAG, "getNextBuffer no data");
								}
							} while (!decoderThreadShouldStop);
						} else {
							debugLog("dequeueInputBuffer:-1 buffer under-run ");						
						}
					} while (!decoderThreadShouldStop);
				} catch (IllegalStateException e) {
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				} catch (Exception e) {
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				} catch (Error e) {
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				}
				Log.d(TAG, Thread.currentThread().getName() + " ends");
			}
			
		});
		decoderThread.setName("AAC-ELD decoder");
		decoderThread.start();
	}
	private void spawnPlayerThread() {
		int minBufferSize = AudioTrack.getMinBufferSize(SAMPLE_RATE_IN_HZ, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);
		track = new AudioTrack(AudioManager.STREAM_MUSIC,
				SAMPLE_RATE_IN_HZ,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				minBufferSize,
				AudioTrack.MODE_STREAM);
		playerThreadShouldStop = false;
		playerThread = new Thread(new Runnable() {


			@Override
			public void run() {
				
				track.play();
				try {
					ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
					final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
					final byte[] packet = new byte[2048];
					while (!playerThreadShouldStop) {
						int outputBufferIndex = -1;
						try {
							outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 500000);
						} catch(Exception e) {
							Log.e(TAG, "dequeueOutputBuffer:"+e.getClass() + android.util.Log.getStackTraceString(e));
							break;
						} catch(Error e) {	
							android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
						} finally {

						}
						//					Log.d(TAG, "dequeueOutputBuffer :"+outputBufferIndex);
						if (outputBufferIndex >= 0) {		
							long timestamp = convertRtpTimestampToNtp(bufferInfo.presentationTimeUs);
							if (timestamp != 0) { // timestamp convert is ready
								if (playbackClock == null) {
									playbackClock = new SimplePlaybackClock(timestamp, 200, TAG);
								}
								if (playbackClock.waitUntilTime(timestamp)) {
									ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
									outputBuffer.position(bufferInfo.offset);
									outputBuffer.get(packet, 0, bufferInfo.size);
									track.write(packet, 0, bufferInfo.size);
								}
							}
							decoder.releaseOutputBuffer(outputBufferIndex, false);
						} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
							outputBuffers = decoder.getOutputBuffers();

						} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
							MediaFormat outputFormat = decoder.getOutputFormat();
							Log.d(TAG, "outputFormat :"+outputFormat);

						} else if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
							debugLog("dequeueOutputBuffer:-1 buffer under-run ");						
						}
					}
				} catch (IllegalStateException e) {
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				} catch(Exception e) {
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				} catch(Error e) {	
					android.util.Log.e(TAG, Thread.currentThread().getName() + android.util.Log.getStackTraceString(e));
				}	
				track.stop();
				track.release();
				Log.d(TAG, Thread.currentThread().getName() + " ends");
			}
			
		});
		playerThread.setName("AAC-ELD player");
		playerThread.start();
	}
	public void stop() {
		try {
			if (debugFile != null) {
				debugFile.close();
			}
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		if (playbackClock != null) {
			playbackClock.release();
		}
		if (controlPortListener != null) {
			controlPortListener.stopThread();
		}
		if (udpListener != null) {
			udpListener.stopThread();
		}		
		if (sock != null) {
			sock.close();
		}
		if (csock != null) {
			csock.close();
		}
		if (decoderThread != null) {
			decoderThreadShouldStop = true;
			ThreadUtils.stopThreadSafely(decoderThread);
			decoderThread = null;
		}
		if (playerThread != null) {
			playerThreadShouldStop = true;
			ThreadUtils.stopThreadSafely(playerThread);
			playerThread = null;
		}
		if (decoder != null) {
			decoder.stop();
			decoder.release();
			decoder = null;
		}
	}
	
	public int getServerPort() {
		return sock.getLocalPort();
	}
	
	private void initRTP(final AudioSession session) {
		try {
			playbackClock = new AirTunesClock(session.getAddress(), session.getTimingPort(), 200, 50);

			sock = new DatagramSocket();
			csock = new DatagramSocket();
			udpListener = new UDPListener(sock, new UDPDelegate() {
				@Override
				public void packetReceived(DatagramSocket socket,
						DatagramPacket packet) {

					//					Real-Time Transport Protocol
					//				    10.. .... = Version: RFC 1889 Version (2)
					//				    ..0. .... = Padding: False
					//				    ...0 .... = Extension: False
					//				    .... 0000 = Contributing source identifiers count: 0
					//				    1... .... = Marker: True
					//				    Payload type: DynamicRTP-Type-96 (96)
					//				    Sequence number: 45457
					//				    Timestamp: 4151908034
					//				    Synchronization Source identifier: 0xe8bb6b2c (3904596780)
					//				    Payload: bb5c8e51aa7cd29600c3fd60ebae6e413138feae909b44f1...

					ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData());
					packetBuffer.order(ByteOrder.BIG_ENDIAN);
					packetBuffer.position(1);
					byte type = (byte) (packetBuffer.get()&~0x80);
					if (type == 0x60 || type == 0x56) { 
						int extraOffset = 0;
						if(type==0x56){
							extraOffset = 4;
						}					
						int seqno = (packetBuffer.getShort() & 0xffff);
						long timestamp = (packetBuffer.getInt() & 0xffffffffL);
						packetBuffer.position(12 + extraOffset);
						int payloadSize = packet.getLength() - extraOffset - 12;
						byte[] pktp = new byte[payloadSize];
						packetBuffer.get(pktp, 0, payloadSize);
						debugLog("server port:"+"receive packet seqno:"+seqno);
						pureAudioBuffer.putPacketInBuffer(seqno, pktp, timestamp);
						debugLog("server port:"+" timestamp:"+timestamp + ", correlated timestamp:"+convertRtpTimestampToNtp(timestamp));
					} else {
						debugLog("server port: unhandled control packet type:"+type);
					}
				}

			});
//			try {
//				debugFile = new File("/sdcard/rtp.send");
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			controlPortListener = new UDPListener(csock, new UDPDelegate() {

				@Override
				public void packetReceived(DatagramSocket socket,
						DatagramPacket packet) {					
					ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData());
					packetBuffer.order(ByteOrder.BIG_ENDIAN);
					packetBuffer.position(1);
					byte type = (byte) (packetBuffer.get()&~0x80);
					debugLog("control port: data type:"+type);
					if (type == 84) { //Sync packets
						packetBuffer.position(4);						
						long timestamp = (packetBuffer.getInt() & 0xffffffffL);
						long ntpTime = packetBuffer.getLong();
						long nextTimestamp = (packetBuffer.getInt() & 0xffffffffL);
						synchronized(AudioPlayer.this) {
							rtpTimestamp = timestamp;
							ntpTimestamp = TimeStamp.getTime(ntpTime);
						}
						debugLog("control port: timestamp:"+timestamp+", ntpTime:"+ntpTimestamp+", nextTimestamp:"+nextTimestamp);
					} else if (type == 86) {
						packetBuffer.position(6);						
						int seqno = (packetBuffer.getShort() & 0xffff);
						long timestamp = (packetBuffer.getInt() & 0xffffffffL);
						int payloadSize = packet.getLength() - 16;
						if (payloadSize > 0) {
							byte[] pktp = new byte[payloadSize];
							packetBuffer.position(16);						
							packetBuffer.get(pktp, 0, payloadSize);
							debugLog("control port: retransmit reply: seqno:"+seqno+", timestamp:"+timestamp+", remaining:"+packetBuffer.remaining());
							pureAudioBuffer.putPacketInBuffer(seqno, pktp, timestamp);
						} else {
							Log.w(TAG, "control port: wrong payload size:"+payloadSize);
						}
					} else {
						Log.w(TAG, "control port: unhandled control packet type:"+type);
					}
				}
				
			});
		} catch (SocketException e) {
			e.printStackTrace();
		}


	}
	public void flush(){
		if (pureAudioBuffer != null) {
			pureAudioBuffer.flush();
		}
	}
	@Override
	public void setVolume(double d) {
		if (track != null) {
			track.setStereoVolume((float)d, (float)d);			
		}
	}
	private void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG, msg);
		}
	}
	@Override
	public int getControlPort() {
		return csock.getLocalPort();
	}
	private synchronized long convertRtpTimestampToNtp(long timestamp) {
		if (ntpTimestamp == 0 && rtpTimestamp == 0) {
			return 0;
		}
		return ntpTimestamp + (timestamp - rtpTimestamp)*1000/SAMPLE_RATE_IN_HZ;
	}
}
