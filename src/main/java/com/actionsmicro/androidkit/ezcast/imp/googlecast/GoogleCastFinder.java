package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import java.util.ArrayList;
import java.util.List;

import android.support.v7.media.MediaControlIntent;
import android.support.v7.media.MediaRouteSelector;
import android.support.v7.media.MediaRouter;
import android.support.v7.media.MediaRouter.Callback;
import android.support.v7.media.MediaRouter.RouteInfo;

import com.actionsmicro.androidkit.ezcast.DeviceFinder;
import com.actionsmicro.androidkit.ezcast.DeviceFinderBase;
import com.actionsmicro.androidkit.ezcast.DeviceInfo;
import com.actionsmicro.utils.Log;
import com.google.android.gms.cast.CastMediaControlIntent;

public class GoogleCastFinder extends DeviceFinderBase {

	public static final String CAST_APP_ID = "E3A71BDC"; //TODO move to app
	public static final String CAST_REMOTEDISPLAY_APPID = "F5836052";
	public static final String CAST_MEDIA_PLAYER_ID = "D3D8AEDC";
	public static final String CAST_DEV_APP_ID = "E3A71BDC";//"B8CFAD80";
	private MediaRouter mediaRouter;
	private Callback callback;
	private MediaRouteSelector mediaSelector = new MediaRouteSelector.Builder()
			.addControlCategory(CastMediaControlIntent.categoryForCast(CAST_APP_ID))
			.addControlCategory(CastMediaControlIntent.categoryForCast(CAST_REMOTEDISPLAY_APPID))
			.addControlCategory(CastMediaControlIntent.categoryForRemotePlayback())
			.build();
	protected String TAG = "GoogleCastFinder";
	public GoogleCastFinder(DeviceFinder deviceFinderProxy) {
		super(deviceFinderProxy);
		mediaRouter = MediaRouter.getInstance(getDeviceFinderProxy().getContext());
	}

	@Override
	public List<DeviceInfo> getDevices() {
		ArrayList<DeviceInfo> devices = new ArrayList<DeviceInfo>();
		for (RouteInfo routeInfo : mediaRouter.getRoutes()) {
			Log.d(TAG, "evaluate route:" + routeInfo);
			if (routeInfo.matchesSelector(mediaSelector)) {
				devices.add(new GoogleCastDeviceInfo(routeInfo));
			}
		}
		return devices;
	}

	@Override
	public void stop() {
		if (callback != null) {
			mediaRouter.removeCallback(callback);
			callback = null;
		}
	}

	@Override
	public void search() {
		stop();
		if (callback == null) {
			Log.d(TAG, "Add route callback");
			mediaRouter.addCallback(mediaSelector, callback = new Callback() {
				@Override
				public void onRouteAdded (MediaRouter router, MediaRouter.RouteInfo route) {
					Log.d(TAG, "onRouteAdded:"+route.getName());
					GoogleCastDeviceInfo device = new GoogleCastDeviceInfo(route);
					notifiyDeviceAddIfNeed(device);
				}
				@Override
				public void onRouteRemoved (MediaRouter router, MediaRouter.RouteInfo route) {
					Log.d(TAG, "onRouteRemoved:"+route.getName());
					getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(new GoogleCastDeviceInfo(route));					
				}
				@Override
				public void onRouteChanged(MediaRouter router, RouteInfo route) {
					Log.d(TAG, "onRouteChanged:"+route.getName());
					GoogleCastDeviceInfo device = new GoogleCastDeviceInfo(route);
					notifiyDeviceAddIfNeed(device);
				}

				private void notifiyDeviceAddIfNeed(GoogleCastDeviceInfo device) {
					String modelName = device.getCastDevice().getModelName();
					if (device.getCastDevice().isOnLocalNetwork() && "Chromecast".equals(modelName)) {
						getDeviceFinderProxy().notifyListeneroOnDeviceAdded(device);
					}
				}

			}, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN | MediaRouter.CALLBACK_FLAG_REQUEST_DISCOVERY);
		}
	}

	@Override
	public void search(String targetHost) {

	}

}
