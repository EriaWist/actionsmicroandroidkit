package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirTunesConnectedState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
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
		return new AirPlayPlayVideoWhenAirTunesConnectedState();
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		return null;
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		stateContext.informDelegateDisconnected();
		return new IdleState();
	}

	@Override
	public State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title) {
		stateContext.showMusicView();
		stateContext.updateAirTunesMetadata(albumName, artist, title);
		return new AirTunesPlayMusicState();
	}

	@Override
	public State onReceiveAirTunesCoverArt(StateContext stateContext,
			byte[] byteArray) {
		stateContext.updateAirTunesCoverArt(byteArray);
		return null;
	}

	@Override
	public State onStartMirroring(StateContext stateContext,
			InetAddress remoteAddress) {
		stateContext.doAirPlayMirror(remoteAddress);
		return new AirPlayMirrorState();
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
		return new EzScreenConnectedState();
	}
}
