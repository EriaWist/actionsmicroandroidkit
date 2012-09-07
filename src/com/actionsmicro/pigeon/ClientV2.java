package com.actionsmicro.pigeon;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.util.Log;

public class ClientV2 extends Client {
	private static final String TAG = "ClientV2";
	private static final int PICO_STATUS_CMD = 1;
	private static final int PICO_HBSC_CMD = 3;
	private static final int PICO_QUERY_INFO = 5;
	private static final int PICO_REQUEST = 6;
	private static final int PICO_NOTIFICATION = 7;
	private static final int PICO_VENDOR_CMD = 8;

	private final Thread receivingThread = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
				final InputStream socketInputStream = socketToServer.getInputStream();
				final ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
				header.order(ByteOrder.LITTLE_ENDIAN);
				while (true) {
					final int headerSize = socketInputStream.read(header.array());
					if (headerSize == EZ_DISPLAY_HEADER_SIZE) {
						@SuppressWarnings("unused")
						final int sequenceNumber = header.getInt();
						final int totalSize = header.getInt();
						final int payloadSize = totalSize - EZ_DISPLAY_HEADER_SIZE;
						@SuppressWarnings("unused")
						ByteBuffer payload = null;
						if (payloadSize > 0) {
							payload = ByteBuffer.allocate(payloadSize);
						}
						try {
							final int tag = header.getInt();
							switch (tag) {
							case PICO_REQUEST:
								handleRequestResponse(header);
								break;
							case PICO_NOTIFICATION:
								handleNotification(header);
								break;
							default:
								Log.d(TAG, "Unhandled command:"+tag);
								break;
							}
						} catch (Exception e) {
							handleExceptionInThread(e);
						}						
					} else {
						
					}
					header.rewind();					
				}
			} catch (Exception e) {
				handleExceptionInThread(e);
			}
		}
	});
	private int request_result = REQUEST_RESULT_STATE_INVALID;
	private Object requestReceivedNotificaiton;
	public ClientV2(String serverAddress, int portNumber) {
		super(serverAddress, portNumber);
		receivingThread.start();
	}
	private enum State {
	    INITIAL, CONNECTED, STREAMING, DISCONNECTED;
	}
	private State state = State.INITIAL;
	private static final int NOTIFICATION_START_STREAM 		  = 1;
	private static final int NOTIFICATION_STOP_STREAM 		  = 2;
	private static final int NOTIFICATION_CHANGE_STREAM = 3;
	private static final int NOTIFICATION_DISCONNECT = 4;
	@SuppressWarnings("unused")
	private void handleNotification(ByteBuffer header) {
		final byte flag = header.get();
		final byte len = header.get();
		final byte reserve0 = header.get();
		final byte reserve1 = header.get();
		final int notification = header.getInt();
		final int notification_data1 = header.getInt();
		final int notification_data2 = header.getInt();
		switch (notification) {
		case NOTIFICATION_START_STREAM:
			startStreaming(notification_data1, notification_data2);
			break;
		case NOTIFICATION_STOP_STREAM:
			stopStreaming();
			break;
		case NOTIFICATION_CHANGE_STREAM:
			changePosition(notification_data1, notification_data2);
			break;
		case NOTIFICATION_DISCONNECT:
			disconnect();
			break;
		}
	}
	private void disconnect() {
		synchronized (this) {
			state = State.DISCONNECTED;
			request_result = REQUEST_RESULT_STATE_INVALID;
		}
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToDisconnect(this);
		}
	}
	private void changePosition(int numberOfWindows, int position) {
		synchronized (this) {
			request_result = REQUEST_RESULT_STATE_INVALID;
		}
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToChangePostion(this, numberOfWindows, position);
		}
	}
	private void stopStreaming() {
		synchronized (this) {
			state = State.CONNECTED;
			request_result = REQUEST_RESULT_STATE_INVALID;
		}
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToStop(this);
		}
	}
	private void startStreaming(final int numberOfWindows, final int position) {
		synchronized (this) {
			request_result = REQUEST_RESULT_STATE_INVALID;
		}
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToStart(this, numberOfWindows, position);
		}
	}
	@Override
	public boolean canSendStream() {
		synchronized (this) {
			if (state == State.STREAMING) {
				return true;
			}
			return false;
		}
	}
	private static final int REQUEST_RESULT_STATE_INVALID = 0;
	private static final int REQUEST_RESULT_ALLOW 		  = 1;
	private static final int REQUEST_RESULT_DENY 		  = 2;
	private static final int REQUEST_RESULT_NOT_SUPPORTED = 3;
	private static final int REQUEST_RESULT_INVALID_PARAM = 4;

	@SuppressWarnings("unused")
	private void handleRequestResponse(final ByteBuffer header) {
		final byte flag = header.get();
		final byte len = header.get();
		final byte reserve0 = header.get();
		final byte reserve1 = header.get();
		final int request = header.getInt();
		final int request_data1 = header.getInt();
		final int request_data2 = header.getInt();
		synchronized (this) {			
			request_result = header.getInt();
			if (request_result == REQUEST_RESULT_ALLOW) {
				state = State.STREAMING;
			}
		}
		requestReceivedNotificaiton.notifyAll();
	}
	@Override
	protected boolean requestStreaming() throws IllegalArgumentException, IOException {
		synchronized (this) {
			if (request_result == REQUEST_RESULT_ALLOW) {
				return true;
			}
		}
		Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
		OutputStream socketOutputStream = socketToServer.getOutputStream();
		Log.d(TAG, "try to requestStreaming("+getServerAddress()+":"+getPortNumber()+")");	
		socketOutputStream.write(createRequestStreamingPacket().array());
		socketOutputStream.flush();
		try {
			requestReceivedNotificaiton.wait(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		synchronized (this) {			
			return request_result == REQUEST_RESULT_ALLOW;
		}
	}
	private ByteBuffer createRequestStreamingPacket() {
		ByteBuffer header = ByteBuffer.allocate(32);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24
		header.putInt(24);
		// send request streaming command
		header.putInt(6); //tag == 6;
		header.put((byte) 0); // flag = 0
		header.put((byte) 16);
		header.put((byte) 0); // reserve0
		header.put((byte) 0); // reserve1
		header.putInt(0); //畫面分割數(1,2,3,4) 0表示小機指定,在小機 reply時填入小機決定的分割數(1,2,3,4)
		header.putInt(0);
		header.putInt(0);
		header.putInt(0);
		return header;
	}
}
