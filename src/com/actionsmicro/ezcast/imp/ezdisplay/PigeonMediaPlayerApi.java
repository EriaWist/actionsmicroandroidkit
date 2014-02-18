package com.actionsmicro.ezcast.imp.ezdisplay;

import java.io.File;
import java.io.InputStream;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.actionsmicro.ezcast.MediaPlayerApi;
import com.actionsmicro.ezcast.MediaPlayerApiBuilder;
import com.actionsmicro.pigeon.Client;
import com.actionsmicro.pigeon.MediaStreaming;
import com.actionsmicro.pigeon.MediaStreaming.DataSource;
import com.actionsmicro.pigeon.MediaStreamingContentUriDataSource;
import com.actionsmicro.pigeon.MediaStreamingFileDataSource;
import com.actionsmicro.pigeon.MediaStreamingHttpDataSource;
import com.actionsmicro.pigeon.MediaStreamingStateListener;
import com.actionsmicro.web.Utils;

public class PigeonMediaPlayerApi extends PigeonApi implements MediaPlayerApi {
	private MediaStreaming mediaStreaming;
	private DataSource dataSource;
	private MediaPlayerStateListener mediaPlayerStateListener;
	private static final String DEFAULT_USER_AGENT_STRING = "Mozilla/5.0 (Linux; U; Android 4.1.1; zh-tw; A210 Build/JRO03H) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Safari/534.30 ezcast";
	
	public PigeonMediaPlayerApi(MediaPlayerApiBuilder mediaPlayerApiBuilder) {
		super(mediaPlayerApiBuilder);
		mediaPlayerStateListener = mediaPlayerApiBuilder.getMediaPlayerStateListener();
	}
	@Override
	protected void onPigeonClientCreated(Client pigeonClient) {
		if (pigeonClient instanceof MediaStreaming) {
			mediaStreaming = (MediaStreaming) pigeonClient;
		}
	}
	@Override
	protected void onPigeonClientReleased(Client pigeonClient) {
		dataSource = null;
	}
	@Override
	public void uploadSubtitle(InputStream is, String fileType)
			throws Exception {
		Utils.uploadInputStreamToServer(
				is, 
				"ezsubtitle" + fileType,
				pigeonClient.getServerAddress(), "/cgi-bin/upload.cgi");
	}
	@Override
	public State getState() {
		return mediaStreaming.getPlayerState();
	}
	@Override
	public boolean pause() {
		return mediaStreaming.pauseMediaStreaming() == 0;
	}
	@Override
	public boolean resume() {
		return mediaStreaming.resumeMediaStreaming() == 0;
	}
	@Override
	public boolean increaseVolume() {
		return mediaStreaming.increaseVolume() == 0;
	}
	@Override
	public boolean decreaseVolume() {
		return mediaStreaming.decreaseVolume() == 0;
	}
	@Override
	public boolean seek(int position) {
		return mediaStreaming.seekTo(position) == 0;
	}
	@Override
	public boolean stop() {
		mediaStreaming.stopMediaStreaming();
		return true;
	}
	@Override
	public boolean play(Context context, String mediaUrl, String userAgentString, Long mediaContentLength) throws Exception {
		if (dataSource != null) {
			dataSource.setMediaStreamingStateListener(null);
			dataSource = null;
		}
		if (mediaUrl.startsWith("http")) {
			dataSource = new MediaStreamingHttpDataSource(mediaUrl, userAgentString != null?userAgentString:DEFAULT_USER_AGENT_STRING, mediaContentLength);
		} else if (mediaUrl.startsWith(ContentResolver.SCHEME_CONTENT)) { 
			dataSource = new MediaStreamingContentUriDataSource(context, Uri.parse(mediaUrl));
		} else if (MediaStreamingFileDataSource.supportsFileExt(com.actionsmicro.utils.Utils.getFileExtension(mediaUrl).toLowerCase())) {
			File mediaFile = new File(mediaUrl);
			dataSource = new MediaStreamingFileDataSource(mediaFile);
		} else {
			return false;
		}
		if (dataSource != null) {
			dataSource.setMediaStreamingStateListener(new MediaStreamingStateListener() {

				@Override
				public void mediaStreamingDidStart(DataSource dataSource) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStart(PigeonMediaPlayerApi.this);
					}
				}

				@Override
				public void mediaStreamingDidStop(DataSource dataSource) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStop(PigeonMediaPlayerApi.this);
					}
				}

				@Override
				public void medisStreamingFail(DataSource dataSource,
						int resultCode) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerFail(PigeonMediaPlayerApi.this, resultCode);
					}
				}

				@Override
				public void medisStreamingTimeDidChange(DataSource dataSource,
						int time) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerTimeDidChange(PigeonMediaPlayerApi.this, time);
					}
				}

				@Override
				public void medisStreamingDurationIsReady(
						DataSource dataSource, int duration) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(PigeonMediaPlayerApi.this, duration);
					}
				}
				
			});
			mediaStreaming.startMediaStreaming(dataSource);
		}
		return true;
	}

	
}
