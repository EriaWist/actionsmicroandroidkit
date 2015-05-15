package com.actionsmicro.androidrx.app;

import java.util.Timer;
import java.util.TimerTask;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * TimeoutableLocationListner is implementation of LocationListener.
 * If onLocationChanged isn't called within XX mili seconds, automatically remove listener.
 *
 * @author amay077 - http://twitter.com/amay077
 */
public class TimeoutableLocationListener implements LocationListener {
	public interface TimeoutLisener {
        void onTimeouted(LocationListener sender);
    }
	
    protected Timer timerTimeout = new Timer();
    protected LocationManager locaMan = null;
 
    /**
     * Initialize instance.
     *
     * @param locaMan the base of LocationManager, can't set null.
     * @param timeOutMS timeout elapsed (mili seconds)
     * @param timeoutListener if timeout, call onTimeouted method of this.
     */
    public TimeoutableLocationListener(LocationManager locaMan, long timeOutMS,
            final TimeoutLisener timeoutListener) {
        this.locaMan  = locaMan;
        timerTimeout.schedule(new TimerTask() {
 
            @Override
            public void run() {
                if (timeoutListener != null) {
                    timeoutListener.onTimeouted(TimeoutableLocationListener.this);
                }
                stopLocationUpdateAndTimer();
            }
        }, timeOutMS);
    }
 
    /***
     * Location callback.
     *
     * If override on your concrete class, must call base.onLocation().
     */
    @Override
    public void onLocationChanged(Location location) {
        stopLocationUpdateAndTimer();
        Log.d("TimeoutableLocationListener", "Location changed: latitude = " + location.getLatitude() + ", longtitude = " + location.getLongitude());
    }
 
    @Override
    public void onProviderDisabled(String s) { }
 
    @Override
    public void onProviderEnabled(String s) { }
 
    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) { }
 
    private void stopLocationUpdateAndTimer() {
        locaMan.removeUpdates(this);
        if (timerTimeout != null) {
        	timerTimeout.cancel();
        	timerTimeout.purge();
        	timerTimeout = null;
        }
    }
}