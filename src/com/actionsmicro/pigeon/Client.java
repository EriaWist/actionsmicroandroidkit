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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
					job = pendingJobs.take();
				} catch (InterruptedException e1) {
					e1.printStackTrace();
					shouldStop = true;
				}
				if (null != job && null != job.bitmap) {
					Log.d(TAG, "job comes in");
					try {
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
					} catch (Exception e) {
						if (null != onExceptionListener) {
							onExceptionListener.onException(Client.this, e);
						}
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
	public void stop() {
		shouldStop = true;
		cleanUp();		
	}
	public void cleanUp() {
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
		if (stop) {
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
		Log.i(TAG, "sentImageToServer Height=" + bitmap.getHeight()+",Width=" + bitmap.getWidth());
		getCompressionBuffer().reset();
		Log.d(TAG, "Start compress");
		bitmap.compress(format, quailty, getCompressionBuffer());
		Log.d(TAG, "Done compress. Size:" + getCompressionBuffer().size());
		Log.d(TAG, "try to connect to ("+serverAddress+":"+portNumber+")");
		Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
		BufferedOutputStream socketStream = null;
		try {
			Log.d(TAG, "try to sentImageToServer("+serverAddress+":"+portNumber+")");	
			socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			socketStream.write(createPacketHeaderForSendingImage(bitmap.getWidth(), bitmap.getHeight(), getCompressionBuffer().size()).array());
			socketStream.write(getCompressionBuffer().toByteArray());
			socketStream.flush();
			Log.d(TAG, "sentImageToServer("+serverAddress+":"+portNumber+") done.");	
		} catch (IOException e) {
			throw e;
		} finally {
			if (socketStream != null) {
				socketStream.close();
			}
			if (socketToServer != null) {
				socketToServer.close();
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
			}
			
			file = new RandomAccessFile(imageFile, "r");
			Log.d(TAG, "sentImageFileToServer file size:" + file.length());
			socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
			socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			socketStream.write(createPacketHeaderForSendingImage(decodeOptions.outWidth, decodeOptions.outHeight, (int)file.length()).array());
			
			BufferedInputStream fileInputStream = new BufferedInputStream(new FileInputStream(imageFile));
			byte[] buffer = new byte[4096];
			for (int read = fileInputStream.read(buffer); read > 0; read = fileInputStream.read(buffer)) {
				socketStream.write(buffer, 0, read);
			}    
			Log.d(TAG, "sentImageToServer("+serverAddress+":"+portNumber+")");	
		} catch (IOException e) {
			throw e;
		} finally {
			if (file != null) {
				file.close();
			}
	        if (socketStream != null) {
				socketStream.close();
			}
			if (socketToServer != null) {
				socketToServer.close();
			}
		}
	}
	private Socket createSocketToServer(int timeout) throws IOException, IllegalArgumentException {
		Socket newSocket = new Socket();
		newSocket.connect(new InetSocketAddress(serverAddress, portNumber), timeout);
		return newSocket;
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
