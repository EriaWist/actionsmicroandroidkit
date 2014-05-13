package com.actionsmicro.airplay.mirror;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.apache.commons.net.ntp.TimeStamp;

import com.actionsmicro.airplay.clock.PlaybackClock;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.ThreadUtils;

public class MirrorClock implements PlaybackClock {
	private static final String TAG = "MirrorClock";
	private static final boolean DEBUG_LOG = false;
	private static final long EARLY_TOLERANCE = 10; //ms
	private Thread timingThread;
	private long latencyTolerance;
	protected long roundTripDelay;
	protected long clockOffset;

	public MirrorClock(final InetAddress ntpServer, final int ntpPort, long latencyTolerance) throws SocketException {
		this.latencyTolerance = latencyTolerance;
		
		timingThread = new Thread(new Runnable() {				 
			@Override
			public void run() {
				NTPUDPClient ntpClient = new NTPUDPClient();
				ntpClient.setDefaultTimeout(10000);
				try {
					ntpClient.open();
					while (!Thread.currentThread().isInterrupted()) {
						try {
							TimeInfo info = ntpClient.getTime(ntpServer, ntpPort);
							info.computeDetails();
							synchronized (MirrorClock.this) {								
								roundTripDelay = info.getDelay();
								clockOffset = info.getOffset();
							}
							debugLog("mirror timing port: roundTripDelay:"+roundTripDelay+", offset:"+clockOffset);						

							Thread.sleep(3000);
						} catch (InterruptedException e) {
							e.printStackTrace();
							break;
						} catch (IOException e) {
							e.printStackTrace();
							break;
						} finally {

						}
					}
				} catch (SocketException e1) {
					e1.printStackTrace();
				} finally {
					ntpClient.close();
					debugLog("Mirror-Timing Thread ends");
				}

			}
			
		});
		timingThread.setName("Mirror-Timing");
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
		ThreadUtils.stopThreadSafely(timingThread);
		timingThread = null;
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
