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
		beginMediaUsageTracking(context, url, userAgentString, title);
		getAirPlayClient().playVideo(url, new AirPlayClient.VideoStateListener() {
			
			@Override
			public void onVideoStopped() {
				commitMediaUsageTracking();
				state = State.STOPPED;
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStop(AirPlayMediaPlayerApi.this);
				}
			}
			
			@Override
			public void onVideoResumed() {
				state = State.PLAYING;
			}
			
			@Override
			public void onVideoPlayed() {
				state = State.PLAYING;				
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidStart(AirPlayMediaPlayerApi.this);
				}
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
				setMediaUsageDuration((int) duration);
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDurationIsReady(AirPlayMediaPlayerApi.this, (long) duration);
				}
			}

			@Override
			public void onVideoError(int errorCode) {
				setMediaUsageResultCode(String.valueOf(errorCode), mappingAirPlayError(errorCode));
				if (mediaPlayerStateListener != null) {
					mediaPlayerStateListener.mediaPlayerDidFailed(AirPlayMediaPlayerApi.this, mappingAirPlayError(errorCode));
				}
			}
		});
		return true;
	}

	

//	08-19 11:36:48.932 D/AirPlayClient( 5216): 	<dict>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<key>NSLocalizedDescription</key>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<string>Could not connect to the server.</string>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<key>code</key>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<integer>-1004</integer>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<key>domain</key>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<string>NSURLErrorDomain</string>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<key>sessionID</key>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<integer>13</integer>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<key>uuid</key>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 		<string>38A9F542-1EAB-425A-92C3-91A214C600CD-25-000000250964CA42</string>
//	08-19 11:36:48.932 D/AirPlayClient( 5216): 	</dict>
	
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 	<dict>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>NSLocalizedDescription</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<string>無法打開</string>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>NSLocalizedFailureReason</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<string>不支援此媒體格式。</string>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>code</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<integer>-11828</integer>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>domain</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<string>AVFoundationErrorDomain</string>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>sessionID</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<integer>24</integer>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<key>uuid</key>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 		<string>5642EE1B-9AAA-4A9E-B4FC-B5DACA572119-25-0000002EC9A50C41</string>
//	08-19 12:05:54.222 D/AirPlayClient( 7243): 	</dict>
	
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 	<dict>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>NSLocalizedDescription</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<string>無法完成操作</string>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>NSLocalizedFailureReason</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<string>發生未知的錯誤 (-12894)</string>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>code</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<integer>-11800</integer>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>domain</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<string>AVFoundationErrorDomain</string>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>sessionID</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<integer>27</integer>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<key>uuid</key>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 		<string>A11CA6E1-BEA9-499B-92F6-184B9068B6C7-25-0000002F35EE86BB</string>
//	08-19 12:07:09.997 D/AirPlayClient( 7243): 	</dict>
	private static final int mappingAirPlayError(int errorCode) {
		switch(errorCode) {
		case -11828:
			return AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSOPPORTED;
		case -1004:
			return AV_RESULT_ERROR_GENERIC;
		default:
			return AV_RESULT_ERROR_GENERIC;
		}
	}

}
