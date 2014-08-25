package com.actionsmicro.androidkit.ezcast.imp.airplay;

import java.io.InputStream;

import android.content.Context;

import com.actionsmicro.airplay.AirPlayClient;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;

public class AirPlayMediaPlayerApi extends AirPlayApi implements MediaPlayerApi {

	private MediaPlayerStateListener mediaPlayerStateListener;
	private State state = State.UNKNOWN;
	
	public AirPlayMediaPlayerApi(MediaPlayerApiBuilder apiBuilder) {
		super(apiBuilder);
		mediaPlayerStateListener = apiBuilder.getMediaPlayerStateListener();
	}

	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public State getState() {
		return state;
	}

	@Override
	public boolean pause() {
		getAirPlayClient().pauseVideo();
		return true;
	}

	@Override
	public boolean resume() {
		getAirPlayClient().resumeVideo();
		return true;
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
		getAirPlayClient().scrubVideo(position);
		return true;
	}

	@Override
	public boolean stop() {
		getAirPlayClient().stopVideo();
		return true;
	}

	@Override
	public boolean play(Context context, String url, String userAgentString,
			Long mediaContentLength, String title) throws Exception {
		getAirPlayClient().playVideo(url, new AirPlayClient.VideoStateListener() {
			
			@Override
			public void onVideoStopped() {
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStop(AirPlayMediaPlayerApi.this);
				}
				state = State.STOPPED;
			}
			
			@Override
			public void onVideoResumed() {
				state = State.PLAYING;
			}
			
			@Override
			public void onVideoPlayed() {
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStart(AirPlayMediaPlayerApi.this);
				}
				state = State.PLAYING;				
			}
			
			@Override
			public void onVideoPaused() {
				state = State.PAUSED;				
			}
			
			@Override
			public void onTimeChanged(float position) {
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerTimeDidChange(AirPlayMediaPlayerApi.this, (long) position);
				}				
			}
			
			@Override
			public void onDurationChanged(float duration) {
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDurationIsReady(AirPlayMediaPlayerApi.this, (long) duration);
				}
			}
		});
		return true;
	}

}
