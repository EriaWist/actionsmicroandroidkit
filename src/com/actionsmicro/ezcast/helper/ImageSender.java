package com.actionsmicro.ezcast.helper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;

import com.actionsmicro.ezcast.DisplayApi;
import com.actionsmicro.pigeon.Client.BitmapManager;
import com.actionsmicro.utils.Log;


public class ImageSender {
	private static class Job {
		public Job() {}
		public Job(Bitmap bitmap2) {
			this.bitmap = bitmap2;
		}
		public static final Job nullJob = new Job();
		public Bitmap bitmap;;
		
	}
	protected static final String TAG = "ImageSender";
	private ArrayBlockingQueue<Job> pendingJobs = new ArrayBlockingQueue<Job>(1);
	private boolean shouldStop;
	private BitmapManager bitmapManager;
	private DisplayApi proxy;
	private ByteArrayOutputStream compressionBuffer;
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
	public ImageSender(DisplayApi proxy, BitmapManager bitmapManager) {
		this.bitmapManager = bitmapManager;
		this.proxy = proxy;
		sendingThread.start();
	}
	public void sendImage(Bitmap bitmap) {
		final ArrayList<Job> expiredJobs = new ArrayList<Job>();
		pendingJobs.drainTo(expiredJobs);
		pendingJobs.add(new Job(bitmap));		
	}
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
						if (bitmapManager != null && proxy != null && bitmapManager.onProcessBitmapBegin(null, job.bitmap)) {
							getCompressionBuffer().reset();
							job.bitmap.compress(CompressFormat.JPEG, 70, getCompressionBuffer());
							bitmapManager.onProcessBitmapEnd(null, job.bitmap);
							Log.d(TAG, "jpeg size:" + getCompressionBuffer().size());
							proxy.sendJpegEncodedScreenData(new ByteArrayInputStream(getCompressionBuffer().toByteArray()), getCompressionBuffer().size());
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
		
	}
}
