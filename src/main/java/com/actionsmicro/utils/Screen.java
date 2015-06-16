package com.actionsmicro.utils;

import java.lang.reflect.Method;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Point;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;

public class Screen {
	public static int calcRotationForBuffer(Context context, int width, int height)  {
		WindowManager wm = (WindowManager)context.getSystemService(Context.WINDOW_SERVICE);
		Display disp = wm.getDefaultDisplay();

		// check whether we operate under Android 2.2 or later
		try {
			Class<?> displayClass = disp.getClass();
			Method getRotation = displayClass.getMethod("getRotation");
			int rot = ((Integer)getRotation.invoke(disp)).intValue();

			if ((width>height && disp.getWidth()>disp.getHeight()) ||
				(height>width && disp.getHeight()>disp.getWidth())) {
				if (rot > Surface.ROTATION_90) {
					return 180;
				} else {
					return 0;
				}
			} else {
				if (rot > Surface.ROTATION_90) {
					return 270;
				} else {
					return 90;
				}
			}
		} catch (NoSuchMethodException e) {
			// no getRotation() method -- fall back to dispation()
			int orientation = disp.getOrientation();

			// Sometimes you may get undefined orientation Value is 0
			// simple logic solves the problem compare the screen
			// X,Y Co-ordinates and determine the Orientation in such cases
			if(orientation==Configuration.ORIENTATION_UNDEFINED){

				Configuration config = context.getResources().getConfiguration();
				orientation = config.orientation;

				if(orientation==Configuration.ORIENTATION_UNDEFINED){
					//if height and widht of screen are equal then
					// it is square orientation
					if(disp.getWidth()==disp.getHeight()){
						orientation = Configuration.ORIENTATION_SQUARE;
					}else{ //if widht is less than height than it is portrait
						if(disp.getWidth() < disp.getHeight()){
							orientation = Configuration.ORIENTATION_PORTRAIT;
						}else{ // if it is not any of the above it will defineitly be landscape
							orientation = Configuration.ORIENTATION_LANDSCAPE;
						}
					}
				}
			}

			return orientation == 1 ? 0 : 90; // 1 for portrait, 2 for landscape
		} catch (Exception e) {
			return 0; // bad, I know ;P
		}
	}
	/**
	 * Control screen lock.
	 * @param activity Current activity.
	 * @param keepScreenOn Set true to prevent screen lock. Otherwise, screen will locked for certain period.
	 */
	public static void setKeepScreenOn(Activity activity, boolean keepScreenOn) {
		if (keepScreenOn) {
			activity.getWindow().
			addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		} else {
			activity.getWindow().
			clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		}
	}
	public static void getResolution(Context context, Point outSize) {
		WindowManager winMgr = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		if (winMgr != null) {
			Display display = winMgr.getDefaultDisplay();
			if (display != null) {
				display.getSize(outSize);
			}
		}
	}
}
