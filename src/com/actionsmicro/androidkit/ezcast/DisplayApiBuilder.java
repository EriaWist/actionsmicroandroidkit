package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.DisplayApi.DisplayListener;
/**
 * ApiBuilder to build DisplayApi.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public class DisplayApiBuilder extends ApiBuilder<DisplayApi> {

	private DisplayListener displayListener;

	/**
	 * Create DisplayApiBuilder.
	 * @param device The device this API will be bound to.
	 * @param context The Android Context object.
	 * @since 2.0
	 */
	public DisplayApiBuilder(DeviceInfo device, Context context) {
		super(device, context);
	}

	/**
	 * Create the DisplayApi object.
	 * @since 2.0
	 */
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
