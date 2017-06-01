package com.biginnov.investigator.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.biginnov.investigator.activity.BiometricActivity;
import com.biginnov.investigator.util.CommonUtils;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.neurotec.biometrics.NBiometricCaptureOption;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NFace;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.biometrics.view.NFaceView;
import com.neurotec.devices.NCamera;
import com.neurotec.devices.NDevice;
import com.neurotec.devices.NDeviceType;
import com.neurotec.lang.NRational;
import com.neurotec.media.NMediaFormat;
import com.neurotec.media.NVideoFormat;
import com.neurotec.util.concurrent.CompletionHandler;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class FaceCaptureFragment extends BiometricFragment implements View.OnClickListener {
    private static final String TAG = FaceCaptureFragment.class.getSimpleName();

    public static final int CAMERA_SELECTION_REAR = 0;
    public static final int CAMERA_SELECTION_FRONT = 1;
    private static final String EXTRA_CAMERA_SELECTION = "selection";
    private static final String CAMERA_DISPLAY_NAME_REAR = "Rear";
    private static final String CAMERA_DISPLAY_NAME_FRONT = "Front";

    public static FaceCaptureFragment getInstance(int requestId) {
        return getInstance(new SpecBuilder(requestId).build());
    }

    public static FaceCaptureFragment getInstance(Bundle spec) {
        if (!spec.containsKey(EXTRA_REQUEST_ID)) {
            throw new IllegalArgumentException("No request id");
        }
        FaceCaptureFragment fragment = new FaceCaptureFragment();
        fragment.setArguments(spec);
        return fragment;
    }

    public static class SpecBuilder extends BiometricFragment.SpecBuilder {

        public SpecBuilder(int requestId) {
            super(requestId);
        }

        public SpecBuilder setCameraSelection(int selection) {
            spec.putInt(EXTRA_CAMERA_SELECTION, selection);
            return this;
        }
    }

    private int mCameraSelection;
    private Button mButtonNegative;
    private Button mButtonPositive;
    private AtomicBoolean mConfirmMode;
    private boolean mPendingSwitchCamera;

    private NFaceView mFaceView;
    private NFace mFace;

    private NonUiHandler mNonUiHandler;
    private CameraManager mCameraManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mCameraSelection = args.getInt(EXTRA_CAMERA_SELECTION, CAMERA_SELECTION_REAR);
        mConfirmMode = new AtomicBoolean(false);
        HandlerThread thread = new HandlerThread(TAG);
        thread.start();
        mNonUiHandler = new NonUiHandler(this, thread.getLooper());
        mCameraManager = (CameraManager) getActivity().getSystemService(Context.CAMERA_SERVICE);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        if (getActivity() instanceof BiometricActivity) {
            view = inflater.inflate(R.layout.fragment_face_capture, container, false);
            findViews(view);
            startCapture();
        } else {
            LogUtils.e(TAG, "Can only attach to BiometricActivity");
        }
        return view; // Return null to throw null pointer exception
    }

    private void findViews(View view) {
        mButtonNegative = (Button) view.findViewById(R.id.action_negative);
        mButtonPositive = (Button) view.findViewById(R.id.action_positive);
        mButtonNegative.setOnClickListener(this);
        mButtonPositive.setOnClickListener(this);
        mFaceView = (NFaceView) view.findViewById(R.id.biometric_view);
        if (mShowToolbar) {
            Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
            toolbar.setVisibility(View.VISIBLE);
            toolbar.inflateMenu(R.menu.face_capture);
            toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (item.getItemId() == R.id.action_switch) {
                        switchCamera();
                        return true;
                    }
                    return false;
                }
            });
            if (mShowToolbarUpButton) {
                toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
                toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mConfirmMode.get()) {
                            getSubject().getFaces().remove(mFace);
                            onCaptureCancelled();
                        } else {
                            stopCapture();
                        }
                    }
                });
            }
            ((RelativeLayout.LayoutParams) mFaceView.getLayoutParams()).addRule(
                    RelativeLayout.BELOW, R.id.toolbar);
        } else {
            setHasOptionsMenu(true);
        }

        Resources res = getResources();
        int cameraViewWidth = res.getDimensionPixelSize(R.dimen.camera_view_width);
        int cameraMaskMargin = res.getDimensionPixelSize(R.dimen.camera_mask_margin);
        int cameraFocusSize = cameraViewWidth - cameraMaskMargin * 2;
        ((RelativeLayout.LayoutParams) view.findViewById(R.id.mask_bottom).getLayoutParams())
                .topMargin = cameraMaskMargin + cameraFocusSize;

        int iconSize = res.getDimensionPixelSize(R.dimen.camera_focus_icon_size);
        int iconMarginTop = cameraMaskMargin + cameraFocusSize - iconSize;
        ((RelativeLayout.LayoutParams) view.findViewById(R.id.focus_bottom_left)
                .getLayoutParams()).topMargin = iconMarginTop;
        ((RelativeLayout.LayoutParams) view.findViewById(R.id.focus_bottom_right)
                .getLayoutParams()).topMargin = iconMarginTop;
    }

    @Override
    public void onDestroy() {
        mNonUiHandler.removeMessages(NON_UI_MESSAGE_SET_CAMERA_FORMAT);
        mNonUiHandler.getLooper().quit();
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.face_capture, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case R.id.action_switch:
                if (!mPendingSwitchCamera) {
                    switchCamera();
                }
                result = true;
                break;
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_negative:
                setButtonsEnabled(false);
                if (mConfirmMode.get()) {
                    getSubject().getFaces().remove(mFace);
                    startCapture();
                } else {
                    stopCapture();
                }
                break;

            case R.id.action_positive:
                setButtonsEnabled(false);
                postSetConfirmMode(true);
                if (mConfirmMode.get()) {
                    if (mSkipSavingTempImage) {
                        getBiometricClient().force();
                        //onCaptureCompleted();
                    } else {
                        LogUtils.d(TAG, "2345 :");
                        getBiometricClient().force();
                        //CommonUtils.executeAsyncTask(new SaveTemplateFaceImageTask());
                    }
                } else {
                    LogUtils.d(TAG, "5678 :");
                    getBiometricClient().force();
                }
                break;
        }
    }

    @Override
    public void startCapture() {
        LogUtils.d(TAG, "Start capture");
        postSetConfirmMode(false);
        try {
            NSubject subject = new NSubject();
            mFace = new NFace();
            EnumSet<NBiometricCaptureOption> options = EnumSet.of(NBiometricCaptureOption.MANUAL);
            mFace.setCaptureOptions(options);
            mFaceView.setFace(mFace);
            subject.getFaces().add(mFace);
            LogUtils.d(TAG, "Face : " + subject.getFaces());
            if (!isDetached()) {
                NBiometricClient client = getBiometricClient();
                NCamera camera = findCamera(client);
                if (camera != null) {
                    client.setFaceCaptureDevice(camera);
                    client.capture(subject, subject, mCaptureCompletionHandler);
                    setSubject(subject);
                    mNonUiHandler.sendEmptyMessageDelayed(NON_UI_MESSAGE_SET_CAMERA_FORMAT, 500);
                } else {
                    LogUtils.w(TAG, "No camera found");
                    showToastOnUiThread(getString(R.string.error_camera_not_found));
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
    }

    @Override
    public void stopCapture() {
        LogUtils.d(TAG, "stopCapture()...");
        getBiometricClient().cancel();
    }

    private NCamera findCamera(NBiometricClient client) {
        NCamera camera = null;
        for (NDevice device : client.getDeviceManager().getDevices()) {
            if (device.getDeviceType().contains(NDeviceType.CAMERA)) {
                String displayName = device.getDisplayName();
                LogUtils.d(TAG, "Camera display name: " + displayName);
                if ((mCameraSelection == CAMERA_SELECTION_FRONT && displayName.contains("Front")) ||
                        mCameraSelection == CAMERA_SELECTION_REAR && displayName.contains("Rear")) {
                    camera = (NCamera) device;
                    LogUtils.d(TAG, "Selected camera: ", camera.getDisplayName());
                    break;
                }
            }
        }
        return camera;
    }

    private void switchCamera() {
        LogUtils.d(TAG, "Switch camera");
        mNonUiHandler.removeMessages(NON_UI_MESSAGE_SET_CAMERA_FORMAT);

        // Switch
        mPendingSwitchCamera = true;
        mCameraSelection = (mCameraSelection == CAMERA_SELECTION_FRONT) ?
                CAMERA_SELECTION_REAR : CAMERA_SELECTION_FRONT;

        // Stop current
        getBiometricClient().cancel();

        // Start new
        // startCapture(); // Restart after cancel event
    }

    private void postSetConfirmMode(boolean confirm) {
        mConfirmMode.set(confirm);
        mHandler.post(mSetConfirmModeRunnable);
    }

    private Runnable mSetConfirmModeRunnable = new Runnable() {
        @Override
        public void run() {
            mButtonNegative.setText(mConfirmMode.get() ?
                    R.string.text_capture_again : android.R.string.cancel);
            mButtonPositive.setText(mConfirmMode.get() ?
                    android.R.string.ok : R.string.text_action_face_capture);
            setButtonsEnabled(true);
        }
    };

    private void setButtonsEnabled(boolean enabled) {
        mButtonPositive.setEnabled(enabled);
        mButtonNegative.setEnabled(enabled);
    }

    private CompletionHandler<NBiometricStatus, NSubject> mCaptureCompletionHandler =
            new CompletionHandler<NBiometricStatus, NSubject>() {
                @Override
                public void completed(NBiometricStatus result, NSubject subject) {
                    if (isDetached()) return;
                    LogUtils.d(TAG, "result: ", result);

                    if (result == NBiometricStatus.OK) {
                        if (mCompleteAfterCapture) {
                            mHandler.post(mOnCaptureCompletedRunnable);
                        } else {
                            LogUtils.d(TAG, "result3333: ", result);
                            postSetConfirmMode(true);
                            CommonUtils.executeAsyncTask(new SaveTemplateFaceImageTask());
                        }
                    } else if (result != NBiometricStatus.CANCELED) {
                        getSubject().getFaces().remove(mFace);
                        showToastOnUiThread(result.toString());
                        startCapture();
                    } else { // Canceled
                        getSubject().getFaces().remove(mFace);
                        if (mPendingSwitchCamera) {
                            mPendingSwitchCamera = false;
                            startCapture();
                            LogUtils.d(TAG, "Switch camera after cancel event");
                        } else {
                            postOnCaptureCancelled();
                        }
                    }
                }

                @Override
                public void failed(Throwable e, NSubject subject) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    getSubject().getFaces().remove(mFace);
                    showToastOnUiThread(getString(R.string.error_operation_failed));
                    postOnCaptureCancelled();
                }
            };


    // TODO: Tune by target device
    private static final double ASPECT = 4d / 3;
    private static final int WIDTH_LIMIT = 1600;
    private static final int HEIGHT_LIMIT = 1200;

    private class NonUiHandler extends Handler {

        private WeakReference<FaceCaptureFragment> fragmentWeakReference;

        public NonUiHandler(FaceCaptureFragment fragment, Looper looper) {
            super(looper);
            this.fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            FaceCaptureFragment fragment = fragmentWeakReference.get();
            if (fragment != null) {
                fragment.handleNonUiMessage(msg);
            }
        }
    }

    private static final int NON_UI_MESSAGE_SET_CAMERA_FORMAT = 9999;

    private void handleNonUiMessage(Message msg) {
        if (isDetached()) return;
        NCamera camera = getBiometricClient().getFaceCaptureDevice();
        NMediaFormat[] formats = camera.getFormats();
        NVideoFormat tempFormat;
        NRational tempRational;
        NVideoFormat candidate = null;
        int candidateFrameRate = 0;
        for (NMediaFormat format : formats) {
            tempFormat = (NVideoFormat) format;
            double width = tempFormat.getWidth();
            double height = tempFormat.getHeight();
            LogUtils.d(TAG, "width: ", width);
            LogUtils.d(TAG, "height: ", height);
            if (candidate != null &&
                    (width < candidate.getWidth() || height < candidate.getHeight())) {
                break;
            }

            double aspect = width / height;
            LogUtils.d(TAG, "aspect: ", aspect);
            if (width > WIDTH_LIMIT || height > HEIGHT_LIMIT || aspect != ASPECT) {
                continue;
            }
            tempRational = tempFormat.getFrameRate();
            int frameRate = tempRational.numerator / tempRational.denominator;
            LogUtils.d(TAG, "frameRate: ", frameRate);
            if (candidate == null || frameRate > candidateFrameRate) {
                candidate = tempFormat;
                candidateFrameRate = frameRate;
            }
        }
        if (candidate == null) {
            candidate = (NVideoFormat) formats[0];
        }
        LogUtils.d(TAG, "candidate: ", candidate);
        camera.setCurrentFormat(candidate);
    }

    private class SaveTemplateFaceImageTask extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected void onPreExecute() {
            showProgressDialog(false);
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            try {
                Bitmap face = CommonUtils.cropFaceImage(getActivity(),
                        mFace.getImage(), getCameraOrientation()).toBitmap();
                int originalSize = face.getWidth();
                int targetSize = mTempImageSize < originalSize ? mTempImageSize : originalSize;
                Bitmap scaledFace = Bitmap.createScaledBitmap(face, targetSize, targetSize, true);

                File target = StorageUtils.getTempFaceImageFile();
                LogUtils.d(TAG, "target: " + target.getAbsolutePath());
                Fresco.getImagePipeline().evictFromCache(Uri.fromFile(target));
                result = CommonUtils.writePngFile(scaledFace, target);
                face.recycle();
                scaledFace.recycle();

                if (!result) {
                    target.delete();
                }

            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
            LogUtils.d(TAG, "SaveTemplateFaceImageTask: ", result);
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!isDetached()) {
                dismissProgressDialog();
                if (!result) {
                    Toast.makeText(getActivity(),
                            R.string.error_save_image, Toast.LENGTH_SHORT).show();
                }
                onCaptureCompleted();
            }
        }

        private int getCameraOrientation() {
            int orientation = -1;
            try {
                for (final String cameraId : mCameraManager.getCameraIdList()) {
                    CameraCharacteristics chara =
                            mCameraManager.getCameraCharacteristics(cameraId);
                    Integer facing = chara.get(CameraCharacteristics.LENS_FACING);
                    if (facing == null) continue;
                    if ((mCameraSelection == CAMERA_SELECTION_FRONT &&
                            facing == CameraCharacteristics.LENS_FACING_FRONT) ||
                            (mCameraSelection == CAMERA_SELECTION_REAR &&
                                    facing == CameraCharacteristics.LENS_FACING_BACK)) {
                        Integer temp = chara.get(CameraCharacteristics.SENSOR_ORIENTATION);
                        if (temp != null) {
                            orientation = temp;
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
            LogUtils.d(TAG, "Camera orientation: ", orientation);
            return orientation;
        }
    }

    private Runnable mOnCaptureCompletedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSkipSavingTempImage) {
                onCaptureCompleted();
            } else {
                CommonUtils.executeAsyncTask(new SaveTemplateFaceImageTask());
            }
        }
    };
}
