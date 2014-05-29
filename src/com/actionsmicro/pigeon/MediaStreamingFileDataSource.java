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
	private static final List<String> SUPPORTED_VIDEO_FILE_EXTENSIONS = Arrays.asList("avi", "divx", "xvid", "mp4", "mov", "vob", "dat", "ts", "m2ts", "mts", "mkv", "rmvb", "rm", "mpg", "mpeg", "wmv", "m4v", "3gp", "asf", "flv"); //"aac", "wav", "ogg", 
	private static final List<String> SUPPORTED_AUDIO_FILE_EXTENSIONS = Arrays.asList("ape", "flac", "ogg", "mp3", "wma", "wav", /*"rm",*/ "m4a", "aac", "drs", "ac3", "ra", "aif", "aiff", "m4a", "mka");
	private static List<String> SUPPORTED_FILE_EXTENSIONS = null;
	static {
		SUPPORTED_FILE_EXTENSIONS = new ArrayList<String>(SUPPORTED_VIDEO_FILE_EXTENSIONS);
		SUPPORTED_FILE_EXTENSIONS.addAll(SUPPORTED_AUDIO_FILE_EXTENSIONS);
	}
	public static List<String> getSupportedFileExt() {
		return SUPPORTED_FILE_EXTENSIONS;
	}
	public static boolean supportsFileExt(String ext) {
		return SUPPORTED_FILE_EXTENSIONS.contains(ext.toLowerCase());
	}
	public static boolean isAudioFileExt(String ext) {
		return SUPPORTED_AUDIO_FILE_EXTENSIONS.contains(ext.toLowerCase());
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
