package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.biginnov.investigator.activity.BiometricActivity;
import com.biginnov.investigator.util.CommonUtils;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NFinger;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.images.NImage;
import com.neurotec.util.concurrent.CompletionHandler;
import com.serenegiant.usb.DeviceFilter;
import com.serenegiant.usb.IFrameCallback;
import com.serenegiant.usb.JmtFpsCamera;
import com.serenegiant.usb.USBMonitor;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import sensor.fpc.hst.fpcsensor.Fingerprint;

public class FingerprintCaptureFragment extends BiometricFragment implements View.OnClickListener {
    private static final String TAG = FingerprintCaptureFragment.class.getSimpleName();

    public static FingerprintCaptureFragment getInstance(int requestId) {
        return getInstance(new SpecBuilder(requestId).build());
    }

    public static FingerprintCaptureFragment getInstance(Bundle spec) {
        if (!spec.containsKey(EXTRA_REQUEST_ID)) {
            throw new IllegalArgumentException("No request id");
        }
        FingerprintCaptureFragment fragment = new FingerprintCaptureFragment();
        fragment.setArguments(spec);
        return fragment;
    }

    private static final int MILLIS_FIND_DEVICE_INTERVAL = 1500;
    //private static final int FINGERPRINT_IMAGE_SIZE = 128; // By finger scanner spec
    private static final float FINGERPRINT_RESOLUTION = 508f; // By finger scanner spec

    private static final int FINGERPRINT_IMAGE_WIDTH    = 192;
    private static final int FINGERPRINT_IMAGE_HEIGHT   = 192;
    private static final int FINGERPRINT_IMAGE_SIZE     = FINGERPRINT_IMAGE_WIDTH * FINGERPRINT_IMAGE_HEIGHT;

    private View mCaptureView;
    private View mConfirmView;

    // For capture
    private Button mButtonNegative;
    private Button mButtonCapture;
    private NFinger mFinger;

    private Surface mFingerprintSurface;
    private ImageView mFingerprintImageView;
    private Bitmap mFingerprintBitmap;
    //private USBMonitor mUsbMonitor;
    //private USBMonitor.UsbControlBlock mUsbControlBlock;
    //private DeviceFilter mDeviceFilter;
    //private JmtFpsCamera mJmtFpsCamera;
    private UiHandler mUiHandler;
    private boolean mFindDeviceOnResume;
    private final Object mSync = new Object(); // For synchronized

    // For confirm
    private RecyclerView mFingerprintList;
    private FingerprintListAdapter mFingerprintListAdapter;
    private NSubject.FingerCollection mFingerprintCollection;

    private boolean mShowConfirmOnStopCapture; // To check if canceling capture will back to confirm
    private AtomicBoolean mDisplayFingerprintImageOnSurfaceCreated = new AtomicBoolean(false);
    //private AtomicBoolean mJmtFpsCameraOpened = new AtomicBoolean(false);
    private AtomicBoolean mFpcDeviceOpened = new AtomicBoolean(false);
    private AtomicBoolean mFpcDeviceExist = new AtomicBoolean(false);

    private boolean debug = false;

    public static D2xxManager d2xxManager;
    public static FT_Device ft_device;

    public emptyQueueThread emptyQueue_thread;
    public readBufferThread readBuffer_thread;
    public captureDetectThread captureDetect_thread;

    private SensorType sensorType = SensorType.Unknown;

    private boolean boolCaptureImage = false;
    private boolean fpc1020Setting_interrupt_header = false;
    private boolean captureImage_end = false;
    private boolean captureImage_interrupt_end = false;

    private int mReadSize = 0;
    private int readBufferSize = 16320;

    public byte[] readBuffer = new byte[readBufferSize];
    public byte[] inBuffer = new byte[readBufferSize];
    public byte[] fullImage = new byte[FINGERPRINT_IMAGE_SIZE];
    public byte[] image = new byte[FINGERPRINT_IMAGE_SIZE];

    public byte[] cacImageArray;

    private Fingerprint fingerprint;

    private enum SensorType {
        FPC_1021(0),
        FPC_1020(1),
        Unknown(2);

        private final int value;

        SensorType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mFingerprintCollection = getSubject().getFingers();
        //Activity activity = getActivity();
        //mUsbMonitor = new USBMonitor(activity, mOnDeviceConnectListener);
        //mDeviceFilter = DeviceFilter.getDeviceFilters(activity, R.xml.device_filter).get(0);
        mUiHandler = new UiHandler(this);
        mUiHandler.sendEmptyMessageDelayed(UI_MESSAGE_FIND_DEVICE, 500);

        fingerprint = new Fingerprint();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        if (getActivity() instanceof BiometricActivity) {
            view = inflater.inflate(R.layout.fragment_fingerprint_capture, container, false);
            findViews(view);
            if (getSubject().getFingers().size() > 0) {
                mShowConfirmOnStopCapture = true;
                mCaptureView.setVisibility(View.GONE);
            } else {
                mShowConfirmOnStopCapture = false;
                mConfirmView.setVisibility(View.GONE);
            }
        } else {
            LogUtils.e(TAG, "Can only attach to BiometricActivity");
        }
        return view; // Return null to throw null pointer exception
    }

    private void findViews(View view) {
        mCaptureView = view.findViewById(R.id.capture_container);
        mConfirmView = view.findViewById(R.id.confirm_container);

        SurfaceView fingerprintSurfaceView =
                (SurfaceView) view.findViewById(R.id.fingerprint_surface_view);
        fingerprintSurfaceView.getHolder().addCallback(mSurfaceViewCallback);
        mFingerprintImageView = (ImageView) view.findViewById(R.id.fingerprint_image_view);
        mButtonNegative = (Button) view.findViewById(R.id.action_negative);
        mButtonNegative.setOnClickListener(this);
        mButtonCapture = (Button) view.findViewById(R.id.action_fingerprint_capture);
        mButtonCapture.setOnClickListener(this);

        view.findViewById(R.id.action_positive).setOnClickListener(this);

        Activity activity = getActivity();
        mFingerprintList = (RecyclerView) view.findViewById(android.R.id.list);
        mFingerprintList.setHasFixedSize(true);
        mFingerprintList.setLayoutManager(new LinearLayoutManager(
                activity, LinearLayoutManager.VERTICAL, false));
        mFingerprintListAdapter = new FingerprintListAdapter(activity);
        mFingerprintList.setAdapter(mFingerprintListAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume");
        if (mFindDeviceOnResume) {
            mFindDeviceOnResume = false;
            mUiHandler.sendEmptyMessage(UI_MESSAGE_FIND_DEVICE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        LogUtils.d(TAG, "onPause");
    }

    @Override
    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy");
        mUiHandler.removeMessages(UI_MESSAGE_FIND_DEVICE);
        synchronized (mSync) {
            disconnectFpcDevice();
            if (mFingerprintSurface != null) {
                mFingerprintSurface.release();
                mFingerprintSurface = null;
            }
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_negative:
                stopCapture();
                break;

            case R.id.action_positive:
                onCaptureCompleted();
                break;

            case R.id.action_fingerprint_capture:
                if (mFingerprintBitmap == null) { // Frame not received
                    break;
                }
                mButtonCapture.setEnabled(false);
                NImage image = null;
                synchronized (mSync) {
                    if (mFingerprintBitmap != null) { // Need to check again in synchronized
                        image = NImage.fromBitmap(mFingerprintBitmap);
                        image.setHorzResolution(FINGERPRINT_RESOLUTION);
                        image.setVertResolution(FINGERPRINT_RESOLUTION);
                    }
                }
                if (image != null) {
                    mFinger = new NFinger();
                    mFinger.setImage(image);
                    NSubject subject = getSubject();
                    subject.getFingers().add(mFinger);
                    setSubject(subject);
                    NBiometricClient client = getBiometricClient();
                    NBiometricTask task = client.createTask(
                            EnumSet.of(NBiometricOperation.CREATE_TEMPLATE), subject);
                    client.performTask(
                            task, NBiometricOperation.CREATE_TEMPLATE, mCompletionHandler);
                } else {
                    startCapture();
                }
                break;
        }
    }

    @Override
    public void startCapture() {
        LogUtils.d(TAG, "Start capture");
        if (!isCaptureMode()) {
            postShowCaptureView();
        }
        CommonUtils.executeAsyncTask(new CaptureImageTask());
    }

    @Override
    public void stopCapture() {
        LogUtils.d(TAG, "stopCapture");
        boolCaptureImage = false;
        mUiHandler.sendEmptyMessage(UI_MESSAGE_CLEAR_IMAGE);
        if (isCaptureMode()) { // Stop from back key or up button
            endCapture(false); // Do not remove finger because mFinger now is added on capture
        } else {
            onCaptureCancelled();
        }
    }

    /**
     * Finish capture process when cancel or error
     */
    private void endCapture() {
        endCapture(true);
    }

    private void endCapture(boolean removeFinger) {
        if (removeFinger && mFinger != null) {
            getSubject().getFingers().remove(mFinger);
        }
        if (mShowConfirmOnStopCapture) {
            postShowConfirmView(false);
        } else {
            postOnCaptureCancelled();
        }
    }

    private CompletionHandler<NBiometricTask, NBiometricOperation> mCompletionHandler =
            new CompletionHandler<NBiometricTask, NBiometricOperation>() {
                @Override
                public void completed(NBiometricTask task, NBiometricOperation operation) {
                    if (isDetached()) return;
                    NBiometricStatus status = task.getStatus();
                    LogUtils.d(TAG, "status: ", status);

                    if (status == NBiometricStatus.OK) {
                        mUiHandler.sendEmptyMessage(UI_MESSAGE_CLEAR_IMAGE);
                        if (mCompleteAfterCapture) {
                            mHandler.post(mOnCaptureCompletedRunnable);
                        } else {
                            postShowConfirmView(true);
                        }
                    } else if (status != NBiometricStatus.CANCELED) {
                        showToastOnUiThread(status.toString());
                        getSubject().getFingers().remove(mFinger);
                        mUiHandler.sendEmptyMessage(UI_MESSAGE_CLEAR_IMAGE);
                        startCapture();
                    } else { // Canceled
                        endCapture();
                    }
                }

                @Override
                public void failed(Throwable e, NBiometricOperation operation) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    showToastOnUiThread(getString(R.string.error_operation_failed));
                    endCapture();
                }
            };

    private boolean isCaptureMode() {
        LogUtils.d(TAG, "isCaptureMode()");
        return mCaptureView.getVisibility() == View.VISIBLE;
    }

    private void showCaptureView() {
        mConfirmView.setVisibility(View.GONE);
        mCaptureView.setVisibility(View.VISIBLE);
        mButtonNegative.setEnabled(true);
    }

    private void postShowCaptureView() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                showCaptureView();
            }
        });
    }

    private void postShowConfirmView(final boolean saveTempImage) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mCaptureView.setVisibility(View.GONE);
                mConfirmView.setVisibility(View.VISIBLE);
                mShowConfirmOnStopCapture = true;
                // Ignore mSkipSavingTempImage because image is necessary in fingerprint list
                if (saveTempImage) {
                    CommonUtils.executeAsyncTask(new SaveTemplateFingerprintImageTask(false));
                } else { // End capture by cancel or error
                    mFingerprintListAdapter.scrollToEnd();
                }
            }
        });
    }

    private class FingerprintListAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_FINGERPRINT = 0;
        private static final int VIEW_TYPE_ADD_FINGERPRINT = 1;

        public class ViewHolderFingerprint extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            private SimpleDraweeView image;

            public ViewHolderFingerprint(View v) {
                super(v);
                image = (SimpleDraweeView) v.findViewById(R.id.image);
                v.findViewById(R.id.action_fingerprint_removal).setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                LogUtils.d(TAG, "onClick remove: ", position);
                StorageUtils.deleteTempFingerprintImageFile(position, mFingerprintCollection.size());
                mFingerprintCollection.remove(position);
                mFingerprintListAdapter.notifyItemRemoved(position);
                mFingerprintListAdapter.notifyItemRangeChanged(position, mFingerprintCollection.size() - 1);
                mUiHandler.sendEmptyMessage(UI_MESSAGE_CLEAR_IMAGE);
                // FIXME: Should use async task?
                // CommonUtils.executeAsyncTaskSerially(new DeleteFingerprintImageTask(position));
            }
        }

        public class ViewHolderAddFingerprint extends RecyclerView.ViewHolder
                implements View.OnClickListener {

            public ViewHolderAddFingerprint(View v) {
                super(v);
                v.findViewById(R.id.image).setOnClickListener(this);
            }

            @Override
            public void onClick(View v) {
                int position = getAdapterPosition();
                LogUtils.d(TAG, "onClick add: ", position);
                // showCaptureView();
                if (getDevCount(getD2xxManager()) > 0) {
                    if (mFpcDeviceExist.get()) {
                        mFpcDeviceExist.set(false);
                        if (!isCaptureMode()) {
                            postShowCaptureView();
                        }
                        mUiHandler.sendEmptyMessage(UI_MESSAGE_FIND_DEVICE);
                    } else {
                        startCapture();
                    }
                } else {
                    mFpcDeviceExist.set(true);
                    showToastOnUiThread(getString(R.string.error_finger_device_not_found));
                }
            }
        }

        private LayoutInflater inflater;

        public FingerprintListAdapter(Activity activity) {
            inflater = activity.getLayoutInflater();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_ADD_FINGERPRINT) {
                View v = inflater.inflate(R.layout.item_fingerprint_add, parent, false);
                return new ViewHolderAddFingerprint(v);
            } else {
                View v = inflater.inflate(R.layout.item_fingerprint, parent, false);
                return new ViewHolderFingerprint(v);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position < mFingerprintCollection.size()) {
                ((ViewHolderFingerprint) holder).image.setImageURI(Uri.fromFile(
                        StorageUtils.getTempFingerprintImageFile(position)));
            }
        }

        @Override
        public int getItemCount() {
            return mFingerprintCollection.size() + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == mFingerprintCollection.size() ?
                    VIEW_TYPE_ADD_FINGERPRINT : VIEW_TYPE_FINGERPRINT;
        }

        private void scrollToEnd() {
            mFingerprintList.smoothScrollToPosition(mFingerprintCollection.size());
        }
    }

    private class DeleteFingerprintImageTask extends AsyncTask<Object, Object, Boolean> {

        private int position;

        public DeleteFingerprintImageTask(int position) {
            this.position = position;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            StorageUtils.deleteTempFingerprintImageFile(
                    position, mFingerprintCollection.size());
            mFingerprintCollection.remove(position);
            return null;
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            mFingerprintListAdapter.notifyItemRemoved(position);
        }
    }

    private class SaveTemplateFingerprintImageTask extends AsyncTask<Object, Object, Boolean> {

        private boolean mCompleteCaptureAfterSave;

        public SaveTemplateFingerprintImageTask(boolean completeCapture) {
            mCompleteCaptureAfterSave = completeCapture;
        }

        @Override
        protected void onPreExecute() {
            showProgressDialog(false);
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean showErrorMessage = false;
            boolean result = false;
            try {
                Matrix matrix = new Matrix();
                matrix.preScale(-1f, -1f); // Flip image by finger scanner spec
                Bitmap bitmap = Bitmap.createBitmap(mFingerprintBitmap, 0, 0,
                        mFingerprintBitmap.getWidth(), mFingerprintBitmap.getHeight(),
                        matrix, true);

                File target = StorageUtils.getTempFingerprintImageFile(
                        mFingerprintCollection.size() - 1);
                result = CommonUtils.writePngFile(bitmap, target);
                if (!result) {
                    target.delete();
                    showErrorMessage = true;
                }
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
                showErrorMessage = true;
            }
            LogUtils.d(TAG, "SaveTemplateFingerprintImageTask: ", result);
            return showErrorMessage;
        }

        @Override
        protected void onPostExecute(Boolean showErrorMessage) {
            if (!isDetached()) {
                dismissProgressDialog();
                if (showErrorMessage) {
                    Toast.makeText(getActivity(),
                            R.string.error_save_image, Toast.LENGTH_SHORT).show();
                }
                if (mCompleteCaptureAfterSave) {
                    onCaptureCompleted();
                } else {
                    int position = mFingerprintCollection.size() - 1;
                    File target = StorageUtils.getTempFingerprintImageFile(position);
                    Fresco.getImagePipeline().evictFromCache(Uri.fromFile(target));
                    mFingerprintListAdapter.notifyItemChanged(position);
                    mFingerprintListAdapter.scrollToEnd();
                }
            }
        }
    }

    private Runnable mOnCaptureCompletedRunnable = new Runnable() {
        @Override
        public void run() {
            if (mSkipSavingTempImage) { // Only check skip if mCompleteAfterCapture
                onCaptureCompleted();
            } else {
                CommonUtils.executeAsyncTask(new SaveTemplateFingerprintImageTask(true));
            }
        }
    };

    private final SurfaceHolder.Callback mSurfaceViewCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(final SurfaceHolder holder) {
            LogUtils.d(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height) {
            LogUtils.d(TAG, "surfaceChanged");
            if ((width == 0) || (height == 0)) return;
            mFingerprintSurface = holder.getSurface();
            if (mDisplayFingerprintImageOnSurfaceCreated.get()) {
                mDisplayFingerprintImageOnSurfaceCreated.set(false);
            }
        }

        @Override
        public void surfaceDestroyed(final SurfaceHolder holder) {
            LogUtils.d(TAG, "surfaceDestroyed");
            disconnectFpcDevice();
            /*
            synchronized (mSync) {
                disconnectFpcDevice();
            }
            */
            mFingerprintSurface = null;
        }
    };

    private class CaptureImageTask extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            LogUtils.d(TAG, "CaptureImageTask doInBackground....");
            boolean result;
            synchronized (mSync) {
                if (mFingerprintSurface != null) {
                    image = captureFingerImage();

                    byte[] pRGBFinger = transPixel(image);

                    mFingerprintBitmap = Bitmap.createBitmap(FINGERPRINT_IMAGE_WIDTH, FINGERPRINT_IMAGE_HEIGHT, Bitmap.Config.ARGB_8888);
                    mFingerprintBitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pRGBFinger));

                    result = true;

                } else {
                    mDisplayFingerprintImageOnSurfaceCreated.set(true);
                    result = true; // TODO: Enable capture button?
                }
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            LogUtils.d(TAG, "CaptureImageTask onPostExecute...");
            if (!isDetached() && result) {
                mFingerprintImageView.setImageBitmap(mFingerprintBitmap);
                mButtonCapture.setEnabled(true);
            }
        }
    }

    private byte[] transPixel(byte[] buffer) {
        int RawIdx;
        int RgbIdx;
        int Pixel;
        byte[] pRGBFinger;

        pRGBFinger = new byte[FINGERPRINT_IMAGE_WIDTH * FINGERPRINT_IMAGE_HEIGHT * 4];
        RgbIdx = 0;

        for (RawIdx = 0; RawIdx < FINGERPRINT_IMAGE_WIDTH * FINGERPRINT_IMAGE_HEIGHT; RawIdx++) {
            Pixel = (buffer[RawIdx] & 0xFF);

            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) Pixel;
            pRGBFinger[RgbIdx++] = (byte) 0xFF;
        }

        return pRGBFinger;
    }

    private class InitFpcDevice extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected Boolean doInBackground(Object... params) {
            LogUtils.d(TAG, "InitFpcDevice doInBackground....");

            boolean result = false;

            ft_device.resetDevice();
            ft_device.setChars((byte) 0, (byte) 0, (byte) 0, (byte) 0);
            ft_device.setLatencyTimer((byte) 16);
            ft_device.setBitMode((byte) 0, D2xxManager.FT_BITMODE_RESET);
            ft_device.setBitMode((byte) 0, D2xxManager.FT_BITMODE_MPSSE);

            emptyQueueThread();

            if (mReadSize > 0) {
                readBufferThread(inBuffer);
            }

            fpcInit();

            if (sensorType == SensorType.FPC_1020) {
                send_FPC_Setting(false);

                fpc1020Setting();

                result = true;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            LogUtils.d(TAG, "InitFpcDevice onPostExecute...");
            if (!isDetached() && result) {
                dismissProgressDialog();
                startCapture();
            }
        }
    }

    private void connectFpcDevice() {
        mFpcDeviceOpened.set(true);
        CommonUtils.executeAsyncTask(new InitFpcDevice());
    }

    private void disconnectFpcDevice() {
        LogUtils.d(TAG, "disconnectFpcDevice");
        if (mFpcDeviceOpened.get()) {
            boolCaptureImage = false;
            ft_device.close();
            mFpcDeviceOpened.set(false);
        }
    }

    private static final int UI_MESSAGE_FIND_DEVICE = 1111;
    private static final int UI_MESSAGE_CLEAR_IMAGE = 1112;

    private static class UiHandler extends Handler {

        private WeakReference<FingerprintCaptureFragment> fragmentWeakReference;

        public UiHandler(FingerprintCaptureFragment fragment) {
            fragmentWeakReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            FingerprintCaptureFragment fragment = fragmentWeakReference.get();
            if (fragment != null) {
                fragment.handleMessage(msg);
            }
        }
    }

    private void handleMessage(Message msg) {
        switch (msg.what) {
            case UI_MESSAGE_FIND_DEVICE:
                LogUtils.d(TAG, "UI_MESSAGE_FIND_DEVICE");

                d2xxManager = getD2xxManager();

                if (d2xxManager != null) {

                    int devCount = getDevCount(getD2xxManager());

                    if (devCount > 0) {
                        ft_device = d2xxManager.openByIndex(getActivity().getApplicationContext(), 0);

                        if (ft_device.isOpen()) {
                            if (!mOnPaused) {
                                showProgressDialog(1, false);
                                connectFpcDevice();
                            } else {
                                mFindDeviceOnResume = true;
                            }
                        }
                    }
                }
                break;

            case UI_MESSAGE_CLEAR_IMAGE:
                LogUtils.d(TAG, "UI_MESSAGE_CLEAR_IMAGE");
                mFingerprintImageView.setImageBitmap(null);
                break;
        }
    }

    public byte[] captureFingerImage() {
        LogUtils.d(TAG, "captureFingerImage");
        if (sensorType == SensorType.FPC_1020) {
            boolCaptureImage = true;
            captureDetect_thread = new captureDetectThread();
            captureDetect_thread.start();

            try {
                captureDetect_thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return fullImage;
        } else {
            return null;
        }
    }

    public boolean fpcInit() {
        boolean result = false;

        if (ft_device == null) return false;

        if (!ft_device.isOpen()) {
            LogUtils.d(TAG, "init : device is not open");
            return false;
        } else {

            send_FPC_Setting(true);

            write_RegSet_with_RecLength(new byte[]{(byte) 0xFC}, 2);

            readBufferThread(inBuffer);

            sensorType = SensorType.Unknown;

            int sensorChip = inBuffer[1] / 16;

            LogUtils.d(TAG, String.format("sensorChip = %d, %d", inBuffer[0], inBuffer[1]));

            if ((inBuffer[0] == (byte) 0x02)) {
                if (sensorChip == 0) {
                    sensorType = SensorType.FPC_1020;
                    result = true;
                }
            }
            LogUtils.d(TAG, "SensorType : " + sensorType);
        }
        return result;
    }

    public boolean fpc1020Setting() {
        set_DataBits_HighByte((byte) 0x40, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x44, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x47, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x41, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x42, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x41, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x44, (byte) 0x47);
        set_DataBits_HighByte((byte) 0x40, (byte) 0x47);

        fpc1020Setting_interrupt_header = true;
        write_RegSet_with_RecLength(new byte[] { (byte) 0x1C}, 1);
        fpc1020Setting_interrupt_header = false;

        readBufferThread(inBuffer);

        if (inBuffer[0] != (byte) 0xFF) {
            LogUtils.e(TAG, "Interrupt reg is not reset value (0xFF) !!");
            return false;
        }

        if (sensorType == SensorType.FPC_1020) {
            runSensorCalibrateProcess();
        }

        write_RegSet_with_RecLength(new byte[] { (byte) 0x90, (byte) 0x09}, 0);

        write_RegSet_with_RecLength(new byte[] { (byte) 0x68,
                (byte) 0x08, (byte) 0x08, (byte) 0x08, (byte) 0x08,
                (byte) 0x14, (byte) 0x14, (byte) 0x14, (byte) 0x14 }, 0);

        write_RegSet_with_RecLength(new byte[] { (byte) 0x8C, (byte) 0x22}, 0);

        write_RegSet_with_RecLength(new byte[] { (byte) 0xA8, (byte) 0x0F, (byte) 0x1A }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0xA0, (byte) 0x00, (byte) 0x0F }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0xD8, (byte) 0x50 }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0x5C, (byte) 0x0B }, 0);

        write_RegSet_with_RecLength(new byte[] { (byte) 0x8C, (byte) 0x22}, 0);

        if (sensorType == SensorType.FPC_1020) {
            fingerprint.preprocessorInit();
        }
        return true;
    }

    private class captureDetectThread extends Thread {

        captureDetectThread() {
            this.setPriority(Thread.MIN_PRIORITY);
        }

        @Override
        public void run() {
            int dataBitValue;
            int bitCount;
            int retryCount = 0;
            boolean detectFinger;

            write_RegSet_with_RecLength(new byte[]{(byte) 0xA8, (byte) 0x0F, (byte) 0x1A}, 0);
            write_RegSet_with_RecLength(new byte[]{(byte) 0xA0, (byte) 0x0E, (byte) 0x00}, 0);

            while (boolCaptureImage) {
                if (retryCount >= 10) {
                    set_DataBits_HighByte((byte) 0x44, (byte) 0x47);
                    set_DataBits_HighByte((byte) 0x40, (byte) 0x47);
                    retryCount = 0;
                }

                write_RegSet_with_RecLength(new byte[]{(byte) 0x24}, 0);

                send_detect_finger_Command(new byte[]{(byte) 0x83, (byte) 0x87});

                readBufferThread(inBuffer);

                dataBitValue = inBuffer[0] & 0xFF;
                //LogUtils.d(TAG, String.format("read data bit high = 0x%02x dataBitValue = %d", inBuffer[0], dataBitValue));

                if (dataBitValue < 0xF8) {
                    retryCount++;
                    continue;
                }

                write_RegSet_with_RecLength(new byte[]{(byte) 0x1C}, 1);

                readBufferThread(inBuffer);
                LogUtils.d(TAG, String.format("interrupt = 0x%02x", inBuffer[0]));

                if (inBuffer[0] != (byte) 0x81) {
                    continue;
                }

                write_RegSet_with_RecLength(new byte[]{(byte) 0xD4}, 2);

                readBufferThread(inBuffer);

                int presentStatus = (inBuffer[0] & 0xFF) | ((inBuffer[1] & 0xFF) << 8);

                bitCount = 0;
                String strPresentStatus = Integer.toBinaryString(presentStatus);
                for (int i = 0; i < strPresentStatus.length(); i++) {
                    if (strPresentStatus.charAt(i) == '1') {
                        bitCount++;
                    }
                }

                //Log.d(TAG, String.format("fingerPresentStatus 0x%02x, 0x%02x, %s, %d", inBuffer[0], inBuffer[1], strPresentStatus, bitCount));

                if (bitCount < 9) {
                    continue;
                } else {
                    detectFinger = true;
                    LogUtils.d(TAG, "Valid finger present, ready to capture image...");
                }

                if (detectFinger) {
                    int status = 0;
                    if (sensorType == SensorType.FPC_1020) {
                        int kFramePixelsPerAdcGroup = fingerprint.kFramePixelsPerAdcGroup;
                        for (int i = 0; i < kFramePixelsPerAdcGroup; i++) {
                            status = runSensorCacProcess();
                            //Log.i(TAG, String.format("cac status = %d", status));

                            if (status > 0) break;
                        }
                    }

                    CaptureImage();

                    boolCaptureImage = false;
                }
            }
        }
    }

    private void CaptureImage() {
        if (sensorType == SensorType.FPC_1020) {
            write_RegSet_with_RecLength(new byte[] { (byte) 0x54, (byte) 0, (byte) 192, (byte) 0, (byte) 192}, 0);
        }

        set_DataBits_HighByte((byte) 0x40, (byte) 0x47);

        fpc1020CaptureImage();

        ReadImageFromSPI();

        if (sensorType == SensorType.FPC_1020) {
            set_DataBits_HighByte((byte) 0x41, (byte) 0x47);
        }
    }

    private void ReadImageFromSPI() {
        int currentIndex = 0;

        Arrays.fill(fullImage, (byte) 0x00);

        while (true) {
            readBufferThread(inBuffer);

            if (mReadSize > 0) {
                System.arraycopy(inBuffer, 0, fullImage, currentIndex, mReadSize);
                currentIndex += mReadSize;
                //Log.i(TAG, "currentIndex: " + currentIndex);
            }

            if (currentIndex == FINGERPRINT_IMAGE_SIZE) break;
        }

        if (sensorType == SensorType.FPC_1020) {
            fingerprint.preprocessor(fullImage);
        }
    }

    private void fpc1020CaptureImage() {
        int retryCount = 0;

        while (boolCaptureImage) {
            if (retryCount >= 10) {
                set_DataBits_HighByte((byte) 0x40, (byte) 0x47);
                set_DataBits_HighByte((byte) 0x41, (byte) 0x47);
                retryCount = 0;
            }
            captureImage_end = true;
            write_RegSet_with_RecLength(new byte[] { (byte) 0xC0}, 0);
            captureImage_end = false;
            captureImage_interrupt_end = true;
            write_RegSet_with_RecLength(new byte[] { (byte) 0x1C}, 1);
            captureImage_interrupt_end = false;

            readBufferThread(inBuffer);

            if (inBuffer[0] != (byte) 0x20) {
                Log.i(TAG, String.format("Image data is not ready!, interrupt is %s", Integer.toBinaryString(inBuffer[0])));
                retryCount++;
                continue;
            }

            if (sensorType == SensorType.FPC_1020) {
                write_RegSet_with_RecLength(new byte[] { (byte) 0xC4, (byte) 0x00}, FINGERPRINT_IMAGE_SIZE);
            }
            break;
        }
    }

    private int runSensorCacProcess() {
        boolean cacInitResult = fingerprint.cacInit();

        if (!cacInitResult) return -1;

        int status;

        int cacRowLength = fingerprint.cacRowLength;
        int cacColLength = fingerprint.cacColLength;
        int kFramePixelsPerAdcGroup = fingerprint.kFramePixelsPerAdcGroup;
        int cacRowStart = fingerprint.cacRowStart;
        int cacColStart = fingerprint.cacColStart;

        int cacImageLength = cacRowLength * cacColLength * kFramePixelsPerAdcGroup;

        cacImageArray = new byte[cacImageLength * 3];

        //Log.i(TAG, String.format("cacRowStart %d, cacRowLength %d, cacColStart %d, cacColLength %d",
                //cacRowStart, cacRowLength, cacColStart, cacColLength));

        do {
            captureCacImage(cacRowStart, cacRowLength, cacColStart, cacColLength);

            status = fingerprint.cacImage(cacImageArray);

            int shiftValue = fingerprint.shiftValue;
            int gainValue = fingerprint.gainValue;
            int px1Value = fingerprint.px1Value;

            byte[] adcShiftGain = new byte[3];
            int index = 0;
            adcShiftGain[index++] = (byte) 0xA0;
            adcShiftGain[index++] = (byte) (shiftValue & 0xFF);
            adcShiftGain[index] = (byte) (gainValue & 0xFF);
            write_RegSet_with_RecLength(adcShiftGain, 0);

            byte[] pxlCtrl = new byte[3];
            index = 0;
            pxlCtrl[index++] = (byte) 0xA8;
            pxlCtrl[index++] = (byte) 0x0F;
            pxlCtrl[index] = (byte) (px1Value & 0xFF);
            write_RegSet_with_RecLength(pxlCtrl, 0);

            //Log.i(TAG, String.format("gainValue %d shiftValue %d px1Value %d", gainValue, shiftValue, px1Value));
        } while (status == 0);

        Log.i(TAG, "cacImageResult complete");
        return status;
    }

    private void runSensorCalibrateProcess() {

        boolean initResult = fingerprint.calibrateInit(sensorType.getValue());

        int cacRowLength = fingerprint.cacRowLength;
        int cacColLength = fingerprint.cacColLength;
        int kFramePixelsPerAdcGroup = fingerprint.kFramePixelsPerAdcGroup;
        int cacRowStart = fingerprint.cacRowStart;
        int cacColStart = fingerprint.cacColStart;

        int cacImageLength = cacRowLength * cacColLength * kFramePixelsPerAdcGroup;

        cacImageArray = new byte[cacImageLength];

        //Log.i(TAG, String.format("cacRowStart %d cacRowLength %d cacColStart %d cacColLength %d", cacRowStart, cacRowLength, cacColStart, cacColLength));

        boolean calibrateImageResult;

        do {
            captureCalibrateImage(cacRowStart, cacRowLength, cacColStart, cacColLength);

            System.arraycopy(readBuffer, 0, cacImageArray, 0, mReadSize);

            calibrateImageResult = fingerprint.calibrateImage(cacImageArray);

            int shiftValue = fingerprint.shiftValue;
            int gainValue = fingerprint.gainValue;

            byte[] adcShiftGain = new byte[3];
            int index = 0;
            adcShiftGain[index++] = (byte) 0xA0;
            adcShiftGain[index++] = (byte) (shiftValue & 0xFF);
            adcShiftGain[index] = (byte) (gainValue & 0xFF);
            write_RegSet_with_RecLength(adcShiftGain, 0);

            write_RegSet_with_RecLength(new byte[] { (byte) 0xA8, (byte) 0x0F, (byte) 0x1A}, 0);

        } while (!calibrateImageResult);

        Log.i(TAG, "runSensorCalibrateProcess complete");
    }

    private void captureCalibrateImage(int rowStart, int rowLength, int colStart, int colLength) {
        int kFramePixelsPerAdcGroup = fingerprint.kFramePixelsPerAdcGroup;

        write_RegSet_with_RecLength(new byte[] { (byte) 0x78, (byte) 0xFF, (byte) 0xFF }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0x8C, (byte) 0x04 }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0x54, (byte) rowStart, (byte) rowLength, (byte) colStart, (byte) (colLength * kFramePixelsPerAdcGroup) }, 0);
        write_RegSet_with_RecLength(new byte[] { (byte) 0xC0 }, 0);

        write_RegSet_with_RecLength(new byte[] { (byte) 0x1C }, 1);

        readBufferThread(inBuffer);

        if (inBuffer[0] != (byte) 0x20) {
            Log.i(TAG, "Image data is not ready !");
        }

        int cacImageLength = rowLength * colLength * kFramePixelsPerAdcGroup;
        //Log.i(TAG, "cacImageLength: " + cacImageLength);

        write_RegSet_with_RecLength(new byte[] { (byte) 0xC4, (byte) 0x00}, cacImageLength);

        readBufferThread(inBuffer);
    }

    private void captureCacImage(int rowStart, int rowLength, int colStart, int colLength) {
        int retryCount = 0;
        boolean needCaptureImage = true;

        setCacMode(true);

        set_DataBits_HighByte((byte) 0x40, (byte) 0x47);

        int kFramePixelsPerAdcGroup = fingerprint.kFramePixelsPerAdcGroup;

        write_RegSet_with_RecLength(new byte[] { (byte) 0x54, (byte) rowStart, (byte) rowLength, (byte) colStart, (byte) (colLength * kFramePixelsPerAdcGroup) }, 0);

        while (needCaptureImage) {
            if (retryCount >= 10) {
                set_DataBits_HighByte((byte) 0x40, (byte) 0x47);
                set_DataBits_HighByte((byte) 0x41, (byte) 0x47);

                retryCount = 0;
            }

            ft_device.purge(D2xxManager.FT_PURGE_RX);

            captureImage_end = true;
            write_RegSet_with_RecLength(new byte[] { (byte) 0xC0}, 0);
            captureImage_end = false;

            captureImage_interrupt_end= true;
            write_RegSet_with_RecLength(new byte[] { (byte) 0x1C}, 1);
            captureImage_interrupt_end = false;

            readBufferThread(inBuffer);

            if (inBuffer[0] != (byte) 0x20) {
                Log.e(TAG, String.format("Image data is not ready!, interrupt is %d %s", inBuffer[0], Integer.toBinaryString(inBuffer[0])));
                retryCount++;
                continue;
            }

            int cacImageLength = rowLength * colLength * kFramePixelsPerAdcGroup;

            if (sensorType == SensorType.FPC_1020) {
                write_RegSet_with_RecLength(new byte[] { (byte) 0xC4, (byte) 0x00}, cacImageLength);
            }

            readBufferThread(inBuffer);

            System.arraycopy(inBuffer, 0, cacImageArray, 0, cacImageLength);

            needCaptureImage = false;

            setCacMode(false);
        }
    }

    private void setCacMode(boolean cacMode) {
        if (cacMode) {
            write_RegSet_with_RecLength(new byte[] { (byte) 0x5C, (byte) 0x0A}, 0);
            write_RegSet_with_RecLength(new byte[] { (byte) 0x64, (byte) 0x0C}, 0);
        } else {
            write_RegSet_with_RecLength(new byte[] { (byte) 0x5C, (byte) 0x0B}, 0);
            write_RegSet_with_RecLength(new byte[] { (byte) 0x64, (byte) 0x0E}, 0);
        }
    }

    private void send_FPC_Setting(boolean flag) {

        int index = 0;
        int dataTransferred;

        byte[] buffer = new byte[10];

        // Disable Setting
        buffer[index++] = (byte) 0x97;
        buffer[index++] = (byte) 0x85;
        buffer[index++] = (byte) 0x8A;
        buffer[index++] = (byte) 0x8D;

        buffer[index++] = (byte) 0x86;
        if (flag) {
            buffer[index++] = (byte) 0x02;
        } else {
            buffer[index++] = (byte) 0x04;
        }
        buffer[index++] = (byte) 0x00;

        // Trigger CS
        buffer[index++] = (byte) 0x80;
        buffer[index++] = (byte) 0x08;
        buffer[index++] = (byte) 0x0B;

        dataTransferred = ft_device.write(buffer, index);

        if (debug) {
            String str1 = "";

            for (int a = 0; a < dataTransferred; a++) {
                str1 += String.format("%02x ", buffer[a]);
            }

            Log.w(TAG, "dataTransferred (" + dataTransferred + ") -> " + str1);
        }

        if (flag) {
            set_DataBits_HighByte((byte) 0x00, (byte) 0x40);
            set_DataBits_HighByte((byte) 0x40, (byte) 0x40);
        } else {
            set_DataBits_HighByte((byte) 0x40, (byte) 0x47);
        }
    }

    private void write_RegSet_with_RecLength(byte[] regSetCmd, int receiveLength) {

        int index = 0;
        int dataTransferred;

        byte[] buffer = new byte[18];
        byte[] cmdLength = new byte[2];
        byte[] receLength = new byte[2];

        int regSetCmdLength = regSetCmd.length;

        cmdLength[0] = (byte)((regSetCmdLength - 1) & 0xFF);
        cmdLength[1] = (byte)(((regSetCmdLength - 1) >> 8) & 0xFF);

        if (fpc1020Setting_interrupt_header) {
            buffer[index++] = (byte) 0x88;
            fpc1020Setting_interrupt_header = false;
        }

        // TriggerCSCode
        buffer[index++] = (byte) 0x80;
        buffer[index++] = (byte) 0x00;
        buffer[index++] = (byte) 0x0B;

        // SendCmdLengthCode
        buffer[index++] = (byte) 0x11;
        buffer[index++] = cmdLength[0];
        buffer[index++] = cmdLength[1];

        // RegSetCmd
        for (byte aRegSetCmd : regSetCmd) {
            buffer[index++] = aRegSetCmd;
        }

        // ReceiveCmdLengthCode
        if (receiveLength > 0) {
            receLength[0] = (byte) ((receiveLength - 1) & 0xFF);
            receLength[1] = (byte) (((receiveLength - 1) >> 8) & 0xFF);

            buffer[index++] = (byte) 0x20;
            buffer[index++] = receLength[0];
            buffer[index++] = receLength[1];
        }

        // TriggerCSCode
        buffer[index++] = (byte) 0x80;
        buffer[index++] = (byte) 0x08;
        buffer[index++] = (byte) 0x0B;

        if (captureImage_end) {
            buffer[index++] = (byte) 0x88;
            captureImage_end = false;
        }

        if (captureImage_interrupt_end) {
            buffer[index++] = (byte) 0x87;
            captureImage_interrupt_end = false;
        }

        dataTransferred = ft_device.write(buffer, index);

        if (debug) {
            String str = "";

            for (int a = 0; a < dataTransferred; a++) {
                str += String.format("%02x ", buffer[a]);
            }

            Log.w(TAG, "dataTransferred (" + dataTransferred + ") -> " + str);
        }
    }

    private void set_DataBits_HighByte(byte value, byte dir) {

        int dataTransferred;

        byte[] buffer = new byte[3];

        buffer[0] = (byte) 0x82;
        buffer[1] = value;
        buffer[2] = dir;

        dataTransferred = ft_device.write(buffer, buffer.length);

        if (debug) {
            String str1 = "";

            for (byte aBuffer : buffer) {
                str1 += String.format("%02x ", aBuffer);
            }

            Log.w(TAG, "dataTransferred (" + dataTransferred + ") -> " + str1);
        }
    }

    private void send_detect_finger_Command(byte[] detectFingerCmd) {

        int index = 0;
        int dataTransferred;

        byte[] buffer = new byte[2];

        buffer[index++] = detectFingerCmd[0];
        buffer[index++] = detectFingerCmd[1];

        dataTransferred = ft_device.write(buffer, index);

        if (debug) {
            String str = "";

            for (int a = 0; a < dataTransferred; a++) {
                str += String.format("%02x ", buffer[a]);
            }

            Log.w(TAG, "dataTransferred (" + dataTransferred + ") -> " + str);
        }
    }

    public void emptyQueueThread() {

        emptyQueue_thread = new emptyQueueThread();
        emptyQueue_thread.start();

        try {
            emptyQueue_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void readBufferThread(byte[] buffer) {

        readBuffer_thread = new readBufferThread();
        readBuffer_thread.start();

        try {
            readBuffer_thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.arraycopy(readBuffer, 0, buffer, 0, mReadSize);
    }

    private class readBufferThread extends Thread {
        @Override
        public void run() {
            int readSize;

            while (true) {
                synchronized (ft_device) {
                    readSize = ft_device.getQueueStatus();
                    if (readSize > 0) {
                        //Log.w(TAG, "readSize: " + readSize);
                        mReadSize = readSize;
                        if (mReadSize > readBufferSize) {
                            mReadSize = readBufferSize;
                        }

                        ft_device.read(readBuffer, mReadSize, 500);

                        if (debug) {
                            String str = "";

                            for (int i = 0; i < readSize; i++) {
                                str += String.format("%02x ", readBuffer[i]);
                            }
                            Log.i(TAG, str);
                        }
                        break;
                    }
                }
            }
        }
    }

    private class emptyQueueThread extends Thread {
        @Override
        public void run() {

            int readSize;

            synchronized (ft_device) {
                readSize = ft_device.getQueueStatus();
                //Log.i(TAG, "xxxx: " + readSize);
                mReadSize = readSize;
            }
        }
    }

}
