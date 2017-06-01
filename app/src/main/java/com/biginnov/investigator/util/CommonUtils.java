package com.biginnov.investigator.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.neurotec.images.NImage;
import com.neurotec.images.NImageRotateFlipType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CommonUtils {
    private static final String TAG = CommonUtils.class.getSimpleName();

    public static void executeAsyncTask(AsyncTask task) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            task.execute();
        } else {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    public static void executeAsyncTaskSerially(AsyncTask task) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            task.execute();
        } else {
            task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
        }
    }

    public static boolean isNetworkConnected(Context context) {
        boolean connected = false;
        if (context != null) {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getActiveNetworkInfo();
            connected = info != null && info.isConnected();
        }
        return connected;
    }

    public static void openWifiSetting(Context context) {
        if (context != null) {
            Intent wifiSettingIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            wifiSettingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(wifiSettingIntent);
        }
    }

    public static String combinePath(String... folders) {
        String path = "";
        for (String folder : folders) {
            path = path.concat(File.separator).concat(folder);
        }
        return path;
    }

    public static void showToast(final Activity activity, final String message) {
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    public static void showToastOnUiThread(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void showToastOnUiThread(final Fragment fragment, final String message) {
        final Activity activity = fragment.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }

            });
        }
    }

    public static boolean writePngFile(Bitmap image, File target) {
        if (image == null) return false;
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(target);
            image.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            fos.getFD().sync();
            fos.close();
            fos = null;
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage(), e);
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                    fos = null;
                }
            } catch (IOException e) {
                LogUtils.w(TAG, e.getMessage(), e);
            }
        }
        return true;
    }

    public static NImage cropFaceImage(Context context, NImage raw, int cameraOrientation) {
        int rawWidth = raw.getWidth();
        int rawHeight = raw.getHeight();
        int rawSize = rawWidth;
//        boolean rotate = false;
        if (rawWidth > rawHeight) {
            rawSize = rawHeight;
//            rotate = true;
        }
        Resources res = context.getResources();
        int cameraViewWidth = res.getDimensionPixelSize(R.dimen.camera_view_width);
        double cameraMaskMargin = res.getDimensionPixelSize(R.dimen.camera_mask_margin);
        double maskPercentage = cameraMaskMargin / cameraViewWidth;
        int maskSize = (int) (rawSize * maskPercentage);
        int targetSize = rawSize - maskSize * 2;
//        if (rotate) {
        if (cameraOrientation > 0) {
            NImageRotateFlipType type;
            if (cameraOrientation <= 90) {
                type = NImageRotateFlipType.ROTATE_90_FLIP_NONE;
            } else if (cameraOrientation <= 180) {
                type = NImageRotateFlipType.ROTATE_180_FLIP_NONE;
            } else {
                type = NImageRotateFlipType.ROTATE_270_FLIP_NONE;
            }
            return NImage.fromImage(raw.getPixelFormat(), raw.getStride(),
                    raw, maskSize, maskSize, targetSize, targetSize).rotateFlip(type);
        } else {
            return NImage.fromImage(raw.getPixelFormat(), raw.getStride(),
                    raw, maskSize, maskSize, targetSize, targetSize);
        }
    }
}
