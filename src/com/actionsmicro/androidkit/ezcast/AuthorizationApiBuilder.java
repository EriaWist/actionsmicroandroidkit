package com.actionsmicro.androidkit.ezcast;

import android.content.Context;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi.AuthorizationListener;
/**
 * Authorization API builder.
 * @author James Chen
 * @version {SDK_VERSION_STRING}
 * @since 2.0
 */
public class AuthorizationApiBuilder extends ApiBuilder<AuthorizationApi> {
	private AuthorizationListener authorizationListener;
	/**
	 * Assign the {@link AuthorizationListener} to handle authorization result.
	 * @param authorizationListener {@link AuthorizationListener}
	 * @return The AuthorizationApiBuilder.
	 * @since 2.0
	 */
	public AuthorizationApiBuilder setAuthorizationListener(AuthorizationListener authorizationListener) {
		this.authorizationListener = authorizationListener;
		return this;
	}
	/**
	 * Create Authorization API builder.
	 * @param device The device this API will be bound to.
	 * @param context The Android Context object.
	 * @since 2.0
	 */
	public AuthorizationApiBuilder(DeviceInfo device, Context context) {
		super(device, context);
	}
	/**
	 * Create the authorization API object.
	 * @since 2.0
	 */
	@Override
	public AuthorizationApi build() {
		return getDevice().createAuthorizationApi(this);
	}
	public AuthorizationListener getAuthorizationListener() {
		return authorizationListener;
	}
}
