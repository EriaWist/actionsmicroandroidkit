package com.actionsmicro.pigeon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.actionsmicro.pigeon.MediaStreaming.FileDataSource;
import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public class MediaStreamingFileDataSource implements FileDataSource {
	
	private static final String TAG = null;
	private File mediaFile;
	private MediaStreaming mediaStreaming;
	private RandomAccessFile mediaFileInput;
	private boolean shouldReadFile;
	private Thread workerThread;
	private MediaStreamingStateListener mediaStreamingStateListener;
	
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
					mediaFileInput.seek(offset);
					final int bufferSize = 1024*32;
					final byte[] buffer = new byte [bufferSize];
					int sizeRead = 0;
					do {
						sizeRead = mediaFileInput.read(buffer);
						if (sizeRead == -1) {
							mediaStreaming.sendEofPacket();
							shouldReadFile = false;
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
	public void setMediaStreamingStateListener(
			MediaStreamingStateListener mediaStreamingStateListener) {
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
	private static final List<String> SUPPORTED_VIDEO_FILE_EXTENSIONS = Arrays.asList("mov", "mkv", "mp4", "avi", "divx", "mpg", "mpeg", "ts", "xvid", "rmvb", "rm", "wmv", "m4v", "3gp", "vob", "dat"); //"aac", "wav", "ogg"
	private static final List<String> SUPPORTED_AUDIO_FILE_EXTENSIONS = Arrays.asList("mp3", "wma", "m4a");
	private static List<String> SUPPORTED_FILE_EXTENSIONS = null;
	static {
		SUPPORTED_FILE_EXTENSIONS = new ArrayList<String>(SUPPORTED_VIDEO_FILE_EXTENSIONS);
		SUPPORTED_FILE_EXTENSIONS.addAll(SUPPORTED_AUDIO_FILE_EXTENSIONS);
	}
	public static List<String> getSupportedFileExt() {
		return SUPPORTED_FILE_EXTENSIONS;
	}
	public static boolean supportsFileExt(String ext) {
		return SUPPORTED_FILE_EXTENSIONS.contains(ext);
	}
	public static boolean isAudioFileExt(String ext) {
		return SUPPORTED_AUDIO_FILE_EXTENSIONS.contains(ext);
	}
	@Override
	public boolean isAudio() {
		return MediaStreamingFileDataSource.isAudioFileExt(Utils.getFileExtension(mediaFile.getAbsolutePath()));
	}

}
