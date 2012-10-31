package com.actionsmicro.pigeon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.util.Log;

import com.actionsmicro.pigeon.MediaStreaming;
import com.actionsmicro.pigeon.MediaStreaming.DataSource;

public class MediaStreamingFileDataSource implements DataSource {
	private static final String TAG = null;
	private File mediaFile;
	private MediaStreaming mediaStreaming;
	private RandomAccessFile mediaFileInput;
	private boolean shouldReadFile;
	private Thread workerThread;
	public MediaStreamingFileDataSource(File mediaFile) throws FileNotFoundException {
		this.mediaFile = mediaFile;
		mediaFileInput = new RandomAccessFile(mediaFile, "r");
		
	}
	@Override
	public long getContentLength() {
		Log.d(TAG, "getContentLength:" + mediaFile.length());
		return mediaFile.length();
	}

	@Override
	public boolean isSeekable() {
		return true;
	}

	@Override
	public void mediaStreamingDidFail(int resultCode) {
		Log.e(TAG, "mediaStreamingDidFail:" + resultCode);
	}

	@Override
	public void startStreamingContents(MediaStreaming ms, final long offset) {
		stopWorkerThread();
		mediaStreaming = ms;
		shouldReadFile = true;
		workerThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					if (offset != 0) {
						mediaFileInput.seek(offset);
					}
					final int bufferSize = 1024*32;
					final byte[] buffer = new byte [bufferSize];
					int sizeRead = 0;
					do {
						sizeRead = mediaFileInput.read(buffer);
						if (sizeRead == -1) {
							mediaStreaming.sendEofPacket();
							shouldReadFile = false;
						} else {
							Log.d(TAG, "sendStreamingContents:" + sizeRead);
							mediaStreaming.sendStreamingContents(buffer, sizeRead);
						}
					} while(shouldReadFile);
					
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
		});
		workerThread.start();
	}

	@Override
	public void pauseStreamingContents() {
		stopWorkerThread();
	}
	private void stopWorkerThread() {
		if (workerThread != null) {
			shouldReadFile = false;
			try {
				workerThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			workerThread = null;
		}
	}

	@Override
	public void stopStreamingContents() {
		stopWorkerThread();
	}

}
