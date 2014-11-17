package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirPlayPhotoState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.hidePhotoView();
		stateContext.showConnectedIndicator();
		return new EzScreenConnectedState();
	}

	@Override
	public State onEzScreenClientDisconnected(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onLoadAirPlayVideo(StateContext stateContext, String url,
			float rate, float position) {
		stateContext.hidePhotoView();
		stateContext.loadVideo(url, rate, position);
		return new AirPlayPlayVideoState();
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onReceiveAirTunesCoverArt(StateContext stateContext,
			byte[] byteArray) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onStartMirroring(StateContext stateContext,
			InetAddress remoteAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onStopMirroring(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		stateContext.hidePhotoView();
		stateContext.informDelegateDisconnected();
		return new IdleState();	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		stateContext.hidePhotoView();
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State displayPhoto(StateContext stateContext, byte[] jpeg, String assetKey,
			String transition) {
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
		stateContext.displayUrl(url);
		return new EzScreenConnectedState();
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
		stateContext.hidePhotoView();
		stateContext.loadEzScreenVideo(url, callback);
		return new EzScreenConnectedState();
	}
}
