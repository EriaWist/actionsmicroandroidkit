package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi;
import com.actionsmicro.androidkit.ezcast.MediaPlayerApiBuilder;
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
		String serverAddress;
		String webroot = projectorInfo.getParameter("webroot");
		if (webroot != null && !webroot.isEmpty()) {
			serverAddress = URLDecoder.decode(webroot, "UTF-8");
		} else {
			serverAddress = "http://" + pigeonClient.getServerAddress() + "/";
		}
		Utils.uploadInputStreamToServer(
				is, 
				"ezsubtitle" + fileType,
				serverAddress, "cgi-bin/upload.cgi");
	}
	@Override
	public State getState() {
		if(mediaStreaming != null){
			return mediaStreaming.getPlayerState();
		}
		return State.UNKNOWN;
		
	}
	@Override
	public boolean pause() {
		if(mediaStreaming != null){
			return mediaStreaming.pauseMediaStreaming() == 0;
		}
		return true;
	}
	@Override
	public boolean resume() {
		if(mediaStreaming != null){
			return mediaStreaming.resumeMediaStreaming() == 0;
		}
		return true;
	}
	@Override
	public boolean increaseVolume() {
		if(mediaStreaming != null){
			return mediaStreaming.increaseVolume() == 0;
		}
		return true;
	}
	@Override
	public boolean decreaseVolume() {
		if(mediaStreaming != null){
			return mediaStreaming.decreaseVolume() == 0;
		}
		return true;
	}
	@Override
	public boolean seek(int position) {
		if(mediaStreaming != null){
			return mediaStreaming.seekTo(position) == 0;
		}
		return true;
	}
	@Override
	public boolean stop() {
		if (mediaStreaming != null) {
			mediaStreaming.stopMediaStreaming();
		}
		commitMediaUsageTracking();
		return true;
	}
	@Override
	public boolean play(Context context, String mediaUrl, String userAgentString, Long mediaContentLength, String title) throws Exception {
		if (dataSource != null) {
			dataSource.setMediaStreamingStateListener(null);
			dataSource = null;
		}
		commitMediaUsageTracking();
		if (mediaUrl.startsWith("http") || mediaUrl.startsWith("rtsp") || mediaUrl.startsWith("mms")) {
			dataSource = new MediaStreamingHttpDataSource(mediaUrl, userAgentString != null?userAgentString:DEFAULT_USER_AGENT_STRING, mediaContentLength);
		} else if (mediaUrl.startsWith(ContentResolver.SCHEME_CONTENT)) { 
			dataSource = new MediaStreamingContentUriDataSource(context, Uri.parse(mediaUrl));
		} else if (MediaStreamingFileDataSource.supportsFileExt(com.actionsmicro.utils.Utils.getFileExtension(mediaUrl).toLowerCase())) {
			File mediaFile = new File(mediaUrl);
			dataSource = new MediaStreamingFileDataSource(mediaFile);
		} else {
			return false;
		}
		beginMediaUsageTracking(context, mediaUrl, userAgentString, title);
		if (dataSource != null) {
			dataSource.setMediaStreamingStateListener(new MediaStreamingStateListener() {

				@Override
				public void mediaStreamingDidStart(DataSource dataSource) {
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStart(PigeonMediaPlayerApi.this);
					}
				}

				@Override
				public void mediaStreamingDidStop(DataSource dataSource, Cause cause) {
					commitMediaUsageTracking();
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidStop(PigeonMediaPlayerApi.this, cause);
					}
				}

				@Override
				public void medisStreamingFail(DataSource dataSource,
						int resultCode) {
					String resultString = String.valueOf(resultCode);
					setMediaUsageResultCode(resultString, resultCode);
					commitMediaUsageTracking();
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDidFailed(PigeonMediaPlayerApi.this, resultCode);
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
					setMediaUsageDuration(duration);
					if (mediaPlayerStateListener != null) {
						mediaPlayerStateListener.mediaPlayerDurationIsReady(PigeonMediaPlayerApi.this, duration);
					}
				}
				
			});
			if(mediaStreaming != null){
				mediaStreaming.startMediaStreaming(dataSource);
			}
		}
		return true;
	}
}
