package com.actionsmicro.pigeon;

import java.io.IOException;

import com.actionsmicro.pigeon.MediaStreaming.FileDataSource;
import com.actionsmicro.utils.Log;

public abstract class MediaStreamingFileBaseDataSource implements FileDataSource {

	private static final String TAG = "MediaStreamingFileBaseDataSource";
	private MediaStreaming mediaStreaming;
	private boolean shouldReadFile;
	private Thread workerThread;
	private MediaStreamingStateListener mediaStreamingStateListener;

	public MediaStreamingFileBaseDataSource() {
		super();
	}

	@Override
	public boolean isSeekable() {
		return true;
	}

	@Override
	public void mediaStreamingDidFail(int resultCode) {
		Log.e(TAG, "mediaStreamingDidFail:" + resultCode);
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingFail(this, resultCode);
		}
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
					seekTo(offset);
					final int bufferSize = 1024*32;
					final byte[] buffer = new byte [bufferSize];
					int sizeRead = 0;
					do {
						sizeRead = read(buffer);
						if (sizeRead == -1) {
							mediaStreaming.sendEofPacket();
							shouldReadFile = false;
							if(mEOFListener != null){
							    mEOFListener.readFileEnd();
							}
						} else {
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
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStart(this);
		}
	}

	protected abstract int read(byte[] buffer) throws IOException;

	protected abstract void seekTo(long offset) throws IOException;

	@Override
	public void pauseStreamingContents() {
		stopWorkerThread();
	}

	@Override
	public void pauseStreamingContents(long offset) {
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
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStop(this);
		}
	}

	public MediaStreamingStateListener getMediaStreamingStateListener() {
		return mediaStreamingStateListener;
	}

	public void setMediaStreamingStateListener(MediaStreamingStateListener mediaStreamingStateListener) {
		this.mediaStreamingStateListener = mediaStreamingStateListener;
	}

	@Override
	public void playerTimeDidChange(int time) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingTimeDidChange(this, time);
		}
	}

	@Override
	public void playerTimeDurationReady(int duration) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingDurationIsReady(this, duration);
		}
	}
	
	private EOFListener mEOFListener = null;
	public interface EOFListener{
	    public void readFileEnd();
	}
	
	public void setEOFListener(EOFListener l){
	    mEOFListener = l;
	}

}