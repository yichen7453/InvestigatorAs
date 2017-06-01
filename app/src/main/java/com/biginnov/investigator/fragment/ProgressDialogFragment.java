package com.biginnov.investigator.fragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

import com.biginnov.investigator.R;

public class ProgressDialogFragment extends DialogFragment {
    private static final String TAG = ProgressDialogFragment.class.getSimpleName();
    private static final String EXTRA_KEY_MESSAGE_ID = "message_id";
    private static final String EXTRA_KEY_CANCELABLE = "cancelable";

    public static ProgressDialogFragment getInstance(boolean cancelable) {
        return getInstance(0, cancelable);
    }

    public static ProgressDialogFragment getInstance(int messageId, boolean cancelable) {
        ProgressDialogFragment fragment = new ProgressDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_KEY_MESSAGE_ID, messageId);
        args.putBoolean(EXTRA_KEY_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    public static String getFragmentTAG() {
        return TAG;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        int messageId = args.getInt(EXTRA_KEY_MESSAGE_ID, 0);
        boolean cancelable = args.getBoolean(EXTRA_KEY_CANCELABLE);

        if (messageId == 0) {
            messageId = R.string.text_processing;
        } else if (messageId == 1) {
            messageId = R.string.text_connect_finger;
        }
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setIndeterminate(true);
        dialog.setMessage(getString(messageId));
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
        return dialog;
    }
}
