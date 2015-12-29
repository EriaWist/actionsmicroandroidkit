package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirPlayPhotoWhenMirrorState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.hidePhotoView();
		stateContext.stopMirror();
		stateContext.showConnectedIndicator();
		return new EzScreenConnectedState();
	}

	@Override
	public State onEzScreenClientDisconnected(StateContext stateContext) {
		return null;
	}

	@Override
	public State onLoadAirPlayVideo(StateContext stateContext, String url,
			float rate, float position) {
		return null;
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		return null;
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		return null;
	}

	@Override
	public State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title) {
		return null;
	}

	@Override
	public State onReceiveAirTunesCoverArt(StateContext stateContext,
			byte[] byteArray) {
		return null;
	}

	@Override
	public State onStartMirroring(StateContext stateContext,
								  InetAddress remoteAddress, int ntpPort) {
		return null;
	}

	@Override
	public State onStopMirroring(StateContext stateContext) {
		stateContext.stopMirror();
		return new AirPlayPhotoState();
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		stateContext.hidePhotoView();
		stateContext.showMirrorView();
		return new AirPlayMirrorState();
	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		return null;
	}

	@Override
	public State displayPhoto(StateContext stateContext, byte[] jpeg,
			String assetKey, String transition) {
		stateContext.displayPhoto(jpeg, assetKey, transition);
		return null;
	}

	@Override
	public State cachePhoto(StateContext stateContext, String assetKey,
			byte[] jpeg) {
		stateContext.cacheImage(assetKey, jpeg);
		return null;
	}

	@Override
	public State displayCached(StateContext stateContext, String assetKey,
			String transition, Boolean result) {
		result = stateContext.displayCached(assetKey, transition);
		return null;
	}

	@Override
	public State onDisplayUrl(StateContext stateContext, String url) {
		stateContext.hidePhotoView();
		stateContext.stopMirror();
		stateContext.displayUrl(url);
		return new EzScreenConnectedState();
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
		stateContext.hidePhotoView();
		stateContext.stopMirror();
		stateContext.loadEzScreenVideo(url, callback);
		return new EzScreenConnectedState();
	}

	@Override
	public State onAirPlayStart(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}

}
