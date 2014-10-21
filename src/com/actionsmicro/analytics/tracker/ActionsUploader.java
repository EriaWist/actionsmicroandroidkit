package com.actionsmicro.analytics.tracker;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONException;
import org.json.JSONObject;

import com.actionsmicro.utils.Log;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.StringBody;

public class ActionsUploader {
	private static final String BASE_URL = "https://cloud.iezvu.com";
	private static final String PATH_UPLOAD_LOG = "/cloud/sdk/api";
	private String appSecret;
	private String appKey;
	public interface RequestHandler {

		void onDependencyError(Exception e, String jsonBody);

		void onNetworkError(Exception e, String jsonBody);

		void onSuccuess(String jsonBody);

		void onServerError(Exception e, String jsonBody);

		void onInvalidJson(String errorMsg, String jsonBody);
		
	}
	// make it testable
	private ResultProcessor resultProcessor = new ResultProcessor();
	public class ResultProcessor {

		public void processResult(final String jsonBody, final RequestHandler requestHandler, JSONObject result) {
			Log.d("ActionsUploader.ResultProcessor", result.toString());
			try {
				if (result.has("status")) {
					if (result.getBoolean("status")) {
						if (requestHandler != null) {
							requestHandler.onSuccuess(jsonBody);
						}
					} else {
						String error = result.getString("error");
						if ("json format".equalsIgnoreCase(error) ||
								"decrypt".equalsIgnoreCase(error) ||
								"record format".equalsIgnoreCase(error)) {
							if (requestHandler != null) {
								requestHandler.onInvalidJson(error, jsonBody);
							}									
						}
					}
				} else {
					if (requestHandler != null) {
						requestHandler.onServerError(null, jsonBody);
					}
				}
			} catch (JSONException e1) {
				e1.printStackTrace();
				if (requestHandler != null) {
					requestHandler.onServerError(e1, jsonBody);
				}
			} finally {
		
			}
		}
		
	}
	
	public ActionsUploader(String appKey, String appSecret) {
		this.appKey = appKey;
		this.appSecret = appSecret;
	}
	private String computeHash(long expire) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return HashUtils.SHA1(appSecret+"\t"+String.valueOf(expire)+"\t"+PATH_UPLOAD_LOG);
	}
	public void uploadLog(final String jsonBody, final RequestHandler requestHandler) {
		long expire = System.currentTimeMillis() * 1000 + 60;
		try {
			AsyncHttpPost post = new AsyncHttpPost(BASE_URL+PATH_UPLOAD_LOG+"?"+"key="+appKey+"&e="+expire+"&c="+computeHash(expire));
			post.setBody(new StringBody(jsonBody));
			AsyncHttpClient.getDefaultInstance().executeJSONObject(post, new JSONObjectCallback() {
			    @Override
				public void onCompleted(Exception e, AsyncHttpResponse source,
						JSONObject result) {
					if (e != null) {
			            e.printStackTrace();
			            if (requestHandler != null) {
			            	requestHandler.onNetworkError(e, jsonBody);
			            }
			            return;
			        }
			        Log.d("ActionsUploader", "Server says: " + result);
			        resultProcessor.processResult(jsonBody, requestHandler, result);
				}
			});
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			if (requestHandler != null) {
	            requestHandler.onDependencyError(e, jsonBody);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			if (requestHandler != null) {
	            requestHandler.onDependencyError(e, jsonBody);
			}
		}

	}
}
