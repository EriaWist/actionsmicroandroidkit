package com.actionsmicro.pigeon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.YuvImage;
import android.util.Log;

public class Client {
	public interface OnExceptionListener {
		public void onException(Client client, Exception e);
	}
	private OnExceptionListener onExceptionListener;
	public interface BitmapManager {
		public boolean onProcessBitmapBegin(Client client, Bitmap bitmap);
		public void onProcessBitmapEnd(Client client, Bitmap bitmap);
	}
	private BitmapManager bitmapManager;
	private static final int DEFAULT_SOCKET_TIMEOUT = 1000;
	private static final String TAG = "pigeon.Client";
	private final String serverAddress;
	private final int portNumber;
	public int getPortNumber() {
		return portNumber;
	}
	private boolean shouldStop = false;
	private static class Job {
		public Bitmap bitmap;
		public Bitmap.CompressFormat format; 
		public int quailty;
		public Job(Bitmap bitmap, Bitmap.CompressFormat format, int quailty) {
			this.bitmap = bitmap;
			this.format = format;
			this.quailty = quailty;
		}
		private Job() {}
		public final static Job nullJob = new Job();
	}
	private ArrayBlockingQueue<Job> pendingJobs = new ArrayBlockingQueue<Job>(1);
	private final Thread backgroundThread = new Thread(new Runnable() {
		@Override
		public void run() {
			Job job = null;
			while (!shouldStop) {
				Log.d(TAG, "waiting for job");
				try {
					job = pendingJobs.poll(1, TimeUnit.SECONDS);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					shouldStop = true;
				}
				try {
					if (null != job && null != job.bitmap) {
						Log.d(TAG, "job comes in");
						boolean shouldProcess = true;
						if (bitmapManager != null) {
							shouldProcess = bitmapManager.onProcessBitmapBegin(Client.this, job.bitmap);
						}
						if (shouldProcess) {
							sentImageToServer(job.bitmap, job.format, job.quailty);
							if (bitmapManager != null) {
								bitmapManager.onProcessBitmapEnd(Client.this, job.bitmap);
							} else {
								job.bitmap.recycle();
							}
						}
					
					} else if (!shouldStop) {
						// TODO send heartbeat
						Log.d(TAG, "Send heartbeat");
						sendHeartbeat();						
					}
				} catch (Exception e) {
					if (null != onExceptionListener) {
						onExceptionListener.onException(Client.this, e);
					}
				}
			}
		}
	});
	public Client(String serverAddress, int portNumber) {
		this.serverAddress = serverAddress;
		this.portNumber = portNumber;
		
		backgroundThread.start();
	}
	private void sendHeartbeat() throws IllegalArgumentException, IOException {
		synchronized (this) {
			Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
			BufferedOutputStream socketStream = null;
			try {
				Log.d(TAG, "try to sendHeartbeat("+serverAddress+":"+portNumber+")");	
				socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			} catch (IOException e) {
				e.printStackTrace();
				resetSocket();
				socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
				socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			}
			try {
				socketStream.write(createPacketHeaderForSendingHeartbeat().array());
				socketStream.flush();
				Log.d(TAG, "sendHeartbeat("+serverAddress+":"+portNumber+") done.");	
			} catch (IOException e) {
				throw e;
			} finally {
			}
		}
	}
	public void stop() {
		shouldStop = true;
		cleanUp(true);		
	}
	public void cleanUp(boolean stop) {
		if (compressionBuffer != null) {
			try {
				compressionBuffer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				compressionBuffer = null;
			}
		}
		final ArrayList<Job> expiredJobs = new ArrayList<Job>();
		pendingJobs.drainTo(expiredJobs);
		if (bitmapManager == null) {
			Iterator<Job> iterator = pendingJobs.iterator();
			while (iterator.hasNext()) {
				iterator.next().bitmap.recycle();
			}
		}
		resetSocket();
		if (stop) {
			// add null job to trigger stop
			pendingJobs.add(Job.nullJob);			
		}
	}
	public String getServerAddress() {
		return serverAddress;
	}
	private ByteArrayOutputStream compressionBuffer;
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
	static private final int SOCKET_OUTPUT_STREAM_BUFFER_SIZE = 8192;
	/**
	 *	Send image to server by using specified format and quality
	 *	@param bitmap the bitmap to be sent to server/device
	 *	@param format specifies the known formats a bitmap can be compressed into
	 *	@param quality Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. Some formats, like PNG which is lossless, will ignore the quality setting
	 *	@see <a href="http://developer.android.com/reference/android/graphics/Bitmap.html#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)">Bitmap.compress()</a>
	 */
	public void sentImageToServer(Bitmap bitmap, Bitmap.CompressFormat format, int quailty) throws IOException, IllegalArgumentException {
		synchronized (this) {
			final int width = bitmap.getWidth();
			final int height = bitmap.getHeight();
			Log.i(TAG, "sentImageToServer width=" + width+",height=" + height);
			getCompressionBuffer().reset();
			Log.d(TAG, "Start compress");
			bitmap.compress(format, quailty, getCompressionBuffer());
			Log.d(TAG, "Done compress. Size:" + getCompressionBuffer().size());
			sendCompressedBufferToServer(width, height);	
		}
	}
	public void sentImageToServer(YuvImage yuvImage, int quailty) throws IOException, IllegalArgumentException {
		synchronized (this) {
			final int width = yuvImage.getWidth();
			final int height = yuvImage.getHeight();
			Log.i(TAG, "sentImageToServer width=" + width+",height=" + height);
			getCompressionBuffer().reset();
			Log.d(TAG, "Start compress");
			android.graphics.Rect rect = new android.graphics.Rect(0, 0, width, height); 
			yuvImage.compressToJpeg(rect, quailty, getCompressionBuffer());
			Log.d(TAG, "Done compress. Size:" + getCompressionBuffer().size());
			sendCompressedBufferToServer(width, height);		
		}
	}
	private void resetSocket() {
		if (socketToServer != null) {
			try {
				socketToServer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			socketToServer = null;
		}
	}
	private void sendCompressedBufferToServer(final int width, final int height)
			throws IOException, IllegalArgumentException {
		synchronized (this) {
			Log.d(TAG, "try to connect to ("+serverAddress+":"+portNumber+")");
			Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
			BufferedOutputStream socketStream = null;
			try {
				Log.d(TAG, "try to sentImageToServer("+serverAddress+":"+portNumber+")");	
				socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			} catch (IOException e) {
				e.printStackTrace();
				resetSocket();
				socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
				socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			}
			try {
				socketStream.write(createPacketHeaderForSendingImage(width, height, getCompressionBuffer().size()).array());
				socketStream.write(getCompressionBuffer().toByteArray());
				socketStream.flush();
				Log.d(TAG, "sentImageToServer("+serverAddress+":"+portNumber+") done.");	
			} catch (IOException e) {
				throw e;
			} finally {
			}
		}
	}
	
	public void sentImageToServerAsync(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
		final ArrayList<Job> expiredJobs = new ArrayList<Job>();
		pendingJobs.drainTo(expiredJobs);
		pendingJobs.add(new Job(bitmap, format, quality));
		if (bitmapManager == null) {
			Iterator<Job> iterator = pendingJobs.iterator();
			while (iterator.hasNext()) {
				iterator.next().bitmap.recycle();
			}
		}
	}
	public void sentImageFileToServer(String imageFile) throws IOException, IllegalArgumentException {
		synchronized (this) {
			Socket socketToServer = null;
			BufferedOutputStream socketStream = null;
			RandomAccessFile file = null;
			try {
				BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
				decodeOptions.inJustDecodeBounds = true; // we just need width and height
				BitmapFactory.decodeFile(imageFile, decodeOptions);
				Log.d(TAG, "width:" + decodeOptions.outWidth + " height:" + decodeOptions.outHeight  + " outMimeType: "+decodeOptions.outMimeType);
				if (!decodeOptions.outMimeType.equals("image/jpeg")) {
					sentImageToServer(BitmapFactory.decodeFile(imageFile, null), Bitmap.CompressFormat.JPEG, 100);
					return;
				} else {
					file = new RandomAccessFile(imageFile, "r");
					Log.d(TAG, "sentImageFileToServer file size:" + file.length());
					socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
					try {
						socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
					} catch (IOException e) {
						e.printStackTrace();
						resetSocket();
						socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
						socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
					}
					
					socketStream.write(createPacketHeaderForSendingImage(decodeOptions.outWidth, decodeOptions.outHeight, (int)file.length()).array());
					
					BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(imageFile));
					byte[] buffer = new byte[4096];
					for (int read = fileInputStream.read(buffer); read > 0; read = fileInputStream.read(buffer)) {
						socketStream.write(buffer, 0, read);
					}    
					Log.d(TAG, "sentImageToServer("+serverAddress+":"+portNumber+")");	
				}
			} catch (IOException e) {
				throw e;
			} finally {
				if (file != null) {
					file.close();
				}
			}
		}
	}
	private Socket socketToServer;
	private Socket createSocketToServer(int timeout) throws IOException, IllegalArgumentException {
		if (socketToServer == null) {
			socketToServer = new Socket();
			socketToServer.connect(new InetSocketAddress(serverAddress, portNumber), timeout);
		}
		return socketToServer;
	}	
	private ByteBuffer createPacketHeaderForSendingImage(int width, int height, int size) {
		ByteBuffer header = ByteBuffer.allocate(32);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(0);
		// TCP packet size = 24 + compressed image size
		header.putInt(24+size);
		// send image command
		header.putInt(2); //tag == 2;
		header.putInt(0); //
		header.putInt(1); //jpeg == 1;
		header.putInt(width);
		header.putInt(height);
		header.putInt(size);
		return header;
	}
	private ByteBuffer createPacketHeaderForSendingHeartbeat() {
		ByteBuffer header = ByteBuffer.allocate(32);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(0);
		// TCP packet size = 24 
		header.putInt(24);
		// send heartbeat command
		header.putInt(4);
		header.putInt(0);
		header.putInt(0); 
		header.putInt(0);
		header.putInt(0);
		header.putInt(0);
		return header;
	}
	public void setOnExceptionListener(OnExceptionListener onExceptionListener) {
		this.onExceptionListener = onExceptionListener;
	}

	public OnExceptionListener getOnExceptionListener() {
		return onExceptionListener;
	}
	public void setBitmapManager(BitmapManager bitmapManager) {
		this.bitmapManager = bitmapManager;
	}
	public BitmapManager getBitmapManager() {
		return bitmapManager;
	}
}
