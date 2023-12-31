package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirPlayPlayVideoState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.stopVideo();
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
		stateContext.loadVideo(url, rate, position);
		return null;
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		return new AirPlayPlayVideoWhenAirTunesConnectedState();
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		return null;
	}

	@Override
	public State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title) {
		stateContext.updateAirTunesMetadata(albumName, artist, title);
		return null;
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
		stateContext.stopVideo();
		stateContext.doAirPlayMirror(remoteAddress);
		return new AirPlayMirrorState();
	}

	@Override
	public State onStopMirroring(StateContext stateContext) {
		return null;
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		stateContext.stopVideo();
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		stateContext.stopVideo();
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State displayPhoto(StateContext stateContext, byte[] jpeg, String assetKey,
			String transition) {
		stateContext.displayPhoto(jpeg, assetKey, transition);
		return new AirPlayPhotoState();
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
		return new AirPlayPhotoState();
	}
	@Override
	public State onDisplayUrl(StateContext stateContext, String url) {
		stateContext.stopVideo();
		stateContext.displayUrl(url);
		return new EzScreenConnectedState();
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
		stateContext.stopVideo();
		stateContext.loadEzScreenVideo(url, callback);
		return new EzScreenConnectedState();
	}

	@Override
	public State onAirPlayStart(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}
}
