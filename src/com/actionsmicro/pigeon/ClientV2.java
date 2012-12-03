package com.actionsmicro.pigeon;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.os.Handler;

import com.actionsmicro.utils.Log;

public class ClientV2 extends Client implements MultiRegionsDisplay, MediaStreaming {
	private static final String TAG = "ClientV2";
	private static final int PICO_STATUS_CMD = 1;
	private static final int PICO_HBSC_CMD = 3;
	private static final int PICO_QUERY_INFO = 5;
	private static final int PICO_REQUEST = 6;
	private static final int PICO_NOTIFICATION = 7;
	private static final int PICO_AV_STREAM_CMD = 8;
	private static final int PICO_SET_DATA = 9;
	private static final int PICO_CONTROL = 10;
	private static final int PICO_VENDOR_CMD = 0x40;

	private final Thread receivingThread = new Thread(new Runnable() {
		@Override
		public void run() {
			try {
				final Socket socketToServer = createSocketToServer(DEFAULT_SOCKET_TIMEOUT);
				final InputStream socketInputStream = socketToServer.getInputStream();
				final ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
				header.order(ByteOrder.LITTLE_ENDIAN);
				while (true) {
					final int headerSize = socketInputStream.read(header.array());
					Log.d(TAG, "receivingThread incoming message header size:"+headerSize);
					if (headerSize == -1) {
						// TODO throw exception;
						break;
					}
					if (headerSize == EZ_DISPLAY_HEADER_SIZE) {
						@SuppressWarnings("unused")
						final int sequenceNumber = header.getInt();
						final int totalSize = header.getInt();
						final int payloadSize = totalSize - (EZ_DISPLAY_HEADER_SIZE - 8);
						Log.d(TAG, "payloadSize:" + payloadSize);
						ByteBuffer payload = null;
						if (payloadSize > 0) {
							payload = ByteBuffer.allocate(payloadSize);
							payload.order(ByteOrder.LITTLE_ENDIAN);
							socketInputStream.read(payload.array());
						}
						try {
							final int tag = header.getInt();
							Log.d(TAG, "wifi tag:" + tag);
							switch (tag) {
							case PICO_REQUEST:
								handleRequestResponse(header);
								break;
							case PICO_NOTIFICATION:
								handleNotification(header);
								break;
							case PICO_AV_STREAM_CMD:
								handleAVStreamCmd(header, payload);
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
			} catch (SocketException e) {
				if (shouldStop == true) {
					//Do Nothing
				} else {
					handleExceptionInThread(e);
				}
			} catch (Exception e) {
				handleExceptionInThread(e);
			}
		}
	});
	private int request_result = REQUEST_RESULT_STATE_INVALID;
	private Object requestReceivedNotificaiton = new Object();
	private Object avCommandVolumeResponseReceivedNotificaiton = new Object();
	private Object avCommandGetDurationResponseReceivedNotificaiton = new Object();
	private Object avCommandGetTimeResponseReceivedNotificaiton = new Object();
	private Object avCommandPauseResponseReceivedNotificaiton = new Object();
	private Object avCommandResumeResponseReceivedNotificaiton = new Object();
	private Object avCommandSeekToResponseReceivedNotificaiton = new Object();
	// TODO remove handler, let client decide when to use handler or not
	private Handler handler = new Handler();
	private String hostname;
	protected ClientV2(String serverAddress, int portNumber, String hostname) {
		super(serverAddress, portNumber);
		this.hostname = hostname;
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
		Log.d(TAG, "handleNotification");
		final byte flag = header.get();
		final byte len = header.get();
		final byte reserve0 = header.get();
		final byte reserve1 = header.get();
		final int notification = header.getInt();
		final int notification_data1 = header.getInt();
		final int notification_data2 = header.getInt();
		handler.post(new Runnable() {
			@Override
			public void run() {
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
		});
	}
	
	
	private void disconnect() {
		synchronized (this) {
			state = State.DISCONNECTED;
			request_result = REQUEST_RESULT_STATE_INVALID;
			requestedNumberOfWindow = 0;
			requestedPosition = 0;
		}
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToDisconnect(this);
		}
	}
	private int requestedNumberOfWindow = 0;
	private int requestedPosition = 0;
	
	private void changePosition(int numberOfWindows, int position) {
		synchronized (this) {
			state = State.CONNECTED;
			request_result = REQUEST_RESULT_STATE_INVALID;
			requestedNumberOfWindow = numberOfWindows;
			requestedPosition = position;
		}
		requestStreaming(numberOfWindows, position);
		Log.d(TAG, "onRemoteRequestToChangePostion:(" + numberOfWindows +"/"+position+")");
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToChangePostion(this, numberOfWindows, position);
		}
	}
	private void stopStreaming() {
		synchronized (this) {
			state = State.CONNECTED;
			request_result = REQUEST_RESULT_STATE_INVALID;
			requestedNumberOfWindow = 0;
			requestedPosition = 0;
		}
		Log.d(TAG, "onRemoteRequestToStop");
		final OnNotificationListener onNotificationListener = getOnNotificationListener();
		if (onNotificationListener != null) {
			onNotificationListener.onRemoteRequestToStop(this);
		}
	}
	private void startStreaming(final int numberOfWindows, final int position) {
		synchronized (this) {
			request_result = REQUEST_RESULT_STATE_INVALID;
			requestedNumberOfWindow = numberOfWindows;
			requestedPosition = position;
		}
		requestStreaming(numberOfWindows, position);
		Log.d(TAG, "onRemoteRequestToStart:(" + numberOfWindows +"/"+position+")");
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
	private static final int REQUEST_RESULT_FULL		  = 5;
	private static final int REQUEST_RESULT_NOT_AVAILABLE = 6;

	@SuppressWarnings("unused")
	private void handleRequestResponse(final ByteBuffer header) {
		Log.d(TAG, "handleRequestResponse");
		final byte flag = header.get();
		final byte len = header.get();
		final byte reserve0 = header.get();
		final byte reserve1 = header.get();
		final int request = header.getInt();
		synchronized (this) {			
			requestedNumberOfWindow = header.getInt();
			requestedPosition = header.getInt();
			request_result = header.getInt();
			Log.d(TAG, "handleRequestResponse:" + request_result + ", number of window:" + requestedNumberOfWindow + ", position:" + requestedPosition);
			if (request_result == REQUEST_RESULT_ALLOW) {
				state = State.STREAMING;
			}
		}
		synchronized (requestReceivedNotificaiton) {			
			requestReceivedNotificaiton.notifyAll();
		}
	}
	@Override
	protected boolean requestStreaming() throws IllegalArgumentException, IOException {
		synchronized (this) {
			if (request_result == REQUEST_RESULT_ALLOW) {
				return true;
			}
		}
		RequestResult result = requestStreaming(requestedNumberOfWindow, requestedPosition);
		return result == RequestResult.ALLOW;
	}
	private static ByteBuffer createRequestStreamingPacket(int streamingFormat, int requestedNumberOfWindow, int requestedPosition) {
		final ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24
		header.putInt(24);
		// send request streaming command
		header.putInt(PICO_REQUEST); //tag == 6;
		header.put((byte) 0); // flag = 0
		header.put((byte) 16);
		header.put((byte) 0); // reserve0
		header.put((byte) 0); // reserve1
		header.putInt(1); 
		header.putInt(requestedNumberOfWindow); //畫面分割數(1,2,3,4) 0表示小機指定,在小機 reply時填入小機決定的分割數(1,2,3,4)
		header.putInt(requestedPosition);
		header.putInt(streamingFormat);
		return header;
	}
	private static ByteBuffer createSetDataHostnamePacket(String hostname) {
		try {
			final byte[] hostnameInBytes = hostname.getBytes("UTF-8");
			final ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE+hostnameInBytes.length);
			header.order(ByteOrder.LITTLE_ENDIAN);	
			// Sequence
			header.putInt(getCommandSequenceNumber());
			// TCP packet size = 24
			header.putInt(24+hostnameInBytes.length);
			// send request streaming command
			header.putInt(PICO_SET_DATA);
			header.put((byte) 0); // flag = 0
			header.put((byte) 16);
			header.put((byte) 0); // reserve0
			header.put((byte) 0); // reserve1
			header.putInt(1); //SET_DATA_HOSTNAME	
			header.putInt(0); 
			header.putInt(0);
			header.putInt(hostnameInBytes.length);
			header.put(hostnameInBytes);
			return header;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	@Override
	public String getVersion() {
		return "2";
	}
	private RequestResult requestResultToEnum(int result) {
		switch (result) {
		case REQUEST_RESULT_ALLOW:
			return RequestResult.ALLOW;
		case REQUEST_RESULT_DENY:
			return RequestResult.DENY;
		case REQUEST_RESULT_NOT_SUPPORTED:
			return RequestResult.NOT_SUPPORTED;
		case REQUEST_RESULT_INVALID_PARAM:
			return RequestResult.INVALID_PARAMETER;
		case REQUEST_RESULT_FULL:
			return RequestResult.FULL;
		case REQUEST_RESULT_NOT_AVAILABLE:
			return RequestResult.NOT_AVAILABLE;
		}
		return RequestResult.UNDIFINED;
		
	}
	@Override
	public RequestResult requestStreaming(final int numberOfRegions, final int position) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				Log.d(TAG, "try to requestStreaming("+getServerAddress()+":"+getPortNumber()+")");	
				// TODO add parameter to let client assign the stream format
				sendDataToRemote(createRequestStreamingPacket(STREAM_FORMAT_JEPG, numberOfRegions, position).array());
			}			
		}).start();
		
		try {
			synchronized (requestReceivedNotificaiton) {			
				requestReceivedNotificaiton.wait(3000);
				Log.d(TAG, "requestReceivedNotificaiton wait returns");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		synchronized (this) {			
			Log.d(TAG, "requestStreaming check:" + request_result);
			return requestResultToEnum(request_result);
		}
	}
	@Override
	public int getNumberOfRegions() {
		return requestedNumberOfWindow;
	}
	@Override
	public int getPosition() {
		return requestedPosition;
	}
	@Override
	protected void onConnectToServer(Socket socketToServer) {
		if (hostname != null) {
			ByteBuffer hostnamePacket = createSetDataHostnamePacket(hostname);
			if (hostnamePacket != null) {
				sendDataToRemote(hostnamePacket.array());
			}
		}
	}
	private static final int AV_FILE_START = 1;
	private static final int AV_FILE_STOP = 2;
	private static final int AV_FILE_GET_LENGTH = 3;
	private static final int AV_FILE_GET_SEEKABLE = 4;
	private static final int AV_FILE_READ = 5;
	private static final int AV_FILE_PAUSE = 6;
	private static final int AV_FILE_EOF = 7;
	private static final int AV_PLAYER_GET_LENGTH = 8;
	private static final int AV_PLAYER_GET_TIME = 9;
	private static final int AV_PLAYER_SEEKPLAY = 10;
	private static final int AV_PLAYER_PAUSE = 11;
	private static final int AV_PLAYER_RESUME = 12;
	private static final int AV_PLAYER_FFPLAY = 13;
	private static final int AV_PLAYER_FBPLAY = 14;
	private static final int AV_PLAYER_RESET = 15;
	private static final int AV_PLAYER_VOLUME_UP = 16;
	private static final int AV_PLAYER_VOLUME_DOWN = 17;
	
	private static final int AV_TYPE_VOID = 0;
	private static final int AV_TYPE_INT32 = 1;
	private static final int AV_TYPE_INT64 = 2;
	
	private static final int AV_SIZE_VOID = 0;
	private static final int AV_SIZE_INT32 = 4;
	private static final int AV_SIZE_INT64 = 8;
	
	private static final int AV_RESULT_OK = 0;
	private static final int AV_RESULT_ERROR = 1;
	
	private static final int STREAM_FORMAT_EZSTREAM = 6;
	
	private DataSource currentDataSource;
	private boolean isStreamingMedia;
	private int intResponse;
	private int avRequestResult;
	private PlayerState playerState = PlayerState.STOPPED;
	private static void prepareHeaderForAvStreamCmd(ByteBuffer header, int payloadSize, int cmd, int type, int size) {
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24
		header.putInt(24 + payloadSize);
		// send request streaming command
		header.putInt(PICO_AV_STREAM_CMD);
		header.put((byte) 0); // flag = 0
		header.put((byte) 16);
		header.put((byte) 0); // reserve0
		header.put((byte) 0); // reserve1
		header.putInt(cmd); 
		header.putInt(type); 
		header.putInt(size);
		header.putInt(AV_RESULT_OK);
	}
	private static ByteBuffer createPlayerResetPacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_PLAYER_RESET, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	private static ByteBuffer createStartFileStreamingPacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_FILE_START, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	private static ByteBuffer createFileEofPacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_FILE_EOF, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	private static ByteBuffer createFileStopPacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_FILE_STOP, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	private static ByteBuffer createFileGetLengthResponsePacket(long contentLength) {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE+AV_SIZE_INT64);
		packet.order(ByteOrder.LITTLE_ENDIAN);	
		prepareHeaderForAvStreamCmd(packet, AV_SIZE_INT64, AV_FILE_GET_LENGTH, AV_TYPE_INT64, AV_SIZE_INT64);
		packet.putLong(contentLength);
		return packet;
	}
	private static ByteBuffer createFileGetSeekableResponsePacket(boolean isSeekable) {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE+AV_SIZE_INT32);
		packet.order(ByteOrder.LITTLE_ENDIAN);	
		prepareHeaderForAvStreamCmd(packet, AV_SIZE_INT32, AV_FILE_GET_SEEKABLE, AV_TYPE_INT32, AV_SIZE_INT32);
		packet.putInt(isSeekable?1:0);
		return packet;
	}
	private ByteBuffer createFileReadResponsePacket(long offset) {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE+AV_SIZE_INT64);
		packet.order(ByteOrder.LITTLE_ENDIAN);	
		prepareHeaderForAvStreamCmd(packet, AV_SIZE_INT64, AV_FILE_READ, AV_TYPE_INT64, AV_SIZE_INT64);
		packet.putLong(offset);
		return packet;
	}
	@SuppressWarnings("unused")
	private void handleAVStreamCmd(final ByteBuffer header, final ByteBuffer payload) {
		Log.d(TAG, "handleAVStreamCmd");
		final byte flag = header.get();
		final byte len = header.get();
		final byte reserve0 = header.get();
		final byte reserve1 = header.get();
		final int request_cmd = header.getInt();
		final int request_data_type = header.getInt();
		final int request_data_size = header.getInt();
		avRequestResult = header.getInt();
//		handler.post(new Runnable() {
//			@Override
//			public void run() {
				switch (request_cmd) {
				case AV_FILE_GET_LENGTH:
					Log.d(TAG, "AV_FILE_GET_LENGTH:" + avRequestResult);
					assert (request_data_type == AV_TYPE_INT64 && request_data_size == AV_SIZE_INT64);
					responseFileGetLength();
					break;
				case AV_FILE_GET_SEEKABLE:
					Log.d(TAG, "AV_FILE_GET_SEEKABLE:" + avRequestResult);
					assert (request_data_type == AV_TYPE_INT32 && request_data_size == AV_SIZE_INT32);
					responseFileGetSeekable();
					break;
				case AV_FILE_START:
					Log.d(TAG, "AV_FILE_START:" + avRequestResult);
					handleFileStart(avRequestResult);
					break;
				case AV_FILE_STOP:
					Log.d(TAG, "AV_FILE_STOP:" + avRequestResult);
					handleFileStop();
					break;
				case AV_FILE_READ:
					Log.d(TAG, "AV_FILE_READ:" + avRequestResult);
					assert (request_data_type == AV_TYPE_INT64 && request_data_size == AV_SIZE_INT64);
					responseFileRead(payload.getLong());
					break;
				case AV_FILE_PAUSE:
					Log.d(TAG, "AV_FILE_PAUSE:" + avRequestResult);
					handleFilePause();
					break;
				case AV_PLAYER_GET_LENGTH:
					Log.d(TAG, "AV_PLAYER_GET_LENGTH:" + avRequestResult);
					handlePlayerGetLength(payload.getInt());
					synchronized(avCommandGetDurationResponseReceivedNotificaiton) {
						avCommandGetDurationResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_GET_TIME:
					Log.d(TAG, "AV_PLAYER_GET_TIME:" + avRequestResult);
					handlePlayerGetTime(payload.getInt());
					synchronized(avCommandGetTimeResponseReceivedNotificaiton) {
						avCommandGetTimeResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_SEEKPLAY:
					Log.d(TAG, "AV_PLAYER_SEEKPLAY:" + avRequestResult);
					synchronized(avCommandSeekToResponseReceivedNotificaiton) {
						avCommandSeekToResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_PAUSE:
					Log.d(TAG, "AV_PLAYER_PAUSE:" + avRequestResult);
					if (avRequestResult == AV_RESULT_OK) {
						playerState = PlayerState.PAUSED;
					}
					synchronized(avCommandPauseResponseReceivedNotificaiton) {
						avCommandPauseResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_RESUME:
					Log.d(TAG, "AV_PLAYER_RESUME:" + avRequestResult);
					if (avRequestResult == AV_RESULT_OK) {
						playerState = PlayerState.PLAYING;
					}
					synchronized(avCommandResumeResponseReceivedNotificaiton) {
						avCommandResumeResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_FFPLAY:
					Log.d(TAG, "AV_PLAYER_FFPLAY:" + avRequestResult);
					break;
				case AV_PLAYER_FBPLAY:
					Log.d(TAG, "AV_PLAYER_FBPLAY:" + avRequestResult);
					break;
				case AV_PLAYER_VOLUME_UP:
					Log.d(TAG, "AV_PLAYER_VOLUME_UP:" + avRequestResult);
					synchronized(avCommandVolumeResponseReceivedNotificaiton) {
						avCommandVolumeResponseReceivedNotificaiton.notifyAll();
					}
					break;
				case AV_PLAYER_VOLUME_DOWN:
					Log.d(TAG, "AV_PLAYER_VOLUME_DOWN:" + avRequestResult);
					synchronized(avCommandVolumeResponseReceivedNotificaiton) {
						avCommandVolumeResponseReceivedNotificaiton.notifyAll();
					}
					break;
				default:
					assert false : request_cmd;
					break;
				}
//			}									
//		});
	}
	private void handlePlayerGetTime(int time) {
		Log.d(TAG, "handlePlayerGetTime:"+time);
		intResponse = time;
		if (currentDataSource != null) {
			currentDataSource.playerTimeDidChange(time);
		}
	}
	private void handlePlayerGetLength(int length) {
		Log.d(TAG, "handlePlayerGetLength:"+length);
		intResponse = length;
		if (currentDataSource != null) {
			currentDataSource.playerTimeDurationReady(intResponse);
		}
	}
	private void handleFileStart(final int request_result) {
		if (request_result == AV_RESULT_ERROR) {
			assert currentDataSource != null:"currentDataSource should not be null";
			if (currentDataSource != null) {
				currentDataSource.mediaStreamingDidFail(request_result);
			} else {
				Log.e(TAG, "AV_FILE_START failed");
			}						
		} else {
			isStreamingMedia = true;
			playerState = PlayerState.PLAYING;
		}
	}
	private void handleFileStop() {
		assert currentDataSource != null:"currentDataSource should not be null";
		isStreamingMedia = false;
		playerState = PlayerState.STOPPED;
		if (currentDataSource != null) {
			currentDataSource.stopStreamingContents();
		}
		sendDataToRemote(createFileStopPacket().array());		
	}
	private void handleFilePause() {
		assert currentDataSource != null:"currentDataSource should not be null";
		isStreamingMedia = false;
		if (currentDataSource != null) {
			currentDataSource.pauseStreamingContents();
		}
	}
	private void responseFileRead(long offset) {
		Log.d(TAG, "responseFileRead(offset:" + offset +")");		
		assert currentDataSource != null:"currentDataSource should not be null";
		if (currentDataSource != null) {
			long contentLength = currentDataSource.getContentLength();
			assert contentLength >= 0:"contentLength should be equal to or larger than zero";
			assert offset <= contentLength;
			if (contentLength >= 0 && offset <= contentLength) {
				isStreamingMedia = true;
				currentDataSource.pauseStreamingContents(offset);
				sendDataToRemote(createFileReadResponsePacket(offset).array());				
				currentDataSource.startStreamingContents(this, offset);
			}
		}
	}
	private void responseFileGetSeekable() {
		assert currentDataSource != null:"currentDataSource should not be null";
		if (currentDataSource != null) {
			sendDataToRemote(createFileGetSeekableResponsePacket(currentDataSource.isSeekable()).array());
		}
	}
	private void responseFileGetLength() {
		assert currentDataSource != null:"currentDataSource should not be null";
		if (currentDataSource != null) {
			long contentLength = currentDataSource.getContentLength();
			assert contentLength >= 0:"contentLength should be equal to or larger than zero";
			if (contentLength >= 0) {
				Log.d(TAG, "try response to AV_FILE_GET_LENGTH");	
				sendDataToRemote(createFileGetLengthResponsePacket(contentLength).array());
			}
		}
	}
	@Override
	public void startMediaStreaming(DataSource dataSource) {
		assert dataSource != null:"dataSource should not be null";
		if (dataSource != null) {
			currentDataSource = dataSource;
			Log.d(TAG, "try to AV_FILE_START");	
			sendDataToRemote(createStartFileStreamingPacket().array());
		}
	}

	private static ByteBuffer createPacketHeaderForSendingEzStream(int payloadSize) {
		ByteBuffer header = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		header.order(ByteOrder.LITTLE_ENDIAN);	
		// Sequence
		header.putInt(getCommandSequenceNumber());
		// TCP packet size = 24 + compressed image size
		header.putInt(24+payloadSize);
		// send image command
		header.putInt(PICO_PIC_FORMAT_CMD); //tag == 2;
		header.put((byte) 0); // flag = 0
		header.put((byte) 16);
		header.put((byte) 0); // reserve0
		header.put((byte) 0); // reserve1
		header.putInt(STREAM_FORMAT_EZSTREAM);
		header.putInt(0);
		header.putInt(0);
		header.putInt(payloadSize);
		return header;
	}
	@Override
	public void sendStreamingContents(byte[] contents, int length) {
		synchronized (this) {
			Log.d(TAG, "sendStreamingContents:"+length);
			sendDataToRemote(createPacketHeaderForSendingEzStream(length).array());
			sendDataToRemote(contents, length);
		}
	}
	@Override
	public void sendEofPacket() {
		sendDataToRemote(createFileEofPacket().array());		
	}
	@Override
	public void stopMediaStreaming() {
		if (currentDataSource != null) {
			currentDataSource.stopStreamingContents();
		}
		sendDataToRemote(createFileStopPacket().array());
		isStreamingMedia = false;
		playerState = PlayerState.STOPPED;
	}

	private DatagramSocket udpSocket;
	private DatagramSocket getUdpSocket() throws SocketException {
		if (udpSocket == null) {
			udpSocket = new DatagramSocket();
		}
		return udpSocket;
	}
	static private final int EZ_WIFI_DISPLAY_PORT_NUMBER = 2425;
	static private void logBytes(final byte[] data, int length) {
		Log.d(TAG, "" + data.toString());
		StringBuilder stringBuilder = new StringBuilder();
		int counter = 0;
		for (byte theByte : data) {
			stringBuilder.append(String.format("%02x ", theByte));
			counter ++;
			if (counter >= length) {
				break;
			}
			if (counter%4 == 0) {
				stringBuilder.append("\n");
			}
			
		}
		Log.d(TAG, stringBuilder.toString());
	}
	@Override
	public void sendStreamingContentsUdp(byte[] contents, int length) {
		try {
			final DatagramSocket udpSocket = getUdpSocket();
			final ByteBuffer header = createPacketHeaderForSendingEzStream(length);
			final ByteBuffer udpPacket = ByteBuffer.allocate(header.position() + length);
			udpPacket.put(header.array(), 0, header.position());
			udpPacket.put(contents, 0, length);
//			logBytes(ByteBuffer.wrap(udpPacket.array(), 0, header.position()).array(), header.position());
			udpSocket.send(new DatagramPacket(udpPacket.array(), 0, udpPacket.position(), InetAddress.getByName(getServerAddress()), EZ_WIFI_DISPLAY_PORT_NUMBER));
//			udpSocket.send(new DatagramPacket(contents, 0, length, InetAddress.getByName("192.168.66.100"), EZ_WIFI_DISPLAY_PORT_NUMBER));
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	@Override
	protected boolean shouldSendHeartbeat() {
		return !isStreamingMedia;
	}
	@Override
	public void resetPlayer() {
		sendDataToRemote(createPlayerResetPacket().array());
	}

	private boolean sendAVCommandAndWaitForResponse(final byte commandPacket[], Object syncObject) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				sendDataToRemote(commandPacket);
			}			
		}).start();
		try {
			synchronized (syncObject) {			
				syncObject.wait(3000);
				Log.d(TAG, "syncObject wait returns");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
	private static ByteBuffer createPlayerGetLengthPacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE + AV_SIZE_INT32);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 4, AV_PLAYER_GET_LENGTH, AV_TYPE_INT32, AV_SIZE_INT32);
		packet.putInt(0);
		return packet;
	}
	@Override
	public int getDuration() {
		if (sendAVCommandAndWaitForResponse(createPlayerGetLengthPacket().array(), avCommandGetDurationResponseReceivedNotificaiton)) {
			return intResponse;
		}
		return -1;
	}
	private static ByteBuffer createPlayerGetTimePacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE + AV_SIZE_INT32);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 4, AV_PLAYER_GET_TIME, AV_TYPE_INT32, AV_SIZE_INT32);
		packet.putInt(0);
		return packet;
	}
	
	@Override
	public int getTime() {
		if (sendAVCommandAndWaitForResponse(createPlayerGetTimePacket().array(), avCommandGetTimeResponseReceivedNotificaiton)) {
			return intResponse;
		}
		return -1;
	}
	private static ByteBuffer createPlayerSeekPlayPacket(int position) {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE + AV_SIZE_INT32);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 4, AV_PLAYER_SEEKPLAY, AV_TYPE_INT32, AV_SIZE_INT32);
		packet.putInt(position);
		return packet;
	}
	@Override
	public int seekTo(int position) {
		Log.d(TAG, "seekTo:"+position);
		if (sendAVCommandAndWaitForResponse(createPlayerSeekPlayPacket(position).array(), avCommandSeekToResponseReceivedNotificaiton)) {
			return avRequestResult;
		}
		return -1;
	}
	private static ByteBuffer createPlayerPausePacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_PLAYER_PAUSE, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	@Override
	public int pauseMediaStreaming() {
		if (sendAVCommandAndWaitForResponse(createPlayerPausePacket().array(), avCommandPauseResponseReceivedNotificaiton)) {
			return avRequestResult;
		}
		return -1;
	}
	private static ByteBuffer createPlayerResumePacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_PLAYER_RESUME, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	@Override
	public int resumeMediaStreaming() {
		if (sendAVCommandAndWaitForResponse(createPlayerResumePacket().array(), avCommandResumeResponseReceivedNotificaiton)) {
			return avRequestResult;
		}
		return -1;
	}

	private static ByteBuffer createPlayerIncreaseVolumePacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_PLAYER_VOLUME_UP, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	@Override
	public int increaseVolume() {
		if (sendAVCommandAndWaitForResponse(createPlayerIncreaseVolumePacket().array(), avCommandVolumeResponseReceivedNotificaiton)) {
			return avRequestResult;
		}
		return -1;
	}

	private static ByteBuffer createPlayerDecreaseVolumePacket() {
		final ByteBuffer packet = ByteBuffer.allocate(EZ_DISPLAY_HEADER_SIZE);
		packet.order(ByteOrder.LITTLE_ENDIAN);
		prepareHeaderForAvStreamCmd(packet, 0, AV_PLAYER_VOLUME_DOWN, AV_TYPE_VOID, AV_SIZE_VOID);
		return packet;
	}
	@Override
	public int decreaseVolume() {
		if (sendAVCommandAndWaitForResponse(createPlayerDecreaseVolumePacket().array(), avCommandVolumeResponseReceivedNotificaiton)) {
			return avRequestResult;
		}
		return -1;
	}
	@Override
	public PlayerState getPlayerState() {
		return playerState;
	}
}
