package com.actionsmicro.androidkit.ezcast;

import java.io.InputStream;

import android.content.Context;
/**
 * API to play video/audio file on remote device.
 * <p>
 * To create MediaPlayerApi you should use {@link MediaPlayerApiBuilder}.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.1
 */
public interface MediaPlayerApi extends Api {
	public static final int AV_RESULT_OK = 0;
	/**
	 * A generic error.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_GENERIC = 1;
	/**
	 * Failed when initialization.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_START_INIT_FAILED = 2;
	/**
	 * Failed because of device is occupied by other user.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_START_OCCUPIED_OTHER_USER = 3;
	/**
	 * Failed because of device is occupied by other streaming session.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_START_OCCUPIED_ALREADY_STREAMING = 4;
	/**
	 * Invalid format error.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_STOP_FILE_FORMAT_UNSUPPORTED = 5;
	/**
	 * Indicates current playback session is aborted by the device.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_STOP_ABORTED = 6;
	/**
	 * Invalid URL error.
	 * @since 2.1
	 */
	public static final int AV_RESULT_ERROR_URL_DIVERT_LINK_ERROR = 7;

	/**
	 * Upload subtitle file to the remote device. Note: Only some kind of devices supports subtitle.
	 * @param is InputStream contains subtitle data.
	 * @param fileType Type of subtitle file.
	 * @throws Exception
	 * @since 2.1
	 */
	public void uploadSubtitle(InputStream is, String fileType) throws Exception;
	
	/**
	 * Player states.
	 * @author James Chen
	 *
	 * @since 2.1
	 */
	public enum State {
		/**
		 * Unknown state.
		 * @since 2.1
		 */
		UNKNOWN, 
		/**
		 * Player is stopped.
		 * @since 2.1
		 */
		STOPPED, 
		/**
		 * Player is playing.
		 * @since 2.1
		 */
		PLAYING, 
		/**
		 * Player is paused.
		 * @since 2.1
		 */
		PAUSED,
		/**
		 * Player is idle.
		 * @since 2.9
		 */
		IDLE,
		/**
		 * Player is ended.
		 * @since 2.9
		 */
		ENDED,
		/**
		 * Player is buffering.
		 * @since 2.9
		 */
		BUFFERING,
		/**
		 * Player is processing.
		 * @since 2.9
		 */
		PROCESSING

	};
	/**
	 * Cause of mediaPlayerDidStop().
	 * @author Laicc
	 *
	 * @since 2.2
	 */
	public enum Cause {
		/**
		 * Unknown cause.
		 * @since 2.2
		 */
		UNKNOWN,
		/**
		 * Caused by user stop.
		 * @since 2.2
		 */
		USER,
		/**
		 * Caused by Remote. ex: media ended.
		 * @since 2.2
		 */
		REMOTE
	}
	/**
	 * Return current state of remote device.
	 * @return State
	 * @since 2.1
	 */
	public State getState();
	/**
	 * Pause current media playback.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean pause();
	/**
	 * Resume current media playback.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean resume();
	/**
	 * Increase volume. Note: Only some kind of devices support it.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean increaseVolume();
	/**
	 * Decrease volume. Note: Only some kind of devices support it.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean decreaseVolume();
	/**
	 * Changes position of current playback.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean seek(int position);
	/**
	 * Stop current playback.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	public boolean stop();
	/**
	 * Play video/audio file.
	 * @return Return true if success. Otherwise, return false.
	 * @since 2.1
	 */
	/**
	 * Play video/audio file.
	 * 
	 * @param context Android Context object.
	 * @param url URL to the media. Supported URL scheme depends on remote devices.
	 * @param userAgentString User agent string for web video. Can be NULL, default user agent string will be used.
	 * @param mediaContentLength Content length of the media if available. Otherwise, pass -1.
	 * @param title Title of the media. Title will be shown on certain devices.
	 * @return Return true if success. Otherwise, return false. The returned value means only the result of play command. The result of video playback will be informed through {@link MediaPlayerStateListener} 
	 * @throws Exception
	 * @since 2.1
	 */
	public boolean play(Context context, String url, String userAgentString, Long mediaContentLength, String title) throws Exception ; //TODO encapsulate parameters as object
	/**
	 * Media playback callback handler.
	 * @author James Chen
	 * @version {SDK_VERSION_STRING}
	 * @since 2.1
	 */
	public interface MediaPlayerStateListener {
		/**
		 * Called when media begins to be played.
		 * @param api The media player API object.
		 * @since 2.1
		 */
		public abstract void mediaPlayerDidStart(MediaPlayerApi api);
		/**
		 * Called when media playback has been stopped. 
		 * @param api The media player API object.
		 * @param cause Cause that stop media player. Added since 2.2
		 */
		public abstract void mediaPlayerDidStop(MediaPlayerApi api, Cause cause);
		/**
		 * Called when media is failed to be played.
		 * @param api The media player API object.
		 * @param resultCode Error code of failure. E.g. {@link #AV_RESULT_ERROR_GENERIC} and so on.
		 * @param videoObj VideoObj is JSONObject which contains error msg, put empty string when
		 *                 you don't have VideoObj
		 * @since 2.14
		 */

		public abstract void mediaPlayerDidFailed(MediaPlayerApi api, int resultCode, String videoObj);
		/**
		 * Called when current position of playback is changed.
		 * @param api The media player API object.
		 * @param time Current position of playback.
		 * @since 2.1
		 */
		public abstract void mediaPlayerTimeDidChange(MediaPlayerApi api, long time);
		/**
		 * Called when media duration is changed.
		 * @param api The media player API object.
		 * @param duration
		 * @since 2.1
		 */
		public abstract void mediaPlayerDurationIsReady(MediaPlayerApi api, long duration);

	}
}
