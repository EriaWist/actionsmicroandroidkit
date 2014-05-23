package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class AirTunesPlayMusicWhenMirrorState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.stopMusic();
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
		return new AirPlayPlayVideoWhenMirrorState();
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		return null;
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		// TODO undefined behavior
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
			InetAddress remoteAddress) {
		stateContext.hideMusicView();
		stateContext.doAirPlayMirror(remoteAddress);
		return new AirPlayMirrorState();
	}

	@Override
	public State onStopMirroring(StateContext stateContext) {
		return new AirTunesPlayMusicState();
	}

	@Override
	public State onStopAirPlayVideo(StateContext stateContext) {
		return null;
	}

	@Override
	public State onAirPlayStop(StateContext stateContext) {
		return null;
	}
}
