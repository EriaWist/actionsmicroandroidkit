package com.actionsmicro.mp4;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.TargetApi;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.actionsmicro.airplay.mirror.AvcEncoder;
import com.actionsmicro.mp4.FragmentedMP4Serializer.OutputListener;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.ByteBufferList;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class Mp4Streamer {
	public interface Delegate {

		void onSizeChanged();
		
	}
	private static final String TAG = "Mp4Streamer";
	private AvcEncoder avcEncoder;
	private byte[] sps;
	private byte[] pps;
	private Delegate delegate;
	private FragmentedMP4Serializer mp4Serializer;
	private int frameCount;
	
	public Mp4Streamer() {
		initHttpServer();
	}
	public synchronized void displayYuvImage(YuvImage yuvImage) {
		if (stop) {
			return;
		}
		if (avcEncoder == null || avcEncoder.getWidth() != yuvImage.getWidth() || avcEncoder.getHeight() != yuvImage.getHeight()) {
			boolean avcEncoderWasNull = avcEncoder == null;
			createAvcEncoder(yuvImage.getWidth(), yuvImage.getHeight());
			if (!avcEncoderWasNull && delegate != null) {
				delegate.onSizeChanged();
			}
		}
		if (currentConnection == null) {
			return;
		}

		if (avcEncoder != null) {
			frameCount ++;
			Log.d(TAG, "offerEncoder:"+frameCount);
			avcEncoder.offerEncoder(yuvImage.getYuvData(), System.currentTimeMillis() * 1000);
		}
	}
	private FileOutputStream mp4FileOut; // debug purpose

	private ByteBuffer mp4Header;
	private void createAvcEncoder(final int width, final int height) {
		releaseAvcEncoder();
		try {

			avcEncoder = new AvcEncoder(width, height, 5*1024*1024, 20, 10) {
				protected void onOutputForamtChanged(MediaFormat outputFormat) {
					
				}
			};
			avcEncoder.setParameterSetsListener(new AvcEncoder.ParameterSetsListener() {
				@Override
				public void avcParametersSetsEstablished(byte[] sps, byte[] pps) {
					Mp4Streamer.this.sps = sps;
					Mp4Streamer.this.pps = pps;
					mp4Serializer = new FragmentedMP4Serializer();
					mp4Serializer.setOutputListener(new OutputListener() {

						@Override
						public void headerReady(byte[] data, int offset,
								int length) {
							Log.d(TAG, "headerReady");
							if (mp4FileOut == null) {
								try {
									mp4FileOut = new FileOutputStream("/sdcard/mp4streamer.mp4");
								} catch (FileNotFoundException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							mp4Header = ByteBuffer.allocate(length);
							mp4Header.put(data, offset, length);
							if (currentConnection != null) {
								writeResponse(currentConnection, mp4Header);
							}
							
						}

						@Override
						public void fragmentDataReady(byte[] data, int offset,
								int length) {
							if (currentConnection != null) {
								ByteBuffer buffer = ByteBuffer.allocate(length);
								buffer.put(data, offset, length);
								writeResponse(currentConnection, buffer);
							}
						}
						
					});
					mp4Serializer.prepare(width, height, sps[1], sps[2], sps[3],
							sps,
							pps);
				}
			});
			avcEncoder.setFrameListener(new AvcEncoder.EncodedFrameListener() {
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void frameReceived(byte[] outData, int index, int length, MediaCodec.BufferInfo bufferInfo) {
					Log.d(TAG, "frameReceived");
					if (mp4Serializer != null) {
						mp4Serializer.addH264Frame(outData, index+4, length-4);
					}
				}

			});

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	private void releaseAvcEncoder() {
		if (avcEncoder != null) {
			try {
				avcEncoder.close();
				avcEncoder = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private boolean stop = false;
	public void release() {
		stop = true;
		stopHttpServer();
		httpServer = null;

		releaseAvcEncoder();

		mp4Serializer = null;
		
		if (mp4FileOut != null) {
			try {
				mp4FileOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
	private void stopHttpServer() {
		if (httpServer != null) {
			httpServer.stop();
		}
	}
	private AsyncHttpServer httpServer = new AsyncHttpServer() {
		@Override
		protected boolean onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getPath());
			return false;
		}
	};
	private AsyncServerSocket serverSocket;
	private AsyncServer httpAsyncServer = new AsyncServer();
	protected AsyncHttpServerResponse currentConnection;
	private int totalWrite; 
	private void startHttpServer() {
		serverSocket = httpServer.listen(httpAsyncServer, 0);
		Log.d(TAG, "MP4Server listen on port:" + serverSocket.getLocalPort());
	}
	private void initHttpServer() {
		httpServer.get("/", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest:"+request.getHeaders().toString());
				
				response.getHeaders().add("Content-Type", "video/mp4");
				response.code(200);
//				synchronized (httpServer) {
//					if (mp4Header == null) {
//						try {
//							httpServer.wait(3000);
//						} catch (InterruptedException e) {
//							e.printStackTrace();
//						}
//					}
//				}
//				if (mp4Header != null) {
//					Log.d(TAG, "write mp4Header");
//					writeResponse(response, mp4Header);
					Mp4Streamer.this.currentConnection = response;
//				}
			}			
		});
	}
	public int getListeningPort() {
		return serverSocket.getLocalPort();
	}
	public Delegate getDelegate() {
		return delegate;
	}
	public void setDelegate(Delegate delegate) {
		this.delegate = delegate;
	}
	public void start() {
		startHttpServer();
	}
	public void writeResponse(AsyncHttpServerResponse response, ByteBuffer buffer) {
		Log.d(TAG, "currentConnection write:"+buffer.position());
		buffer.rewind();
		int tryCount = 0;
		ByteBufferList bb = new ByteBufferList(buffer);
		do {
			response.write(bb);
			Log.d(TAG, "currentConnection write remaining:"+bb.remaining());
			tryCount++;
			if (tryCount > 1) {
				Log.w(TAG, "drop this frame");
				break;
			}
		} while (bb.hasRemaining() && !stop);
		if (!buffer.hasRemaining()) {
			totalWrite += buffer.array().length;
			if (mp4FileOut != null) {
				try {
					mp4FileOut.write(buffer.array());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		Log.d(TAG, "currentConnection done. totalWrite:"+totalWrite);
	}
}
