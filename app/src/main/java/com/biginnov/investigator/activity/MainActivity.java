package com.biginnov.investigator.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.biginnov.investigator.R;
import com.biginnov.investigator.util.LicenseManager;
import com.biginnov.investigator.util.LogUtils;

public class MainActivity extends BaseActivity implements View.OnClickListener {
    private static final String TAG = MainActivity.class.getSimpleName();

    private boolean mLoggingOut = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViews();
    }

    private void findViews() {
        findViewById(R.id.action_identification).setOnClickListener(this);
        findViewById(R.id.action_enrollment).setOnClickListener(this);
        findViewById(R.id.action_settings).setOnClickListener(this);
    }

    @Override
    protected void onDestroy() {
        if (!mLoggingOut) {
            LicenseManager.release();
        }
        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.action_identification:
                LogUtils.d(TAG, "onClick: action_identification");
                intent = new Intent(this, IdentificationActivity.class);
                startActivity(intent);
                break;

            case R.id.action_enrollment:
                LogUtils.d(TAG, "onClick: action_enrollment");
                intent = new Intent(this, AddUserAccountActivity.class);
                startActivity(intent);
                break;

            case R.id.action_settings:
                LogUtils.d(TAG, "onClick: action_settings");
                intent = new Intent(this, SettingActivity.class);
                startActivityForResult(intent, REQUEST_SETTINGS);
                break;
        }
    }

    private static final int REQUEST_SETTINGS = 1;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            mLoggingOut = true;
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
