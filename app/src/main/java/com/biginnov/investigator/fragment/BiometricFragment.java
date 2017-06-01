package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import com.biginnov.investigator.activity.BiometricActivity;
import com.biginnov.investigator.util.LogUtils;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;

import sensor.fpc.hst.fpcsensor.Fingerprint;

public abstract class BiometricFragment extends BaseFragment {
    private static final String TAG = BiometricFragment.class.getSimpleName();

    protected static final String EXTRA_REQUEST_ID = "request";
    protected static final String EXTRA_COMPLETE_AFTER_CAPTURE = "complete";
    protected static final String EXTRA_SHOW_TOOLBAR = "show_toolbar";
    protected static final String EXTRA_SHOW_TOOLBAR_UP_BUTTON = "show_toolbar_up";
    protected static final String EXTRA_SKIP_SAVING_TEMP_IMAGE = "skip_saving";
    protected static final String EXTRA_TEMP_IMAGE_SIZE = "image_size";

    protected static final int DEFAULT_TEMP_IMAGE_SIZE = 1920;

    public static class SpecBuilder {

        protected Bundle spec;

        public SpecBuilder(int requestId) {
            spec = new Bundle();
            spec.putInt(EXTRA_REQUEST_ID, requestId);
        }

        public Bundle build() {
            return spec;
        }

        public SpecBuilder setCompleteAfterCapture() {
            spec.putBoolean(EXTRA_COMPLETE_AFTER_CAPTURE, true);
            return this;
        }

        public SpecBuilder setToolbarVisible() {
            spec.putBoolean(EXTRA_SHOW_TOOLBAR, true);
            return this;
        }

        public SpecBuilder setToolbarUpButtonVisible() {
            spec.putBoolean(EXTRA_SHOW_TOOLBAR_UP_BUTTON, true);
            return this;
        }

        public SpecBuilder setSkipSavingTempImage() {
            spec.putBoolean(EXTRA_SKIP_SAVING_TEMP_IMAGE, true);
            return this;
        }

        public SpecBuilder setTempImageSize(int size) {
            spec.putInt(EXTRA_TEMP_IMAGE_SIZE, size);
            return this;
        }
    }

    public abstract void startCapture();
    public abstract void stopCapture();

    protected int mRequestId;
    protected boolean mCompleteAfterCapture;
    protected boolean mShowToolbar;
    protected boolean mShowToolbarUpButton;
    protected boolean mSkipSavingTempImage;
    protected int mTempImageSize;

    protected Handler mHandler;

    protected boolean mOnPaused;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRequestId = args.getInt(EXTRA_REQUEST_ID);
        mCompleteAfterCapture = args.getBoolean(EXTRA_COMPLETE_AFTER_CAPTURE, false);
        mShowToolbar = args.getBoolean(EXTRA_SHOW_TOOLBAR, false);
        mShowToolbarUpButton = args.getBoolean(EXTRA_SHOW_TOOLBAR_UP_BUTTON, false);
        mSkipSavingTempImage = args.getBoolean(EXTRA_SKIP_SAVING_TEMP_IMAGE, false);
        mTempImageSize = args.getInt(EXTRA_TEMP_IMAGE_SIZE, DEFAULT_TEMP_IMAGE_SIZE);
        mHandler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();
        mOnPaused = false;
    }

    @Override
    public void onPause() {
        mOnPaused = true;
        super.onPause();
    }

    protected NBiometricClient getBiometricClient() {
        NBiometricClient client = null;
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            client = ((BiometricActivity) activity).getBiometricClient();
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
        return client;
    }

    protected NSubject getSubject() {
        NSubject subject = null;
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            subject = ((BiometricActivity) activity).getSubject();
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
        return subject;
    }

    protected void setSubject(NSubject subject) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            ((BiometricActivity) activity).setSubject(subject);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected D2xxManager getD2xxManager() {
        D2xxManager d2xxManager = null;
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            d2xxManager = ((BiometricActivity) activity).getD2xxManager();
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
        return d2xxManager;
    }

    protected int getDevCount(D2xxManager d2xxManager) {
        int devCount = 0;
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            devCount = ((BiometricActivity) activity).getDevCount(d2xxManager);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
        return devCount;
    }

    protected void setFTDevice(FT_Device ftDevice) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            ((BiometricActivity) activity).setFTDevice(ftDevice);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected FT_Device getFTDevice() {
        FT_Device ftDevice = null;
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            ftDevice = ((BiometricActivity) activity).getFtDevice();
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
        return ftDevice;
    }

    protected void onCaptureCompleted() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            ((BiometricActivity) activity).onCaptureCompleted(mRequestId);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected void postOnCaptureCompleted() {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((BiometricActivity) activity).onCaptureCompleted(mRequestId);
                }
            });
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected void onCaptureCancelled() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            ((BiometricActivity) activity).onCaptureCancelled(mRequestId);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected void postOnCaptureCancelled() {
        final Activity activity = getActivity();
        if (activity != null && activity instanceof BiometricActivity) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((BiometricActivity) activity).onCaptureCancelled(mRequestId);
                }
            });
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    protected void runOnUiThread(Runnable runnable) {
        mHandler.post(runnable);
    }
}
