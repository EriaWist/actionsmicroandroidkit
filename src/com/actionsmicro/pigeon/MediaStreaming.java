package com.actionsmicro.pigeon;

public interface MediaStreaming {
	public static final int AV_RESULT_OK = 0;
	public static final int AV_RESULT_ERROR_GENERIC = 1;
	public static final int AV_RESULT_ERROR_START_INIT_FAILED = 2;
	public static final int AV_RESULT_ERROR_START_OCCUPIED_OTHER_USER = 3;
	public static final int AV_RESULT_ERROR_START_OCCUPIED_ALREADY_STREAMING = 4;
	public static final int AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSOPPORTED = 5;
	public static final int AV_RESULT_ERROR_STOP_ABORTED = 6;	
	public static final int AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR = 7;

	public interface DataSource {
		public void mediaStreamingDidFail(int resultCode);
		public void stopStreamingContents();
		public void playerTimeDidChange(int time);
		public void playerTimeDurationReady(int duration);
		public void setMediaStreamingStateListener(MediaStreamingStateListener mediaStreamingStateListener);
	}
	public interface FileDataSource extends DataSource {
		public boolean isSeekable();
		public long getContentLength();		
		public void startStreamingContents(MediaStreaming ms, long offset);
		public void pauseStreamingContents(long offset);
		public void pauseStreamingContents();		
	}
	public interface HttpDataSource extends DataSource {
		public void startStreaming(MediaStreaming ms);
		public String getUrl();
		public String getUserAgent();		
	}
	public void startMediaStreaming(DataSource dataSource);
	public int  getDuration();
	public int  getTime();
	public int  seekTo(int position);
	public int 	pauseMediaStreaming();
	public int 	resumeMediaStreaming();
	public int  increaseVolume();
	public int  decreaseVolume();
	public void stopMediaStreaming();
	public void sendStreamingContents(final byte[] contents, int length);
	public void sendStreamingContentsUdp(final byte[] contents, int length);
	public void sendEofPacket();
	public void resetPlayer();
	public enum PlayerState {
		STOPPED, PLAYING, PAUSED
	};
	public PlayerState getPlayerState();
}
