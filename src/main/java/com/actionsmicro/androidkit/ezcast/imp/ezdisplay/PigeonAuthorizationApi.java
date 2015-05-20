package com.actionsmicro.androidkit.ezcast.imp.ezdisplay;

import com.actionsmicro.androidkit.ezcast.AuthorizationApi;
import com.actionsmicro.androidkit.ezcast.AuthorizationApiBuilder;
import com.actionsmicro.androidkit.ezcast.AuthorizationApi.AuthorizationListener.DeniedReason;
import com.actionsmicro.pigeon.Client.RequestResult;
import com.actionsmicro.pigeon.MultiRegionsDisplay;

public class PigeonAuthorizationApi extends PigeonApi implements AuthorizationApi {

	private AuthorizationListener authorizationListener;

	public PigeonAuthorizationApi(AuthorizationApiBuilder authorizerApiBuilder) {
		super(authorizerApiBuilder);
		authorizationListener = authorizerApiBuilder.getAuthorizationListener();
	}
	
	@Override
	public void requestToDisplay(int splitCount, int position) {
		if (pigeonClient != null) {
			RequestResult result = pigeonClient.requestStreaming(splitCount, position);
			if (result == RequestResult.ALLOW) {
				if (authorizationListener != null) {
					if (pigeonClient instanceof MultiRegionsDisplay) {
						authorizationListener.authorizationIsGranted(this, ((MultiRegionsDisplay) pigeonClient).getNumberOfRegions(), ((MultiRegionsDisplay) pigeonClient).getPosition());
					} else {
						authorizationListener.authorizationIsGranted(this, 1, 1);
					}
				}
			} else {
				if (authorizationListener != null) {
					authorizationListener.authorizationIsDenied(this, DeniedReason.FULLY_OCCUPIED);
				}
			}
		}
	}

	@Override
	public void cancelPendingRequest() {
		// not supported
		
	}

}
