package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi.AuthorizationListener;

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
		return getDevice().createAuthorizationApi(this);
	}
	public AuthorizationListener getAuthorizationListener() {
		return authorizationListener;
	}
}
