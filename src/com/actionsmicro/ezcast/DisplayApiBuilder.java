package com.actionsmicro.ezcast;

import android.content.Context;

import com.actionsmicro.ezcast.DisplayApi.DisplayListener;

public class DisplayApiBuilder extends ApiBuilder<DisplayApi> {

	private DisplayListener displayListener;


	public DisplayApiBuilder(DeviceInfo device, Context context) {
		super(device, context);
	}

	@Override
	public DisplayApi build() {
		return device.createDisplayApi(this);
	}

	public DisplayListener getDisplayListener() {
		return displayListener;
	}
	public DisplayApiBuilder setDisplayListener(DisplayListener displayListener) {
		this.displayListener = displayListener;
		return this;
	}

}
