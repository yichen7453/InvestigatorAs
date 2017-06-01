package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.biginnov.investigator.activity.BaseActivity;
import com.biginnov.investigator.util.LogUtils;

public class BaseFragment extends Fragment {
    private static final String TAG = BaseFragment.class.getSimpleName();

    protected void showProgressDialog() {
        showProgressDialog(false);
    }

    protected void showProgressDialog(boolean cancelable) {
        showProgressDialog(0, cancelable);
    }

    protected void showProgressDialog(int messageId, boolean cancelable) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BaseActivity) {
            ((BaseActivity) activity).showProgressDialog(messageId, cancelable);
        } else {
            LogUtils.w(TAG, "Activity is not BaseActivity");
        }
    }

    protected void dismissProgressDialog() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof BaseActivity) {
            ((BaseActivity) activity).dismissProgressDialog();
        } else {
            LogUtils.w(TAG, "Activity is not BaseActivity");
        }
    }

    protected void showToast(final String message) {
        final Activity activity = getActivity();
        if (activity != null) {
            Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showToastOnUiThread(final String message) {
        final Activity activity = getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }

            });
        }
    }
}
