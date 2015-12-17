package com.actionsmicro.airplay.mirror;

import com.actionsmicro.airplay.clock.AirPlayPlaybackClockBase;
import com.actionsmicro.airplay.clock.PlaybackClock;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.ThreadUtils;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.NtpV3Packet;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class MirrorClock extends AirPlayPlaybackClockBase implements PlaybackClock {
	private static final String TAG = "MirrorClock";
	private static final boolean DEBUG_LOG = false;
	private Thread timingThread;
	public MirrorClock(final InetAddress ntpServer, final int ntpPort, long latencyTolerance, int exceptionLimit) throws SocketException {
		super(latencyTolerance, exceptionLimit, DEBUG_LOG, TAG);
		timingThread = new Thread(new Runnable() {				 
			@Override
			public void run() {
				NTPUDPClient ntpClient = new NTPUDPClient();
				ntpClient.setDefaultTimeout(5000);
				try {
					ntpClient.open();
					while (!Thread.currentThread().isInterrupted()) {
						try {
							TimeInfo info = ntpClient.getTime(ntpServer, ntpPort);
							info.computeDetails();
							updateSyncInfo(info.getDelay(), info.getOffset());
							Thread.sleep(3000);
						} catch (InterruptedException e) {
							break;
						} catch (SocketTimeoutException e) {
							e.printStackTrace();
							continue;
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
					debugLog(Thread.currentThread().getName() + " Thread ends");
				}

			}
			
		});
		timingThread.setName("Mirror-Timing");
		timingThread.start();
	}
	@Override
	public void release() {
		ThreadUtils.stopThreadSafely(timingThread);
		timingThread = null;
	}
}
