package com.actionsmicro.analytics.tracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStore.Entry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.spec.KeySpec;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.actionsmicro.BuildConfig;
import com.actionsmicro.analytics.AppInfo;
import com.actionsmicro.analytics.Tracker;
import com.actionsmicro.analytics.Usage;
import com.actionsmicro.analytics.tracker.uploader.ActionsUploader;
import com.actionsmicro.analytics.tracker.uploader.CompoundUploader;
import com.actionsmicro.analytics.tracker.uploader.LogUploader;
import com.actionsmicro.analytics.tracker.uploader.Uploader;
import com.actionsmicro.utils.Log;
import com.google.gson.Gson;

public class ActionsTracker implements Tracker {
	private static final int RETRY_DELAY = 60;
	private static final int DEFAULT_UPLOAD_DELAY = 60;
	private static final String TAG = "ActionsTracker";
	private final Uploader uploader;
	private final Gson gson = new Gson();
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1);
	private Context context;
	private String appKey;
	private SecretKey secretKey;
	private static SecretKey generateKey(String seed) throws Exception {
//        KeyGenerator kgen = KeyGenerator.getInstance("AES");
//        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
//        sr.setSeed(seed);
//        kgen.init(128, sr); // 192 and 256 bits may not be available
//        return kgen.generateKey();
		
		int iterationCount = 1000;
		int keyLength = 128;
		int saltLength = keyLength / 8; // same size as key output

		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[saltLength];
		random.nextBytes(salt);
		KeySpec keySpec = new PBEKeySpec(seed.toCharArray(), salt,
		                    iterationCount, keyLength);
		SecretKeyFactory keyFactory = SecretKeyFactory
		                    .getInstance("PBKDF2WithHmacSHA1");
		return keyFactory.generateSecret(keySpec);
    }
	public ActionsTracker(Context context, String appKey, String appSecret) {
		this.context = context;
		this.appKey = appKey;
		
		setupCipher(appKey, appSecret);

		CompoundUploader compoundUploader = new CompoundUploader();
		compoundUploader.add(new ActionsUploader(appKey, appSecret));
		if (BuildConfig.DEBUG) {
			compoundUploader.add(new LogUploader());
		}
		uploader = compoundUploader;
		androidDefaultUEH = Thread.getDefaultUncaughtExceptionHandler();
		Thread.setDefaultUncaughtExceptionHandler(uncaughtExceptionHandler);
		
		checkPendingLogsAndScheduleUploadIfNeeded();
	}
	private void setupCipher(String appKey, String appSecret) {		
		try {
			KeyStore ks = getKeyStore(appKey, appSecret);
			try {
				Entry keyEntry = ks.getEntry(appKey, null);
				if (keyEntry != null) {
					secretKey = ((KeyStore.SecretKeyEntry) keyEntry).getSecretKey();
				}
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnrecoverableEntryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				
			}
			if (secretKey == null) {
				FileOutputStream keystoreOutputStream = null;
				try {
					secretKey = generateKey(appSecret);
					ks.setEntry(appKey, new KeyStore.SecretKeyEntry(secretKey), null);
					keystoreOutputStream = new FileOutputStream(getKeyStoreFile(appKey));
					ks.store(keystoreOutputStream, appSecret.toCharArray());
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					if (keystoreOutputStream != null) {
						keystoreOutputStream.close();
					}
				}
			}
		} catch (KeyStoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (CertificateException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	private KeyStore getKeyStore(String appKey, String appSecret) throws KeyStoreException, IOException,
			NoSuchAlgorithmException, CertificateException {
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		File keystoreFile = getKeyStoreFile(appKey);
		if (keystoreFile.exists()) {
			ks.load(new FileInputStream(keystoreFile), appSecret.toCharArray());
		} else {
			ks.load(null);
		}
		return ks;
	}
	private File getKeyStoreFile(String appKey) {
		File dataFolder = context.getDir("actions", Context.MODE_PRIVATE);
		File keystoreFile = new File(dataFolder, appKey);
		return keystoreFile;
	}

	private void checkPendingLogsAndScheduleUploadIfNeeded() {
		File dataFolder = context.getDir(appKey, Context.MODE_PRIVATE);
		File[] pendingLogFiles = dataFolder.listFiles();
		if (pendingLogFiles != null && pendingLogFiles.length > 0) {
			scheduleUploadIfNoOneScheduled(DEFAULT_UPLOAD_DELAY);
		}
	}
	private Thread.UncaughtExceptionHandler androidDefaultUEH;
	private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = new Thread.UncaughtExceptionHandler() {
		public void uncaughtException(Thread thread, Throwable ex) {
			Log.d(TAG, "uncaughtException:"+ex);
			executor.shutdown();
			try {
				executor.awaitTermination(3, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			Thread.setDefaultUncaughtExceptionHandler(androidDefaultUEH);
			androidDefaultUEH.uncaughtException(thread, ex);
		}
	};
	private class SaveLog implements Runnable {
		private String jsonString;
		int delay = 5;
		public SaveLog(String jsonString, int uploadDelay) {
			this.jsonString = jsonString;
			delay = uploadDelay;
		}
		@Override
		public void run() {
			synchronized (uploadLog) {
				Log.d(TAG, "saveLogToFile:\n"+jsonString);
				saveLogToFile(UUID.randomUUID().toString(), jsonString);
			}
			
			scheduleUploadIfNoOneScheduled(delay);
		}
		
	};
	private static final String CIPHER_ALGORITHM = "AES";
	private ScheduledFuture<?> scheduledUpload;
	private UploadLog uploadLog = new UploadLog();
	private class UploadLog implements Runnable {


		@Override
		public void run() {
			synchronized (uploadLog) {
				if (hasPendingLogToUpload()) {
					JSONArray jsonArray = consolidatePendingLogsFromFiles(pendingLogFiles());
					if (jsonArray.length() > 0) {
						synchronized (executor) {
							scheduledUpload = null; // allow others to schedule another upload
						}
						batchUploadLogs(jsonArray);
					}
				}
			}			
		}
		private boolean hasPendingLogToUpload() {
			File[] pendingLogFiles = pendingLogFiles();
			return pendingLogFiles != null && pendingLogFiles.length > 0;
		}
		private File[] pendingLogFiles() {
			File dataFolder = context.getDir(appKey, Context.MODE_PRIVATE);
			File[] pendingLogFiles = dataFolder.listFiles();
			return pendingLogFiles;
		}
		private void batchUploadLogs(JSONArray jsonArray) {
			String logString = jsonArray.toString();
			// save it just in case app crash or terminated during uploading.
			final File tempFileForUploadingData = saveLogToFile(UUID.randomUUID().toString(), logString);
			uploader.uploadLog(logString,  new Uploader.RequestHandler() {
				
				@Override
				public void onSuccuess(String jsonBody) {
					Log.d(TAG, "Upload onSuccuess:\n"+jsonBody);
				}
				
				@Override
				public void onServerError(Exception e, String jsonBody) {
					Log.e(TAG, "Upload onServerError:\n"+e);
					// backup failed upload
					executor.execute(new SaveLog(jsonBody, RETRY_DELAY));
				}
				
				@Override
				public void onRequestError(String error, String jsonBody) {
					Log.e(TAG, "Upload onRequestError:"+error);
					// backup failed upload
					executor.execute(new SaveLog(jsonBody, RETRY_DELAY));
				}
				
				@Override
				public void onNetworkError(Exception e, String jsonBody) {
					Log.e(TAG, "Upload onNetworkError:\n"+e);
					scheduleUploadIfNoOneScheduled(60);
					// we don't need to, since we have saved a copy to tempFileForUploadingData.
				}
				
				@Override
				public void onInvalidJson(String errorMsg, String jsonBody) {
					Log.e(TAG, "Upload onInvalidJson:"+errorMsg);			
				}
				
				@Override
				public void onDependencyError(Exception e, String jsonBody) {
					// backup failed upload
					executor.execute(new SaveLog(jsonBody, RETRY_DELAY));
				}

				@Override
				public void onProcessBatchResultEnd(String jsonBody, JSONArray result) {
					tempFileForUploadingData.delete();
					synchronized (tempFileForUploadingData) {
						tempFileForUploadingData.notify();
					}
				}
			});		
			// wait until upload finished.
			synchronized (tempFileForUploadingData) {
				try {
					tempFileForUploadingData.wait(10000);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
			}
		}

		private JSONArray consolidatePendingLogsFromFiles(File[] pendingLogFiles) {
			JSONArray jsonArray = new JSONArray();
			for (File pendingLogFile : pendingLogFiles) {
				if (pendingLogFile.isFile()) {
					String jsonString = readFileAsString(pendingLogFile);
					Log.d(TAG, "readFileAsString: "+pendingLogFile.getName()+" :\n"+jsonString);

					JSONArray isArray = null;
					try { isArray = new JSONArray(jsonString); } catch (JSONException e1) {}
					if (isArray != null) {
						appendJSONArrayFromJSONArray(jsonArray, isArray);
					} else {
						try {
							jsonArray.put(new JSONObject(jsonString));
						} catch (JSONException e) {
							// corrupted file or so	
							Log.e(TAG, pendingLogFile.getName()+" is corrupted or so!");
						}
					}
					pendingLogFile.delete();
				}
			}
			return jsonArray;
		}

		private void appendJSONArrayFromJSONArray(JSONArray jsonArray,
				JSONArray isArray) {
			for (int i = 0; i < isArray.length(); i++) {
				try {
					jsonArray.put(isArray.get(i));
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			}
		}

		private String readFileAsString(File pendingLogFile) {
			StringBuilder sb = new StringBuilder();
			BufferedReader reader = null;
			try {
				InputStream in = new FileInputStream(pendingLogFile);
				if (secretKey != null) {
					try {
						Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
						cipher.init(Cipher.DECRYPT_MODE, secretKey);
						in = new CipherInputStream(in, cipher);
					} catch (NoSuchAlgorithmException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (NoSuchPaddingException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (InvalidKeyException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} finally {

					}
				}
				reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
			    String line = null;
			    while ((line = reader.readLine()) != null) {
			    	sb.append(line).append("\n");
			    }
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {reader.close();} catch (Exception ex) {}
			}
			return sb.toString();
		}
		
	}

	@Override
	public void log(Usage usage) {
		executor.execute(new SaveLog(gson.toJson(usage), DEFAULT_UPLOAD_DELAY));
	}
	
	@Override
	public void log(AppInfo appInfo) {
		executor.execute(new SaveLog(gson.toJson(appInfo), DEFAULT_UPLOAD_DELAY));
	}

	@Override
	public void log(Map<String, Object> map) {
		executor.execute(new SaveLog(gson.toJson(map), DEFAULT_UPLOAD_DELAY));
	}
	private void scheduleUploadIfNoOneScheduled(int delay) {
		synchronized (executor) {
			if (scheduledUpload == null) {
				Log.d(TAG, "Schedule an upload on "+delay+" seconds later.");
				scheduledUpload = executor.schedule(uploadLog, delay, TimeUnit.SECONDS);
			}
		}
	}
	private File saveLogToFile(String filename, String logData) {
		File logFile = new File(context.getDir(appKey, Context.MODE_PRIVATE), filename);
		OutputStream out = null;
		try {
			logFile.createNewFile();
			out = new FileOutputStream(logFile);
			if (secretKey != null) {
				try {
					Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
					cipher.init(Cipher.ENCRYPT_MODE, secretKey);
					out = new CipherOutputStream(out, cipher);
				} catch (NoSuchAlgorithmException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (NoSuchPaddingException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvalidKeyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {

				}
			}
			out.write(logData.getBytes());					
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {out.close();} catch (Exception ex) {}
		}
		return logFile;
	}
}
