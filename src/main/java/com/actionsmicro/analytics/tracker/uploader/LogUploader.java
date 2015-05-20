package com.actionsmicro.analytics.tracker.uploader;

import com.actionsmicro.utils.Log;

public class LogUploader implements Uploader {

	@Override
	public void uploadLog(String jsonBody, RequestHandler requestHandler) {
		Log.d("LogUploader", "uploadLog:\n"+jsonBody);
	}

}
