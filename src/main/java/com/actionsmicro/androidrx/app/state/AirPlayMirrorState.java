package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirPlayMirrorState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
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
		stateContext.hideMirrorView();
		stateContext.loadVideo(url, rate, position);
		return new AirPlayPlayVideoWhenMirrorState();
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
		stateContext.showMusicView();
		stateContext.updateAirTunesMetadata(albumName, artist, title);
		return new AirTunesPlayMusicWhenMirrorState();
	}

	@Override
	public State onReceiveAirTunesCoverArt(StateContext stateContext,
			byte[] byteArray) {
		stateContext.updateAirTunesCoverArt(byteArray);
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
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		return null;
	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State displayPhoto(StateContext stateContext, byte[] jpeg, String assetKey,
			String transition) {
		stateContext.displayPhoto(jpeg, assetKey, transition);
		return new AirPlayPhotoWhenMirrorState();
	}

	@Override
	public State cachePhoto(StateContext stateContext, String assetKey,
			byte[] jpeg) {
		stateContext.cacheImage(assetKey, jpeg);
		return new AirPlayPhotoWhenMirrorState();
	}

	@Override
	public State displayCached(StateContext stateContext, String assetKey,
			String transition, Boolean result) {
		result = stateContext.displayCached(assetKey, transition);
		return new AirPlayPhotoWhenMirrorState();
	}

	@Override
	public State onDisplayUrl(StateContext stateContext, String url) {
		stateContext.stopMirror();
		stateContext.displayUrl(url);
		return new EzScreenConnectedState();
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
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
