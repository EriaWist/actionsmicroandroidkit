package com.actionsmicro.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashSet;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;


public class DownloadHttpContentToCacheFolderTask extends AsyncTask<Void, Void, File> {
	private String cacheFolderName;
	private String preferenceKey;
	private Context context;
	private String preferenceName;
	private URL source;
	private int downloadTimeout = 2000;
	private int connectTimeout = 2000;
	private static final HashSet<URL> downloadingTasks = new HashSet<URL>();
	private static final String TAG = "DownloadHttpContentToCacheFolderTask";
	public static boolean isScheduledToDownloadUrl(URL source) {
		return downloadingTasks.contains(source);
	}
	public DownloadHttpContentToCacheFolderTask(Context context, String cacheFolderName, String preferenceName, String preferenceKey, URL source) {
		this.context = context;
		this.cacheFolderName = cacheFolderName;
		this.preferenceKey = preferenceKey;
		this.preferenceName = preferenceName;
		this.source = source;
		downloadingTasks.add(source);
	}
	private File getFileCacheDir() {
		if (context == null) return null;
		final File cacheDir = new File(context.getCacheDir(), cacheFolderName);
		if (!cacheDir.exists()) {
			cacheDir.mkdir();
		}
		return cacheDir;
	}
	@Override
	protected void onPreExecute () {
		Log.d(TAG, "Begin to download "+source+" to " + preferenceKey);
	}
	@Override
	protected File doInBackground(Void... params) {
		if (context == null) 
			return null;
		final File localFile = new File(getFileCacheDir(), "temp");
		OutputStream os = null;
		InputStream is = null;
		boolean success = false;
		try {
			if (source.getHost() != null) {
				Reachability.resolveAddressByName(source.getHost(), 2000);
			}
			os = new FileOutputStream(localFile);
			URLConnection con = source.openConnection();
			con.setConnectTimeout(connectTimeout);
			con.setReadTimeout(downloadTimeout);
			is = con.getInputStream();
			com.actionsmicro.utils.Utils.dump(is, os);
			success = true;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (os != null) {
					os.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (success) {
			File newFile = new File(getFileCacheDir(), preferenceKey);
			localFile.renameTo(newFile);
			return newFile;
		} else {
			return null;
		}
	}
	@Override
	protected void onPostExecute (File result) {
		if (result != null && context != null) {
			Log.d(TAG, "download "+source+" completed and save to " + result.getAbsolutePath() + " with preference name:"+preferenceName+" key:"+preferenceKey);
			SharedPreferences preferences = context.getSharedPreferences(preferenceName, 0);
			Editor editor = preferences.edit();
			editor.putString(preferenceKey, result.getAbsolutePath());
			editor.commit();
		} else {
			Log.d(TAG, "download "+source+" falied");
		}
		downloadingTasks.remove(source);
	}
	public void setDownloadTimeout(int timeout) {
		this.downloadTimeout = timeout;
	}
	public void setConnectTimeout(int timeout) {
		this.connectTimeout = timeout;
	}
}