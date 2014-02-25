package com.actionsmicro.ezcast;

import android.content.Context;

import com.actionsmicro.ezcast.AuthorizationApi.AuthorizationListener;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonAuthorizationApi;
import com.actionsmicro.ezcast.imp.ezdisplay.PigeonDeviceInfo;

public class AuthorizationApiBuilder extends ApiBuilder<AuthorizationApi> {
	private AuthorizationListener authorizationListener;
	public AuthorizationApiBuilder setAuthorizationListener(AuthorizationListener authorizationListener) {
		this.authorizationListener = authorizationListener;
		return this;
	}
	public AuthorizationApiBuilder(DeviceInfo device, Context context) {
		super(device, context);
	}
	@Override
	public AuthorizationApi build() {
		if (getDevice() instanceof PigeonDeviceInfo) {
			return new PigeonAuthorizationApi(this);
		}		
		return getDevice().createAuthorizationApi(this);
	}
	public AuthorizationListener getAuthorizationListener() {
		return authorizationListener;
	}
}
