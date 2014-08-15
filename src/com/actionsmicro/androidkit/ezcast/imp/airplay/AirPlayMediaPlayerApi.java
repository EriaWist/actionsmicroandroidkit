package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;

public class AirPlayMediaPlayerApi extends AirPlayApi implements MediaPlayerApi {

	public AirPlayMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
	}

	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public State getState() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean pause() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean resume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean increaseVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean decreaseVolume() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean seek(int position) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean stop() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		getAirPlayClient().playVideo(url);
		return true;
	}

}
