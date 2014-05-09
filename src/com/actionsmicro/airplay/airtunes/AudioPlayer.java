package com.actionsmicro.airplay.airtunes;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

import com.actionsmicro.airplay.clock.SimplePlaybackClock;
import com.actionsmicro.utils.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioPlayer implements vavi.apps.shairport.AudioPlayer {
	private static final String TAG = "AudioPlayer";
	private static final boolean DEBUG_LOG = false;
	private DatagramSocket sock;
	private DatagramSocket csock;
	private UDPListener udpListener;
	private PureAudioBuffer pureAudioBuffer;
	private InetAddress rtpClient;
	private Thread decoderThread;
	private MediaCodec decoder;
	private Thread playerThread;
	private boolean decoderThreadShouldStop;
	private boolean playerThreadShouldStop;
	private AudioTrack track;

	public AudioPlayer(final AudioSession session) {
		this.pureAudioBuffer = new PureAudioBuffer(new PureAudioBuffer.ResendDelegate() {

			@Override
			public void request_resend(int begin, int end) {
				Log.i(TAG, "Resend Request: " + begin + "::" + end);
//				if (end < begin) {
//					return;
//				}
//				
//				int len = end - begin + 1;
//			    byte[] request = new byte[] { (byte) 0x80, (byte) (0x55|0x80), 0x01, 0x00, (byte) ((begin & 0xFF00) >> 8), (byte) (begin & 0xFF), (byte) ((len & 0xFF00) >> 8), (byte) (len & 0xFF)};
//
//			    try {
//			    	DatagramPacket temp = new DatagramPacket(request, request.length, rtpClient, session.getControlPort());
//			    	csock.send(temp);
//
//			    } catch (IOException e) {
//			    	e.printStackTrace();
//			    }
			}
			
		});
    	initDecoder();
		spawnDecoderThread(session);
		spawnPlayerThread();
		initRTP();		
	}
	private void initDecoder() {
		MediaCodec mediaCodec = MediaCodec.createByCodecName("OMX.google.aac.decoder");//MediaCodec.createDecoderByType("audio/mp4a-latm");
    	MediaFormat mediaFormat = MediaFormat.createAudioFormat("audio/mp4a-latm", 0, 0);
    	byte[] bytes = new byte[]{(byte) 0xF8, (byte) 0xE8, 0x50, 0x00};
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        mediaFormat.setByteBuffer("csd-0", bb);
    	mediaCodec.configure(mediaFormat, null, null, 0);
    	decoder = mediaCodec;
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
				do {
					int bufferIndex = decoder.dequeueInputBuffer(10000);
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
				Log.d(TAG, decoderThread.getName() + " ends");
			}
			
		});
		decoderThread.setName("AAC-ELD decoder");
		decoderThread.start();
	}
	private void spawnPlayerThread() {
		track = new AudioTrack(AudioManager.STREAM_MUSIC,
				44100,
				AudioFormat.CHANNEL_OUT_STEREO,
				AudioFormat.ENCODING_PCM_16BIT,
				44100 * 2 * 4,
				AudioTrack.MODE_STREAM);
		playerThreadShouldStop = false;
		playerThread = new Thread(new Runnable() {

			private SimplePlaybackClock playbackClock;

			@Override
			public void run() {
				
				track.play();
				ByteBuffer[] outputBuffers = decoder.getOutputBuffers();
				final MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
				final byte[] packet = new byte[2048];
				while (!playerThreadShouldStop) {
					int outputBufferIndex = -1;
					try {
						outputBufferIndex = decoder.dequeueOutputBuffer(bufferInfo, 10000);
					} catch(Exception e) {
						Log.e(TAG, "dequeueOutputBuffer:"+e.getClass());
						break;
					} finally {
					
					}
//					Log.d(TAG, "dequeueOutputBuffer :"+outputBufferIndex);
					if (outputBufferIndex >= 0) {		
						if (playbackClock == null) {
							playbackClock = new SimplePlaybackClock(bufferInfo.presentationTimeUs*1000/44100, 200, TAG);
						}
						if (playbackClock.waitUntilTime(bufferInfo.presentationTimeUs*1000/44100)) {
							ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
							outputBuffer.position(bufferInfo.offset);
							outputBuffer.get(packet, 0, bufferInfo.size);
							track.write(packet, 0, bufferInfo.size);
						}
						decoder.releaseOutputBuffer(outputBufferIndex, false);
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
						outputBuffers = decoder.getOutputBuffers();
						
					} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
						MediaFormat outputFormat = decoder.getOutputFormat();
						Log.d(TAG, "outputFormat :"+outputFormat);
						
					}
				}
				track.stop();
				track.release();
				Log.d(TAG, playerThread.getName() + " ends");
			}
			
		});
		playerThread.setName("AAC-ELD player");
		playerThread.start();
	}
	public void stop() {
		udpListener.stopThread();
		sock.close();
		csock.close();

		if (decoderThread != null) {
			decoderThreadShouldStop = true;
			decoderThread.interrupt();
			try {
				decoderThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (playerThread != null) {
			playerThreadShouldStop = true;
			playerThread.interrupt();
			try {
				playerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		if (decoder != null) {
			decoder.stop();
		}
	}
	public int getServerPort() {
		return sock.getLocalPort();
	}
	
	private void initRTP() {
		try {
			sock = new DatagramSocket();
			csock = new DatagramSocket();
			udpListener = new UDPListener(sock, new UDPDelegate() {
				@Override
				public void packetReceived(DatagramSocket socket,
						DatagramPacket packet) {
					rtpClient = packet.getAddress();

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
					int type = packetBuffer.get()&~0x80;
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
						pureAudioBuffer.putPacketInBuffer(seqno, pktp, timestamp);
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
	
}
