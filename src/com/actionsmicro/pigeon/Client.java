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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

public class Client {
	private static final int DEFAULT_SOCKET_TIMEOUT = 1000;
	private static final String TAG = "pigeon.Client";
	private final String serverAddress;
	private final int portNumber;
	public Client(String serverAddress, int portNumber) {
		this.serverAddress = serverAddress;
		this.portNumber = portNumber;
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
}
