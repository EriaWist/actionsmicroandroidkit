package com.actionsmicro.androidrx.app.state;

import java.net.InetAddress;

public interface State {

	State onEzScreenClientConnected(StateContext stateContext);

	State onEzScreenClientDisconnected(StateContext stateContext);

	State onLoadAirPlayVideo(StateContext stateContext, String url, float rate,
			float position);

	State onStartAirTunes(StateContext stateContext, InetAddress inetAddress);

	State onStopAirTunes(StateContext stateContext);

	State onReceiveAirTunesMetadata(StateContext stateContext,
			String albumName, String artist, String title);

	State onReceiveAirTunesCoverArt(StateContext stateContext, byte[] byteArray);

	State onStartMirroring(StateContext stateContext, InetAddress remoteAddress, int ntpPort);

	State onStopMirroring(StateContext stateContext);

	State onStopAirPlayVideo(StateContext stateContext);

	State onAirPlayStop(StateContext stateContext);

	State displayPhoto(StateContext stateContext, byte[] jpeg, String assetKey, String transition);

	State cachePhoto(StateContext stateContext, String assetKey, byte[] jpeg);

	State displayCached(StateContext stateContext, String assetKey, String transition, Boolean result);

	State onDisplayUrl(StateContext stateContext, String url);

	State onLoadEzScreenVideo(StateContext stateContext, String url,
			String callback);

	State onAirPlayStart(StateContext stateContext);

}
