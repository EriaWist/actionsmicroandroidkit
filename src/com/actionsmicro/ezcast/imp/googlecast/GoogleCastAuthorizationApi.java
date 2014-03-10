package com.actionsmicro.ezcast.imp.googlecast;

import com.actionsmicro.ezcast.AuthorizationApi;
import com.actionsmicro.ezcast.AuthorizationApiBuilder;

public class GoogleCastAuthorizationApi extends GoogleCastApi implements AuthorizationApi {

	private AuthorizationListener authorizationListener;

	public GoogleCastAuthorizationApi(
			AuthorizationApiBuilder authorizationApiBuilder) {
		super(authorizationApiBuilder);
		authorizationListener = authorizationApiBuilder.getAuthorizationListener();
	}

	@Override
	public void requestToDisplay(int splitCount, int position) {
		EZCastOverGoogleCast googleCastClient = getGoogleCastClient();
		if (googleCastClient != null) {
			if (googleCastClient.isReadyToDisplay()) {
				if (authorizationListener != null) {
					authorizationListener.authorizationIsGranted(this, 1, 1);
				}			
			} else {
				new Thread(new Runnable() {

					@Override
					public void run() {
						EZCastOverGoogleCast googleCastClient = null;
						while ((googleCastClient = getGoogleCastClient()) != null && 
								!googleCastClient.isReadyToDisplay()) { // TODO use wait/notify instead of polling 
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						if (authorizationListener != null) {
							authorizationListener.authorizationIsGranted(GoogleCastAuthorizationApi.this, 1, 1);
						}
					}				
				}).start();
			}
		}
	}

	@Override
	public void cancelPendingRequest() {
		// TODO Auto-generated method stub

	}

}
