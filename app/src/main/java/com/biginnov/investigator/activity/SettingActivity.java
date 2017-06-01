package com.biginnov.investigator.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.biginnov.investigator.R;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.LogUtils;
import com.neurotec.biometrics.NSubject;

import java.util.concurrent.ThreadPoolExecutor;


public class SettingActivity extends BiometricActivity {
    private static final String TAG = SettingActivity.class.getSimpleName();

    private ListView mListView;
    private SettingAdapter mAdapter;
    private Toolbar mToolbar;
    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_setting);
        mToolbar = (Toolbar)findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.text_setting_activity_title);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        mListView = (ListView)findViewById(R.id.listview);
        mAdapter = new SettingAdapter();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent;
                switch (position){
                    case 1:
                        //start account management
                        intent = new Intent(SettingActivity.this, AccountManagerActivity.class);
                        startActivity(intent);
                        break;
                    case 2:
                        if (mInitCompleted) {
                            showDialog();
                        }
                        break;
                    case 5:
                        setResult(RESULT_OK);
                        finish();
                        break;
                }
            }
        });
        mListView.setAdapter(mAdapter);
    }

    @Override
    public void onInitCompleted() {
        LogUtils.d(TAG, "onInitCompleted");
    }

    @Override
    public void onCaptureCompleted(int requestId) { }

    @Override
    public void onCaptureCancelled(int requestId) { }

    int[] mStringArray = { R.string.text_normal_setting ,
                        R.string.text_account_management ,
                        R.string.text_clear_data ,
                        R.string.text_about,
                        R.string.text_application_version,
                        R.string.text_logout};


    public  class SettingAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;

        public SettingAdapter(){
            mLayoutInflater = LayoutInflater.from(SettingActivity.this);
        }

        @Override
        public int getCount() {
            return mStringArray.length;
        }

        @Override
        public Object getItem(int position) {
            return Integer.valueOf(mStringArray[position]);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.setting_item, parent, false);
                holder = new ViewHolder();
                holder.mLayout = (RelativeLayout) convertView.findViewById(R.id.layout);
                holder.mImageView = (ImageView)convertView.findViewById(R.id.image);
                holder.mTitle = (TextView)convertView.findViewById(R.id.title);
                holder.mSubTitle = (TextView)convertView.findViewById(R.id.subtitle);
                holder.mVersion = (TextView)convertView.findViewById(R.id.version);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            switch (position) {
                case 0:
                case 3:
                    holder.mImageView.setVisibility(View.INVISIBLE);
                    holder.mTitle.setVisibility(View.VISIBLE);
                    holder.mSubTitle.setVisibility(View.INVISIBLE);
                    holder.mVersion.setVisibility(View.INVISIBLE);
                    holder.mLayout.setBackgroundColor(getResources().getColor(android.R.color.transparent));
                    holder.mTitle.setText(mStringArray[position]);
                    break;
                case 1:
                case 2:
                case 5:
                    holder.mVersion.setVisibility(View.INVISIBLE);
                    holder.mImageView.setVisibility(View.VISIBLE);
                    holder.mTitle.setVisibility(View.INVISIBLE);
                    holder.mSubTitle.setVisibility(View.VISIBLE);
                    holder.mSubTitle.setText(mStringArray[position]);
                    break;
                case 4:
                    holder.mVersion.setVisibility(View.VISIBLE);
                    holder.mImageView.setVisibility(View.VISIBLE);
                    holder.mTitle.setVisibility(View.INVISIBLE);
                    holder.mSubTitle.setVisibility(View.VISIBLE);
                    holder.mSubTitle.setText(mStringArray[position]);
                    try {
                        String versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
                        holder.mVersion.setText("V" + versionName);
                    }catch (Exception e){
                        LogUtils.d(TAG, e.getMessage(), e);
                    }
                    break;
            }
            switch (position) {
                case 1:
                    holder.mImageView.setImageResource(R.drawable.ic_people_white_24dp);
                    break;
                case 2:
                    holder.mImageView.setImageResource(R.drawable.ic_restore_white_24dp);
                    break;
                case 5:
                    holder.mImageView.setImageResource(R.drawable.ic_info_white_24dp);
                    break;
                case 4:
                    holder.mImageView.setImageResource(R.drawable.ic_login_out_24dp);
                    break;
            }
            return convertView;
        }

        class ViewHolder {
            public RelativeLayout mLayout;
            public ImageView mImageView;
            public TextView mTitle;
            public TextView mSubTitle;
            public TextView mVersion;
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // Respond to the action bar's Up/Home button
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(int title) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("title", title);
            frag.setArguments(args);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int title = getArguments().getInt("title");
            String message = MyAlertDialogFragment.this.getString(R.string.alert_dialog_clear_user_data);
            return new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
            //return new AlertDialog.Builder(getActivity())
                    .setMessage(message)
                    .setTitle(title)
                    .setPositiveButton(R.string.alert_dialog_ok,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((SettingActivity)getActivity()).doPositiveClick();
                                }
                            }
                    )
                    .setNegativeButton(R.string.alert_dialog_cancel,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    ((SettingActivity)getActivity()).doNegativeClick();
                                }
                            }
                    )
                    .create();
        }
    }

    void showDialog() {
        DialogFragment newFragment = MyAlertDialogFragment.newInstance(R.string.text_clear_data);
        newFragment.show(getFragmentManager(), "dialog");
    }

    public void doPositiveClick() {
        // Do stuff here.
        LogUtils.d("FragmentAlertDialog", "Positive click!");
        //Clear all user data
        showProgressDialog(false);
        new DeleteUserDataTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    public void doNegativeClick() {
        // Do stuff here.
        LogUtils.d("FragmentAlertDialog", "Negative click!");
    }

    public class DeleteUserDataTask extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            if (mBiometricClient != null) {
                NSubject[] list = mBiometricClient.list();
                LogUtils.d(TAG, "List size = ", list.length);
                for (NSubject subject : list) {
                    mSubject.clear();
                    mSubject.setId(subject.getId());
                    mBiometricClient.get(mSubject);
                    long role = (Long)mSubject.getProperty(BiographicData.ROLE);
                    if (role != BiographicData.ROLE_ADMINISTRATOR) {
                        String uuid = (String) mSubject.getProperty(BiographicData.UUID);
                        mBiometricClient.deleteAsync(uuid);
                    }
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void param) {
            LogUtils.d(TAG, "DeleteUserDataTask onPostExecute");
            dismissProgressDialog();
        }
    }
}