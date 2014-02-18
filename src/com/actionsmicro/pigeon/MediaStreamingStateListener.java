package com.actionsmicro.pigeon;

import com.actionsmicro.pigeon.MediaStreaming.DataSource;


public interface MediaStreamingStateListener {

	public abstract void mediaStreamingDidStart(
			DataSource dataSource);

	public abstract void mediaStreamingDidStop(
			DataSource dataSource);

	public abstract void medisStreamingFail(
			DataSource dataSource, int resultCode);

	public abstract void medisStreamingTimeDidChange(
			DataSource dataSource, int time);

	public abstract void medisStreamingDurationIsReady(
			DataSource dataSource, int duration);

}