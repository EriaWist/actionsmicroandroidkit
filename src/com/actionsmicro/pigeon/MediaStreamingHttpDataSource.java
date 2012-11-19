package com.actionsmicro.pigeon;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.DefaultHttpClient;

import android.util.Log;

import com.actionsmicro.pigeon.MediaStreaming.DataSource;

public class MediaStreamingHttpDataSource implements DataSource {
	private MediaStreamingStateListener mediaStreamingStateListener;
	private static final String TAG = "MediaStreamingHttpDataSource";
	final HttpClient client = new DefaultHttpClient();
	private String urlString;
	private long contentLength = -1;
	private String userAgentString;

	public MediaStreamingHttpDataSource(String url, String userAgentString) {
		this.urlString = url;
		this.userAgentString = userAgentString;
	}
	public MediaStreamingHttpDataSource(String url) {
		this.urlString = url;
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
	private PipedOutputStream outputPipe;
	private PipedInputStream inputPipe;
	private Thread uploadThread;
	private boolean shouldUpload;
	protected boolean reachEof;
	private void stopDownloadThread() {
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
	private void stopUploadThread() {
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
				final int pipeSize = 1024*1024;
				outputPipe = new PipedOutputStream();
				try {
					inputPipe = new PipedInputStream(outputPipe, pipeSize);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				shouldDownload = true;
				downloadThread = new Thread(new Runnable() {
					
					@Override
					public void run() {
						InputStream inputStream = null;
						try {
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
								Log.d(TAG, "start reading");	
								reachEof = false;
								do {
									sizeRead = inputStream.read(buffer);
//									Log.d(TAG, "inputStream.read:"+sizeRead);
									if (sizeRead == -1) {
										// TODO
										//									mediaStreaming.sendEofPacket();
										reachEof = true;
										outputPipe.flush();
										shouldDownload = false;
									} else {
										totalRead += sizeRead;
										if (totalRead >= pipeSize - bufferSize && downloadingOffset != -1) {
											//downloadingOffset = -1;
											//startUploadThread(); 
										}
										do {
											try {
//												Log.d(TAG, "outputPipe.write:"+sizeRead);
												outputPipe.write(buffer, 0, sizeRead);
//												Log.d(TAG, "outputPipe.write done");
												break;
											} catch (InterruptedIOException e) {
												e.printStackTrace();
												try {
													Thread.sleep(100);
												} catch (InterruptedException e1) {
													// TODO Auto-generated catch block
													e1.printStackTrace();
													break;
												}
											}
										} while (true);
									}
								} while(shouldDownload);
								Log.d(TAG, "end reading. totalRead:" + totalRead);
								outputPipe.close();
								inputStream.close();
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
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
				try {
					final int bufferSize = 1024*32;
					final byte[] buffer = new byte [bufferSize];
					int sizeRead = 0;
					long totalRead = 0;
					int result;
					Log.d(TAG, "start reading inputPipe");	
					do {
						result = inputPipe.read(buffer, sizeRead, bufferSize-sizeRead);
						synchronized (this) {
							downloadingOffset = -1;
						}						
//						Log.d(TAG, "inputPipe.read:"+sizeRead);
						if (result == -1) {
							if (sizeRead > 0) {
								totalRead += sizeRead;
//								Log.d(TAG, "sendStreamingContents:"+sizeRead);
								mediaStreaming.sendStreamingContents(buffer, sizeRead);
								sizeRead = 0;								
							}
							if (reachEof) {
								mediaStreaming.sendEofPacket();
							}
							shouldUpload = false;
						} else {
							sizeRead += result;
							if (sizeRead >= 64) {
								totalRead += sizeRead;
//								Log.d(TAG, "sendStreamingContents:"+sizeRead);
								mediaStreaming.sendStreamingContents(buffer, sizeRead);
								sizeRead = 0;								
							}
						}
					} while(shouldUpload);
					Log.d(TAG, "end reading inputPipe. totalRead:" + totalRead);
					inputPipe.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
