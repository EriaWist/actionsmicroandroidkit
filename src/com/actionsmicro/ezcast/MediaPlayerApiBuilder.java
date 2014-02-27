package com.actionsmicro.ezcast;

import android.content.Context;

import com.actionsmicro.ezcast.MediaPlayerApi.MediaPlayerStateListener;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonMediaPlayerApi;

public class MediaPlayerApiBuilder extends ApiBuilder<MediaPlayerApi> {
	MediaPlayerStateListener mediaPlayerStateListener;
	public MediaPlayerStateListener getMediaPlayerStateListener() {
		return mediaPlayerStateListener;
	}

	public MediaPlayerApiBuilder setMediaPlayerStateListener(
			MediaPlayerStateListener mediaPlayerStateListener) {
		this.mediaPlayerStateListener = mediaPlayerStateListener;
		return this;
	}

	public MediaPlayerApiBuilder(DeviceInfo device, Context context) {
		super(device, context);
	}

	@Override
	public MediaPlayerApi build() {
		return device.createMediaPlayerApi(this);
	}

}