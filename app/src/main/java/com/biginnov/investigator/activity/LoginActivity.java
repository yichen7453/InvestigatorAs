package com.biginnov.investigator.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.BiometricFragment;
import com.biginnov.investigator.fragment.FaceCaptureFragment;
import com.biginnov.investigator.fragment.FingerprintCaptureFragment;
import com.biginnov.investigator.provider.ContentHelper;
import com.biginnov.investigator.provider.dto.Admin;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.LicenseManager;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.SecureUtils;
import com.ftdi.j2xx.D2xxManager;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.lang.NCore;
import com.neurotec.util.concurrent.CompletionHandler;

import java.util.EnumSet;

public class LoginActivity extends BiometricActivity implements View.OnClickListener {
    private static final String TAG = LoginActivity.class.getSimpleName();

    private static final String DEFAULT_QUERY_STRING =
            BiographicData.ROLE + " = " + BiographicData.ROLE_ADMINISTRATOR;

    private EditText mInputAccount;
    private EditText mInputPassword;
    private View mButtonFace;
    private View mButtonFingerprint;
    private View mLayout;

    private ContentHelper mContentHelper;
    private boolean mLoggedIn;

    private D2xxManager d2xxManager = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        NCore.setContext(this);
        setContentView(R.layout.activity_login);
        findViews();
        mContentHelper = new ContentHelper(this);
    }

    private void findViews() {
        mInputAccount = (EditText) findViewById(R.id.input_account);
        mInputPassword = (EditText) findViewById(R.id.input_password);
        mButtonFace = findViewById(R.id.action_face_capture);
        mLayout = findViewById(R.id.layout);
        mLayout.setOnClickListener(this);
        mButtonFace.setOnClickListener(this);
        mButtonFingerprint = findViewById(R.id.action_fingerprint_capture);
        mButtonFingerprint.setOnClickListener(this);
        findViewById(R.id.action_login).setOnClickListener(this);
        if (mInitCompleted) {
            onInitCompleted();
        }
    }

    @Override
    protected void onDestroy() {
        if (!mLoggedIn) {
            LicenseManager.release();
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (!stopCapture()) {
            super.onBackPressed();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_face_capture:
                Fragment fragment = FaceCaptureFragment.getInstance(
                        new FaceCaptureFragment.SpecBuilder(CAPTURE_REQUEST_FACE)
                                .setCameraSelection(FaceCaptureFragment.CAMERA_SELECTION_FRONT)
                                .setCompleteAfterCapture()
                                .setToolbarVisible()
                                .setSkipSavingTempImage()
                                .build());
                mFragmentManager.beginTransaction().add(
                        R.id.fragment_container, fragment).commitAllowingStateLoss();
                break;

            case R.id.action_fingerprint_capture:
                if (getDevCount(getD2xxManager()) > 0) {
                    fragment = FingerprintCaptureFragment.getInstance(
                            new FaceCaptureFragment.SpecBuilder(CAPTURE_REQUEST_FINGERPRINT)
                                    .setCompleteAfterCapture()
                                    .setToolbarVisible()
                                    .setSkipSavingTempImage()
                                    .build());
                    mFragmentManager.beginTransaction().add(
                            R.id.fragment_container, fragment).commitAllowingStateLoss();
                } else {
                    showToastOnUiThread(getString(R.string.error_finger_device_not_found));
                }
                break;
            case R.id.action_login:
                login();
                break;

            case R.id.layout:
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(mInputAccount.getWindowToken(), 0);
                keyboard.hideSoftInputFromWindow(mInputPassword.getWindowToken(), 0);
                break;
        }
    }

    @Override
    public void onInitCompleted() {
        mButtonFace.setEnabled(true);
        mButtonFingerprint.setEnabled(true);
    }

    private static final int CAPTURE_REQUEST_FACE           = 1;
    private static final int CAPTURE_REQUEST_FINGERPRINT    = 2;

    @Override
    public void onCaptureCompleted(int requestId) {
        LogUtils.d(TAG, "onCaptureCompleted: ", requestId);
        // Currently same behavior for face and fingerprint
        mSubject.setQueryString(DEFAULT_QUERY_STRING);
        NBiometricTask task = mBiometricClient.createTask(EnumSet.of(NBiometricOperation.IDENTIFY), mSubject);
        mBiometricClient.performTask(task, NBiometricOperation.IDENTIFY, mBiometricTaskHandler);
        showProgressDialog(false);
    }

    @Override
    public void onCaptureCancelled(int requestId) {
        LogUtils.d(TAG, "onCaptureCancelled: ", requestId);
        // Currently same behavior for face and fingerprint
        removeCurrentFragment();
    }

    private void login() {
        CharSequence accountInput = mInputAccount.getText();
        if (TextUtils.isEmpty(accountInput)) {
            mInputAccount.setError(getString(R.string.error_empty_account));
            return;
        }
        CharSequence passwordInput = mInputPassword.getText();
        if (TextUtils.isEmpty(passwordInput)) {
            mInputPassword.setError(getString(R.string.error_empty_password));
            return;
        }
        String account = accountInput.toString();
        String password = passwordInput.toString();
        Admin admin = mContentHelper.getAdmin(account);
        if (admin != null) {
            if (SecureUtils.encrypt(this, password).equals(admin.getPassword())) {
                // Login successfully
                launchMainActivity();
            } else {
                showToast(getString(R.string.error_invalid_account_password));
            }
            return;
        }
        if (!mContentHelper.hasAdmin()) {
            LogUtils.d(TAG, "No admin");
            if (Constants.DEFAULT_ADMIN_ACCOUNT.equals(account) &&
                    Constants.DEFAULT_ADMIN_PASSWORD.equals(password)) {
                // Login successfully
                launchMainActivity();
                return;
            } else {
                LogUtils.critical(TAG, "account: ", account);
                LogUtils.critical(TAG, "password: ", password);
                LogUtils.d(TAG, "Invalid account or password");
            }
        }
        showToast(getString(R.string.error_invalid_account_password));
    }

    private void launchMainActivity() {
        mLoggedIn = true;
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private CompletionHandler<NBiometricTask, NBiometricOperation> mBiometricTaskHandler =
            new CompletionHandler<NBiometricTask, NBiometricOperation>() {

                @Override
                public void completed(NBiometricTask task, NBiometricOperation operation) {
                    if (isFinishing()) return;
                    dismissProgressDialog();

                    NBiometricStatus status = task.getStatus();
                    LogUtils.d(TAG, "Operation: ", operation, "; Status: ", status);
                    if (status == NBiometricStatus.CANCELED) {
                        handleOperationFailed(null);
                        return;
                    }

                    // Check error
                    try {
                        Throwable error = task.getError();
                        if (error != null) {
                            handleOperationFailed();
                            LogUtils.e(TAG, "Error: ", error);
                            return;
                        }
                    } catch (Exception e) {
                        handleOperationFailed();
                        LogUtils.e(TAG, e.getMessage(), e);
                        return;
                    }

                    switch (operation) {
                        case IDENTIFY:
                            if (status == NBiometricStatus.OK) {
                                launchMainActivity();
                                return;
                            }
                            handleOperationFailed(getString(R.string.error_identification_failed));
                            break;

                        default:
                            LogUtils.d(TAG, "Invalid operation");
                            break;
                    }
                }

                @Override
                public void failed(Throwable th, NBiometricOperation operation) {
                    dismissProgressDialog();
                    handleOperationFailed();
                    LogUtils.e(TAG, th.getMessage(), th);
                }
            };

    private void handleOperationFailed() {
        handleOperationFailed(getString(R.string.error_operation_failed));
    }

    private void handleOperationFailed(String message) {
        if (!TextUtils.isEmpty(message)) {
            showToastOnUiThread(message);
        }
        mSubject.getFaces().clear();
        mSubject.getFingers().clear();
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment instanceof BiometricFragment) {
            ((BiometricFragment) fragment).startCapture();
        } else {
            removeCurrentFragment();
        }
    }
}
