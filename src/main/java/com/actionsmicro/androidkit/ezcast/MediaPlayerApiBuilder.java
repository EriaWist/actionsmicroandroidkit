package com.actionsmicro.androidkit.ezcast;

import com.actionsmicro.androidkit.ezcast.MediaPlayerApi.MediaPlayerStateListener;

/**
 * Media player API builder.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.1
 */
public class MediaPlayerApiBuilder extends ApiBuilder<MediaPlayerApi> {
	MediaPlayerStateListener mediaPlayerStateListener;
	public MediaPlayerStateListener getMediaPlayerStateListener() {
		return mediaPlayerStateListener;
	}
	/**
	 * Assign the {@link MediaPlayerStateListener} to handle playback related callbacks.
	 * @param mediaPlayerStateListener {@link MediaPlayerStateListener}
	 * @return The MediaPlayerApiBuilder
	 * @since 2.1
	 */
	public MediaPlayerApiBuilder setMediaPlayerStateListener(
			MediaPlayerStateListener mediaPlayerStateListener) {
		this.mediaPlayerStateListener = mediaPlayerStateListener;
		return this;
	}
	/**
	 * Create Media player API builder.
	 * @param sdk The EzCaskSdk instance.
	 * @param device The device this API will be bound to.
	 * @since 2.1
	 */	
	public MediaPlayerApiBuilder(EzCastSdk sdk, DeviceInfo device) {
		super(sdk, device);
	}
	/**
	 * Create the media player API object.
	 * @since 2.1
	 */
	@Override
	public MediaPlayerApi build() {
		return device.createMediaPlayerApi(this);
	}

}
