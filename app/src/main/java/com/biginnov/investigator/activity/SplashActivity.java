package com.biginnov.investigator.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.widget.TextView;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.CommonDialogFragment;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.CommonUtils;
import com.biginnov.investigator.util.LicenseManager;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.lang.NCore;
import com.neurotec.plugins.NDataFileManager;
import com.neurotec.util.concurrent.CompletionHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class SplashActivity extends BaseActivity
        implements CommonDialogFragment.ActionListener, EasyPermissions.PermissionCallbacks {
    private static final String TAG = SplashActivity.class.getSimpleName();

    private static final int STATE_NOT_INITIALIZED          = 0;
    private static final int STATE_OBTAINING_LICENSES       = 1;
    private static final int STATE_LICENCES_OBTAINED        = 2;
    private static final int STATE_LICENCES_NOT_OBTAINED    = 3;
    private static final int STATE_INITIALIZING_CLIENT      = 4;
    private static final int STATE_CLIENT_INITIALIZED       = 5;
    private static final int STATE_CLIENT_NOT_INITIALIZED   = 6;

    private static final int GET_PERMISSION_CODE            = 123;

    private static final String[] getPermissionArray = {
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.READ_PHONE_STATE,
                                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                            Manifest.permission.RECORD_AUDIO };

    private AtomicInteger mState = new AtomicInteger(STATE_NOT_INITIALIZED);
    private TextView mStateText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NCore.setContext(this);
        Fresco.initialize(this);
        setContentView(R.layout.activity_splash);

        requestPermission();

        mStateText = (TextView) findViewById(R.id.state);
    }

    @Override
    protected void onResume() {
        super.onResume();
        /*
        if (CommonUtils.isNetworkConnected(this)) {
            onNetworkConnected();
        } else {
            onNetworkDisconnected();
        }
        */
    }

    @Override
    protected void onPause() {
        unregisterNetworkChangedReceiver();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        int state = mState.get();
        if (state != STATE_CLIENT_NOT_INITIALIZED // Release in ObtainLicensesTask onPostExecute
                && state != STATE_CLIENT_INITIALIZED) { // Launching main, unnecessary to release
            LicenseManager.release();
        }
        super.onDestroy();
    }

    private void obtainLicenses() {
        if (mState.get() == STATE_NOT_INITIALIZED) {
            mState.set(STATE_OBTAINING_LICENSES);
            LogUtils.d(TAG, "Obtaining licenses");
            CommonUtils.executeAsyncTask(new ObtainLicensesTask());
        }
    }

    private void initializeClient() {
        if (mState.get() == STATE_LICENCES_OBTAINED) {
            mState.set(STATE_INITIALIZING_CLIENT);
            mStateText.setText(R.string.splash_state_initializing_client);
            LogUtils.d(TAG, "Initializing client");
            CommonUtils.executeAsyncTask(new InitializeClientTask());
        }
    }

    @AfterPermissionGranted(GET_PERMISSION_CODE)
    private void requestPermission() {
        if (EasyPermissions.hasPermissions(this, getPermissionArray)) {
            Log.i(TAG, "has permission");
            NCore.setContext(this);
            if (CommonUtils.isNetworkConnected(this)) {
                onNetworkConnected();
            } else {
                onNetworkDisconnected();
            }
        } else {
            Log.i(TAG, "request permission");
            EasyPermissions.requestPermissions(this, "Get permission", GET_PERMISSION_CODE, getPermissionArray);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Log.i(TAG, "onRequestPermissionsResult");
        /*
        if (requestCode == GET_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                NCore.setContext(this);
                if (CommonUtils.isNetworkConnected(this)) {
                    onNetworkConnected();
                } else {
                    onNetworkDisconnected();
                }
            }
        }
        */
        // EasyPermissions handles the request result.
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        LogUtils.d(TAG, "onPermissionsGranted:" + requestCode + ":" + perms.size());
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        LogUtils.d(TAG, "onPermissionsDenied:" + requestCode + ":" + perms.size());
        // (Optional) Check whether the user denied any permissions and checked "NEVER ASK AGAIN."
        // This will display a dialog directing them to enable the permission in app settings.
        if (EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }

    private class ObtainLicensesTask extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            StorageUtils.ensureBiometricDir(SplashActivity.this);
            loadNdfFiles();
            return LicenseManager.getInstance().obtain(
                    SplashActivity.this, Constants.LICENSE_LIST);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!isFinishing()) {
                LogUtils.d(TAG, "Obtain licenses result: ", result);
                if (result) {
                    mState.set(STATE_LICENCES_OBTAINED);
                    initializeClient();
                } else {
                    mState.set(STATE_LICENCES_NOT_OBTAINED);
                    mStateText.setText(R.string.splash_state_licenses_not_obtained);
                }
            } else { // User press back key to exit
                LicenseManager.release();
            }
        }
    }

    private class InitializeClientTask extends AsyncTask<Object, Object, Boolean> {

        @Override
        protected Boolean doInBackground(Object... params) {
            boolean result = false;
            try {
                NBiometricClient biometricClient = new NBiometricClient();
                biometricClient.setBiographicDataSchema(BiographicData.getSchema());
                biometricClient.setDatabaseConnectionToSQLite(CommonUtils.combinePath(
                        NCore.getContext().getFilesDir().getAbsolutePath(), "Biometrics.db"));
                biometricClient.setUseDeviceManager(true);
                NDeviceManager deviceManager = biometricClient.getDeviceManager();
                // set type of the device used
                deviceManager.setDeviceTypes(EnumSet.copyOf(Constants.DEVICE_TYPE_LIST));
                if (!isFinishing()) {
                    biometricClient.initialize();
                }
                biometricClient.list(NBiometricOperation.LIST, subjectListHandler);
                result = true;
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!isFinishing()) {
                if (result) {
                    mState.set(STATE_CLIENT_INITIALIZED);
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                } else {
                    mState.set(STATE_CLIENT_NOT_INITIALIZED);
                    mStateText.setText(R.string.splash_state_client_not_initialized);
                }
            }
        }
    }

    private CompletionHandler<NSubject[], ? super NBiometricOperation> subjectListHandler = new CompletionHandler<NSubject[], NBiometricOperation>() {
        @Override
        public void completed(NSubject[] nSubjects, NBiometricOperation nBiometricOperation) {
            Log.i(TAG, "result -> " + nSubjects.length);
        }

        @Override
        public void failed(Throwable throwable, NBiometricOperation nBiometricOperation) {
            Log.e(TAG, throwable.toString(), throwable);
        }
    };

    private static final int DIALOG_REQUEST_ID_NO_NETWORK = 1;

    @Override
    public void onPositiveButtonClick(int requestId) {
        if (requestId == DIALOG_REQUEST_ID_NO_NETWORK) {
            CommonUtils.openWifiSetting(this);
        }
    }

    @Override
    public void onNegativeButtonClick(int requestId) {
        if (requestId == DIALOG_REQUEST_ID_NO_NETWORK) {
            finish();
        }
    }

    @Override
    public void onNeutralButtonClick(int requestId) {
        // Not called
    }

    @Override
    public void onCancel(int requestId) {
        if (requestId == DIALOG_REQUEST_ID_NO_NETWORK) {
            finish();
        }
    }

    @Override
    protected void onNetworkConnected() {
        dismissNoNetworkDialog();
        obtainLicenses();
        unregisterNetworkChangedReceiver();
    }

    @Override
    protected void onNetworkDisconnected() {
        showNoNetworkDialog(DIALOG_REQUEST_ID_NO_NETWORK);
        registerNetworkChangedReceiver(false);
    }

    private static final String[] NDF_FILE_NAMES = new String[] {
            "FacesCreateTemplateLarge.ndf",
            "FacesCreateTemplateLargeLite.ndf",
            "FacesCreateTemplateMedium.ndf",
            "FacesCreateTemplateMediumLite.ndf",
            "FacesCreateTemplateSmall.ndf",
            "FacesDetect45.ndf",
            "FacesDetect180.ndf",
            "FacesDetectSegmentsAge.ndf",
            "FacesDetectSegmentsAttributes.ndf",
            "FacesDetectSegmentsEmotions.ndf",
            "FacesDetectSegmentsFeaturePointsTrack.ndf",
            "FacesDetectSegmentsLiveness.ndf",
            "FacesDetectSegmentsOrientation.ndf"
    };

    private void loadNdfFiles() {
        String dirPath = getNdfDirectoryPath(this);
        if (!TextUtils.isEmpty(dirPath)) {
            File dir = new File(dirPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            AssetManager assetManager = getAssets();
            for (String name : NDF_FILE_NAMES) {
                File file = new File(dirPath, name);
                if (!file.exists()) {
                    writeAssetToFile(assetManager, name, file);
                }
            }
            NDataFileManager.getInstance().addFromDirectory(dirPath, false);
        }
    }

    private static final String NDF_DIRECTORY = "ndf";

    private static String getNdfDirectoryPath(Context context) {
        String path = null;
        File external = context.getExternalFilesDir(null);
        if (external != null) {
            path = external.getAbsolutePath() + File.separator + NDF_DIRECTORY;
        }
        LogUtils.critical(TAG, "Ndf dir path: ", path);
        return path;
    }

    private static String getNdfFilePath(Context context, String fileName) {
        String path = null;
        String dir = getNdfDirectoryPath(context);
        if (!TextUtils.isEmpty(dir)) {
            path = dir + File.separator + fileName;
        }
        LogUtils.critical(TAG, "File path: ", path);
        return path;
    }

    private static boolean writeAssetToFile(
            AssetManager assetManager, String assetName, File target) {
        boolean success = false;
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetName);
            out = new FileOutputStream(target);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            success = true;
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
        }
        LogUtils.d(TAG, "Write asset: ", success);
        return success;
    }
}
