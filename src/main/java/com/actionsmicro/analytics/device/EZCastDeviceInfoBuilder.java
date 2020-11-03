package com.actionsmicro.analytics.device;

import android.content.Context;

import com.actionsmicro.analytics.DeviceInfoBuilder;
import com.actionsmicro.androidkit.ezcast.imp.ezdisplay.PigeonDeviceInfo;

import java.util.Map;

public class EZCastDeviceInfoBuilder extends DeviceInfoBuilder<PigeonDeviceInfo> {

    public EZCastDeviceInfoBuilder(Context context, PigeonDeviceInfo device, String appId) {
        super(context, device, appId, "device", "2014-10-24", EZCastFamilyDeviceTypeBuilder.getType(device));
    }

    @Override
    public Map<String, Object> buildDeviceInfo() {
        Map<String, Object> deviceInfo = super.buildDeviceInfo();
        deviceInfo.put("device_id", getDevice().getParameter("deviceid"));
        return deviceInfo;
    }

}
