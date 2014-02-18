package com.actionsmicro.ezcast;

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

	public MediaPlayerApiBuilder(DeviceInfo device) {
		super(device);
	}

	@Override
	public MediaPlayerApi build() {
		if (device instanceof PigeonDeviceInfo) {
			return new PigeonMediaPlayerApi(this);
		}
		return null;
	}

}
