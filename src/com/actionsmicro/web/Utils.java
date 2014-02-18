package com.actionsmicro.web;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.actionsmicro.utils.InputStreamKnownSizeBody;
import com.actionsmicro.utils.Log;

public class Utils {
	public static HttpResponse uploadInputStreamToServer(InputStream source, String fileName, String server, String dir) throws IOException {
		HttpResponse response = null;
		try {
			URI dest = new URI("http://"+server + dir);
			final HttpParams httpParams = new BasicHttpParams();
		    HttpConnectionParams.setConnectionTimeout(httpParams, 1000);
		    HttpConnectionParams.setSoTimeout(httpParams, 1000);
			HttpClient httpclient = new DefaultHttpClient(httpParams);
			HttpPost httppost = new HttpPost(dest);
			MultipartEntity reqEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE);
			reqEntity.addPart("fileName", new InputStreamKnownSizeBody(source, 30000000, fileName));
			httppost.setEntity(reqEntity);
			response = httpclient.execute(httppost);
			Log.d("uploadFileToServer", "uploading "+source+" to "+ server +" done");			
		} catch (Throwable e) {
			Log.d("uploadFileToServer", "exception:" + e);
			e.printStackTrace();
		}
		return response;
	}
}
