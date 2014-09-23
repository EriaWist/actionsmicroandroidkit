package com.actionsmicro.airplay.mirror;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import android.annotation.TargetApi;
import android.graphics.YuvImage;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.Build;

import com.actionsmicro.androidkit.ezcast.imp.airplay.SimpleMpegTsPacketizer;
import com.actionsmicro.utils.Log;
import com.koushikdutta.async.AsyncServer;
import com.koushikdutta.async.AsyncServerSocket;
import com.koushikdutta.async.callback.WritableCallback;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

public class TsStreamer {
	public interface Delegate {

		void onSizeChanged();
		
	}
	private static final String TAG = "TsStreamer";
	private AvcEncoder avcEncoder;
	private FileOutputStream avcOut;
	private static final byte[] nalHeader = {0x00,0x00,0x00,0x01};
	private SimpleMpegTsPacketizer tsPacketizer;
	private FileOutputStream tsFileOutputStream;
	private byte[] sps;
	private byte[] pps;
	private Delegate delegate;
	public TsStreamer() {
		prepareBuffers();
		initHttpServer();
	}
	private void createAvcEncoder(int width, int height) {
		releaseAvcEncoder();
		try {
			avcEncoder = new AvcEncoder(width, height, 1400000, 15, 5) {
				protected void onOutputForamtChanged(MediaFormat outputFormat) {
					closeTsFileOutput();
					try {
//						tsFileOutputStream = new FileOutputStream("/sdcard/test.ts");
						tsPacketizer = new SimpleMpegTsPacketizer(new SimpleMpegTsPacketizer.PacketReceiver() {

							private int debugCounter = 1;

							@Override
							public void onPacketReady(byte[] tsPacket) {
								if (stop) {
									return;
								}
								try {
									ByteBuffer idleBuffer = idleBuffers.poll(0, TimeUnit.MILLISECONDS);
									if (idleBuffer == null) {
//										if (debugCounter++ % 1000 == 0) {
										Log.w(TAG, "Idle Buffer under-run! :"+debugCounter);
//										}
									}
									if (idleBuffer != null) {
										idleBuffer.clear();
										idleBuffer.put(tsPacket);
										idleBuffer.rewind();
										tsBuffers.add(idleBuffer);
//										debugCounter = 1;
									}
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} finally {

								}
								try {
									if (tsFileOutputStream != null) {
										tsFileOutputStream.write(tsPacket);
									}
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}								
							}


						});
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			};
			avcEncoder.setParameterSetsListener(new AvcEncoder.ParameterSetsListener() {
				@Override
				public void avcParametersSetsEstablished(byte[] sps, byte[] pps) {
					try {
						TsStreamer.this.sps = sps;
						TsStreamer.this.pps = pps;
//						avcOut = new FileOutputStream("/sdcard/testavcencoder.h264");
						if (avcOut != null) {
							avcOut.write(nalHeader);
							avcOut.write(sps);
							avcOut.write(nalHeader);
							avcOut.write(pps);
						}
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});
			avcEncoder.setFrameListener(new AvcEncoder.EncodedFrameListener() {
				@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
				@Override
				public void frameReceived(byte[] outData, int index, int length, MediaCodec.BufferInfo bufferInfo) {
					try {
						if (avcOut != null) {
							avcOut.write(outData, index, length);
						}
						if (tsPacketizer != null) {
							if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_SYNC_FRAME) != 0) {
								ByteBuffer temp = ByteBuffer.allocate(length + 8 + sps.length + pps.length);
								temp.order(ByteOrder.BIG_ENDIAN);
								temp.putInt(0x00000001);
								temp.put(sps);
								temp.putInt(0x00000001);
								temp.put(pps);
								temp.put(outData, index, length);
								tsPacketizer.writeFrame(temp.array(), 0, temp.limit(), bufferInfo.presentationTimeUs);
							} else {
								tsPacketizer.writeFrame(outData, index, length, bufferInfo.presentationTimeUs);
							}
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			});

		} catch (Throwable t) {

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

		if (avcOut != null) {
			try {
				avcOut.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		tsPacketizer = null;

		closeTsFileOutput();
	}
	private void stopHttpServer() {
		if (httpServer != null) {
			httpServer.stop();
		}
	}
	public void displayYuvImage(YuvImage yuvImage) {
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
		if (avcEncoder != null) {
			avcEncoder.offerEncoder(yuvImage.getYuvData(), System.currentTimeMillis() * 1000);
		}
	}
	private void closeTsFileOutput() {
		if (tsFileOutputStream != null) {
			try {
				tsFileOutputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	private AsyncHttpServer httpServer = new AsyncHttpServer() {
		@Override
		protected void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
			Log.d(TAG, "onRequest:"+request.getHeaders().getHeaders().getStatusLine());
		}
	};
	private static final int NUMBER_OF_PACKET_BUFFER = 256;
	private static final int TS_PACKET_LENGTH = 188;
	private BlockingQueue<ByteBuffer> tsBuffers = new ArrayBlockingQueue<ByteBuffer>(NUMBER_OF_PACKET_BUFFER);
	private BlockingQueue<ByteBuffer> idleBuffers = new ArrayBlockingQueue<ByteBuffer>(NUMBER_OF_PACKET_BUFFER);
	private void prepareBuffers() {
		for (int i = 0; i < NUMBER_OF_PACKET_BUFFER; i++) {
			idleBuffers.add(ByteBuffer.allocate(TS_PACKET_LENGTH));
		}
	}
	private AsyncServerSocket serverSocket;
	private AsyncServer httpAsyncServer = new AsyncServer(); 
	private void startHttpServer() {
		serverSocket = httpServer.listen(httpAsyncServer, 0);
		Log.d(TAG, "tsServer listen on port:" + serverSocket.getLocalPort());
	}
	private void initHttpServer() {
		httpServer.get("/", new HttpServerRequestCallback() {

			@Override
			public void onRequest(AsyncHttpServerRequest request,
					final AsyncHttpServerResponse response) {
				Log.d(TAG, "onRequest");
				response.getHeaders().getHeaders().add("Content-Type", "video/MP2T");
				response.responseCode(200);
				WritableCallback writer = new WritableCallback() {
					private int debugCounter = 0;
					@Override
					public void onWriteable() {
						try {
							while (!stop) {
								boolean shouldPollItOut = true;
								ByteBuffer tsBuffer = tsBuffers.peek();
								if (tsBuffer == null) {
									shouldPollItOut = false;
									tsBuffer = tsBuffers.poll(1, TimeUnit.SECONDS);
								}
								if (tsBuffer != null) {
									tsBuffer.rewind();
									response.write(tsBuffer);
									if (tsBuffer.hasRemaining()) {
										Log.w(TAG, "socket can't write out:"+debugCounter);
										debugCounter = 0;
										return;
									} else {
										if (shouldPollItOut) {
											tsBuffers.poll();
										}
										idleBuffers.add(tsBuffer);
										if (debugCounter++%1000==0) {
											Log.d(TAG, "packet write out:"+debugCounter);
										}
									}
								} else {
									Log.w(TAG, "TS Buffer under-run!");
								}
							};
						} catch (InterruptedException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}						
					}

				};
				response.setWriteableCallback(writer);
				writer.onWriteable();
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
}
