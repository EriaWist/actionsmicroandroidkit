package com.actionsmicro.androidkit.ezcast.helper;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.actionsmicro.androidkit.ezcast.DisplayApi;
import com.actionsmicro.utils.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Helper class to send image(Bitmap) to the device.
 * This class is not part of this SDK officially yet, please use it at your own risk.
 * 
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public class ImageSender {
	private static class Job {
		public Job() {}
		public Job(Bitmap bitmap2) {
			this.bitmap = bitmap2;
		}
		public static final Job nullJob = new Job();
		public Bitmap bitmap;;
		
	}
	/**
	 * BitmapManager defines interface for asynchronous mode clients to manage buffer life-cycle.
	 *
	 * @see ImageSender#sendImage(Bitmap)
	 */
	public interface BitmapManager {
		public boolean onProcessBitmapBegin(Bitmap bitmap);
		public void onProcessBitmapEnd(Bitmap bitmap);
		public boolean shouldSendImage();
	}
	protected static final String TAG = "ImageSender";
	private ArrayBlockingQueue<Job> pendingJobs = new ArrayBlockingQueue<Job>(1);
	private boolean shouldStop;
	private BitmapManager bitmapManager;
	private DisplayApi displayApi;
	private ByteArrayOutputStream compressionBuffer;
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
	/**
	 * Create an ImageSender.
	 * @param displayApi The {@link DisplayApi} object.
	 * @param bitmapManager The {@link BitmapManager} 
	 */
	public ImageSender(DisplayApi displayApi, BitmapManager bitmapManager) {
		this.bitmapManager = bitmapManager;
		this.displayApi = displayApi;
		sendingThread.start();
	}
	/**
	 * Send bitmap to the device.
	 * @param bitmap
	 */
	public void sendImage(Bitmap bitmap) {
		final ArrayList<Job> expiredJobs = new ArrayList<Job>();
		pendingJobs.drainTo(expiredJobs);
		pendingJobs.add(new Job(bitmap));		
	}
	/**
	 * Stop sending and release resources.
	 */
	public void stop() {
		shouldStop = true;
		// add null job to trigger stop
		final ArrayList<Job> expiredJobs = new ArrayList<Job>();
		pendingJobs.drainTo(expiredJobs);
		pendingJobs.add(Job.nullJob);
		try {
			sendingThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private final Thread sendingThread = new Thread(new Runnable() {
		
		@Override
		public void run() {
			Job job = null;
			while (!shouldStop) {
//				Log.d(TAG, "waiting for job");
				try {
					job = pendingJobs.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					shouldStop = true;
				}
				try {
					if (null != job && null != job.bitmap) {
						Log.d(TAG, "job comes in");
						if (bitmapManager != null && displayApi != null && bitmapManager.onProcessBitmapBegin(job.bitmap)) {
							getCompressionBuffer().reset();
							job.bitmap.compress(CompressFormat.JPEG, 70, getCompressionBuffer());
							bitmapManager.onProcessBitmapEnd(job.bitmap);
							Log.d(TAG, "jpeg size:" + getCompressionBuffer().size());
							if (bitmapManager.shouldSendImage()) {
								displayApi.sendJpegEncodedScreenData(new ByteArrayInputStream(getCompressionBuffer().toByteArray()), getCompressionBuffer().size());
							}

						}
					} else if (!shouldStop) {
					}
				} catch (Exception e) {
					handleException(e);
					shouldStop = true;
				}
			}
		}
	});

	protected void handleException(Exception e) {
		// TODO Auto-generated method stub
		Log.e(TAG, "", e);
	}
}
