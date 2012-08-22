package com.actionsmicro.falcon;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.os.Debug;
import android.text.TextPaint;
import android.util.Log;

import com.actionsmicro.pigeon.Client;
import com.actionsmicro.utils.Screen;

public class AMCapDDataConsumer extends DataConsumer {

	static private final String TAG = "AMCapDDataConsumer";
	static private Bitmap s_originalBitmap = null;
	static private Bitmap s_rotateBitmap = null;
	static private ByteBuffer s_framebuffer = null;
	
	private boolean shouldStop = true;
	private final Context context;
	private long s_duration; 
	private final Client ezDisplayClient;
	private class Configs {
		static final boolean ENABLE_PERFORMACE_TRACE = false;
		static final boolean USE_THREAD_TO_COMPRESS_AND_SEND_IMAGE = false;
		static final boolean DRAW_FPS = true;
		static final int COMPRESSOR_QUALITY = 50;
	}
	
	private class Screenshot {
		public ByteBuffer pixels;
		public int width;
		public int height;
		public int bpp;

		public boolean isValid() {
			if (pixels == null || pixels.capacity() == 0 || pixels.limit() == 0) return false;
			if (width <= 0 || height <= 0)	return false;
			return true;
		}

		@Override
		protected void finalize() throws Throwable {
			super.finalize();
		}
		protected void convertFromARGBtoRGBA() {
			pixels.position(0);			
			pixels.put(pixels.array(), 1, pixels.limit() - 1);
			pixels.position(0);
		}
	}
	public AMCapDDataConsumer(Context context, String serverAddress, int portNumber) {
		this.context = context;
		ezDisplayClient = new Client(serverAddress, portNumber);
	}
	public AMCapDDataConsumer(Context context, InetAddress ipAddress, int portNumber) {
		this(context, ipAddress.getHostAddress(), portNumber);
	}
	public String getServerAddress() {
		if (ezDisplayClient != null) {
			return ezDisplayClient.getServerAddress();
		}
		return null;
	}
	@Override
	public void start() {
		synchronized(this) {
			if (!isWorkingThreadRunning()) { // not running
				shouldStop = false;
				new Thread(new Runnable() {
						@Override
					public void run() {
						if (Configs.ENABLE_PERFORMACE_TRACE) {
							Debug.startMethodTracing("AMCapDDataConsumer");
						}
						s_duration = 0;
						while (!shouldStop) {
							long startTime = System.currentTimeMillis();
							Screenshot screenShot = fetchScreenshot();
							if (screenShot != null && screenShot.isValid()) {
								try {
									sendBitmapToServer(convertScreenshotToBitmap(screenShot));
								} catch (UnknownHostException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								} catch (ConnectException e) {
									e.printStackTrace();
									shouldStop = true;
									break;
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								
								updateFPSProbe(startTime);
							} else {
								Log.e(TAG, "screenShot is invalid");						
							}					
						}				
						if (Configs.ENABLE_PERFORMACE_TRACE) {
							Debug.stopMethodTracing();
						}
						Falcon.sendExitCommand();
						if (getDataConsumerDidEndListener() != null) {
							getDataConsumerDidEndListener().dataConsumerDidEnd(AMCapDDataConsumer.this);
						}
					}
					
				}).start();
			}
		}
		
	}
	private boolean isWorkingThreadRunning() {
		return shouldStop == false;
	}

	@Override
	public void stop() {
		if (isWorkingThreadRunning()) {
			synchronized(this) {
				shouldStop = true;
			}
		}
	}

	private Screenshot fetchScreenshot() {
		try {
			Screenshot screenshot = new Screenshot();
			InputStream socketInput = getProvider().getInputStream();
			readHeaderFromInput(socketInput, screenshot);
			Log.d(TAG, "width:"+screenshot.width+", height:"+screenshot.height+", bpp:"+screenshot.bpp);
			readBitmapDataFromInput(socketInput, screenshot);
			return screenshot;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	private void readBitmapDataFromInput(InputStream socketInput,
			Screenshot screenshot) throws IOException {
		int sizeRead;
		ByteBuffer buffer = getSharedFrameBuffer(screenshot.width, screenshot.height, screenshot.bpp);
		sizeRead = 0;
		while (sizeRead < buffer.limit()) {
			sizeRead += socketInput.read(buffer.array(), sizeRead, buffer.limit() - sizeRead);
			Log.d(TAG, "size read:" + sizeRead);
		}
		buffer.position(0);					// reset position to the beginning of ByteBuffer
		screenshot.pixels = buffer;
		buffer.clear();
	}
	private Bitmap.Config convertBppToBitmapConfig(int bpp) {
		Bitmap.Config bitmapConfig = Config.RGB_565;
		switch (bpp) {
		case 16:	
			bitmapConfig = Config.RGB_565;
			break;
		case 32:	
			bitmapConfig = Config.ARGB_8888;
			break;
		default:	
			bitmapConfig = Config.RGB_565; Log.d(TAG, "BPP is a weird value"); 
			break;
		}
		return bitmapConfig;
	}

	private static ByteBuffer getSharedFrameBuffer(int width, int height, int bpp) {
		if (s_framebuffer == null) {
			s_framebuffer = ByteBuffer.allocate (width * height * bpp / 8);
		}
		s_framebuffer.clear();
		return s_framebuffer;
	}
	private static void readHeaderFromInput(InputStream socketInput, Screenshot screenshot) throws IOException {
		ByteBuffer header = ByteBuffer.allocate(3*4).order(ByteOrder.LITTLE_ENDIAN);
		int sizeRead = 0;
		while (sizeRead < header.limit()) {
			sizeRead += socketInput.read(header.array(), sizeRead, header.limit() - sizeRead);
		}
		
		header.position(0);
		screenshot.width = header.getInt();
		screenshot.height = header.getInt();
		screenshot.bpp = header.getInt();
	}
	private void drawFPSOnBitmap(Bitmap bmpToWrite) {
		Canvas canvas = new Canvas(bmpToWrite); 
		Paint paint = new TextPaint();
		paint.setColor(Color.RED);
		paint.setStyle(Paint.Style.FILL);
		paint.setTextSize(24);
		canvas.drawText(String.format("fps:%.3f", (1000.0/s_duration)), 10, 20, paint);
	}
	private void prepareSharedBitmapIfNeeded(int width, int height,
			Bitmap.Config bitmapConfig) {
		if (s_originalBitmap == null) {
			s_originalBitmap = Bitmap.createBitmap(width, height, bitmapConfig);
		}
		if (s_rotateBitmap == null) {
			s_rotateBitmap = Bitmap.createBitmap(height, width, bitmapConfig);
		}
	}
	private Bitmap convertScreenshotToBitmap(Screenshot screenShot) {
		Bitmap.Config bitmapConfig = convertBppToBitmapConfig(screenShot.bpp);
		if (bitmapConfig == Config.ARGB_8888) {
			// TODO This is not necessary on 2.3. Need to figure out when to convert
			screenShot.convertFromARGBtoRGBA();
		}
		prepareSharedBitmapIfNeeded(screenShot.width, screenShot.height, bitmapConfig);
		
		Bitmap bitmap = s_originalBitmap;
		screenShot.pixels.position(0);
		bitmap.copyPixelsFromBuffer(screenShot.pixels);
		
		int rot = Screen.calcRotationForBuffer(context, screenShot.width, screenShot.height);
		if (rot != 0) {
			bitmap = rotateBitmap(bitmap, rot);
		}
		if (Configs.DRAW_FPS) {
			drawFPSOnBitmap(bitmap);
		}
		return bitmap;
	}
	private Bitmap rotateBitmap(Bitmap bitmap, int rot) {
		Matrix matrix = new Matrix();
		matrix.preTranslate(bitmap.getHeight()/2, bitmap.getWidth()/2);
		matrix.preRotate(-rot);
		matrix.preTranslate(-bitmap.getWidth()/2, -bitmap.getHeight()/2);
		
		Canvas canvas = new Canvas(s_rotateBitmap);
		canvas.setMatrix(matrix);
		canvas.drawBitmap(bitmap, 0, 0, null);
		bitmap = s_rotateBitmap;
		return bitmap;
	}
	private void sendBitmapToServer(final Bitmap bmpToWrite) throws IOException, UnknownHostException {
		if (Configs.USE_THREAD_TO_COMPRESS_AND_SEND_IMAGE) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					synchronized(bmpToWrite) {
						try {
							ezDisplayClient.sentImageToServer(bmpToWrite, Bitmap.CompressFormat.JPEG, Configs.COMPRESSOR_QUALITY);
						} catch (UnknownHostException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
			}).start();
		} else {
			ezDisplayClient.sentImageToServer(bmpToWrite, Bitmap.CompressFormat.JPEG, Configs.COMPRESSOR_QUALITY);
		}
	}
	private void updateFPSProbe(long startTime) {
		if (s_duration == 0) {
			s_duration = System.currentTimeMillis() - startTime;
		} else {
			s_duration = (s_duration * 7 + (System.currentTimeMillis() - startTime) * 3)/10;
		}
	}
}
