package com.actionsmicro.analytics.device;

import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;
import com.actionsmicro.falcon.Falcon.ProjectorInfo;

public class EZCastFamilyDeviceTypeBuilder {
	public static String getType(PigeonDeviceInfo deviceInfo) {
		if (deviceInfo != null) {
			return getType(deviceInfo.getProjectorInfo());
		}
		return null;
	}
	public static String getType(ProjectorInfo deviceInfo) {
		if (deviceInfo != null) {
			String family = deviceInfo.getParameter("family");
			if (family == null || family.equals("ezcast")) {
				String type = deviceInfo.getParameter("type");
				if (type != null) {
					if (type.equals("music")) {
						return "ezcastmusic";
					} else if (type.equals("car")) {
						return "ezcastcar";
					} else if (type.equals("lite")) {
						return "ezcastlite";						
					}
				}
				return "ezcast";
			}
			return family;
		}
		return null;
	}
}
