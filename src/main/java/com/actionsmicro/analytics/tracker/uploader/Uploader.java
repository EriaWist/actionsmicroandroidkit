package com.actionsmicro.analytics.tracker.uploader;

import org.json.JSONArray;


public interface Uploader {

	public interface RequestHandler {
	
		void onDependencyError(Exception e, String jsonBody);
	
		void onNetworkError(Exception e, String jsonBody);
	
		void onSuccuess(String jsonBody);
	
		void onServerError(Exception e, String jsonBody);
	
		void onInvalidJson(String errorMsg, String jsonBody);
	
		void onRequestError(String error, String jsonBody);

		void onProcessBatchResultEnd(String jsonBody, JSONArray result);
		
	}

	public abstract void uploadLog(String jsonBody,
			Uploader.RequestHandler requestHandler);

}