package com.actionsmicro.airplay.airtunes;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.apache.commons.net.ntp.TimeStamp;

import vavi.apps.shairport.UDPDelegate;
import vavi.apps.shairport.UDPListener;

import com.actionsmicro.airplay.clock.PlaybackClock;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.ThreadUtils;

public class AirTunesClock implements PlaybackClock {
	private static final String TAG = "AirTunesClock";
	private static final boolean DEBUG_LOG = false;
	private static final long EARLY_TOLERANCE = 10; //ms

	private DatagramSocket tsock;
	private UDPListener timingPortListener;
	private Thread timingThread;
	protected long roundTripDelay;
	protected long clockOffset;
	private long latencyTolerance;

	public AirTunesClock(final InetAddress ntpServer, final int ntpPort, long latencyTolerance) throws SocketException {
		this.latencyTolerance = latencyTolerance;
		tsock = new DatagramSocket();
		timingPortListener = new UDPListener(tsock, new UDPDelegate() {

			@Override
			public void packetReceived(DatagramSocket socket,
					DatagramPacket packet) {
				
				
				ByteBuffer packetBuffer = ByteBuffer.wrap(packet.getData());
				packetBuffer.order(ByteOrder.BIG_ENDIAN);
				packetBuffer.position(1);
				byte type = (byte) (packetBuffer.get()&~0x80);
				debugLog("timing port: data type:"+type);
				if (type == 83) { //Timing packets
					packetBuffer.position(4);						
					long timestamp = (packetBuffer.getInt() & 0xffffffffL);
					long originateNtpTime = packetBuffer.getLong();
					long receiveNtpTime = packetBuffer.getLong();
					long transmitNtpTime = packetBuffer.getLong();
					debugLog("timing port: originalNtpTime.ntpValue:"+originateNtpTime);
					long originateTime = TimeStamp.getTime(originateNtpTime);
					long receiveTime = TimeStamp.getTime(receiveNtpTime);
					long transmitTime = TimeStamp.getTime(transmitNtpTime);
					long returnTime = now();
					debugLog("timing port: timestamp:"+timestamp+ ", originateTime:"+originateTime+", receiveTime:"+receiveTime+", transmitTime:"+transmitTime+", returnTime:"+returnTime);
					synchronized (AirTunesClock.this) {
						roundTripDelay = (returnTime - originateTime) - (transmitTime - receiveTime);
						clockOffset = ((receiveTime - originateTime) + (transmitTime - returnTime))/2;
						debugLog("timing port: roundTripDelay:"+roundTripDelay+", offset:"+clockOffset);						
					}
				} else {
					Log.w(TAG, "timing port: unhandled control packet type:"+type);
				}
			}
			
		});
		timingThread = new Thread(new Runnable() {				 
			@Override
			public void run() {
				while (!Thread.currentThread().isInterrupted()) {
					try {
						ByteBuffer timingPacket = ByteBuffer.allocate(32);
						timingPacket.order(ByteOrder.BIG_ENDIAN);
						timingPacket.put((byte)0x80);
						timingPacket.put((byte)0xd2);
						timingPacket.putShort((short) 0);
						timingPacket.putInt(0);;
						timingPacket.putLong(0);
						timingPacket.putLong(0);
						TimeStamp currentTime = TimeStamp.getCurrentTime();
						timingPacket.putLong(currentTime.ntpValue());
						debugLog("timing port: currentNtpTime:"+currentTime.getTime());
						tsock.send(new DatagramPacket(timingPacket.array(), timingPacket.capacity(), ntpServer, ntpPort));
				    	Thread.sleep(3000);
					} catch (IOException e) {
						e.printStackTrace();
						break;
					} catch (InterruptedException e) {
						e.printStackTrace();
						break;
					} finally {
						
					}
				}
			}
			
		});
		timingThread.setName("RTP-Timing");
		timingThread.start();
	}
	@Override
	public boolean waitUntilTime(long presentationTime) {
		presentationTime = presentationTime - clockOffset;
		if (presentationTime < now()) {
			if ((now() - presentationTime) > latencyTolerance) {
				debugLogW("presentationTime:"+presentationTime+" is too late for "+(now() - presentationTime)+"ms");
				return false;
			}
		} else if ((presentationTime - now()) > EARLY_TOLERANCE) {
			while (presentationTime > now() && !Thread.currentThread().isInterrupted()) {
				try {
					long wait = presentationTime - now();
					debugLogW("presentationTime:"+presentationTime+" is too early for "+wait+"ms. let's wait");
					if (wait <= 0) {
						break;
					}
					Thread.sleep(wait);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return false;
				}
			}
		}
		debugLog("presentationTime:"+presentationTime+" is good to go");
		
		return true;
	}
	private long now() {
		return TimeStamp.getCurrentTime().getTime();
	}
	@Override
	public void release() {
		timingPortListener.stopThread();		
		ThreadUtils.stopThreadSafely(timingThread);
		timingThread = null;
		tsock.close();
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
}
