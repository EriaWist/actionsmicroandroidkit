package com.actionsmicro.ezcast;

import java.io.InputStream;

import android.content.Context;

public interface MediaPlayerApi extends Api {
	public static final int AV_RESULT_OK = 0;
	public static final int AV_RESULT_ERROR_GENERIC = 1;
	public static final int AV_RESULT_ERROR_START_INIT_FAILED = 2;
	public static final int AV_RESULT_ERROR_START_OCCUPIED_OTHER_USER = 3;
	public static final int AV_RESULT_ERROR_START_OCCUPIED_ALREADY_STREAMING = 4;
	public static final int AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSOPPORTED = 5;
	public static final int AV_RESULT_ERROR_STOP_ABORTED = 6;
	public static final int AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR = 7;

	public void uploadSubtitle(InputStream is, String fileType) throws Exception;
	
	public enum State {
		STOPPED, PLAYING, PAUSED
	};
	public State getState();
	
	public boolean pause();
	public boolean resume();
	public boolean increaseVolume();
	public boolean decreaseVolume();
	public boolean seek(int position);
	public boolean stop();
	public boolean play(Context context, String url, String userAgentString, Long mediaContentLength, String title) throws Exception ; //TODO encapsulate parameters as object
	
	public interface MediaPlayerStateListener {

		public abstract void mediaPlayerDidStart(MediaPlayerApi api);

		public abstract void mediaPlayerDidStop(MediaPlayerApi api);

		public abstract void mediaPlayerDidFailed(MediaPlayerApi api, int resultCode);

		public abstract void mediaPlayerTimeDidChange(MediaPlayerApi api, long time);

		public abstract void mediaPlayerDurationIsReady(MediaPlayerApi apie, long duration);

	}
}
