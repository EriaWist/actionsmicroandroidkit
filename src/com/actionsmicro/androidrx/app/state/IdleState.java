package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public class IdleState implements State {

	@Override
	public State onEzScreenClientConnected(StateContext stateContext) {
		stateContext.showConnectedIndicator();
		stateContext.informDelegateConnected();
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
		stateContext.informDelegateConnected();
		return new AirPlayPlayVideoState();
	}

	@Override
	public State onStartAirTunes(StateContext stateContext,
			InetAddress inetAddress) {
		stateContext.showConnectedIndicator();
		stateContext.informDelegateConnected();
		return new AirTunesConnectedState();
	}

	@Override
	public State onStopAirTunes(StateContext stateContext) {
		return null;
	}

	@Override
	public State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title) {
//		when switching between Music/Mirror, rstp connections may intercept in current system. 		
//		05-23 10:14:33.698 D/StateContext(13310): state==>com.actionsmicro.androidrx.app.state.AirPlayMirrorState@441c2718
//		05-23 10:14:41.348 D/StateContext(13310): onStopMirroring
//		05-23 10:14:41.863 D/StateContext(13310): state==>com.actionsmicro.androidrx.app.state.IdleState@444436d8
//		05-23 10:14:41.863 D/StateContext(13310): onStartAirTunes
//		05-23 10:14:41.863 D/StateContext(13310): state==>com.actionsmicro.androidrx.app.state.AirTunesConnectedState@449030a8
//		05-23 10:14:41.868 D/StateContext(13310): onStopAirTunes
//		05-23 10:14:41.868 D/StateContext(13310): state==>com.actionsmicro.androidrx.app.state.IdleState@4446e6e8
//		05-23 10:14:44.388 D/StateContext(13310): onReceiveAirTunesCoverArt
//		05-23 10:14:44.588 D/StateContext(13310): onReceiveAirTunesMetadata
		stateContext.showMusicView();
		stateContext.updateAirTunesMetadata(albumName, artist, title);
		stateContext.informDelegateConnected();
		return new AirTunesPlayMusicState();
	}

	@Override
	public State onReceiveAirTunesCoverArt(StateContext stateContext,
			byte[] byteArray) {
//		when switching between Music/Mirror, rstp connections may intercept in current system. 		
		stateContext.updateAirTunesCoverArt(byteArray);
		stateContext.informDelegateConnected();
		return new AirTunesConnectedState();
	}

	@Override
	public State onStartMirroring(StateContext stateContext,
			InetAddress remoteAddress) {
//		when switching between Music/Mirror, rstp connections may intercept in current system. 		
//		05-22 18:29:29.218 D/StateContext(10041): onStopMirroring
//		05-22 18:29:39.603 D/StateContext(10041): state==>com.actionsmicro.androidrx.app.state.IdleState@45277d58
//		05-22 18:29:39.608 D/StateContext(10041): onStopAirTunes
//		05-22 18:29:39.608 D/StateContext(10041): onStartAirTunes
//		05-22 18:29:39.608 D/StateContext(10041): state==>com.actionsmicro.androidrx.app.state.AirTunesConnectedState@452781f8
//		05-22 18:29:42.053 D/StateContext(10041): onReceiveAirTunesCoverArt
//		05-22 18:29:42.303 D/StateContext(10041): onReceiveAirTunesMetadata
//		05-22 18:29:42.303 D/StateContext(10041): state==>com.actionsmicro.androidrx.app.state.AirTunesPlayMusicState@44475c40
//		05-22 18:29:58.863 D/StateContext(10041): onStartAirTunes
//		05-22 18:29:58.868 D/StateContext(10041): onStopAirTunes
//		05-22 18:29:58.873 D/StateContext(10041): state==>com.actionsmicro.androidrx.app.state.IdleState@457b0ca8
//		05-22 18:30:00.943 D/StateContext(10041): onStartMirroring
//		05-22 18:31:23.308 D/StateContext(10041): onStopMirroring
//		05-22 18:31:23.403 D/StateContext(10041): onStopAirTunes
		stateContext.doAirPlayMirror(remoteAddress);
		stateContext.informDelegateConnected();
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
		stateContext.informDelegateConnected();
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
		stateContext.informDelegateConnected();
		return new AirPlayPhotoState();
	}
}
