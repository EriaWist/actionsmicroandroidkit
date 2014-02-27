package com.actionsmicro.ezcast.imp.googlecast;

import com.actionsmicro.utils.Log;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;

public class GoogleCastApp {
	private static final String TAG = "GoogleCastApp";
	private boolean applicationStarted;
	private GoogleApiClient googleCastApiClient;
	private String castAppId;

	public GoogleCastApp(GoogleApiClient googleApiClient, String castAppId) {
		this.googleCastApiClient = googleApiClient;
		this.castAppId = castAppId;
	}


	public boolean isApplicationStarted() {
		return applicationStarted;
	}

	public void launcheApplication(final ResultCallback<Cast.ApplicationConnectionResult> resultCallback) {
		if (googleCastApiClient != null && !applicationStarted) {
			try {
				Cast.CastApi.launchApplication(googleCastApiClient, castAppId, false)
				.setResultCallback(
						new ResultCallback<Cast.ApplicationConnectionResult>() {
							@Override
							public void onResult(Cast.ApplicationConnectionResult result) {
								Status status = result.getStatus();
								Log.d(TAG, GoogleCastApp.this + ": launchApplication("+castAppId+").onResult:"+status);
								if (status.isSuccess()) {
									ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
									String sessionId = result.getSessionId();
									String applicationStatus = result.getApplicationStatus();
									boolean wasLaunched = result.getWasLaunched();
									applicationStarted = true;					
								} else {
								}
								if (resultCallback != null) {
									resultCallback.onResult(result);
								}
							}
						});

			} catch (Exception e) {
				Log.e(TAG, "Failed to launch application", e);
			}
		}
	}

	public void stopApplication() {
		if (googleCastApiClient != null && applicationStarted) {
			Cast.CastApi.stopApplication(googleCastApiClient);
			applicationStarted = false;
		}
	}


	public String getAppId() {
		return castAppId;
	}
}