package com.actionsmicro.androidkit.ezcast.imp.androidrx;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;

public class AndroidRxMediaPlayerApi extends AndroidRxApi implements
		MediaPlayerApi {

	protected static final String TAG = "AndroidRxMediaPlayerApi";
	public AndroidRxMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
	}
	@Override
	public void connect() {
		super.connect();
		final MediaPlayerStateListener mediaPlayerStateListener = ((MediaPlayerApiBuilder) getApiBuilder()).getMediaPlayerStateListener();
		if (mediaPlayerStateListener != null) {
			getAndroidRxClient().setMediaPlayerStateListener(new MediaPlayerStateListener() {

				@Override
				public void mediaPlayerDidStart(MediaPlayerApi api) {
					mediaPlayerStateListener.mediaPlayerDidStart(AndroidRxMediaPlayerApi.this);
				}

				@Override
				public void mediaPlayerDidStop(MediaPlayerApi api, Cause cause) {
					mediaPlayerStateListener.mediaPlayerDidStop(AndroidRxMediaPlayerApi.this, cause);
				}

				@Override
				public void mediaPlayerDidFailed(MediaPlayerApi api,
						int resultCode) {
					mediaPlayerStateListener.mediaPlayerDidFailed(AndroidRxMediaPlayerApi.this, resultCode);
				}

				@Override
				public void mediaPlayerTimeDidChange(MediaPlayerApi api,
						long time) {
					mediaPlayerStateListener.mediaPlayerTimeDidChange(AndroidRxMediaPlayerApi.this, time);
				}

				@Override
				public void mediaPlayerDurationIsReady(MediaPlayerApi api,
						long duration) {
					mediaPlayerStateListener.mediaPlayerDurationIsReady(AndroidRxMediaPlayerApi.this, duration);
				}
				
			});			
		}
	}

	@Override
	public void disconnect() {
		getAndroidRxClient().setMediaPlayerStateListener(null);
		super.disconnect();
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidRxClient().uploadSubtitle(is, fileType);
	}
	@Override
	public State getState() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().getState();
	}

	@Override
	public boolean pause() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().pause();
	}

	@Override
	public boolean resume() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().resume();
	}

	@Override
	public boolean increaseVolume() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().increaseVolume();
	}

	@Override
	public boolean decreaseVolume() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().decreaseVolume();
	}

	@Override
	public boolean seek(int position) {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().seek(position);
	}

	@Override
	public boolean stop() {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().stop();
	}
	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		if (getAndroidRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidRxClient().play(context, url, userAgentString, mediaContentLength, title);
	}	

}
