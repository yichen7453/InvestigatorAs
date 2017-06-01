package com.biginnov.investigator.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.CommonDialogFragment;
import com.biginnov.investigator.fragment.ProgressDialogFragment;
import com.biginnov.investigator.util.CommonUtils;

public abstract class BaseActivity extends AppCompatActivity {

    protected ProgressDialogFragment mProgressDialogFragment;
    protected boolean mPaused;
    protected boolean mDismissProgressOnResume;

    private CommonDialogFragment mNoNetworkDialogFragment;
    private NetworkChangedReceiver mNetworkChangedReceiver;
    private boolean mNetworkChangedReceiverRegistered;

    @Override
    protected void onResume() {
        super.onResume();
        mPaused = false;
        if (mDismissProgressOnResume) {
            mDismissProgressOnResume = false;
            dismissProgressDialog();
        }
    }

    @Override
    protected void onPause() {
        mPaused = true;
        super.onPause();
    }

    public void showProgressDialog() {
        showProgressDialog(false);
    }

    public void showProgressDialog(boolean cancelable) {
        showProgressDialog(0, cancelable);
    }

    public void showProgressDialog(int messageId, boolean cancelable) {
        if (mProgressDialogFragment == null) {
            mProgressDialogFragment = ProgressDialogFragment.getInstance(messageId, cancelable);
            mProgressDialogFragment.setCancelable(cancelable);
            mProgressDialogFragment.show(
                    getSupportFragmentManager(), ProgressDialogFragment.getFragmentTAG());
        }
    }

    public void dismissProgressDialog() {
        if (mPaused) {
            mDismissProgressOnResume = true;
        } else if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    protected void showNoNetworkDialog(int requestId) {
        if (!isNoNetworkDialogShowing()) {
            mNoNetworkDialogFragment = CommonDialogFragment.getInstance(
                    requestId, R.string.dialog_no_network,
                    android.R.string.ok, android.R.string.cancel,
                    true);
            mNoNetworkDialogFragment.show(
                    getSupportFragmentManager(), CommonDialogFragment.getFragmentTAG());
        }
    }

    protected void dismissNoNetworkDialog() {
        if (isNoNetworkDialogShowing()) {
            mNoNetworkDialogFragment.dismiss();
        }
    }

    protected boolean isNoNetworkDialogShowing() {
        return mNoNetworkDialogFragment != null && mNoNetworkDialogFragment.isAdded();
    }

    protected void registerNetworkChangedReceiver(boolean networkConnected) {
        if (mNetworkChangedReceiver == null) {
            mNetworkChangedReceiver = new NetworkChangedReceiver(networkConnected);
        } else {
            mNetworkChangedReceiver.mNetworkConnected = networkConnected;
        }
        if (!mNetworkChangedReceiverRegistered) {
            registerReceiver(mNetworkChangedReceiver,
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            mNetworkChangedReceiverRegistered = true;
        }
    }

    protected void unregisterNetworkChangedReceiver() {
        if (mNetworkChangedReceiverRegistered) {
            unregisterReceiver(mNetworkChangedReceiver);
            mNetworkChangedReceiverRegistered = false;
        }
    }

    protected void onNetworkConnected() {
        // Let child inherit it
    }

    protected void onNetworkDisconnected() {
        // Let child inherit it
    }

    private class NetworkChangedReceiver extends BroadcastReceiver {

        protected boolean mNetworkConnected;

        public NetworkChangedReceiver(boolean connected) {
            mNetworkConnected = connected;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CommonUtils.isNetworkConnected(context)) {
                if (!mNetworkConnected) {
                    mNetworkConnected = true;
                    onNetworkConnected();
                }
            } else {
                if (mNetworkConnected) {
                    mNetworkConnected = false;
                    onNetworkDisconnected();
                }
            }
        }
    }

    protected void showToast(final String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected void showToastOnUiThread(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BaseActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}
