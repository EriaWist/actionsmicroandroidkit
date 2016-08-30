package com.actionsmicro.analytics.tracker.uploader;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.analytics.tracker.HashUtils;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.http.AsyncHttpClient;
import com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback;
import com.koushikdutta.async.http.AsyncHttpClient.StringCallback;
import com.koushikdutta.async.http.AsyncHttpPost;
import com.koushikdutta.async.http.AsyncHttpResponse;
import com.koushikdutta.async.http.body.StringBody;

public class ActionsUploader implements Uploader {
	private static final String BASE_URL = "https://cloud.iezvu.com";
	private static final String DEV_BASE_URL = "https://dev-cloud.iezvu.com";
	private static final String PATH_UPLOAD_LOG = "/cloud/sdk/api";
	private String appSecret;
	private String appKey;
	// make it testable
	private ResultProcessor resultProcessor = new ResultProcessor();
	private String packageId;
	public class ResultProcessor {

		public void processResult(final String jsonBody, final Uploader.RequestHandler requestHandler, JSONObject result) {
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
						} else {
							if (requestHandler != null) {
								requestHandler.onRequestError(error, jsonBody);
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

		public void processResult(String jsonBody,
				Uploader.RequestHandler requestHandler, JSONArray result) {
			try {
				JSONArray jsonBodies = new JSONArray(jsonBody);
				if (jsonBodies.length() == result.length()) {
					for (int i = 0; i < jsonBodies.length(); i++) {
						processResult(jsonBodies.get(i).toString(), requestHandler, (JSONObject)result.get(i));
					}						
				} else {
					if (requestHandler != null) {
						requestHandler.onServerError(new Exception("Result count mismatch. Expect "+jsonBodies.length()+", but it's "+result.length()), jsonBody);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			} finally {
				if (requestHandler != null) {
					requestHandler.onProcessBatchResultEnd(jsonBody, result);
				}
			}
		}
		
	}
	
	public ActionsUploader(Context context, String appKey, String appSecret) {
		this.appKey = appKey;
		this.appSecret = appSecret;
		this.packageId = context.getPackageName();
	}
	private String computeHash(long expire) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return HashUtils.EzCastHash(appSecret, expire, PATH_UPLOAD_LOG, packageId);
	}
	@Override
	public void uploadLog(final String jsonBody, final Uploader.RequestHandler requestHandler) {
		long expire = System.currentTimeMillis() * 1000 + 60;
		boolean isArray = false;
		try {
			@SuppressWarnings("unused")
			JSONArray test = new JSONArray(jsonBody);
			isArray = true;
		} catch (JSONException e1) {
		}
		try {
			String uriString = BASE_URL+PATH_UPLOAD_LOG+"?"+"key="+appKey+"&e="+expire+"&c="+computeHash(expire)+"&p=1";
			if (BuildConfig.DEBUG) {
				uriString = DEV_BASE_URL+PATH_UPLOAD_LOG+"?"+"key="+appKey+"&e="+expire+"&c="+computeHash(expire)+"&p=1&validator=1";				
			}
			AsyncHttpPost post = new AsyncHttpPost(uriString);
			post.setTimeout(10000);
			post.setBody(new StringBody(jsonBody));
			if (isArray) {
				AsyncHttpClient.getDefaultInstance().executeString(post, new StringCallback() {

					@Override
					public void onCompleted(Exception e,
							AsyncHttpResponse source, String result) {
						if (e != null) {
							e.printStackTrace();
							if (requestHandler != null) {
								requestHandler.onNetworkError(e, jsonBody);
							}
							return;
						}
						// try to treat it as JSON array
						try {
							JSONArray jsonArrayResult = new JSONArray(result);
							resultProcessor.processResult(jsonBody, requestHandler, jsonArrayResult);
						} catch (JSONException e1) {
							// try to treat it as JSON object
							try {
								JSONObject jsonResult = new JSONObject(result);
								resultProcessor.processResult(jsonBody, requestHandler, jsonResult);
							} catch (JSONException e2) {
								e2.printStackTrace();
								if (requestHandler != null) {
									requestHandler.onServerError(e2, jsonBody);
								}
							} finally {
								if (requestHandler != null) {
									requestHandler.onProcessBatchResultEnd(jsonBody, null);
								}
							}
						}
					}
					
				});
			} else {
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
			}
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
