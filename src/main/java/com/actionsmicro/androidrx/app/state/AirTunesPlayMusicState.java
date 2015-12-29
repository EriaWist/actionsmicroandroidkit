package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirTunesPlayMusicState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.stopMusic();
		stateContext.hideMusicView();
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
		stateContext.hideMusicView();
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
		stateContext.hideMusicView();
		stateContext.informDelegateDisconnected();
		return new IdleState();
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
		stateContext.hideMusicView();
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
		stateContext.stopMusic();
		stateContext.hideMusicView();
		stateContext.displayUrl(url);
		return new EzScreenConnectedState();
	}

	@Override
	public State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback) {
		stateContext.stopMusic();
		stateContext.hideMusicView();
		stateContext.loadEzScreenVideo(url, callback);
		return new EzScreenConnectedState();
	}

	@Override
	public State onAirPlayStart(StateContext stateContext) {
		// TODO Auto-generated method stub
		return null;
	}
}
