package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

import com.actionsmicro.utils.Log;

public abstract class StateContext {
	private static final String TAG = "StateContext";
	private State currentState;
	public StateContext(State initialState) {
		currentState = initialState;
	}
	public synchronized void onEzScreenClientConnected() {
		Log.d(TAG, "onEzScreenClientConnected");
		setCurrentState(currentState.onEzScreenClientConnected(this));
	}
	private synchronized void setCurrentState(State nextState) {
		if (nextState != null) {			
			currentState = nextState;
			Log.d(TAG, "state==>"+nextState.toString());
		}
	}
	protected abstract void showConnectedIndicator();
	protected abstract void informDelegateConnected();
	public synchronized void onEzScreenClientDisconnected() {
		Log.d(TAG, "onEzScreenClientDisconnected");
		setCurrentState(currentState.onEzScreenClientDisconnected(this));
	}
	protected abstract void resetToStandby();
	protected abstract void informDelegateDisconnected();
	public synchronized void onLoadAirPlayVideo(String url, float rate, float position) {
		Log.d(TAG, "onLoadAirPlayVideo");
		setCurrentState(currentState.onLoadAirPlayVideo(this, url, rate, position));
	}
	protected abstract void loadVideo(String url, float rate, float position);
	public synchronized void onStartAirTunes(InetAddress inetAddress) {
		Log.d(TAG, "onStartAirTunes");
		setCurrentState(currentState.onStartAirTunes(this, inetAddress));
	}
	public synchronized void onStopAirTunes() {
		Log.d(TAG, "onStopAirTunes");
		setCurrentState(currentState.onStopAirTunes(this));
	}
	public synchronized void onReceiveAirTunesMetadata(String albumName, String artist,
			String title) {
		Log.d(TAG, "onReceiveAirTunesMetadata");
		setCurrentState(currentState.onReceiveAirTunesMetadata(this, albumName, artist, title));
	}
	protected abstract void showMusicView();
	protected abstract void hideMusicView();
	protected abstract void updateAirTunesMetadata(String albumName, String artist,
			String title);
	protected abstract void stopVideo();
	public synchronized void onReceiveAirTunesCoverArt(byte[] byteArray) {
		Log.d(TAG, "onReceiveAirTunesCoverArt");
		setCurrentState(currentState.onReceiveAirTunesCoverArt(this, byteArray));		
	}
	protected abstract void updateAirTunesCoverArt(byte[] byteArray);
	public synchronized void onStartMirroring(InetAddress remoteAddress) {
		Log.d(TAG, "onStartMirroring");
		setCurrentState(currentState.onStartMirroring(this, remoteAddress));
	}
	protected abstract void doAirPlayMirror(InetAddress remoteAddress);
	protected abstract void stopMirror();
	protected abstract void stopMusic();
	public synchronized void onStopMirroring() {
		Log.d(TAG, "onStopMirroring");
		setCurrentState(currentState.onStopMirroring(this));
	}
	public synchronized void onStopAirPlayVideo() {
		Log.d(TAG, "onStopAirPlayVideo");
		setCurrentState(currentState.onStopAirPlayVideo(this));
	}
	protected abstract void showMirrorView();
	public synchronized void onAirPlayStop() {
		Log.d(TAG, "onAirPlayStop");
		setCurrentState(currentState.onAirPlayStop(this));
	}
	public void onDisplayPhoto(byte[] jpeg, String assetKey, String transition) {
		Log.d(TAG, "onDisplayPhoto");
		setCurrentState(currentState.displayPhoto(this, jpeg, assetKey, transition));
	}
	public boolean onDisplayCached(String assetKey, String transition) {
		Log.d(TAG, "onDisplayCached");
		Boolean result = false;
		setCurrentState(currentState.displayCached(this, assetKey, transition, result));
		return result;
	}
	public void onCachePhoto(String assetKey, byte[] jpeg) {
		Log.d(TAG, "onCachePhoto");
		setCurrentState(currentState.cachePhoto(this, assetKey, jpeg));
	}
	protected abstract void displayPhoto(byte[] jpeg, String assetKey, String transition);
	protected abstract void cacheImage(String assetKey, byte[] jpeg);
	protected abstract boolean displayCached(String assetKey, String transition);
	protected abstract void hidePhotoView();
	
}
