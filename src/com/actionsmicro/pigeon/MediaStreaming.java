package com.actionsmicro.pigeon;

import com.actionsmicro.ezcast.MediaPlayerApi.State;

public interface MediaStreaming {
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
		public boolean isAudio();
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
	public State getPlayerState();
}
