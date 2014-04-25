package com.actionsmicro.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.TypedValue;


public class Utils {
	/**
	 * Get the file path of the media referred by given URI if there's any.
	 * @param activity Current activity.
	 * @param contentUri The URI to the media stored in system media store. 
	 * @return Return the full path of given media. 
	 */
	public static String getRealPathFromURI(Activity activity, Uri contentUri) {
		
		return getPath(activity,contentUri);
		
//		if (contentUri.getScheme().equals("file")) {
//			return contentUri.getPath();
//		}
//        String[] proj = { MediaStore.Images.Media.DATA };
//        try {
//        	Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
//        	int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
//        	cursor.moveToFirst();
//        	return cursor.getString(column_index);
//        } catch(IllegalArgumentException e) {
//        	e.printStackTrace();
//        }
//
//        return null;
    }
	private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];
    /**
     * Creates a formated time string.
     * @param durationformat Specify the format.
     * @param secs Time in seconds.
     * @return A formated time string.
     */
    public static String makeTimeString(String durationformat, long secs) {
        sFormatBuilder.setLength(0);
    	boolean isNegative = false;
    	if (secs < 0) {
    		secs = -secs;
    		isNegative = true;
    	}
        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;
        if (isNegative) {
        	return "-" + sFormatter.format(durationformat, timeArgs).toString();        	
        } else {
        	return sFormatter.format(durationformat, timeArgs).toString();
        }
    }
    /**
     * Get latest path component of given path. For example, by given '/test/folder/filename.ext', this method returns 'filename.ext'.
     * @param path The path.
     * @return Return last path component.
     */
    public static String getLastPathComponent(String path) {
    	if (path != null) {
    		final String[] segments = path.split("/");
    		if (segments != null && segments.length > 0) {
    			return segments[segments.length - 1];
    		}
    	}
    	return path;
    }
    public static String getFileExtension(String path) {
    	final String lastPathComponent = getLastPathComponent(path);
    	if (lastPathComponent.contains(".") && !lastPathComponent.endsWith(".")) {
    		return lastPathComponent.substring(lastPathComponent.lastIndexOf(".")+1);
    	} else {
    		return "";
    	}
    }
    /**
     * Convert data from given input stream into string
     * @param inputStream The input stream contains string data. 
     * @return The string converted from given input stream.
     * @throws IOException
     */
    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
    	InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
    	StringWriter stringWriter = new StringWriter();
    	char buffer[] = new char[512];
    	int sizeRead;
    	while ((sizeRead = inputStreamReader.read(buffer, 0, 512)) != -1) {
    		stringWriter.write(buffer, 0, sizeRead);
    	}
    	return  stringWriter.toString();
    }
    
    public static void deleteRecursively(File fileOrDirectory) {
    	if (fileOrDirectory.isDirectory()) {
    		for (File child : fileOrDirectory.listFiles()) {
    			deleteRecursively(child);
    		}
    	}
    	fileOrDirectory.delete();
    }
    
    public static String concatStringsWithSeparator(List<String> strings, String separator) {
        StringBuilder sb = new StringBuilder();
        String sep = "";
        for(String s: strings) {
            sb.append(sep).append(s);
            sep = separator;
        }
        return sb.toString();                           
    }
    public static String md5(String inputString) {
    	MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("MD5");
            byte utf8_bytes[] = inputString.getBytes();
            digest.update(utf8_bytes, 0, utf8_bytes.length);
            String hash = String.format("%032x", new BigInteger(1, digest.digest()));
            return hash;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }
    public static void popAllBackStackAndFinish(Activity activity) {
    	if (activity != null) {
    		final FragmentManager fragmentManager = activity.getFragmentManager();
    		if (fragmentManager != null) {
    			while (fragmentManager.popBackStackImmediate()) {
    				
    			}
    		}
    		activity.finish();    		
    	}
    }
    public static void dump(InputStream is, OutputStream os) throws IOException {
		byte buffer[] = new byte[4096];
		int rc = is.read(buffer, 0, buffer.length);
		while (rc > 0) {
		    os.write(buffer, 0, rc);
		    rc = is.read(buffer, 0, buffer.length);
		}
		os.flush();
	}
    public static boolean isActionBarSplitted(){
		try {
			Resources res = Resources.getSystem();
			return res.getBoolean(res.getIdentifier("split_action_bar_is_narrow", "bool", "android"));
		} catch (Resources.NotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}
	public static int getBottomMargin(Activity activity){
		if (!activity.getActionBar().isShowing()) {
			return 0;
		} else {
			TypedValue tv = new TypedValue();
			if (activity.getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
				return TypedValue.complexToDimensionPixelSize(tv.data,activity.getResources().getDisplayMetrics());
			}   			
		}
		return 0;
	}
	public static void executeOnThreadAndWait(Runnable runnable) {
		Thread thread = new Thread(runnable);
		thread.start();
		try {
			thread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     * 
     * @param context The context.
     * @param uri The Uri to query.
     * @return file path
     */
    private static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    
    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    
    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }	
	
}
