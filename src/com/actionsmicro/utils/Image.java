package com.actionsmicro.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;

import com.actionsmicro.R;

public class Image {
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

	public static Bitmap readBitmapInHalfSize(Uri selectedImage, Context context) { 
		Bitmap bitmap = null; 
		BitmapFactory.Options options = new BitmapFactory.Options(); 
		options.inSampleSize = 2; 
		AssetFileDescriptor fileDescriptor =null; 
		try { 
			fileDescriptor = context.getContentResolver().openAssetFileDescriptor(selectedImage,"r"); 
		} catch (FileNotFoundException e) { 
			e.printStackTrace(); 
		} 
		finally{ 
			try { 
				bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor.getFileDescriptor(), null, options); 
				fileDescriptor.close(); 
			} catch (IOException e) { 
				e.printStackTrace(); 
			} 
		} 
		return bitmap; 
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
		final File outputFile = new File(picturesFolder, Image.getImageFileNamer(context).generateName(dateTaken)+".jpg");
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

	private static ImageFileNamer sImageFileNamer;

	private static ImageFileNamer getImageFileNamer(Context context) {
		if (sImageFileNamer == null) {
			sImageFileNamer = new ImageFileNamer(context.getString(R.string.image_file_name_format));
		}
		return sImageFileNamer;
	} 
}
