package com.actionsmicro.falcon;

public abstract class DataConsumer {
	private DataProvider provider;
	public final void setProvider(DataProvider provider) {
		this.provider = provider;
	}
	public final DataProvider getProvider() {
		return provider;
	}
	public abstract void start();
	public abstract void stop();
	public interface DataConsumerDidEndListener {
		public abstract void dataConsumerDidEnd(DataConsumer dataConsumer);
	}
	private DataConsumerDidEndListener dataConsumerDidEndListener; 
	public DataConsumerDidEndListener getDataConsumerDidEndListener() {
		return dataConsumerDidEndListener;
	}
	public void setDataConsumerDidEndListener(DataConsumerDidEndListener dataConsumerDidEndListener) {
		this.dataConsumerDidEndListener = dataConsumerDidEndListener;
	}
}
