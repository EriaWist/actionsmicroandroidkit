package com.actionsmicro.androidkit.ezcast.imp.googlecast;

import java.util.ArrayList;
import java.util.List;

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
	public static final String CAST_MEDIA_PLAYER_ID = "D3D8AEDC";
	public static final String CAST_DEV_APP_ID = "E3A71BDC";//"B8CFAD80"; 
	private MediaRouter mediaRouter;
	private Callback callback;
	private MediaRouteSelector mediaSelector = new MediaRouteSelector.Builder()
	.addControlCategory(CastMediaControlIntent.categoryForCast(CAST_APP_ID))
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
			
			mediaRouter.addCallback(mediaSelector, callback = new Callback() {
				@Override
				public void onRouteAdded (MediaRouter router, MediaRouter.RouteInfo route) {
					Log.d(TAG, "onRouteAdded:"+route.getName());
					getDeviceFinderProxy().notifyListeneroOnDeviceAdded(new GoogleCastDeviceInfo(route));
				}
				@Override
				public void onRouteRemoved (MediaRouter router, MediaRouter.RouteInfo route) {
					Log.d(TAG, "onRouteRemoved:"+route.getName());
					getDeviceFinderProxy().notifyListeneroOnDeviceRemoved(new GoogleCastDeviceInfo(route));					
				}
			}, MediaRouter.CALLBACK_FLAG_PERFORM_ACTIVE_SCAN);
		}
	}

}
