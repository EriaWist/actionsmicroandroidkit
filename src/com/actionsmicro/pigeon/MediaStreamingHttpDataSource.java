package com.actionsmicro.pigeon;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

import com.actionsmicro.utils.Log;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi.Cause;
import com.actionsmicro.pigeon.MediaStreaming.HttpDataSource;

public class MediaStreamingHttpDataSource implements HttpDataSource {
	private MediaStreamingStateListener mediaStreamingStateListener;
	private static final String TAG = "MediaStreamingHttpDataSource";
	final HttpClient client = new DefaultHttpClient();
	private String urlString;
	private long contentLength = -1;
	private String userAgentString;

	public MediaStreamingHttpDataSource(String url, String userAgentString, Long contentLength) {
		this.urlString = url;
		this.userAgentString = userAgentString;
		this.contentLength = contentLength;
	}
	
	public long getContentLength() {
		return contentLength;
	}
	
	@Override
	public void mediaStreamingDidFail(int resultCode) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingFail(this, resultCode);
		}
	}

	@Override
	public void stopStreamingContents(Cause cause) {		
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStop(this, cause);
		}
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

	public MediaStreamingStateListener getMediaStreamingStateListener() {
		return mediaStreamingStateListener;
	}

	public void setMediaStreamingStateListener(
			MediaStreamingStateListener mediaStreamingStateListener) {
		this.mediaStreamingStateListener = mediaStreamingStateListener;
	}
	@Override
	public String getUrl() {
		Log.d(TAG, "getUrl:"+urlString);
		return urlString;
	}
	@Override
	public String getUserAgent() {
		Log.d(TAG, "getUserAgent:"+userAgentString);
		return userAgentString;
	}

	@Override
	public void startStreaming(MediaStreaming ms) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStart(this);
		}
	}

}
