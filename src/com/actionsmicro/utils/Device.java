package com.actionsmicro.utils;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.util.DisplayMetrics;
import android.view.Surface;
import android.view.WindowManager;

public class Device {
	
	private static final String TAG = "Device";

	static public int getDeviceNaturlOrientation(Activity activity) {

		WindowManager windowManager = activity.getWindowManager();

		Configuration cfg = activity.getResources().getConfiguration();
		int lRotation = windowManager.getDefaultDisplay().getRotation();
		Log.d(TAG, "lRotation:"+lRotation+", cfg.orientation:"+cfg.orientation);
		DisplayMetrics dm = new DisplayMetrics();
		windowManager.getDefaultDisplay().getMetrics(dm);
		Log.d(TAG, "dm.widthPixels:"+dm.widthPixels+", dm.heightPixels:"+dm.heightPixels);
		if( (((lRotation == Surface.ROTATION_0) ||(lRotation == Surface.ROTATION_180)) &&   
				(cfg.orientation == Configuration.ORIENTATION_LANDSCAPE)) ||
				(((lRotation == Surface.ROTATION_90) ||(lRotation == Surface.ROTATION_270)) &&    
						(cfg.orientation == Configuration.ORIENTATION_PORTRAIT))){

			return Configuration.ORIENTATION_LANDSCAPE;
		}     

		return Configuration.ORIENTATION_PORTRAIT;
	}

	static public boolean isTablet(Context context) {
	    boolean xlarge = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE);
	    boolean large = ((context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE);
	    return (xlarge || large);
	}
}
