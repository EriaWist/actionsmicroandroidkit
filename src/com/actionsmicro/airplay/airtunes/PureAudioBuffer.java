package com.actionsmicro.airplay.airtunes;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.util.Log;

public class PureAudioBuffer {

	private static final int START_FILL = 0;		
	private static final int BUFFER_FRAMES = 512;	// Total buffer size (number of frame)
	private static final String TAG = "PureAudioBuffer";
	private static final boolean DEBUG_LOG = false;
	
	private ResendDelegate server;
	private final Lock lock = new ReentrantLock();    
	private boolean synced = false;
	private int readIndex;							
	private int writeIndex;
	private int actualBufSize;						// The number of packet in buffer
	private AudioData[] audioBuffer;
	private boolean decoder_isStopped = false;		//The decoder stops 'cause the isn't enough packet. Waits till buffer is ok
	public interface ResendDelegate {

		void request_resend(int begin, int end);
		
	}
	public class AudioData {
		public boolean ready;
		public ByteBuffer data;
		public long timestamp;
	}
	public static class BufferInfo {
		public BufferInfo() {
			
		}
		public long timestamp;
	}
	public PureAudioBuffer(ResendDelegate resendDelegate) {
		server = resendDelegate;
		audioBuffer = new AudioData[BUFFER_FRAMES];
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i] = new AudioData();
			audioBuffer[i].data = ByteBuffer.allocate(2048);	
		}
	}
	public void putPacketInBuffer(int seqno, byte[] data, long timestamp){
	    // Ring buffer may be implemented in a Hashtable in java (simplier), but is it fast enough?		
		// We lock the thread
		synchronized(lock){
		
			if (!synced) {
				writeIndex = seqno;
				readIndex = seqno;
				synced = true;
			}
	
			if (seqno == writeIndex) {													// Packet we expected
				updateBuffer(seqno, data, timestamp);
				writeIndex++;
			} else if(seqno > writeIndex) {										// Too early, did we miss some packet between writeIndex and seqno?
				if (seqno - writeIndex > 65000 && seqno >= readIndex) {
					// it's too late? we assume it's rolled over situation
					if (!audioBuffer[(seqno % BUFFER_FRAMES)].ready) {
						debugLog("readIndex < seqno not yet played but too late. Still ok");
						updateBuffer(seqno, data, timestamp);
					}
				} else {
					if ((seqno - readIndex) > 5) { // enough time gap to receive retransmit replies
						server.request_resend(writeIndex, seqno - 1);
					} else {
						debugLogW("not enough time to request resend; seqno:"+seqno);
					}
					if (!audioBuffer[(seqno % BUFFER_FRAMES)].ready) {
						updateBuffer(seqno, data, timestamp);
						writeIndex = seqno + 1;
					}
				}
			} else if(seqno >= readIndex) {												// readIndex < seqno < writeIndex not yet played but too late. Still ok
				if (!audioBuffer[(seqno % BUFFER_FRAMES)].ready) {
					debugLog("readIndex < seqno < writeIndex not yet played but too late. Still ok");
					updateBuffer(seqno, data, timestamp);
				}
			} else {
				if (!audioBuffer[(seqno % BUFFER_FRAMES)].ready) {
					debugLogW("Late packet with seq. numb.: " + seqno + ", readIndex:" + readIndex +", distance:"+(readIndex-seqno));			// Really to late
				}
			}
			debugLog("writeIndex:"+writeIndex);
			updateActualBufferSize();
		    
		    if (decoder_isStopped && actualBufSize > START_FILL) {
			    lock.notify();
		    }
		    
		    // SEQNO is stored in a short an come back to 0 when equal to 65536 (2 bytes)
		    if (writeIndex == 65536){
		    	writeIndex = 0;
		    }
		}
	}
	private void updateBuffer(int seqno, byte[] data, long timestamp) {
		audioBuffer[(seqno % BUFFER_FRAMES)].data.position(0);
		audioBuffer[(seqno % BUFFER_FRAMES)].data.put(data);
		audioBuffer[(seqno % BUFFER_FRAMES)].ready = true;
		audioBuffer[(seqno % BUFFER_FRAMES)].timestamp = timestamp;
	}
	public void flush(){
		for (int i = 0; i< BUFFER_FRAMES; i++){
			audioBuffer[i].ready = false;
			synced = false;
		}
	}
	private void debugLog(String msg) {
		if (DEBUG_LOG) {
			Log.d(TAG, msg);
		}
	}
	private void debugLogW(String msg) {
		if (DEBUG_LOG) {
			Log.w(TAG, msg);
		}
	}
	public ByteBuffer getNextBuffer(BufferInfo bufferInfo){
	    synchronized (lock) {	    	
			updateActualBufferSize();
		    
			if(actualBufSize<1 || !synced){			// If no packets more or Not synced (flush: pause)
				if(synced){							// If it' because there is not enough packets
					debugLogW("Underrun!!! Not enough frames in buffer!");
				}
				
				try {
					// We say the decoder is stopped and we wait for signal
					debugLog("Waiting");
					decoder_isStopped = true;
				    lock.wait();
				    decoder_isStopped = false;
				    debugLog("re-starting");					

					updateActualBufferSize();
				    
				} catch (InterruptedException e) {
					e.printStackTrace();
					return null;
				}				
			}
			
			// Overrunning. Restart at a sane distance
		    if (actualBufSize >= BUFFER_FRAMES) {   // overrunning! uh-oh. restart at a sane distance
		    	debugLogW("Overrun!!! Too much frames in buffer!");
		        readIndex = writeIndex - START_FILL;
		        if (readIndex < 0) {
		        	readIndex += 65536;
		        }
		    }
			// we get the value before the unlock ;-)
		    int read = readIndex;
		    readIndex++;
		     
		    updateActualBufferSize();
		    
		    debugLog("Read index:"+read);
			
		    AudioData buf = audioBuffer[read % BUFFER_FRAMES];
		    
		    if(!buf.ready){
		    	debugLogW("Missing Frame! index:"+read);
		    	// Set to zero then
		    	buf.data.clear();
		    }
		    buf.ready = false;
		    
		    // SEQNO is stored in a short an come back to 0 when equal to 65536 (2 bytes)
		    if(readIndex == 65536){
		    	readIndex = 0;
		    }
		    bufferInfo.timestamp = buf.timestamp;
			return buf.data;

		}
	}
	private void updateActualBufferSize() {
		actualBufSize = writeIndex-readIndex;	// Packets in buffer
		if(actualBufSize<0){	// If loop
			actualBufSize = 65536-readIndex+ writeIndex;
		}
	}
}
