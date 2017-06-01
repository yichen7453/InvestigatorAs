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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.FaceCaptureFragment;
import com.biginnov.investigator.fragment.FingerprintCaptureFragment;
import com.biginnov.investigator.provider.ContentHelper;
import com.biginnov.investigator.provider.dto.Admin;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.SecureUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.neurotec.biometrics.NBiometricOperation;
import com.neurotec.biometrics.NBiometricStatus;
import com.neurotec.biometrics.NBiometricTask;

import com.neurotec.util.concurrent.CompletionHandler;

import java.io.File;
import java.util.EnumSet;


public class AddAccountActivity extends BiometricActivity {
	private static final String TAG = AddAccountActivity.class.getSimpleName();

	private EditText mName;
	private EditText mPassword;
	private EditText mConfirmPassword;
    private ImageView mFaceImage;
    private ImageView mFingerImage;
    private View mFaceView;
    private View mFingerView;
    private View mFaceCheck;
    private View mFingerCheck;
    private TextView mFingerCount;
	private ContentHelper mHelper;
	private Toolbar mToolbar;

	private boolean mNameError = false;
	private boolean mPasswordError = false;
	private boolean mConfirmError = false;
	private Toast mNameErrorToast ;
	private Toast mPasswordErrorToast ;
	private Toast mConfirmErrorToast;
    private boolean mIsFragmentEnabled = false;

	@Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
		setContentView(R.layout.activity_add_account);
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
        mPassword = (EditText)findViewById(R.id.password);
        mConfirmPassword = (EditText)findViewById(R.id.confirm);
        initViews();
	}

    private void initViews() {

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
                Fragment fragment = FaceCaptureFragment.getInstance(
                new FaceCaptureFragment.SpecBuilder(REQUEST_FACE_CAPTURE)
                        .setCameraSelection(FaceCaptureFragment.CAMERA_SELECTION_FRONT)
                        .setCompleteAfterCapture()
                        .setToolbarVisible()
                        //.setSkipSavingTempImage()
                        .build());
                mFragmentManager.beginTransaction().add(
                        R.id.fragment_container, fragment).commitAllowingStateLoss();
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

        mPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mPasswordError) {
                    mPasswordError = false;
                    if (mPasswordErrorToast!=null) {
                        mPasswordErrorToast.cancel();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (mConfirmError) {
                    mConfirmError = false;
                    if ( mConfirmErrorToast != null){
                        mConfirmErrorToast.cancel();
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

	//inheritate from BiometricActivity

    private static final int REQUEST_FACE_CAPTURE = 1;
    private static final int REQUEST_FINGERPRINT_CAPTURE = 2;

    //inheritate from BiometricActivity
	@Override
	public void onInitCompleted() {
        LogUtils.d(TAG, "onInitCompleted");
	}

    //inheritate from BiometricActivity
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
    }

    //inheritate from BiometricActivity
	@Override
	public void onCaptureCancelled(int requestId) {
        LogUtils.d(TAG, "onCaptureCancelled");
        removeCurrentFragment();
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu_action_done, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (!mIsFragmentEnabled) {
            menu.findItem(R.id.action_done).setEnabled(true);
            menu.findItem(R.id.action_done).setVisible(true);
        } else {
            menu.findItem(R.id.action_done).setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean result = false;
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
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
            mNameErrorToast = Toast.makeText(AddAccountActivity.this
                    , R.string.text_toast_type_name, Toast.LENGTH_SHORT);
            mNameErrorToast.show();
            mNameError = true;
            return;
        } else {
            if (mHelper.getAdmin(name) != null) {
                mNameError = true;
                mNameErrorToast = Toast.makeText(AddAccountActivity.this
                        , R.string.text_toast_duplicate_name, Toast.LENGTH_SHORT);
                mNameErrorToast.show();
                return;
            }
        }

        int faceCount = 0 ;
        int fingerCount = 0;
        String password = null;

        if (mHasFaceImage || mHasFingerImage) {
            if (mHasFaceImage) {
                faceCount = 1;
            }
            if (mHasFingerImage) {
                fingerCount = getSubject().getFingers().size();
            }
            password = mPassword.getText().toString();
            if (!TextUtils.isEmpty(password)) {
                String confirmpassword = mConfirmPassword.getText().toString();
                if (!TextUtils.equals(password, confirmpassword)) {
                    mConfirmErrorToast = Toast.makeText(AddAccountActivity.this
                            , R.string.text_toast_password_not_matched, Toast.LENGTH_SHORT);
                    mConfirmErrorToast.show();
                    mConfirmError = true;
                    return;
                }
            }
        } else {
            password = mPassword.getText().toString();
            if (TextUtils.isEmpty(password)) {
                mPasswordErrorToast = Toast.makeText(AddAccountActivity.this
                        , R.string.text_toast_password, Toast.LENGTH_SHORT);
                mPasswordErrorToast.show();
                mPasswordError = true;
                return;
            }
            String confirmpassword = mConfirmPassword.getText().toString();
            if (!TextUtils.equals(password, confirmpassword)) {
                mConfirmErrorToast = Toast.makeText(AddAccountActivity.this
                        , R.string.text_toast_password_not_matched, Toast.LENGTH_SHORT);
                mConfirmErrorToast.show();
                mConfirmError = true;
                return;
            }
        }

        Admin admin = new Admin(name, SecureUtils.encrypt(AddAccountActivity.this, password), faceCount, fingerCount);
        //add to admin db
        mHelper.addAdmin(admin);

        //add to Biometric table
        mSubject.setId(admin.getUuid());
        mSubject.setProperty(BiographicData.UUID , admin.getUuid());
        mSubject.setProperty(BiographicData.ROLE , BiographicData.ROLE_ADMINISTRATOR);

        NBiometricOperation operation = NBiometricOperation.ENROLL;
        NBiometricTask task = mBiometricClient.createTask(EnumSet.of(operation), mSubject);
        mBiometricClient.performTask(task, NBiometricOperation.ENROLL, completionHandler);

        //move temp file to UUID
        if (mHasFaceImage) {
            StorageUtils.saveTempFaceImage(admin.getUuid());
        }

        if (mHasFingerImage) {
            int size = mSubject.getFingers().size();
            for (int i = 0; i < size; i++) {
                StorageUtils.saveTempFingerprintImage(admin.getUuid(), i);
            }
        }
        finish();
    }

    protected boolean removeCurrentFragment() {
        boolean removed = false;
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            mFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            removed = true;
        }
        mToolbar.setTitle(R.string.text_add_account_title);
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
            if (!target.exists()) {
                LogUtils.d(TAG, "target image path not exist");
            } else {
                LogUtils.d(TAG, "target image path exist");
            }
            return mHasFaceImage ? BitmapFactory.decodeFile(target.getAbsolutePath()) : null;
        }

        @Override
        protected void onPostExecute(Bitmap face) {
            LogUtils.d(TAG, "ShowFaceTask onPostExecute");
            if (face != null) {
                mFaceImage.setVisibility(View.VISIBLE);
                mFaceCheck.setVisibility(View.VISIBLE);
                mFaceImage.setImageBitmap(face);
                mFaceImage.setScaleX(-1f);
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
                        /*
                        if (status == NBiometricStatus.OK) {
                            message = getString(R.string.msg_enrollment_succeeded);
                        } else {
                            message = getString(R.string.msg_enrollment_failed, status.toString());
                        }
                        client.list(NBiometricOperation.LIST, subjectListHandler);
                        */
                        break;
                    default:
                        throw new AssertionError("Invalid NBiometricOperation");
                }
            }
        }

        @Override
        public void failed(Throwable th, NBiometricOperation operation) {
            LogUtils.d(TAG,th.getMessage(),th);
            //onOperationCompleted(operation, null);
        }
    };
}