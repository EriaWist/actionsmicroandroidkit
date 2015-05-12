package com.actionsmicro.androidkit.ezcast.imp.androidp2prx;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;

public class AndroidP2PRxMediaPlayerApi extends AndroidP2PRxApi implements
		MediaPlayerApi {

	protected static final String TAG = "AndroidP2PRxMediaPlayerApi";
	public AndroidP2PRxMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
	}
	@Override
	public void connect() {
		super.connect();
		final MediaPlayerStateListener mediaPlayerStateListener = ((MediaPlayerApiBuilder) getApiBuilder()).getMediaPlayerStateListener();
		if (mediaPlayerStateListener != null) {
			getAndroidP2PRxClient().setMediaPlayerStateListener(new MediaPlayerStateListener() {

				@Override
				public void mediaPlayerDidStart(MediaPlayerApi api) {
					mediaPlayerStateListener.mediaPlayerDidStart(AndroidP2PRxMediaPlayerApi.this);
				}

				@Override
				public void mediaPlayerDidStop(MediaPlayerApi api, Cause cause) {
					mediaPlayerStateListener.mediaPlayerDidStop(AndroidP2PRxMediaPlayerApi.this, cause);
				}

				@Override
				public void mediaPlayerDidFailed(MediaPlayerApi api,
						int resultCode) {
					mediaPlayerStateListener.mediaPlayerDidFailed(AndroidP2PRxMediaPlayerApi.this, resultCode);
				}

				@Override
				public void mediaPlayerTimeDidChange(MediaPlayerApi api,
						long time) {
					mediaPlayerStateListener.mediaPlayerTimeDidChange(AndroidP2PRxMediaPlayerApi.this, time);
				}

				@Override
				public void mediaPlayerDurationIsReady(MediaPlayerApi api,
						long duration) {
					mediaPlayerStateListener.mediaPlayerDurationIsReady(AndroidP2PRxMediaPlayerApi.this, duration);
				}
				
			});			
		}
	}

	@Override
	public void disconnect() {
		getAndroidP2PRxClient().setMediaPlayerStateListener(null);
		super.disconnect();
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		getAndroidP2PRxClient().uploadSubtitle(is, fileType);
	}
	@Override
	public State getState() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().getState();
	}

	@Override
	public boolean pause() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().pause();
	}

	@Override
	public boolean resume() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().resume();
	}

	@Override
	public boolean increaseVolume() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().increaseVolume();
	}

	@Override
	public boolean decreaseVolume() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().decreaseVolume();
	}

	@Override
	public boolean seek(int position) {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().seek(position);
	}

	@Override
	public boolean stop() {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().stop();
	}
	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		if (getAndroidP2PRxClient() == null) {
			throw new IllegalStateException("AndroidRxClient should not be null. API is not connected or API was disconnted.");
		}
		return getAndroidP2PRxClient().play(context, url, userAgentString, mediaContentLength, title);
	}	

}
