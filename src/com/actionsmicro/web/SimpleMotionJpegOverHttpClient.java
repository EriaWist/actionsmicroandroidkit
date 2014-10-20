package com.actionsmicro.web;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.actionsmicro.utils.Log;

public class SimpleMotionJpegOverHttpClient {
	public interface JpegCallback {

		void onJpegAvaiable(byte[] jpegData, int size);
		
	}
	public interface ConnectionCallback {

		void onConnectionFailed(IOException e);

		void onDisconnected();
		
	}
	protected static final String TAG = "SimpleMotionJpegOverHttpClient";
	private Thread clientThread;
	private ConnectionCallback connectionCallback;
	private JpegCallback jpegCallback;
	private boolean stopped;
	private HttpURLConnection urlConnection;
	private static final Pattern numberHeaderPattern = Pattern.compile(".*:\\ *([0-9]+)");
	public SimpleMotionJpegOverHttpClient(String urlString, JpegCallback jpegCallback, ConnectionCallback connectionCallback) throws MalformedURLException {
		this.connectionCallback = connectionCallback;
		this.jpegCallback = jpegCallback;
		final URL url = new URL(urlString);
		clientThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					synchronized (clientThread) {
						clientThread.notifyAll();
					}
					urlConnection = (HttpURLConnection)url.openConnection();
					ByteBuffer buffer = ByteBuffer.allocate(1024*1024);
					InputStream in = new BufferedInputStream(urlConnection.getInputStream());
					int contentLength = 0;
					do {
						String meta = readLine(in);
						Log.d(TAG, "meta:"+meta);
						if ((meta == null || meta.length() == 0) && contentLength > 0) {
							Log.d(TAG, "jpeg size:"+contentLength);
							readJpegData(buffer, in, contentLength);
							String boundary = readLine(in);
							Log.d(TAG, "boundary:"+boundary);
							contentLength = 0;
						} else {
							if (meta.toLowerCase(Locale.getDefault()).startsWith("content-length")) {
								Matcher matcher = numberHeaderPattern.matcher(meta);
								if (matcher.find()) {
									contentLength = Integer.valueOf(matcher.group(1));
									Log.d(TAG, "content-length:"+contentLength);										
								}
							}
						}
					} while(!stopped);
				} catch (IOException e) {
					if (!stopped) {
						if (SimpleMotionJpegOverHttpClient.this.connectionCallback != null) {
							SimpleMotionJpegOverHttpClient.this.connectionCallback.onConnectionFailed(e);
						}
					}
				} finally {
					stopped = true;
				}
			}

			private void readJpegData(ByteBuffer buffer, InputStream in,
					int contentLength) throws IOException {
				int offset = 0;
				int remaining = contentLength;
				while (remaining > 0) {
					int read = in.read(buffer.array(), offset, remaining);
					if (read == -1) {
						if (SimpleMotionJpegOverHttpClient.this.connectionCallback != null) {
							SimpleMotionJpegOverHttpClient.this.connectionCallback.onDisconnected();
						}
						break;
					} else {
						remaining -= read;
						offset += read;
					}
				}
				if (SimpleMotionJpegOverHttpClient.this.jpegCallback != null && contentLength > 0) {
					SimpleMotionJpegOverHttpClient.this.jpegCallback.onJpegAvaiable(buffer.array(), contentLength);
				}
			}

			private String readLine(InputStream in)
					throws IOException {
				Log.d(TAG, "readLine begin");
				String meta;
				StringBuilder sb = new StringBuilder();
				do {
					int singleChar = in.read();
					if (singleChar == -1) {
						break;
					} else if (singleChar == '\n' || singleChar == '\r') {
						if (singleChar == '\r') {
							singleChar = in.read();
						}
						break;
					} else {
						sb.append((char)singleChar);
					}
				} while (!stopped);
				meta = sb.toString();
				Log.d(TAG, "readLine end");
				return meta;
			}
			
		});
		clientThread.start();
		synchronized (clientThread) {
			try {
				clientThread.wait();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	public void stop() {
		if (!stopped) {
			stopped = true;
			try {
				clientThread.join();
			} catch (InterruptedException e) {
			}
		}
	}
}
