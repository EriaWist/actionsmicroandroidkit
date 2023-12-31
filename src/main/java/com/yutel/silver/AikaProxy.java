package com.yutel.silver;

import com.yutel.silver.exception.AirplayException;
import com.yutel.silver.vo.AirplayState;

public class AikaProxy implements Aika.AikaConnectListener,
		Aika.AikaControlListener {
	private Aika.AikaConnectListener connectListener;
	private Aika.AikaControlListener controlListener;

	public AikaProxy() {
	}

	public Aika.AikaConnectListener getConnectListener() {
		return connectListener;
	}

	public void setConnectListener(Aika.AikaConnectListener connectListener) {
		this.connectListener = connectListener;
	}

	public Aika.AikaControlListener getControlListener() {
		return controlListener;
	}

	public void setControlListener(Aika.AikaControlListener controlListener) {
		this.controlListener = controlListener;
	}
	
	public void clearControlListener() {
		controlListener = null;
	}

	/**
	 * pair-setup
	 */
	@Override
	public byte[] pairSetup() {
		return connectListener.pairSetup();
	}

	/**
	 * pair-verify
	 */
	@Override
	public byte[] pairVerify(byte[] requestBody) {
		return connectListener.pairVerify(requestBody);
	}

	/**
	 * connect
	 */
	@Override
	public void video(String url, String rate, String pos)
			throws AirplayException {
		connectListener.video(url, rate, pos);
	}

	@Override
	public void photo() throws AirplayException {
		connectListener.photo();
	}

	/*
	 * control
	 */
	@Override
	public void videoStop() throws AirplayException {
		if (controlListener != null) {
			controlListener.videoStop();
		}
	}

	@Override
	public void videoPause() throws AirplayException {
		if (controlListener != null) {
			controlListener.videoPause();
		}
	}

	@Override
	public void videoResume() throws AirplayException {
		if (controlListener != null) {
			controlListener.videoResume();
		}
	}

	@Override
	public void videoSeek(int position) throws AirplayException {
		if (controlListener != null) {
			controlListener.videoSeek(position);
		}
	}

	@Override
	public int videoStatus() throws AirplayException {
		if (controlListener != null) {
			return controlListener.videoStatus();
		}
		return AirplayState.CACHING;
	}

	@Override
	public int videoPostion() throws AirplayException {
		if (controlListener != null) {
			return controlListener.videoPostion();
		}
		return AirplayState.ERROR;
	}

	@Override
	public int videoDuration() throws AirplayException {
		if (controlListener != null) {
			return controlListener.videoDuration();
		}
		return AirplayState.ERROR;
	}

	public void onAirPlayStop() {
		if (controlListener != null) {
			controlListener.onAirPlayStop();
		}
	}

	public void onAirPlayStart() {
		if (controlListener != null) {
			controlListener.onAirPlayStart();
		}
	}

	public void setVolume(float volume) {
		if (controlListener != null) {
			controlListener.setVolume(volume);
		}
	}

	public void displayPhoto(byte[] jpeg, String assetKey, String transition) {
		if (controlListener != null) {
			controlListener.displayPhoto(jpeg, assetKey, transition);
		}
	}

	public void cachePhoto(String assetKey, byte[] jpeg) {
		if (controlListener != null) {
			controlListener.cachePhoto(assetKey, jpeg);
		}
	}

	public boolean displayCached(String assetKey, String transition) {
		if (controlListener != null) {
			return controlListener.displayCached(assetKey, transition);
		}
		return false;
	}

}
