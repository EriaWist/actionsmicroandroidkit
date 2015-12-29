package com.actionsmicro.utils;

import android.util.Base64;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtil {
    private static final String TAG = "CipherUtil";
    public static final String ALGORITHM_AES_CBC = "AES/CBC/PKCS5Padding";
    private static final byte[] IV_BYTES = {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

    public static String DecryptAES(String content, String key, byte[] iv) {
        byte[] textByte;
        try {
            textByte = DecryptAESCBC(key.getBytes("UTF-8"), content.getBytes("UTF-8"), iv, true);
            return new String(textByte);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] DecryptAESCBC(byte[] key, byte[] text, byte[] iv, boolean isBase64Encoded) {
        try {
            byte[] encryted_bytes;
            if (isBase64Encoded) {
                encryted_bytes = Base64.decode(text, Base64.DEFAULT);
            } else {
                encryted_bytes = text;
            }
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_CBC);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(encryted_bytes);
        } catch (Exception ex) {
            StringWriter stringWriter = new StringWriter();
            ex.printStackTrace(new PrintWriter(stringWriter));
            String errorMsg = ex.getLocalizedMessage() + "\n" + stringWriter.toString();
            Log.d(TAG, errorMsg);
            return null;
        }
    }

    public static byte[] EncryptAESCBC(byte[] key, byte[] text, byte[] iv) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(iv);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_CBC);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(text);
        } catch (Exception ex) {
            return null;
        }
    }
}
