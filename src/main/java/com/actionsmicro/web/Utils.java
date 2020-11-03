package com.actionsmicro.web;

import android.net.Uri;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Utils {
	private static final int SOCKET_OPERATION_TIMEOUT = 5 * 1000;
	private static final String TAG = "Utils";
	private static final String CHARSET = "utf-8";
	private static final int READ_TIME_OUT = 5 * 1000;

	// http://qianzui.github.io/blog/2013-04-17-use-httpurlconnection-implements-file-upload/
	public static String uploadInputStreamToServer(InputStream source, String fileName, String server, String dir) {
		String BOUNDARY = UUID.randomUUID().toString();
		String PREFIX = "--";
		String LINE_END = "\r\n"; //换行符
		String CONTENT_TYPE = "multipart/form-data";
		HttpURLConnection conn = null;
		BufferedReader br = null;
		try {
			URL url = new URL(server + dir);
			if (url.getProtocol().toUpperCase().equals("HTTPS")) {
				trustAllHosts();
				HttpsURLConnection https = (HttpsURLConnection) url
						.openConnection();
				https.setHostnameVerifier(DO_NOT_VERIFY);
				conn = https;
			} else {
				conn = (HttpURLConnection) url.openConnection();
			}

			conn.setReadTimeout(READ_TIME_OUT);
			conn.setConnectTimeout(SOCKET_OPERATION_TIMEOUT);
			conn.setDoInput(true);
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod(HttpPost.METHOD_NAME);
			conn.setRequestProperty("Charset", "utf-8");
			conn.setRequestProperty("connection", "keep-alive");
			conn.setRequestProperty("Content-Type", CONTENT_TYPE + ";boundary=" + BOUNDARY);
			DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
			StringBuffer sb = new StringBuffer();
			sb.append(PREFIX);
			sb.append(BOUNDARY);
			sb.append(LINE_END);
			// name里面的值为服务器端需要的Form表单对应的key
			// filename是文件的名字，包含后缀名的
			sb.append("Content-Disposition: form-data; name=\"fileName\"; filename=\"" + fileName + "\""
					+ LINE_END);
			sb.append("Content-Type: application/octet-stream; charset=" + CHARSET + LINE_END); //此处待优化，应该根据不同文件类型生成不同的Content-Type
			sb.append(LINE_END);
			dos.write(sb.toString().getBytes());
			byte[] bytes = new byte[8 * 1024];
			int len = 0;
			while ((len = source.read(bytes)) != -1) {
				dos.write(bytes, 0, len);
			}
			source.close();
			dos.write(LINE_END.getBytes());
			byte[] end_data = (PREFIX + BOUNDARY + PREFIX + LINE_END).getBytes();
			dos.write(end_data);
			dos.flush();
			int responseCode = conn.getResponseCode();
			if (responseCode == HttpStatus.SC_OK) {
				Log.d(TAG, "request success");
				br = new BufferedReader(new InputStreamReader(
						conn.getInputStream()));
				StringBuilder res = new StringBuilder();
				String line = null;
				while ((line = br.readLine()) != null) {
					res.append(line);
				}
				Log.d("uploadFileToServer", "uploading "+source+" to "+ server +" done");
				return res.toString();
			} else {
				Log.d(TAG, "response error");
			}
		} catch (MalformedURLException e) {
			Log.d("uploadFileToServer", "exception:" + e);
			e.printStackTrace();
		} catch (IOException e) {
			Log.d("uploadFileToServer", "exception:" + e);
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (conn != null) {
				conn.disconnect();
			}
		}
		return null;
	}

	private static void trustAllHosts() {
		TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return new java.security.cert.X509Certificate[]{};
			}

			public void checkClientTrusted(X509Certificate[] chain,
										   String authType) throws CertificateException {
			}

			public void checkServerTrusted(X509Certificate[] chain,
										   String authType) throws CertificateException {
			}
		}};
		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("TLS");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static final HostnameVerifier DO_NOT_VERIFY = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession session) {
			return true;
		}
	};
}
