package com.actionsmicro.utils;

import android.util.Base64;

import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class CipherUtil {
    public static final String ALGORITHM_AES_ECB = "AES/ECB/PKCS5Padding";
    public static final String ALGORITHM_AES_CBC = "AES/CBC/PKCS5Padding";
    private static final String TAG = "CipherUtil";
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

    public static byte[] EncryptAES(byte[] key, byte[] text) {
        return EncryptAES(key, text, ALGORITHM_AES_ECB);
    }

    public static byte[] EncryptAES(byte[] key, byte[] text, String algorithm) {
        if (algorithm.equals(ALGORITHM_AES_ECB)) {
            return EncryptAESECB(key, text);
        } else if (algorithm.equals(ALGORITHM_AES_CBC)) {
            return EncryptAESCBC(key, text);
        } else {
            return null;
        }
    }

    private static byte[] EncryptAESECB(byte[] key, byte[] text) {
        try {
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_ECB);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            return cipher.doFinal(text);
        } catch (Exception ex) {
            return null;
        }
    }

    private static byte[] EncryptAESCBC(byte[] key, byte[] text) {
        try {
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_CBC);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(text);
        } catch (Exception ex) {
            return null;
        }
    }

    public static byte[] DecryptAES(byte[] key, byte[] text) {
        return DecryptAES(key, text, ALGORITHM_AES_ECB);
    }

    public static byte[] DecryptAES(byte[] key, byte[] text, String algorithm) {
        if (algorithm.equals(ALGORITHM_AES_ECB)) {
            return DecryptAESECB(key, text);
        } else if (algorithm.equals(ALGORITHM_AES_CBC)) {
            return DecryptAESCBC(key, text);
        } else {
            return null;
        }
    }

    private static byte[] DecryptAESECB(byte[] key, byte[] text) {
        try {
            byte[] encryted_bytes = Base64.decode(text, Base64.DEFAULT);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_ECB);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            return cipher.doFinal(encryted_bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    private static byte[] DecryptAESCBC(byte[] key, byte[] text) {
        try {
            byte[] encryted_bytes = Base64.decode(text, Base64.DEFAULT);
            AlgorithmParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);
            SecretKeySpec skeySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance(ALGORITHM_AES_CBC);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, ivSpec);
            return cipher.doFinal(encryted_bytes);
        } catch (Exception ex) {
            return null;
        }
    }

    public static String EncryptAES(String content, String key, String algorithm) {
        byte[] textByte;
        try {
            textByte = EncryptAES(key.getBytes("UTF-8"), content.getBytes("UTF-8"), algorithm);
            return Base64.encodeToString(textByte, Base64.NO_WRAP);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String EncryptAES(String content, String key) {
        byte[] textByte;
        try {
            textByte = EncryptAES(key.getBytes("UTF-8"), content.getBytes("UTF-8"));
            return Base64.encodeToString(textByte, Base64.DEFAULT);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String EncryptAES(JSONObject jsonObject, String key) {
        if (null != jsonObject) {
            return EncryptAES(jsonObject.toString(), key);
        }
        return null;
    }

    public static String DecryptAES(String content, String key, String algorithm) {
        byte[] textByte;
        try {
            textByte = DecryptAES(key.getBytes("UTF-8"), content.getBytes("UTF-8"), algorithm);
            return new String(textByte);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String DecryptAES(String content, String key) {
        return DecryptAES(content, key, ALGORITHM_AES_ECB);
    }
}
