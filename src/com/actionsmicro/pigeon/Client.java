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
/**
 * Client is a gateway for client to send image data to EZ Wifi server.
 * @author James Chen
 *
 */
public class Client {
	/**
	 * OnExceptionListener defines interface for asynchronous mode clients to handle exception thrown in working thread.
	 *
	 * @see Client#sendImageToServerAsync
	 * @see Client#setOnExceptionListener 
	 * @see Client#getOnExceptionListener
	 */
	public interface OnExceptionListener {
		public void onException(Client client, Exception e);
	}
	private OnExceptionListener onExceptionListener;
	/**
	 * BitmapManager defines interface for asynchronous mode clients to manage buffer life-cycle.
	 *
	 * @see Client#sendImageToServerAsync
	 * @see Client#setBitmapManager 
	 * @see Client#getBitmapManager
	 */
	public interface BitmapManager {
		public boolean onProcessBitmapBegin(Client client, Bitmap bitmap);
		public void onProcessBitmapEnd(Client client, Bitmap bitmap);
	}
	private BitmapManager bitmapManager;
	private static final int DEFAULT_SOCKET_TIMEOUT = 2000;
	private static final String TAG = "pigeon.Client";
	private final String serverAddress;
	private final int portNumber;
	/**
	 * Retrieve the port number of the server this Client currently connects to.
	 * @return The port number of the server this Client currently connects to.
	 */
	public int getPortNumber() {
		return portNumber;
	}
	/**
	 * Retrieve the IP address of the server this Client currently connects to.
	 * @return The IP address of the server this Client currently connects to.
	 */
	public String getServerAddress() {
		return serverAddress;
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
							sendImageToServer(job.bitmap, job.format, job.quailty);
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
	/**
	 * Create a Client to connect to given IP address and port number.
	 * @param serverAddress The IP address of the server.
	 * @param portNumber The port number of the server.
	 */
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
	/**
	 * Stop and clean up this Client. You should not call any method of Client after {@link #stop()} is called.
	 */
	public void stop() {
		shouldStop = true;
		cleanUp(true);		
	}
	private void cleanUp(boolean stop) {
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
	 * Send image to server by using specified format and quality synchronously.
	 * @param bitmap The bitmap to be sent to the server/device.
	 * @param format Specifies the known formats a bitmap can be compressed into.
	 * @param quailty Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. Some formats, like PNG which is lossless, will ignore the quality setting.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @see <a href="http://developer.android.com/reference/android/graphics/Bitmap.html#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)">Bitmap.compress()</a>
	 */
	public void sendImageToServer(Bitmap bitmap, Bitmap.CompressFormat format, int quailty) throws IOException, IllegalArgumentException {
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
	/**
	 * Send YUV image to server by using specified quality synchronously. YUV image is encoded as JPEG.
	 * @param yuvImage The image to be sent to the server/device.
	 * @param quailty Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality.
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @see <a href="http://developer.android.com/reference/android/graphics/YuvImage.html">YuvImage</a>
	 * @see <a href="http://developer.android.com/reference/android/graphics/YuvImage.html#compressToJpeg(android.graphics.Rect,%20int,%20java.io.OutputStream)">compressToJpeg()</a>
	 */
	public void sendImageToServer(YuvImage yuvImage, int quailty) throws IOException, IllegalArgumentException {
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
	/**
	 * Send image to server by using specified format and quality asynchronously. You can use {@link OnExceptionListener} and {@link BitmapManager} to handle exception and manage buffer life-cycles respectively.
	 * @param bitmap The bitmap to be sent to the server/device.
	 * @param format Specifies the known formats a bitmap can be compressed into.
	 * @param quality Hint to the compressor, 0-100. 0 meaning compress for small size, 100 meaning compress for max quality. Some formats, like PNG which is lossless, will ignore the quality setting.
	 * @see #setBitmapManager(BitmapManager)
	 * @see #setOnExceptionListener(OnExceptionListener)
	 * @see <a href="http://developer.android.com/reference/android/graphics/Bitmap.html#compress(android.graphics.Bitmap.CompressFormat,%20int,%20java.io.OutputStream)">Bitmap.compress()</a>
	 */
	public void sendImageToServerAsync(Bitmap bitmap, Bitmap.CompressFormat format, int quality) {
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
	/**
	 * Send image file to server synchronously. Image file will be re-encoded as JPEG unless it is a JPEG file.
	 * @param imageFile The path to the image file to be sent.
	 * @throws IOException 
	 * @throws IllegalArgumentException
	 */
	public void sendImageFileToServer(String imageFile) throws IOException, IllegalArgumentException {
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
					sendImageToServer(BitmapFactory.decodeFile(imageFile, null), Bitmap.CompressFormat.JPEG, 100);
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
			try {
				socketToServer.connect(new InetSocketAddress(serverAddress, portNumber), timeout);
			} catch (IOException e) {
				socketToServer = null;
				throw e;
			} catch (RuntimeException e) {
				socketToServer = null;
				throw e;
			}
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
	/**
	 * Set the OnExceptionListener to the Client to handle exception for asynchronous mode methods, such as {@link #sendImageToServerAsync}.
	 * @param onExceptionListener
	 * @see OnExceptionListener
	 * @see #sendImageToServerAsync
	 * @see #getOnExceptionListener
	 */
	public void setOnExceptionListener(OnExceptionListener onExceptionListener) {
		this.onExceptionListener = onExceptionListener;
	}
	/**
	 * Get the OnExceptionListener.
	 * @return The OnExceptionListener.
	 */
	public OnExceptionListener getOnExceptionListener() {
		return onExceptionListener;
	}
	/**
	 * Set the BitmapManager to the Client to manage buffer life-cycles for asynchronous mode methods, such as {@link #sendImageToServerAsync}.
	 * @param bitmapManager
	 * @see BitmapManager
	 * @see #sendImageToServerAsync
	 * @see #getBitmapManager
	 */
	public void setBitmapManager(BitmapManager bitmapManager) {
		this.bitmapManager = bitmapManager;
	}
	/**
	 * Get the BitmapManager.
	 * @return The BitmapManager.
	 */
	public BitmapManager getBitmapManager() {
		return bitmapManager;
	}
}
