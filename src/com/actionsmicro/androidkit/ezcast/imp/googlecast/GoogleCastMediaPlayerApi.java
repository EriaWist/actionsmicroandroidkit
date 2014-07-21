package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;

public class GoogleCastMediaPlayerApi extends GoogleCastApi implements
		MediaPlayerApi {

	private MediaPlayerStateListener mediaPlayerStateListener;
	public GoogleCastMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
		mediaPlayerStateListener = apiBuilder.getMediaPlayerStateListener();
	}

	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		getGoogleCastClient().uploadSubtitle(is, fileType);
	}

	@Override
	public State getState() {
		return getGoogleCastClient().getState();
	}

	@Override
	public boolean pause() {
		return getGoogleCastClient().pause();
	}

	@Override
	public boolean resume() {
		return getGoogleCastClient().resume();
	}

	@Override
	public boolean increaseVolume() {
		return getGoogleCastClient().increaseVolume();
	}

	@Override
	public boolean decreaseVolume() {
		return getGoogleCastClient().decreaseVolume();
	}

	@Override
	public boolean seek(int position) {
		return getGoogleCastClient().seek(position);
	}

	@Override
	public boolean stop() {
		return getGoogleCastClient().stop();
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		return getGoogleCastClient().play(context, url, userAgentString, mediaContentLength, title);
	}
	@Override
	protected void onCreateGoogleCastClient(EZCastOverGoogleCast googleCastClient) {
		googleCastClient.setMediaPlayerStateListeners(mediaPlayerStateListener);
	}

}
