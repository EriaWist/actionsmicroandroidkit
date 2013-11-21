package com.actionsmicro.pigeon;

import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

public class MediaStreamingContentUriDataSource extends
		MediaStreamingFileBaseDataSource {

	private Context context;
	private Uri uri;
	private InputStream inputStream;
	private long contentLength = -1;
	private String mimeType;

	public MediaStreamingContentUriDataSource(Context context, Uri uri) {
		this.context = context;
		this.uri = uri;
	}
	@Override
	public long getContentLength() {
		if (contentLength  == -1) {
			String[] proj = { OpenableColumns.SIZE };
			Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
			if (cursor != null && cursor.moveToFirst()) {
				int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
				if (!cursor.isNull(sizeIndex)) {
					contentLength = Long.valueOf(cursor.getString(sizeIndex));
				} else {
					contentLength = cursor.getInt(sizeIndex);
				}
			}
		}
		return contentLength;
	}

	@Override
	public boolean isAudio() {
		if (mimeType == null) {
			mimeType = context.getContentResolver().getType(uri);
		}
		if (mimeType != null && mimeType.startsWith("audio")) {
			return true;
		}
		return false;
	}

	@Override
	protected int read(byte[] buffer) throws IOException {
		if (inputStream != null) {
			return inputStream.read(buffer);
		}
		return 0;
	}

	@Override
	protected void seekTo(long offset) throws IOException {
		if (inputStream != null) {
			inputStream.close();
		}
		inputStream = context.getContentResolver().openInputStream(uri);
		inputStream.skip(offset);
	}

}
