package com.actionsmicro.pigeon;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import com.actionsmicro.pigeon.MediaStreaming.*;
import com.actionsmicro.utils.Log;

public class MediaStreamingHttpFileDataSource implements FileDataSource {
	private MediaStreamingStateListener mediaStreamingStateListener;
	private static final String TAG = "MediaStreamingHttpDataSource";
	final HttpClient client = new DefaultHttpClient();
	private String urlString;
	private long contentLength = -1;
	private String userAgentString;

	public MediaStreamingHttpFileDataSource(String url, String userAgentString, Long contentLength) {
		this.urlString = url;
		this.userAgentString = userAgentString;
		this.contentLength = contentLength;
	}
	public MediaStreamingHttpFileDataSource(String url, Long contentLength) {
		this.urlString = url;
		this.contentLength = contentLength;
	}
	private MediaStreaming mediaStreaming;
	private boolean seekable = true;
	@Override
	public long getContentLength() {
		if (contentLength == -1) {
			fetchContentInfo();
		}
		return contentLength;
	}

	private void fetchContentInfo() {
		try {
			final HttpHead httpHead = new HttpHead(urlString);
			if (userAgentString != null) {
				httpHead.addHeader("User-Agent", userAgentString);
			}
			
			final HttpResponse responseHead = client.execute(httpHead);
			logHeaders("httpHead:" + urlString, responseHead.getAllHeaders());
			final Header contentLengthHeader = responseHead.getFirstHeader("Content-Length");
			if (contentLengthHeader != null) {
				contentLength  = Long.valueOf(contentLengthHeader.getValue());
			}
		    final Header acceptRanges = responseHead.getFirstHeader("Accept-Ranges");
		    if (acceptRanges != null) {
		    	seekable = acceptRanges.getValue().equals("bytes");
		    }			    
		} catch (ClientProtocolException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean isSeekable() {
		return seekable;
	}

	@Override
	public void mediaStreamingDidFail(int resultCode) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingFail(this, resultCode);
		}
	}
	private static void logHeaders(final String title, final Header[] headers) {
		Log.d(TAG, title);
		for (Header header : headers) {
			Log.d(TAG, header.getName() + ":" + header.getValue());
		}
	}
	
	@Override
	public void startStreamingContents(MediaStreaming ms, long offset) {
		Log.d(TAG, "startStreamingContents offset:" + offset);
		mediaStreaming = ms;
		startDownloadIfNeeded(offset);
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStart(this);
		}
	}

	
	@Override
	public void pauseStreamingContents() {
		Log.d(TAG, "pauseStreamingContents");		
		stopDownloadThread();
		stopUploadThread();
	}

	@Override
	public void pauseStreamingContents(long offset) {
		Log.d(TAG, "pauseStreamingContents:"+offset);		
		if (offset != downloadingOffset) {
			stopDownloadThread();
			stopUploadThread();
		}
	}
	@Override
	public void stopStreamingContents() {
		stopDownloadThread();
		stopUploadThread();
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.mediaStreamingDidStop(this);
		}
	}

	@Override
	public void playerTimeDidChange(int time) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingTimeDidChange(this, time);
		}
	}

	@Override
	public void playerTimeDurationReady(int duration) {
		if (mediaStreamingStateListener != null) {
			mediaStreamingStateListener.medisStreamingDurationIsReady(this, duration);
		}
	}
	private long downloadingOffset = -1;
	private Thread downloadThread;
	private boolean shouldDownload;
	private LinkedBlockingQueue<ByteBuffer> linkedBlockingQueue = new LinkedBlockingQueue<ByteBuffer>();
	private Thread uploadThread;
	private boolean shouldUpload;
	protected boolean reachEof;
	private synchronized void stopDownloadThread() {
		if (downloadThread != null) {
			shouldDownload = false;
			try {
				downloadThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			downloadThread = null;
		}
	}
	private synchronized void stopUploadThread() {
		if (uploadThread != null) {
			shouldUpload = false;
			try {
				uploadThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			uploadThread = null;
		}
	}
	private void startDownloadIfNeeded(final long offset) {
		synchronized (this) {
			if (downloadingOffset != offset) {
				stopDownloadThread();
				stopUploadThread();
				shouldDownload = true;
				downloadThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						InputStream inputStream = null;
						try {
							Log.d(TAG, "start HttpGet:"+urlString);
							HttpGet get = new HttpGet(urlString);
							get.addHeader("Range", "bytes="+offset+"-"+contentLength);
							if (userAgentString != null) {
								get.addHeader("User-Agent", userAgentString);
							}
							HttpResponse responseGet = client.execute(get);  
//							logHeaders("HttpGet:" + urlString, responseGet.getAllHeaders());
							HttpEntity resEntityGet = responseGet.getEntity();  
							if (resEntityGet != null) {  
								inputStream = resEntityGet.getContent();
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
						if (inputStream != null) {
							try {
								final int bufferSize = 1024*32;
								final byte[] buffer = new byte [bufferSize];
								int sizeRead = 0;
								long totalRead = 0;
								Log.d(TAG, "start reading" + urlString);	
								reachEof = false;
								do {
									sizeRead = inputStream.read(buffer);
									Log.d(TAG, "inputStream.read:"+sizeRead);
									if (sizeRead == -1) {
										reachEof = true;
										shouldDownload = false;
									} else {
										totalRead += sizeRead;
										final ByteBuffer byteBuffer = ByteBuffer.allocate(sizeRead);
										byteBuffer.put(buffer, 0, sizeRead);
										linkedBlockingQueue.add(byteBuffer);
									}
								} while(shouldDownload);
								Log.d(TAG, "end reading. totalRead:" + totalRead);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						Log.d(TAG, "download thread ends");
					}

				});
				downloadThread.start();
				downloadingOffset = offset;
				startUploadThread();
			}
		}
	}

	private void startUploadThread() {
		shouldUpload = true;
		uploadThread = new Thread(new Runnable() {
			@Override
			public void run() {
				long totalRead = 0;
				Log.d(TAG, "start reading linkedBlockingQueue");	
				do {
					ByteBuffer byteBuffer = null;
					try {
						byteBuffer = linkedBlockingQueue.poll(1, TimeUnit.SECONDS);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					if (byteBuffer == null) {
						if (reachEof) {
							mediaStreaming.sendEofPacket();
							shouldUpload = false;
						}
						
					} else {
						synchronized (this) {
							downloadingOffset = -1;
						}						
						final byte buffer[] = byteBuffer.array();
						Log.d(TAG, "linkedBlockingQueue.read:"+buffer.length);
						totalRead += buffer.length;
						mediaStreaming.sendStreamingContents(buffer, buffer.length);
					}
				} while(shouldUpload);
				Log.d(TAG, "end reading linkedBlockingQueue. totalRead:" + totalRead);
			}

		});
		uploadThread.start();
	}

	public MediaStreamingStateListener getMediaStreamingStateListener() {
		return mediaStreamingStateListener;
	}

	public void setMediaStreamingStateListener(
			MediaStreamingStateListener mediaStreamingStateListener) {
		this.mediaStreamingStateListener = mediaStreamingStateListener;
	}

}
