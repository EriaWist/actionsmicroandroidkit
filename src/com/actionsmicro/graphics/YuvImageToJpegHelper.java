package com.actionsmicro.graphics;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import android.graphics.Rect;
import android.graphics.YuvImage;

public class YuvImageToJpegHelper {
	private ByteArrayOutputStream compressionBuffer;
	Rect bounds = new Rect();
	public synchronized InputStream compressYuvImageToJpegStream(YuvImage yuvImage, int quailty) {
		ByteArrayOutputStream compressionBuffer = getCompressionBuffer();
		compressionBuffer.reset();
		bounds.set(0, 0, yuvImage.getWidth(), yuvImage.getHeight());
		yuvImage.compressToJpeg(bounds, quailty, compressionBuffer);
		return new ByteArrayInputStream(compressionBuffer.toByteArray(), 0, compressionBuffer.size());
	}
	private ByteArrayOutputStream getCompressionBuffer() {
		// for performance reason we keep it as member
		if (compressionBuffer == null) {
			compressionBuffer = new ByteArrayOutputStream(1024*1024);
		}
		return compressionBuffer;
	}
	private static YuvImageToJpegHelper defaultHelper = new YuvImageToJpegHelper();
	public static YuvImageToJpegHelper getDefaultHelper() {
		return defaultHelper;
	}
}
