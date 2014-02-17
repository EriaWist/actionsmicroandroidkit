package com.actionsmicro.ezcast.imp;

import com.actionsmicro.ezcast.DeviceInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class FalconDeviceInfo extends DeviceInfo {

	private ProjectorInfo projectorInfo;

	public ProjectorInfo getProjectorInfo() {
		return projectorInfo;
	}

	public FalconDeviceInfo(ProjectorInfo projectorInfo) {
		this.projectorInfo = projectorInfo;
	}

}
