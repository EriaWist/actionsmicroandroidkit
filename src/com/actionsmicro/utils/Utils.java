package com.actionsmicro.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import com.actionsmicro.R;

public class Utils {
	private static class ImageFileNamer {
        private SimpleDateFormat mFormat;

        // The date (in milliseconds) used to generate the last name.
        private long mLastDate;

        // Number of names generated for the same second.
        private int mSameSecondCount;

        public ImageFileNamer(String format) {
            mFormat = new SimpleDateFormat(format, Locale.US);
        }

        public String generateName(long dateTaken) {
            Date date = new Date(dateTaken);
            String result = mFormat.format(date);

            // If the last name was generated for the same second,
            // we append _1, _2, etc to the name.
            if (dateTaken / 1000 == mLastDate / 1000) {
                mSameSecondCount++;
                result += "_" + mSameSecondCount;
            } else {
                mLastDate = dateTaken;
                mSameSecondCount = 0;
            }

            return result;
        }
    }
	private static ImageFileNamer sImageFileNamer;
	private static ImageFileNamer getImageFileNamer(Context context) {
		if (sImageFileNamer == null) {
			sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
		}
		return sImageFileNamer;
	}
	/**
	 * Save bitmap to Pictures folder and add it to system media store.
	 * @param context The context that provides the application resource.
	 * @param bitmap The bitmap contains the image to be saved.
	 */
	public static final void saveImageToUniqueJpegFileInPicturesFolder(final Context context, final Bitmap bitmap) throws FileNotFoundException{
		final File picturesFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
		final long dateTaken = new Date().getTime();
		if (!picturesFolder.exists()) {
			picturesFolder.mkdir();
		}
		final File outputFile = new File(picturesFolder, getImageFileNamer(context).generateName(dateTaken)+".jpg");
		final FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
		bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
		final ContentValues contentValues = new ContentValues();
		//			contentValues.put(Images.Media.TITLE, title);
		//			contentValues.put(Images.Media.DISPLAY_NAME, displayName);
		//			contentValues.put(Images.Media.DESCRIPTION, description);

		contentValues.put(Images.Media.DATE_ADDED, dateTaken);
		contentValues.put(Images.Media.DATE_TAKEN, dateTaken);
		contentValues.put(Images.Media.DATE_MODIFIED, dateTaken) ;
		contentValues.put(Images.Media.MIME_TYPE, "image/jpeg");
		//			contentValues.put(Images.Media.ORIENTATION, orientation);

		final String path = picturesFolder.toString().toLowerCase(Locale.US) ;
		final String name = picturesFolder.getName().toLowerCase(Locale.US) ;
		contentValues.put(Images.ImageColumns.BUCKET_ID, path.hashCode());
		contentValues.put(Images.ImageColumns.BUCKET_DISPLAY_NAME, name);
		contentValues.put(Images.Media.SIZE, outputFile.length()) ;
		contentValues.put("_data", outputFile.getAbsolutePath()) ;
		ContentResolver c = context.getContentResolver() ;
		c.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);			
	}
	/**
	 * Get the file path of the media referred by given URI if there's any.
	 * @param activity Current activity.
	 * @param contentUri The URI to the media stored in system media store. 
	 * @return Return the full path of given media. 
	 */
	public static String getRealPathFromURI(Activity activity, Uri contentUri) {
		if (contentUri.getScheme().equals("file")) {
			return contentUri.getPath();
		}
        String[] proj = { MediaStore.Images.Media.DATA };
        Cursor cursor = activity.managedQuery(contentUri, proj, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
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
    	final String[] segments = path.split("/");
    	if (segments != null && segments.length > 0) {
    		return segments[segments.length - 1];
    	}
    	return path;
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
}
