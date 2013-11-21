package com.actionsmicro.pigeon;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.actionsmicro.utils.Log;
import com.actionsmicro.utils.Utils;

public class MediaStreamingFileDataSource extends MediaStreamingFileBaseDataSource {
	
	static final String TAG = "MediaStreamingFileDataSource";
	private File mediaFile;
	private RandomAccessFile mediaFileInput;
	public MediaStreamingFileDataSource(File mediaFile) throws FileNotFoundException {
		this.mediaFile = mediaFile;
		mediaFileInput = new RandomAccessFile(mediaFile, "r");
		
	}
	@Override
	public long getContentLength() {
		Log.d(TAG, "getContentLength:" + mediaFile.length());
		return mediaFile.length();
	}

	private static final List<String> SUPPORTED_VIDEO_FILE_EXTENSIONS = Arrays.asList("mov", "mkv", "mp4", "avi", "divx", "mpg", "mpeg", "ts", "xvid", "rmvb", "rm", "wmv", "m4v", "3gp", "vob", "dat"); //"aac", "wav", "ogg"
	private static final List<String> SUPPORTED_AUDIO_FILE_EXTENSIONS = Arrays.asList("mp3", "wma", "m4a");
	private static List<String> SUPPORTED_FILE_EXTENSIONS = null;
	static {
		SUPPORTED_FILE_EXTENSIONS = new ArrayList<String>(SUPPORTED_VIDEO_FILE_EXTENSIONS);
		SUPPORTED_FILE_EXTENSIONS.addAll(SUPPORTED_AUDIO_FILE_EXTENSIONS);
	}
	public static List<String> getSupportedFileExt() {
		return SUPPORTED_FILE_EXTENSIONS;
	}
	public static boolean supportsFileExt(String ext) {
		return SUPPORTED_FILE_EXTENSIONS.contains(ext);
	}
	public static boolean isAudioFileExt(String ext) {
		return SUPPORTED_AUDIO_FILE_EXTENSIONS.contains(ext);
	}
	@Override
	public boolean isAudio() {
		return MediaStreamingFileDataSource.isAudioFileExt(Utils.getFileExtension(mediaFile.getAbsolutePath()));
	}
	@Override
	protected void seekTo(final long offset) throws IOException {
		mediaFileInput.seek(offset);
	}
	@Override
	protected int read(final byte[] buffer) throws IOException {
		int sizeRead;
		sizeRead = mediaFileInput.read(buffer);
		return sizeRead;
	}

}
