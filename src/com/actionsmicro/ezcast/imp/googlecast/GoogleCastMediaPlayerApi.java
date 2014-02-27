package com.actionsmicro.ezcast.imp.googlecast;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.ezcast.MediaPlayerApi;
import com.actionsmicro.ezcast.MediaPlayerApiBuilder;

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
		googleCastClient.uploadSubtitle(is, fileType);
	}

	@Override
	public State getState() {
		return googleCastClient.getState();
	}

	@Override
	public boolean pause() {
		return googleCastClient.pause();
	}

	@Override
	public boolean resume() {
		return googleCastClient.resume();
	}

	@Override
	public boolean increaseVolume() {
		return googleCastClient.increaseVolume();
	}

	@Override
	public boolean decreaseVolume() {
		return googleCastClient.decreaseVolume();
	}

	@Override
	public boolean seek(int position) {
		return googleCastClient.seek(position);
	}

	@Override
	public boolean stop() {
		return googleCastClient.stop();
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		return googleCastClient.play(context, url, userAgentString, mediaContentLength, title);
	}
	@Override
	protected void onCreateGoogleCastClient(EZCastOverGoogleCast googleCastClient) {
		googleCastClient.setMediaPlayerStateListeners(mediaPlayerStateListener);
	}

}
