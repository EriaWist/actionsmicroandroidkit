package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import android.support.annotation.NonNull;
import android.view.Display;

import com.actionsmicro.utils.Log;
import com.google.android.gms.cast.ApplicationMetadata;
import com.google.android.gms.cast.Cast;
import com.google.android.gms.cast.CastRemoteDisplay;
import com.google.android.gms.cast.LaunchOptions;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;

public class GoogleCastApp {
	private static final String TAG = "GoogleCastApp";
	private boolean applicationStarted;
	private GoogleApiClient googleCastApiClient;
	private final String castAppId;

	public GoogleCastApp(GoogleApiClient googleApiClient, String castAppId) {
		this.googleCastApiClient = googleApiClient;
		this.castAppId = castAppId;
	}

	public boolean isApplicationStarted() {
		return applicationStarted;
	}

	public interface RemoteDisplayListener {
		void onConnectSuccessed(Display display);
		void onConnectFailed(Status status);
	}

	
	public void launcheApplication(final ResultCallback<Cast.ApplicationConnectionResult> resultCallback, LaunchOptions launchOptions, final RemoteDisplayListener remoteDisplayListener) {
		if (googleCastApiClient != null && !applicationStarted) {
			try {
				Log.d(TAG, "launching application("+castAppId);
				Cast.CastApi.launchApplication(googleCastApiClient, castAppId, launchOptions)
				.setResultCallback(
						new ResultCallback<Cast.ApplicationConnectionResult>() {
							@Override
							public void onResult(Cast.ApplicationConnectionResult result) {
								Status status = result.getStatus();
								Log.d(TAG, "launchApplication("+castAppId+").onResult:"+status);
								if (status.isSuccess()) {
									ApplicationMetadata applicationMetadata = result.getApplicationMetadata();
									String sessionId = result.getSessionId();
									String applicationStatus = result.getApplicationStatus();
									boolean wasLaunched = result.getWasLaunched();
									applicationStarted = true;

									// Don't start remote display again when it was already launched before
									if (wasLaunched && GoogleCastFinder.CAST_REMOTEDISPLAY_APPID.equals(castAppId)) {
										startRemoteDisplay(remoteDisplayListener);
									}
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
		} else {
			if (resultCallback != null) {
				resultCallback.onResult(null);
			}
		}
	}

	public void stopApplication(final ResultCallback<Status> resultCallback) {
		if (googleCastApiClient != null && applicationStarted) {
			applicationStarted = false;
			if (googleCastApiClient != null && googleCastApiClient.isConnected()) {
				Log.d(TAG, "stopping application("+castAppId);
				PendingResult<Status> pendingResult = Cast.CastApi.stopApplication(googleCastApiClient);
				pendingResult.setResultCallback(new ResultCallback<Status>() {

					@Override
					public void onResult(Status result) {
						if (resultCallback != null) {
							resultCallback.onResult(result);
						}
					}					
				});
				return;
			}
		}
		if (resultCallback != null) {
			resultCallback.onResult(null);
		}
	}


	public String getAppId() {
		return castAppId;
	}

	private void startRemoteDisplay(final RemoteDisplayListener listener) {
		PendingResult<CastRemoteDisplay.CastRemoteDisplaySessionResult> result =
				CastRemoteDisplay.CastRemoteDisplayApi.startRemoteDisplay(googleCastApiClient, getAppId());
		result.setResultCallback(new ResultCallbacks<CastRemoteDisplay.CastRemoteDisplaySessionResult>() {
			@Override
			public void onSuccess(@NonNull CastRemoteDisplay.CastRemoteDisplaySessionResult castRemoteDisplaySessionResult) {
				Display remoteDisplay = castRemoteDisplaySessionResult.getPresentationDisplay();
				listener.onConnectSuccessed(remoteDisplay);
			}

			@Override
			public void onFailure(@NonNull Status status) {
				Log.i(TAG, "Stop Casting because startRemoteDisplay failed");
				listener.onConnectFailed(status);
			}
		});
	}
}