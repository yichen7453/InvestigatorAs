package com.biginnov.investigator.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferenceUtils {
    private static final String TAG = PreferenceUtils.class.getSimpleName();

    private static final String KEY_SECRET_KEY = "magic";

    private static SharedPreferences sPreferences;

    public static void setSecretKey(Context context, String key) {
        ensurePreferences(context);
        sPreferences.edit().putString(KEY_SECRET_KEY, key).apply();
    }

    public static String getSecretKey(Context context) {
        ensurePreferences(context);
        return sPreferences.getString(KEY_SECRET_KEY, null);
    }

    private static void ensurePreferences(Context context) {
        if (sPreferences == null) {
            sPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }
}
