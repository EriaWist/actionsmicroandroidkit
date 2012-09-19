package com.actionsmicro.pigeon;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
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
	protected static final int STREAM_FORMAT_JEPG = 1;
	protected static final int EZ_DISPLAY_HEADER_SIZE = 32;
	private static final int PICO_PIC_FORMAT_CMD = 2;
	private static final int PICO_HEARTBEAT = 4;
	
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
	
	public interface OnNotificationListener {
		public void onRemoteRequestToStart(final Client client, final int numberOfWindows, final int position);
		public void onRemoteRequestToStop(final Client client);
		public void onRemoteRequestToChangePostion(final Client client, final int numberOfWindows, final int position);
		public void onRemoteRequestToDisconnect(final Client client);		
	}
	private OnNotificationListener onNotificationListener;
	
	protected static final int DEFAULT_SOCKET_TIMEOUT = 2000;
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
	
	protected boolean shouldStop = false;
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
	private final Thread sendingThread = new Thread(new Runnable() {
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
						if (shouldSendHeartbeat()) {
							Log.d(TAG, "Send heartbeat");
							sendHeartbeat();			
						}
					}
				} catch (Exception e) {
					handleExceptionInThread(e);
					shouldStop = true;
				}
			}
		}
	});
	/**
	 * Create a Client to connect to given IP address and port number.
	 * @param serverAddress The IP address of the server.
	 * @param portNumber The port number of the server.
	 */
	protected Client(String serverAddress, int portNumber) {
		this.serverAddress = serverAddress;
		this.portNumber = portNumber;
		
		sendingThread.start();
	}
	/**
	 * Working thread determines whether it should send heartbeat packet or not by calling this method. It returns true by default. Subclass can override this method to alter the behavior.
	 * @return Return true if heartbeat packet should be sent in working thread. Otherwise, return false.
	 */
	protected boolean shouldSendHeartbeat() {
		return true;
	}
	private void sendHeartbeat() throws IllegalArgumentException, IOException {
		synchronized (this) {
			Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
			final OutputStream socketStream = socketToServer.getOutputStream();
			Log.d(TAG, "try to sendHeartbeat("+serverAddress+":"+portNumber+")");	
			socketStream.write(createPacketHeaderForSendingHeartbeat().array());
			socketStream.flush();
			Log.d(TAG, "sendHeartbeat("+serverAddress+":"+portNumber+") done.");
		}
	}
	/**
	 * Stop and clean up this Client. You should not call any method of Client after {@link #stop()} is called.
	 */
	protected void stop() {
		shouldStop = true;
		cleanUp(true);	
		bitmapManager = null;
		onExceptionListener = null;
		onNotificationListener = null;
	}
	private void cleanUp(boolean stop) {
		if (compressionBuffer != null) {
			try {
				compressionBuffer.close();
			} catch (IOException e) {
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
	protected static final int SOCKET_OUTPUT_STREAM_BUFFER_SIZE = 8192;
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
		if (canSendStream() || requestStreaming()) {
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
	}
	/**
	 * Ask server's permission to send image data. Return true by default. Subclass can override this method to implement different mechanism.
	 * @return Return true if it's able to send image; otherwise, return false;
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 */
	protected boolean requestStreaming() throws IllegalArgumentException, IOException {
		return true;
	}
	public boolean canSendStream() {
		return true;
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
		if (canSendStream() || requestStreaming()) {
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
	}
	private void resetSocket() {
		if (socketToServer != null) {
			try {
				socketToServer.close();
			} catch (IOException e) {
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
			Log.d(TAG, "try to sentImageToServer("+serverAddress+":"+portNumber+")");	
			socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
			socketStream.write(createPacketHeaderForSendingImage(width, height, getCompressionBuffer().size()).array());
			socketStream.write(getCompressionBuffer().toByteArray());
			socketStream.flush();
			Log.d(TAG, "sentImageToServer("+serverAddress+":"+portNumber+") done.");
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
		if (canSendStream() || requestStreaming()) {
			synchronized (this) {
				Socket socketToServer = null;
				BufferedOutputStream socketStream = null;
				RandomAccessFile file = null;
				BufferedInputStream fileInputStream = null;
				try {
					final BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
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
						socketStream = new BufferedOutputStream(socketToServer.getOutputStream(), SOCKET_OUTPUT_STREAM_BUFFER_SIZE);
						socketStream.write(createPacketHeaderForSendingImage(decodeOptions.outWidth, decodeOptions.outHeight, (int)file.length()).array());
						
						fileInputStream = new BufferedInputStream(new FileInputStream(imageFile));
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
					if (fileInputStream != null) {
						fileInputStream.close();
					}
				}
			}
		}
	}
	private Socket socketToServer;
	protected Socket createSocketToServer(int timeout) throws IOException, IllegalArgumentException {
		synchronized(this) {
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
	}	
	private static int commandSequenceNumber = 0;
	protected static int getCommandSequenceNumber() {
		return commandSequenceNumber++;
	}
	private ByteBuffer createPacketHeaderForSendingImage(int width, int height, int size) {
		ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24 + compressed image size
		header.putInt(24+size);
		// send image command
		header.putInt(PICO_PIC_FORMAT_CMD); //tag == 2;
		header.put((byte) 0); // flag = 0
		header.put((byte) 16);
		header.put((byte) 0); // reserve0
		header.put((byte) 0); // reserve1
		header.putInt(STREAM_FORMAT_JEPG); //jpeg == 1;
		header.putInt(width);
		header.putInt(height);
		header.putInt(size);
		return header;
	}
	private ByteBuffer createPacketHeaderForSendingHeartbeat() {
		ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24 
		header.putInt(24);
		// send heartbeat command
		header.putInt(PICO_HEARTBEAT);
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
	public OnNotificationListener getOnNotificationListener() {
		return onNotificationListener;
	}
	public void setOnNotificationListener(OnNotificationListener onNotificationListener) {
		this.onNotificationListener = onNotificationListener;
	}
	protected void handleExceptionInThread(Exception e) {
		if (null != onExceptionListener) {
			onExceptionListener.onException(Client.this, e);
		}
	}
	public String getVersion() {
		return "1";
	}
}
