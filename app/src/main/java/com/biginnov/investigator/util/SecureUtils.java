package com.biginnov.investigator.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;

import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SecureUtils {
    private static final String TAG = SecureUtils.class.getSimpleName();

    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] IV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}; // TODO: Random generate

    private static SecretKey getSecretKey(Context context) throws NoSuchAlgorithmException {
        SecretKey secretKey;
        String stringKey = PreferenceUtils.getSecretKey(context);
        if (TextUtils.isEmpty(stringKey)) {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            keyGen.init(256);
            secretKey = keyGen.generateKey();
            stringKey = Base64.encodeToString(secretKey.getEncoded(), Base64.DEFAULT);
            PreferenceUtils.setSecretKey(context, stringKey);
        } else {
            byte[] encodedKey = Base64.decode(stringKey, Base64.DEFAULT);
            secretKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
        }
        return secretKey;
    }

    public static String encrypt(Context context, String original) {
        String result = null;
        if (!TextUtils.isEmpty(original)) {
            try {
                // Get key and initial vector
                SecretKey secretKey = getSecretKey(context);
                AlgorithmParameterSpec ivSpec = new IvParameterSpec(IV);
                // Init cipher
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
                // Encrypt
                byte[] encrypted = cipher.doFinal(original.getBytes());
                result = Base64.encodeToString(encrypted, Base64.DEFAULT);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
        return result;
    }

    public static String decrypt(Context context, String encrypted) {
        String result = null;
        if (!TextUtils.isEmpty(encrypted)) {
            try {
                // Get key and initial vector
                SecretKey secretKey = getSecretKey(context);
                AlgorithmParameterSpec ivSpec = new IvParameterSpec(IV);
                // Init cipher
                Cipher cipher = Cipher.getInstance(ALGORITHM);
                cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec);
                // Decrypt
                byte[] original = cipher.doFinal(Base64.decode(encrypted, Base64.DEFAULT));
                result = new String(original);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
        return result;
    }
}
