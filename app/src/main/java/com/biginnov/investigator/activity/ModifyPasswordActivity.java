package com.biginnov.investigator.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.util.LogUtils;

public class ModifyPasswordActivity extends AppCompatActivity {
    private static final String TAG = ModifyPasswordActivity.class.getSimpleName();

    private Toolbar mToolbar;
    private String mPassword;
    private EditText mOldPasswordEdit;
    private EditText mNewPassworkdEdit;
    private EditText mConfirmPasswordEdit;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_modify_password);
        mToolbar = (Toolbar) findViewById(R.id.toolbar);
        mToolbar.setTitle(R.string.text_modify_password);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        mOldPasswordEdit = (EditText)findViewById(R.id.oldpw);
        mNewPassworkdEdit =(EditText)findViewById(R.id.newpw);
        mConfirmPasswordEdit = (EditText)findViewById(R.id.confirmpw);

        mPassword = getIntent().getExtras().getString(Constants.BUNDLE_PARAMETER_PASSWORD);
        LogUtils.d(TAG, "Receive password ", mPassword);

        initViews();
    }

    private void initViews(){
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.common_menu_action_done, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.action_done).setEnabled(true);
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
        String oldPassword = null , newPassword = null, confirmPassword =null;
        if ( mOldPasswordEdit != null ){
            oldPassword = mOldPasswordEdit.getText().toString();
            if (TextUtils.isEmpty(oldPassword) || !TextUtils.equals(oldPassword,mPassword)){
                Toast toast = Toast.makeText(ModifyPasswordActivity.this,"Password is incorrect", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }
        }

        if (mNewPassworkdEdit!=null){
            newPassword = mNewPassworkdEdit.getText().toString();
        }

        if (mConfirmPasswordEdit != null){
            confirmPassword = mConfirmPasswordEdit.getText().toString();
        }

        if ( TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword) ||
                !TextUtils.equals(newPassword , confirmPassword)){
            Toast toast = Toast.makeText(ModifyPasswordActivity.this,"New Password is not matched", Toast.LENGTH_SHORT);
            toast.show();
            return;
        }

        Intent result = new Intent();
        Bundle bundle = new Bundle();
        bundle.putString(Constants.BUNDLE_PARAMETER_PASSWORD , newPassword);
        result.putExtras(bundle);
        setResult(Activity.RESULT_OK, result);
        finish();
    }
}