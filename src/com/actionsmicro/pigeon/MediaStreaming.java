package com.actionsmicro.pigeon;

public interface MediaStreaming {
	public interface DataSource {
		public long getContentLength();
		public boolean isSeekable();
		public void mediaStreamingDidFail(int resultCode);
		public void startStreamingContents(MediaStreaming ms, long offset);
		public void pauseStreamingContents();
		public void stopStreamingContents();
	}
	public void startMediaStreaming(DataSource dataSource);
	public void sendStreamingContents(final byte[] contents, int length);
	public void sendEofPacket();
	public void stopMediaStreaming();
}
