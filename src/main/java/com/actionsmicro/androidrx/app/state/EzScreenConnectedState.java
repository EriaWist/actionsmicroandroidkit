package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class EzScreenConnectedState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		return null;
	}

	@Override
	public State onEzScreenClientDisconnected(StateContext stateContext) {
		stateContext.resetToStandby();
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State onLoadAirPlayVideo(StateContext stateContext, String url,
			float rate, float position) {
		stateContext.loadVideo(url, rate, position);
		return new AirPlayPlayVideoState();
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		return new AirTunesConnectedState();
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
			InetAddress remoteAddress) {
		return null;
	}

	@Override
	public State onStopMirroring(StateContext stateContext) {
		return null;
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		return null;
	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		return null;
	}

	@Override
	public State displayPhoto(StateContext stateContext, byte[] jpeg, String assetKey,
			String transition) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State cachePhoto(StateContext stateContext, String assetKey,
			byte[] jpeg) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State displayCached(StateContext stateContext, String assetKey,
			String transition, Boolean result) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State onDisplayUrl(StateContext stateContext, String url) {
		stateContext.displayUrl(url);
		return null;
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
		stateContext.loadEzScreenVideo(url, callback);
		return null;
	}

	@Override
	public State onAirPlayStart(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}
}