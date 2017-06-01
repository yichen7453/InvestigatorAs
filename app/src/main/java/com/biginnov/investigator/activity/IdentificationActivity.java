package com.biginnov.investigator.activity;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.FaceCaptureFragment;
import com.biginnov.investigator.fragment.FingerprintCaptureFragment;
import com.biginnov.investigator.fragment.IdentifyingFragment;
import com.biginnov.investigator.fragment.MatchingResultDetailFragment;
import com.biginnov.investigator.fragment.MatchingResultListFragment;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.CommonUtils;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.view.SimpleDraweeView;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.util.concurrent.CompletionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicInteger;

public class IdentificationActivity extends BiometricActivity implements View.OnClickListener {
    private static final String TAG = IdentificationActivity.class.getSimpleName();

    private static final int STATE_METHOD_SELECTION = 0;
    private static final int STATE_FACE_CAPTURE = 1;
    private static final int STATE_FINGERPRINT_CAPTURE = 2;
    private static final int STATE_PREPARING_IDENTIFICATION = 3;
    private static final int STATE_IDENTIFYING = 4;
    private static final int STATE_PARSING_MATCHING_RESULTS = 5;
    private static final int STATE_MATCHING_RESULT_LIST = 6;
    private static final int STATE_MATCHING_RESULT_DETAIL = 7;

    private static final String DEFAULT_QUERY_STRING =
            BiographicData.ROLE + " <> " + BiographicData.ROLE_ADMINISTRATOR;

    private SimpleDraweeView mDraweeFace;
    private View mHintFace;
    private View mButtonFaceRemoval;
    private View mIndicatorFace;

    private SimpleDraweeView mDraweeFingerprint;
    private View mHintFingerprint;
    private View mButtonFingerprintRemoval;
    private View mIndicatorFingerprint;
    private TextView mCountFingerprint;

    private Spinner mSpinnerAge;
    private Spinner mSpinnerSex;

    private ArrayList<NSubject> mMatchingSubjects;
    private AtomicInteger mState = new AtomicInteger(STATE_METHOD_SELECTION);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_identification);
        findViews();
        mMatchingSubjects = new ArrayList<>();
        CommonUtils.executeAsyncTask(new ClearTempFaceImageTask());
    }

    private void findViews() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Face
        mDraweeFace = (SimpleDraweeView) findViewById(R.id.action_face_capture);
        mDraweeFace.setOnClickListener(this);
        mHintFace = findViewById(R.id.hint_face_capture);
        mButtonFaceRemoval = findViewById(R.id.action_face_removal);
        mButtonFaceRemoval.setOnClickListener(this);
        mIndicatorFace = findViewById(R.id.indicator_face);

        // Fingerprint
        mDraweeFingerprint = (SimpleDraweeView) findViewById(R.id.action_fingerprint_capture);
        mDraweeFingerprint.setOnClickListener(this);
        mHintFingerprint = findViewById(R.id.hint_fingerprint_capture);
        mButtonFingerprintRemoval = findViewById(R.id.action_fingerprint_removal);
        mButtonFingerprintRemoval.setOnClickListener(this);
        mIndicatorFingerprint = findViewById(R.id.indicator_fingerprint);
        mCountFingerprint = (TextView) findViewById(R.id.count_fingerprint);

        // Sex
        mSpinnerSex = (Spinner) findViewById(R.id.selector_sex);
        ArrayAdapter<CharSequence> mSexAdapter = ArrayAdapter.createFromResource(
                this, R.array.sex_selection, android.R.layout.simple_spinner_item);
        mSexAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerSex.setAdapter(mSexAdapter);

        // Age
        mSpinnerAge = (Spinner) findViewById(R.id.selector_age);
        ArrayList<String> ageList = new ArrayList<>();
        ageList.add(getString(R.string.text_age));
        for (int i = Constants.SPINNER_AGE_START; i < Constants.SPINNER_AGE_END; i++) {
            ageList.add(String.valueOf(i));
        }
        ArrayAdapter<String> mAgeAdapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, ageList);
        mSpinnerAge.setAdapter(mAgeAdapter);

        if (mInitCompleted) {
            onInitCompleted();
        }
    }

    @Override
    protected void onDestroy() {
        mHandler.removeCallbacks(mIdentificationRunnable);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        finishCurrentOperation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.identification, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (mState.get() != STATE_METHOD_SELECTION) {
            menu.findItem(R.id.action_done).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case android.R.id.home:
                finishCurrentOperation();
                result = true;
                break;

            case R.id.action_done:
                if (allowIdentification()) {
                    mState.set(STATE_PREPARING_IDENTIFICATION);
                    invalidateOptionsMenu();
                    Fragment fragment = IdentifyingFragment.getInstance();
                    mFragmentManager.beginTransaction().add(
                            R.id.fragment_container, fragment).commitAllowingStateLoss();
                    mMatchingSubjects.clear();
                    applyQueryString();
                    // To avoid identifying fragment flash
                    mHandler.postDelayed(mIdentificationRunnable, 300);
                } else {
                    Toast.makeText(this, R.string.error_no_identification_method,
                            Toast.LENGTH_SHORT).show();
                }
                break;
        }
        if (!result) {
            result = super.onOptionsItemSelected(item);
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_face_capture:
                if (mInitCompleted) {
                    mState.set(STATE_FACE_CAPTURE);
                    int targetImageSize = getResources().getDimensionPixelSize(
                            R.dimen.common_face_image_size);
                    Fragment fragment = FaceCaptureFragment.getInstance(
                            new FaceCaptureFragment.SpecBuilder(REQUEST_FACE_CAPTURE)
                                    .setTempImageSize(targetImageSize)
                                    .build());
                    mFragmentManager.beginTransaction()
                            .add(R.id.fragment_container, fragment)
                            .commitAllowingStateLoss();
                }
                break;

            case R.id.action_fingerprint_capture:
                if (mInitCompleted) {
                    if (getDevCount(getD2xxManager()) > 0) {
                        mState.set(STATE_FINGERPRINT_CAPTURE);
                        Fragment fragment = FingerprintCaptureFragment.getInstance(
                                REQUEST_FINGERPRINT_CAPTURE);
                        mFragmentManager.beginTransaction()
                                .add(R.id.fragment_container, fragment)
                                .commitAllowingStateLoss();
                        invalidateOptionsMenu();
                    } else {
                        showToastOnUiThread(getString(R.string.error_finger_device_not_found));
                    }
                }
                break;

            case R.id.action_face_removal:
                CommonUtils.executeAsyncTask(new ClearTempFaceImageTask());
                getSubject().getFaces().clear();
                setFaceReady(false);
                break;

            case R.id.action_fingerprint_removal:
                getSubject().getFingers().clear();
                mDraweeFingerprint.setImageURI(null);
                setFingerprintReady(0);
                break;
        }
    }

    @Override
    public void onInitCompleted() {
        // Do nothing
    }

    private static final int REQUEST_FACE_CAPTURE = 1;
    private static final int REQUEST_FINGERPRINT_CAPTURE = 2;

    @Override
    public void onCaptureCompleted(int requestId) {
        LogUtils.d(TAG, "onCaptureCompleted: ", requestId);
        // Currently same behavior for face and fingerprint
        switch (requestId) {
            case REQUEST_FACE_CAPTURE:
                NSubject.FaceCollection faces = mSubject.getFaces();
                LogUtils.d(TAG, "face size: " + faces.size());
                if (faces.size() > 1) {
                    faces.remove(0);
                }
                removeCurrentFragment();
                setFaceReady(true);
                break;

            case REQUEST_FINGERPRINT_CAPTURE:
                removeCurrentFragment();
                setFingerprintReady(mSubject.getFingers().size());
                break;
        }
        mState.set(STATE_METHOD_SELECTION);
        invalidateOptionsMenu();
    }

    @Override
    public void onCaptureCancelled(int requestId) {
        LogUtils.d(TAG, "onCaptureCancelled: ", requestId);
        removeCurrentFragment();
        if (requestId == REQUEST_FINGERPRINT_CAPTURE) { // Maybe user remove fingerprints and back
            setFingerprintReady(mSubject.getFingers().size());
        }
        mState.set(STATE_METHOD_SELECTION);
        invalidateOptionsMenu();
    }

    public void cancelIdentification() {
        finishCurrentOperation();
    }

    public ArrayList<NSubject> getMatchingSubjects() {
        return mMatchingSubjects;
    }

    public NSubject getMatchingSubject(int position) {
        NSubject subject = null;
        if (position < mMatchingSubjects.size()) {
            subject = mMatchingSubjects.get(position);
        }
        return subject;
    }

    public void showMatchingResultDetail(int position) {
        mState.set(STATE_MATCHING_RESULT_DETAIL);
        Fragment fragment = MatchingResultDetailFragment.getInstance(position);
        mFragmentManager.beginTransaction()
                .add(R.id.fragment_container, fragment)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    private boolean allowIdentification() {
        return !mSubject.getFaces().isEmpty() || !mSubject.getFingers().isEmpty();
    }

    private void applyQueryString() {
        StringBuilder queryString = new StringBuilder(DEFAULT_QUERY_STRING);
        int position = mSpinnerSex.getSelectedItemPosition();
        if (position != 0) {
            queryString.append(" AND ").append(BiographicData.SEX)
                    .append(" = ").append(position - 1);
        }
        position = mSpinnerAge.getSelectedItemPosition();
        if (position != 0) {
            queryString.append(" AND ").append(BiographicData.AGE)
                    .append(" = ").append(Constants.SPINNER_AGE_START + position - 1);
        }
        LogUtils.critical(TAG, "queryString: ", queryString);
        mSubject.setQueryString(queryString.toString());
    }

    private void finishCurrentOperation() {
        switch (mState.get()) {
            case STATE_METHOD_SELECTION:
                finish();
                break;

            case STATE_FACE_CAPTURE:
                LogUtils.d(TAG, "STATE_FACE_CAPTURE");
            case STATE_FINGERPRINT_CAPTURE:
                stopCapture();
                break;

            case STATE_PREPARING_IDENTIFICATION:
                mHandler.removeCallbacks(mIdentificationRunnable);
                removeCurrentFragment();
                mState.set(STATE_METHOD_SELECTION);
                break;

            case STATE_IDENTIFYING:
                mBiometricClient.cancel();
                mState.set(STATE_METHOD_SELECTION);
                break;

            case STATE_PARSING_MATCHING_RESULTS:
                removeCurrentFragment();
                mState.set(STATE_METHOD_SELECTION);
                break;

            case STATE_MATCHING_RESULT_LIST:
                removeCurrentFragment();
                // mFragmentManager.popBackStack();
                mState.set(STATE_METHOD_SELECTION);
                break;

            case STATE_MATCHING_RESULT_DETAIL:
                mFragmentManager.popBackStack();
                mState.set(STATE_MATCHING_RESULT_LIST);
                break;
        }
        if (!isFinishing()) {
            invalidateOptionsMenu();
        }
    }

    private CompletionHandler<NBiometricTask, NBiometricOperation> mBiometricTaskHandler =
            new CompletionHandler<NBiometricTask, NBiometricOperation>() {

                @Override
                public void completed(NBiometricTask task, NBiometricOperation operation) {
                    if (isFinishing()) return;
                    // removeCurrentFragment(); // Identifying fragment

                    NBiometricStatus status = task.getStatus();
                    LogUtils.d(TAG, "Operation: ", operation, "; Status: ", status);
                    if (status == NBiometricStatus.CANCELED) {
                        removeCurrentFragment(); // Identifying fragment
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
                                mState.set(STATE_PARSING_MATCHING_RESULTS);
                                // Must to parse in another thread
                                new ParseMatchingResultsThread().start();
                                return;
                            } else if (status == NBiometricStatus.MATCH_NOT_FOUND) {
                                showToastOnUiThread(getString(R.string.error_no_matching_result));
                                removeCurrentFragment(); // Identifying fragment
                                mState.set(STATE_METHOD_SELECTION);
                                invalidateOptionsMenu();
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
                    handleOperationFailed();
                    LogUtils.e(TAG, th.getMessage(), th);
                }
            };

    private void handleOperationFailed() {
        handleOperationFailed(getString(R.string.error_identification_failed));
    }

    private void handleOperationFailed(String message) {
        if (!TextUtils.isEmpty(message)) {
            showToastOnUiThread(message);
        }
        removeCurrentFragment();
        mState.set(STATE_METHOD_SELECTION);
        invalidateOptionsMenu();
    }

    private class ParseMatchingResultsThread extends Thread {
        @Override
        public void run() {
            LogUtils.d(TAG, "Parsing matching results");
            for (NMatchingResult result : mSubject.getMatchingResults()) {
                NSubject subject = new NSubject();
                subject.setId(result.getId());
                mBiometricClient.get(subject);
                mMatchingSubjects.add(subject);
                if (mState.get() != STATE_PARSING_MATCHING_RESULTS) {
                    return; // Cancelled during parsing
                }
            }
            if (mState.get() != STATE_PARSING_MATCHING_RESULTS) {
                return; // Cancelled during parsing
            }
            // removeCurrentFragment(); // Identifying fragment
            mState.set(STATE_MATCHING_RESULT_LIST);
            MatchingResultListFragment fragment = MatchingResultListFragment.getInstance();
            mFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commitAllowingStateLoss();
        }
    }

    private void setFaceReady(boolean ready) {
        mHintFace.setVisibility(ready ? View.GONE : View.VISIBLE);
        mButtonFaceRemoval.setVisibility(ready ? View.VISIBLE : View.GONE);
        mIndicatorFace.setVisibility(ready ? View.VISIBLE : View.GONE);
        mDraweeFace.setImageURI(ready ?
                Uri.fromFile(StorageUtils.getTempFaceImageFile()) : null);
    }

    private void setFingerprintReady(int count) {
        boolean ready = count > 0;
        // mHintFingerprint.setVisibility(ready ? View.GONE : View.VISIBLE);
        mButtonFingerprintRemoval.setVisibility(ready ? View.VISIBLE : View.GONE);
        mIndicatorFingerprint.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (count > 0) {
            mCountFingerprint.setText("x" + count);
            mCountFingerprint.setVisibility(View.VISIBLE);
        } else {
            mCountFingerprint.setVisibility(View.GONE);
        }
    }

    private Runnable mIdentificationRunnable = new Runnable() {
        @Override
        public void run() {
            mState.set(STATE_IDENTIFYING);
            NBiometricTask task = mBiometricClient.createTask(EnumSet.of(NBiometricOperation.IDENTIFY), mSubject);
            mBiometricClient.performTask(task, NBiometricOperation.IDENTIFY, mBiometricTaskHandler);
        }
    };

    private class ClearTempFaceImageTask extends AsyncTask<Object, Object, Object> {
        @Override
        protected Object doInBackground(Object... params) {
            File image = StorageUtils.getTempFaceImageFile();
            Fresco.getImagePipeline().evictFromCache(Uri.fromFile(image));
            image.delete();
            return null;
        }
    }
}
