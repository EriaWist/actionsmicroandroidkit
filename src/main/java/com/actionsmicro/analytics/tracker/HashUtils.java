package com.actionsmicro.analytics.tracker;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {

	private static String convertToHex(byte[] data) {
	    StringBuilder buf = new StringBuilder();
	    for (byte b : data) {
	        int halfbyte = (b >>> 4) & 0x0F;
	        int two_halfs = 0;
	        do {
	            buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte) : (char) ('a' + (halfbyte - 10)));
	            halfbyte = b & 0x0F;
	        } while (two_halfs++ < 1);
	    }
	    return buf.toString();
	}

	public static String SHA1(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
	    MessageDigest md = MessageDigest.getInstance("SHA-1");
	    md.update(text.getBytes("iso-8859-1"), 0, text.length());
	    byte[] sha1hash = md.digest();
	    return convertToHex(sha1hash);
	}
	public static String EzCastHash(String appSecret, long expire, String path, String packageId) throws NoSuchAlgorithmException, UnsupportedEncodingException {
		return HashUtils.SHA1(appSecret+"\t"+String.valueOf(expire)+"\t"+path+"\t"+packageId);
	}
}
