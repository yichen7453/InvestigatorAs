package com.biginnov.investigator.activity;


import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.biginnov.investigator.Constants;
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

public class ModifyAccountActivity extends BiometricActivity {
    private static final String TAG = ModifyAccountActivity.class.getSimpleName();

    private ListView mListView;
    private ListViewAdapter mAdapter;
    private View mFaceView;
    private View mFingerView;
    private View mFaceCheck;
    private View mFingerCheck;
    private TextView mFingerCount;
    private ImageView mFaceImage;
    private ImageView mFingerImage;
    private ContentHelper mHelper;
    private Toolbar mToolbar;
    private Admin mAdmin;

    private String mNewPassword = null;
    private boolean mIsFragmentEnabled = false;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_modify_account);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.text_modify_account_title);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);

        mFragmentManager = getSupportFragmentManager();
        mHelper = new ContentHelper(this);
        mFaceView = findViewById(R.id.faceimagecardview);
        mFingerView = findViewById(R.id.fingerimagecardview);
        mFingerCheck = findViewById(R.id.fingercheck);
        mFaceCheck = findViewById(R.id.facecheck);
        mFingerCount = (TextView)findViewById(R.id.fingercount);
        mFaceImage = (ImageView) findViewById(R.id.faceimage);
        mFingerImage = (ImageView) findViewById(R.id.fingerimage);

        String name = getIntent().getExtras().getString(Constants.BUNDLE_PARAMETER_NAME);
        mAdmin = mHelper.getAdmin(name);

        mListView = (ListView) findViewById(R.id.listview);
        mAdapter = new ListViewAdapter();
        mListView.setAdapter(mAdapter);
        initViews();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == Constants.REQUEST_CODE_MODIFY_PASSWORD) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                Bundle bundle = data.getExtras();
                if ( bundle != null){
                    mNewPassword = bundle.getString(Constants.BUNDLE_PARAMETER_PASSWORD);
                    LogUtils.d(TAG, "Receive new Password ", mNewPassword);
                }
            }
        }
    }

    private void initViews() {
        mFaceView.setClickable(true);
        if (mAdmin.getFaceCount() > 0) {
            String path = StorageUtils.getFaceImageFilePath(mAdmin.getUuid());
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            mFaceImage.setImageBitmap(bitmap);
            mFaceImage.setVisibility(View.VISIBLE);
        }
        int fingercount = mAdmin.getFingerprintCount();
        if (fingercount > 0) {
            mFingerCheck.setVisibility(View.VISIBLE);
            mFingerCount.setText("x"+fingercount);
        }

        mFaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mInitCompleted == false) {
                    LogUtils.d(TAG, "mInitCompleted is false");
                    return;
                }

                //mSubject.clear();
                //start FaceCaptureFragment
                Fragment fragment = FaceCaptureFragment.getInstance(REQUEST_FACE_CAPTURE);
                mFragmentManager.beginTransaction().add(
                        R.id.fragment_container, fragment).commitAllowingStateLoss();
                mToolbar.setTitle(R.string.text_capture_face_title);
                mIsFragmentEnabled = true;
                invalidateOptionsMenu();
            }
        });

        mFingerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mInitCompleted) {
                    LogUtils.d(TAG, "mInitCompleted is false");
                    return;
                }

                //mSubject.clear();
                Fragment fragment = FingerprintCaptureFragment.getInstance(
                        REQUEST_FINGERPRINT_CAPTURE);
                mFragmentManager.beginTransaction()
                        .add(R.id.fragment_container, fragment)
                        .commitAllowingStateLoss();
                mToolbar.setTitle(R.string.text_capture_finger);
                mIsFragmentEnabled = true;
                invalidateOptionsMenu();
            }
        });


        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (position == 1) {
                    //start modify password activity
                    LogUtils.d(TAG, "launch start password activity");
                    Intent intent = new Intent(ModifyAccountActivity.this, ModifyPasswordActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString(Constants.BUNDLE_PARAMETER_PASSWORD,
                            SecureUtils.decrypt(ModifyAccountActivity.this, mAdmin.getPassword()));
                    intent.putExtras(bundle);
                    startActivityForResult(intent, Constants.REQUEST_CODE_MODIFY_PASSWORD);
                }
            }
        });
    }


    int[] mItemArray = {0, 1};


    public class ListViewAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        public ListViewAdapter() {
            mLayoutInflater = LayoutInflater.from(ModifyAccountActivity.this);
        }

        @Override
        public int getCount() {
            return mItemArray.length;
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(mItemArray[position]);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {

                convertView = mLayoutInflater.inflate(R.layout.modify_account_item, parent, false);
                holder = new ViewHolder();

                holder.mImageView = (ImageView) convertView.findViewById(R.id.icon);
                holder.mTitle = (TextView) convertView.findViewById(R.id.title);
                holder.mContent = (TextView) convertView.findViewById(R.id.content);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            switch (position) {
                case 0:
                    holder.mImageView.setImageResource(R.drawable.ic_people_white_24dp);
                    holder.mTitle.setText(R.string.text_english_name);
                    holder.mContent.setText(mAdmin.getName());
                    break;
                case 1:
                    holder.mImageView.setImageResource(R.drawable.ic_people_white_24dp);
                    holder.mTitle.setText(R.string.text_modify_password);
                    holder.mContent.setVisibility(View.INVISIBLE);
                    break;

            }
            return convertView;
        }

        class ViewHolder {
            public ImageView mImageView;
            public TextView mTitle;
            public TextView mContent;
        }
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
                if ( size == 0 ) {
                    mFingerCount.setVisibility(View.INVISIBLE);
                    mFingerCheck.setVisibility(View.INVISIBLE);
                    mHasFingerImage = false ;
                } else {
                    mFingerCount.setVisibility(View.VISIBLE);
                    mFingerCheck.setVisibility(View.VISIBLE);
                    mFingerCount.setText("x"+size);
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
        if ( mIsFragmentEnabled == false) {
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

    private void actionDone(){
        boolean updateFlag = false;
        if ( !TextUtils.isEmpty(mNewPassword)){
            //modify password
            mAdmin.setmPassword(SecureUtils.encrypt(ModifyAccountActivity.this,mNewPassword));
            updateFlag = true;
        }
        if ( mHasFaceImage || mHasFingerImage ) {
            String uuid = mAdmin.getUuid();
            //delete old Biometric table face data
            mBiometricClient.delete(uuid);
            //add to Biometric table
            mSubject.setId(uuid);
            mSubject.setProperty(BiographicData.UUID, uuid);
            mSubject.setProperty(BiographicData.ROLE, BiographicData.ROLE_ADMINISTRATOR);
            NBiometricOperation operation = NBiometricOperation.ENROLL;
            NBiometricTask task = mBiometricClient.createTask(EnumSet.of(operation), mSubject);
            mBiometricClient.performTask(task, NBiometricOperation.ENROLL, completionHandler);
            if ( mHasFaceImage) {
                StorageUtils.saveTempFaceImage(uuid);
                mAdmin.setFaceCount(1);
                updateFlag = true;
            }
            if ( mHasFingerImage ) {
                int size = mSubject.getFingers().size();
                for ( int i = 0 ; i < size ; i ++){
                    StorageUtils.saveTempFingerprintImage(uuid , i);
                }
                mAdmin.setFingerprintCount(size);
                updateFlag = true;
            }

        }

        if ( updateFlag ) {
            mHelper.updateAdmin(mAdmin);
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
        mToolbar.setTitle(R.string.text_modify_account_title);
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

            //onOperationCompleted(operation, task);

            if (status == NBiometricStatus.CANCELED) return;

            if (task.getError() != null) {
                //showError(task.getError());
                LogUtils.d(TAG, task.getError());
            } else {
                switch (operation) {

                    case ENROLL:
                    case ENROLL_WITH_DUPLICATE_CHECK: {
                        LogUtils.d(TAG, "ENROLL OK");
//                        if (status == NBiometricStatus.OK) {
//                            message = getString(R.string.msg_enrollment_succeeded);
//                        } else {
//                            message = getString(R.string.msg_enrollment_failed, status.toString());
//                        }
//                        client.list(NBiometricOperation.LIST, subjectListHandler);
                    }
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
            LogUtils.d(TAG, th.getMessage(), th);
//            onOperationCompleted(operation, null);
//            showError(th);
        }
    };

}