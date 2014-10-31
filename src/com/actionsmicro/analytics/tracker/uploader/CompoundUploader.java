package com.actionsmicro.analytics.tracker.uploader;

import java.util.concurrent.CopyOnWriteArrayList;


public class CompoundUploader implements Uploader {

	private CopyOnWriteArrayList<Uploader> uploaders = new CopyOnWriteArrayList<Uploader>();
	
	@Override
	public void uploadLog(String jsonBody, RequestHandler requestHandler) {
		for (Uploader uploader : uploaders) {
			uploader.uploadLog(jsonBody, requestHandler);
		}
	}
	
	public void add(Uploader uploader) {
		uploaders.add(uploader);
	}
	public void remove(Uploader uploader) {
		uploaders.remove(uploader);
	}
	public void clear() {
		uploaders.clear();
	}
}
