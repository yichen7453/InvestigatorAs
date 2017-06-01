package com.biginnov.investigator.util;


import android.content.Context;
import android.net.Uri;

import com.facebook.drawee.backends.pipeline.Fresco;

import java.io.File;

public class StorageUtils {
    private static final String TAG = StorageUtils.class.getSimpleName();
    private static final String FACE_DIR = "face";
    private static final String FINGERPRINT_DIR = "fingerprint";
    private static final String TEMP_IMAGE_FILE_NAME = "temp";
    private static final String IMAGE_FILE_EXTENSION = ".png";
    private static final String IMAGE_FILE_SEPARATOR = "_";

    private static String sFaceDirPath;
    private static String sFingerprintDirPath;

    /**
     * Must be called on application start
     *
     * @param context Application context
     */
    public static void ensureBiometricDir(Context context) {
        sFaceDirPath = getFaceDirPath(context);
        File dir = new File(sFaceDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        sFingerprintDirPath = getFingerprintDirPath(context);
        dir = new File(sFingerprintDirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    private static String getFaceDirPath(Context context) {
        return context.getFilesDir().getAbsolutePath() +
                File.separator + FACE_DIR + File.separator;
    }

    private static String getFingerprintDirPath(Context context) {
        return context.getFilesDir().getAbsolutePath() +
                File.separator + FINGERPRINT_DIR + File.separator;
    }

    private static String getImageFileName(String name) {
        return name + IMAGE_FILE_EXTENSION;
    }

    private static String getImageFileName(String name, int index) {
        return name + IMAGE_FILE_SEPARATOR + index + IMAGE_FILE_EXTENSION;
    }

    public static File getFaceImageFile(String uuid) {
        return new File(getFaceImageFilePath(uuid));
    }

    public static File getFingerprintImageFile(String uuid, int index) {
        return new File(getFingerprintImageFilePath(uuid, index));
    }

    public static String getFaceImageFilePath(String uuid) {
        return sFaceDirPath + getImageFileName(uuid);
    }

    public static String getFingerprintImageFilePath(String uuid, int index) {
        return sFingerprintDirPath + getImageFileName(uuid, index);
    }

    public static File getTempFaceImageFile() {
        return new File(getTempFaceImageFilePath());
    }

    public static File getTempFingerprintImageFile(int index) {
        return new File(getTempFingerprintImageFilePath(index));
    }

    public static String getTempFaceImageFilePath() {
        return sFaceDirPath + getImageFileName(TEMP_IMAGE_FILE_NAME);
    }

    public static String getTempFingerprintImageFilePath(int index) {
        return sFingerprintDirPath + getImageFileName(TEMP_IMAGE_FILE_NAME, index);
    }

    public static boolean saveTempFaceImage(String uuid) {
        boolean result = false;
        File tempFile = getTempFaceImageFile();
        File targetFile = getFaceImageFile(uuid);
        if (tempFile.exists()) {
            result = tempFile.renameTo(targetFile);
        }
        return result;
    }

    public static boolean saveTempFingerprintImage(String uuid, int index) {
        boolean result = false;
        File tempFile = getTempFingerprintImageFile(index);
        File targetFile = getFingerprintImageFile(uuid, index);
        if (tempFile.exists()) {
            result = tempFile.renameTo(targetFile);
        }
        return result;
    }

    public static void deleteFaceImageFile(String uuid) {
        File targetFile = getFaceImageFile(uuid);
        if (targetFile.exists()) {
            targetFile.delete();
        }
    }

    public static void deleteFingerprintImageFile(String uuid, int index) {
        File targetFile = getFingerprintImageFile(uuid, index);
        if (targetFile.exists()) {
            targetFile.delete();
        }
    }

    public static void deleteTempFingerprintImageFile(int index, int fingerprintCount) {
        LogUtils.d(TAG, "Delete image: ", index);
        File targetFile = getTempFingerprintImageFile(index);
        if (targetFile.exists()) {
            targetFile.delete();
        }
        if (index == fingerprintCount - 1) { // The last
            Fresco.getImagePipeline().evictFromCache(Uri.fromFile(targetFile));
        } else {
            for (int i = index + 1; i < fingerprintCount; i++) {
                File tempFile = getTempFingerprintImageFile(i);
                targetFile = getTempFingerprintImageFile(i - 1);
                if (tempFile.exists()) {
                    tempFile.renameTo(targetFile);
                    LogUtils.d(TAG, "Move image ", i, " to ", i - 1);
                } else {
                    LogUtils.w(TAG, "Why the fingerprint file is removed?");
                }
                Fresco.getImagePipeline().evictFromCache(Uri.fromFile(targetFile));
            }
        }

    }
}