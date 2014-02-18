package com.actionsmicro.ezcast;

import com.actionsmicro.ezcast.DisplayApi.DisplayListener;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonDisplayApi;

public class DisplayApiBuilder extends ApiBuilder<DisplayApi> {

	private DisplayListener displayListener;


	public DisplayApiBuilder(DeviceInfo device) {
		super(device);
	}

	@Override
	public DisplayApi build() {
		if (device instanceof PigeonDeviceInfo) {
			return new PigeonDisplayApi(this);
		}	
		return null;
	}

	public DisplayListener getDisplayListener() {
		return displayListener;
	}
	public DisplayApiBuilder setDisplayListener(DisplayListener displayListener) {
		this.displayListener = displayListener;
		return this;
	}

}
