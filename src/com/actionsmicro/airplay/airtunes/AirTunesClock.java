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

import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.ThreadUtils;

public class AirTunesClock extends AirPlayPlaybackClockBase {
	static final String TAG = "AirTunesClock";
	static final boolean DEBUG_LOG = false;
	private DatagramSocket tsock;
	private UDPListener timingPortListener;
	private Thread timingThread;
	public AirTunesClock(final InetAddress ntpServer, final int ntpPort, long latencyTolerance) throws SocketException {
		super(latencyTolerance, DEBUG_LOG, TAG);
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
						updateSyncInfo((returnTime - originateTime) - (transmitTime - receiveTime), ((receiveTime - originateTime) + (transmitTime - returnTime))/2);
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
						break;
					} finally {
						
					}
				}
				debugLog(Thread.currentThread().getName() + " Thread ends");
			}
			
		});
		timingThread.setName("RTP-Timing");
		timingThread.start();
	}
	@Override
	public void release() {
		timingPortListener.stopThread();		
		ThreadUtils.stopThreadSafely(timingThread);
		timingThread = null;
		tsock.close();
	}
}
