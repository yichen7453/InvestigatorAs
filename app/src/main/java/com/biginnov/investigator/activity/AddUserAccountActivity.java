package com.biginnov.investigator.activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.FaceCaptureFragment;
import com.biginnov.investigator.fragment.FingerprintCaptureFragment;
import com.biginnov.investigator.provider.ContentHelper;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.ftdi.j2xx.D2xxManager;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;
import com.neurotec.util.concurrent.CompletionHandler;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class AddUserAccountActivity extends BiometricActivity {
    private static final String TAG = AddUserAccountActivity.class.getSimpleName();

    private static final int STATE_METHOD_SELECTION = 0;
    private static final int STATE_FACE_CAPTURE = 1;
    private static final int STATE_FINGERPRINT_CAPTURE = 2;

    private EditText mName;
    private ImageView mFaceImage;
    private ImageView mFingerImage;
    private View mFaceView;
    private View mFingerView;
    private View mFaceCheck;
    private View mFingerCheck;
    private TextView mFingerCount;
    private ContentHelper mHelper;
    private Toolbar mToolbar;
    private Spinner mSpinnerAge;
    private Spinner mSpinnerGender;

    private boolean mNameError = false;
    private Toast mNameErrorToast ;

    private ArrayAdapter<CharSequence> mGenderAdapter;
    private int mGenderPosition = 0;
    private ArrayAdapter<String> mAgeAdapter;
    private int mAgePosition = 0;

    private int mAge;
    private int mGender;
    //private boolean mActionDoneStatus = false;
    private boolean mIsFragmentEnabled = false;

    private AtomicInteger mState = new AtomicInteger(STATE_METHOD_SELECTION);

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_add_user_account);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mFragmentManager = getSupportFragmentManager();
        mHelper = new ContentHelper(this);

        mFaceView = findViewById(R.id.faceimagecardview);
        mFingerView = findViewById(R.id.fingerimagecardview);
        mFingerCheck = findViewById(R.id.fingercheck);
        mFaceCheck = findViewById(R.id.facecheck);
        mFingerCount = (TextView)findViewById(R.id.fingercount);
        mFaceImage = (ImageView)findViewById(R.id.faceimage);
        mFingerImage = (ImageView)findViewById(R.id.fingerimage);
        mName = (EditText)findViewById(R.id.englishname);
        mSpinnerAge = (Spinner)findViewById(R.id.age);
        mSpinnerGender = (Spinner)findViewById(R.id.gender);

        initViews();
    }

    private void initViews() {

        mGenderAdapter = ArrayAdapter.createFromResource(this,
                R.array.sex_selection, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        mGenderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        mSpinnerGender.setAdapter(mGenderAdapter);

        mSpinnerGender.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mGenderPosition = position;
                if (position != 0) {
                    mGender = position - 1;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        final ArrayList<String> ageList = new ArrayList<>();
        ageList.add(getString(R.string.text_age));
        for (int i = Constants.SPINNER_AGE_START; i < Constants.SPINNER_AGE_END; i++) {
            ageList.add(Integer.toString(i));
        }
        mAgeAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, ageList);
        mSpinnerAge.setAdapter(mAgeAdapter);

        mSpinnerAge.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mAgePosition = position;
                if (position != 0) {
                    mAge = Integer.parseInt(ageList.get(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mFaceView.setClickable(true);
        mFaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mInitCompleted) {
                    LogUtils.d(TAG, "mInitCompleted is false");
                    return;
                }
                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(mName.getWindowToken(), 0);

                //start FaceCaptureFragment
                mState.set(STATE_FACE_CAPTURE);
                Fragment fragment = FaceCaptureFragment.getInstance(
                        REQUEST_FACE_CAPTURE);
                mFragmentManager.beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .commitAllowingStateLoss();
                mToolbar.setTitle(R.string.text_capture_face_title);
                mIsFragmentEnabled = true;
                invalidateOptionsMenu();
            }
        });

        mFingerView.setClickable(true);
        mFingerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mInitCompleted) {
                    LogUtils.d(TAG, "mInitCompleted is false");
                    return;
                }

                InputMethodManager keyboard = (InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                keyboard.hideSoftInputFromWindow(mName.getWindowToken(), 0);

                if (getDevCount(getD2xxManager()) > 0) {
                    mState.set(STATE_FINGERPRINT_CAPTURE);
                    Fragment fragment = FingerprintCaptureFragment.getInstance(
                            REQUEST_FINGERPRINT_CAPTURE);
                    mFragmentManager.beginTransaction()
                            .add(R.id.fragment_container, fragment)
                            .commitAllowingStateLoss();
                    mToolbar.setTitle(R.string.text_capture_finger);
                    mIsFragmentEnabled = true;
                    invalidateOptionsMenu();
                } else {
                    showToastOnUiThread(getString(R.string.error_finger_device_not_found));
                }
            }
        });

        mName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mNameError) {
                    mNameError = false;
                    if (mNameErrorToast != null) {
                        mNameErrorToast.cancel();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu_action_done, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mIsFragmentEnabled) {
            menu.findItem(R.id.action_done).setVisible(true);
        } else {
            menu.findItem(R.id.action_done).setVisible(false);
        }

        menu.findItem(R.id.action_done).setEnabled(true);

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
                actionDone();
                break;
        }
        if (!result) {
            result = super.onOptionsItemSelected(item);
        }
        return result;
    }

    private void actionDone() {
        String name = mName.getText().toString();

        if (TextUtils.isEmpty(name)) {
            if (mNameErrorToast == null || !mNameErrorToast.getView().isShown()) {
                mNameErrorToast = Toast.makeText(AddUserAccountActivity.this
                        , R.string.text_toast_type_name, Toast.LENGTH_SHORT);
                mNameErrorToast.show();
                mNameError = true;
                return;
            }
        }

        if (mAgePosition == 0) {
            Toast toast = Toast.makeText(AddUserAccountActivity.this, R.string.text_toast_select_age, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (mGenderPosition == 0) {
            Toast toast = Toast.makeText(AddUserAccountActivity.this, R.string.text_toast_select_gender, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (!mHasFaceImage) {
            Toast toast = Toast.makeText(AddUserAccountActivity.this, R.string.text_toast_capture_face, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        if (!mHasFingerImage) {
            Toast toast =Toast.makeText(AddUserAccountActivity.this, R.string.text_toast_capture_finger, Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        String uuid = UUID.randomUUID().toString();

        //add to Biometric table
        new BiographicData.Injector(mSubject)
                .setId(uuid)
                .setUuid(uuid)
                .setName(mName.getText().toString())
                .setSex(mGender)
                .setAge(mAge)
                .setRole(BiographicData.ROLE_IMMIGRANT);

        NBiometricOperation operation = NBiometricOperation.ENROLL;
        NBiometricTask task = mBiometricClient.createTask(EnumSet.of(operation), mSubject);
        mBiometricClient.performTask(task, NBiometricOperation.ENROLL, completionHandler);

        if (mHasFaceImage) {
            StorageUtils.saveTempFaceImage(uuid);
        }

        if (mHasFingerImage) {
            int size = mSubject.getFingers().size();
            for (int i = 0; i < size; i++) {
                StorageUtils.saveTempFingerprintImage(uuid , i);
            }
        }
        finish();
    }

    private void finishCurrentOperation() {
        switch (mState.get()) {
            case STATE_METHOD_SELECTION:
                finish();
                break;
            case STATE_FACE_CAPTURE:
            case STATE_FINGERPRINT_CAPTURE:
                stopCapture();
                break;
        }
        if (!isFinishing()) {
            invalidateOptionsMenu();
        }
    }

    //inherited from BiometricActivity
    private static final int REQUEST_FACE_CAPTURE = 1;
    private static final int REQUEST_FINGERPRINT_CAPTURE = 2;

    //inherited from BiometricActivity
    @Override
    public void onInitCompleted() {
        LogUtils.d(TAG, "onInitCompleted");
    }

    //inherited from BiometricActivity
    @Override
    public void onCaptureCompleted(int requestId) {
        LogUtils.d(TAG, "onCaptureCompleted");
        removeCurrentFragment();
        switch (requestId) {
            case REQUEST_FACE_CAPTURE:
                new ShowFaceTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                break;
            case REQUEST_FINGERPRINT_CAPTURE:
                int size = mSubject.getFingers().size();
                if (size == 0) {
                    mFingerCount.setVisibility(View.INVISIBLE);
                    mFingerCheck.setVisibility(View.INVISIBLE);
                    mHasFingerImage = false ;
                } else {
                    mFingerCount.setVisibility(View.VISIBLE);
                    mFingerCheck.setVisibility(View.VISIBLE);
                    mFingerCount.setText("x" + size);
                    mHasFingerImage = true;
                }
                break;
        }
        mState.set(STATE_METHOD_SELECTION);
        invalidateOptionsMenu();
    }

    //inherited from BiometricActivity
    @Override
    public void onCaptureCancelled(int requestId) {
        LogUtils.d(TAG, "onCaptureCancelled: " + requestId);
        removeCurrentFragment();
        mState.set(STATE_METHOD_SELECTION);
        invalidateOptionsMenu();
    }

    protected boolean removeCurrentFragment() {
        boolean removed = false;
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            mFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            removed = true;
        }
        mToolbar.setTitle(R.string.text_enrollment);
        mIsFragmentEnabled = false;
        invalidateOptionsMenu();
        return removed;
    }

    private boolean mHasFaceImage = false;
    private boolean mHasFingerImage = false;

    public class ShowFaceTask extends AsyncTask<Void, Void, Bitmap> {

        @Override
        protected Bitmap doInBackground(Void... params) {
            LogUtils.d(TAG, "ShowFaceTask doInBackground");
            File target = StorageUtils.getTempFaceImageFile();
            mHasFaceImage = target.exists();
            return mHasFaceImage ? BitmapFactory.decodeFile(target.getAbsolutePath()) : null;
        }

        @Override
        protected void onPostExecute(Bitmap face) {
            LogUtils.d(TAG, "ShowFaceTask onPostExecute");
            if (face != null) {
                mFaceImage.setVisibility(View.VISIBLE);
                mFaceCheck.setVisibility(View.VISIBLE);
                mFaceImage.setImageBitmap(face);
            }
        }
    }

    private CompletionHandler<NBiometricTask, NBiometricOperation> completionHandler = new CompletionHandler<NBiometricTask, NBiometricOperation>() {
        @Override
        public void completed(NBiometricTask task, NBiometricOperation operation) {
            String message = null;
            NBiometricStatus status = task.getStatus();
            LogUtils.d(TAG, String.format("Operation: %s, Status: %s", operation, status));

            if (status == NBiometricStatus.CANCELED) return;

            if (task.getError() != null) {
                LogUtils.d(TAG,task.getError());
            } else {
                switch (operation) {
                    case ENROLL:
                    case ENROLL_WITH_DUPLICATE_CHECK:
                        LogUtils.d(TAG,"ENROLL OK");
//                        if (status == NBiometricStatus.OK) {
//                            message = getString(R.string.msg_enrollment_succeeded);
//                        } else {
//                            message = getString(R.string.msg_enrollment_failed, status.toString());
//                        }
//                        client.list(NBiometricOperation.LIST, subjectListHandler);
                        break;
                    default: {
                        throw new AssertionError("Invalid NBiometricOperation");
                    }
                }
                //showInfo(message);
            }
        }

        @Override
        public void failed(Throwable th, NBiometricOperation operation) {
            LogUtils.d(TAG,th.getMessage(),th);
//            onOperationCompleted(operation, null);
//            showError(th);
        }
    };

//    private void checkAddStatusDone() {
//        String name = mName.getText().toString();
//        if (mHasFaceImage && mAgePosition != 0 && mGenderPosition != 0 && !TextUtils.isEmpty(name)) {
//            mActionDoneStatus = true;
//        } else {
//            mActionDoneStatus = false;
//        }
//    }
}