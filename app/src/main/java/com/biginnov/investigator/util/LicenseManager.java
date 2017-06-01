package com.biginnov.investigator.util;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.neurotec.licensing.NLicense;
import com.neurotec.licensing.gui.LicensingPreferencesFragment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;


public final class LicenseManager {
    private static final String TAG = LicenseManager.class.getSimpleName();

    public interface LicensingStateCallback {
        void onLicensingStateChanged(int state);
    }

    public static final String LICENSE_MEDIA = "Media";

    public static final String LICENSE_DEVICES_CAMERAS = "Devices.Cameras";

    public static final String LICENSE_FACE_DETECTION = "Biometrics.FaceDetection";
    public static final String LICENSE_FACE_EXTRACTION = "Biometrics.FaceExtraction";
    public static final String LICENSE_FACE_MATCHING = "Biometrics.FaceMatching";
    public static final String LICENSE_FACE_MATCHING_FAST = "Biometrics.FaceMatchingFast";
    public static final String LICENSE_FACE_SEGMENTATION = "Biometrics.FaceSegmentation";
    public static final String LICENSE_FACE_STANDARDS = "Biometrics.Standards.Faces";
    public static final String LICENSE_FACE_SEGMENTS_DETECTION = "Biometrics.FaceSegmentsDetection";

    public static final String LICENSE_FINGER_DETECTION = "Biometrics.FingerDetection";
    public static final String LICENSE_FINGER_EXTRACTION = "Biometrics.FingerExtraction";
    public static final String LICENSE_FINGER_MATCHING = "Biometrics.FingerMatching";
    public static final String LICENSE_FINGER_MATCHING_FAST = "Biometrics.FingerMatchingFast";
    public static final String LICENSE_FINGER_STANDARDS_FINGERS = "Biometrics.Standards.Fingers";
    public static final String LICENSE_FINGER_STANDARDS_FINGER_TEMPLATES = "Biometrics.Standards.FingerTemplates";
    public static final String LICENSE_FINGER_DEVICES_SCANNERS = "Devices.FingerScanners";
    public static final String LICENSE_FINGER_WSQ = "Images.WSQ";
    public static final String LICENSE_FINGER_NFIQ = "Biometrics.FingerQualityAssessmentBase";
    public static final String LICENSE_FINGER_CLASSIFICATION = "Biometrics.FingerSegmentsDetection";
    public static final String LICENSE_FINGER_SEGMENTATION = "Biometrics.FingerSegmentation";

    public static final String LICENSING_PREFERENCES = "com.neurotec.samples.licensing.preference.LicensingPreferences";
    public static final String LICENSING_SERVICE = "com.neurotec.samples.licensing.LicensingService";

    public static final int LICENSING_STATE_NOT_OBTAINED = 0;
    public static final int LICENSING_STATE_OBTAINING = 1;
    public static final int LICENSING_STATE_OBTAINED = 2;

    private static LicenseManager sInstance;

    public static synchronized LicenseManager getInstance() {
        if (sInstance == null) {
            sInstance = new LicenseManager();
        }
        return sInstance;
    }

    public static boolean isActivated(String license) {
        if (TextUtils.isEmpty(license))
            throw new NullPointerException("license");
        try {
            return NLicense.isComponentActivated(license);
        } catch (IOException e) {
            LogUtils.e(TAG, e.getMessage(), e);
            return false;
        }
    }

    public static void release() {
        if (sInstance != null && !sInstance.mComponents.isEmpty()) {
            List<String> components = new ArrayList<>(sInstance.mComponents);
            sInstance.release(components);
        }
    }

    private HashSet<String> mComponents;

    private LicenseManager() {
        mComponents = new HashSet<>();
    }

    public boolean obtain(Context context, List<String> components) {
        return obtain(components, LicensingPreferencesFragment.getServerAddress(context),
                LicensingPreferencesFragment.getServerPort(context));
    }

    public boolean obtain(List<String> components, String address, int port) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("Empty components");
        }
        LogUtils.critical(TAG, "Obtaining licenses from server ", address, ":", port);

        boolean result = true;
        for (String component : components) {
            boolean available = false;
            if (mComponents.contains(component)) {
                available = true;
            } else {
                try {
                    available = NLicense.obtainComponents(address, port, component);
                    if (available) {
                        mComponents.add(component);
                    }
                    LogUtils.critical(TAG, "Obtaining ", component, " license ",
                            available ? "succeeded" : "failed");
                } catch (IOException e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
            result &= available;
        }
        return result;
    }

    public void obtainAsync(Context context, List<String> components,
                            LicensingStateCallback callback) {
        if (context == null)
            throw new NullPointerException("context");
        obtainAsync(components, LicensingPreferencesFragment.getServerAddress(context),
                LicensingPreferencesFragment.getServerPort(context), callback);
    }

    public void obtainAsync(final List<String> components,
                            final String address, final int port, final LicensingStateCallback callback) {
        new AsyncTask<Object, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (callback != null) {
                    callback.onLicensingStateChanged(LICENSING_STATE_OBTAINING);
                }
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                try {
                    return obtain(components, address, port);
                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (callback != null) {
                    callback.onLicensingStateChanged(result ?
                            LICENSING_STATE_OBTAINED : LICENSING_STATE_NOT_OBTAINED);
                }
            }
        }.execute();
    }

    public boolean reobtain(Context context) throws IOException {
        List<String> reobtainedComponents = new ArrayList<String>(mComponents);
        release(reobtainedComponents);
        return obtain(context, reobtainedComponents);
    }

    public void reobtainAsync(final Context context, final LicensingStateCallback callback)
            throws IOException {

        new AsyncTask<Object, Boolean, Boolean>() {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                if (callback != null) {
                    callback.onLicensingStateChanged(LICENSING_STATE_OBTAINING);
                }
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                try {
                    return reobtain(context);
                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean result) {
                super.onPostExecute(result);
                if (callback != null) {
                    callback.onLicensingStateChanged(result ?
                            LICENSING_STATE_OBTAINED : LICENSING_STATE_NOT_OBTAINED);
                }
            }
        }.execute();
    }

    public void release(List<String> components) {
        if (components != null && !components.isEmpty()) {
            try {
                LogUtils.d(TAG, "Releasing licenses: ", components);
                NLicense.releaseComponents(components.toString()
                        .replace("[", "").replace("]", "").replace(" ", ""));
                mComponents.removeAll(components);
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
    }
}
