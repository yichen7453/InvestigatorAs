package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;

import com.biginnov.investigator.util.LogUtils;

public class CommonDialogFragment extends DialogFragment {
    private static final String TAG = CommonDialogFragment.class.getSimpleName();
    private static final String EXTRA_KEY_REQUEST_ID = "request_id";
    private static final String EXTRA_KEY_TITLE_ID = "title_id";
    private static final String EXTRA_KEY_MESSAGE_ID = "message_id";
    private static final String EXTRA_KEY_MESSAGE = "message";
    private static final String EXTRA_KEY_POSITIVE_ID = "positive_id";
    private static final String EXTRA_KEY_NEGATIVE_ID = "negative_id";
    private static final String EXTRA_KEY_NEUTRAL_ID = "neutral_id";
    private static final String EXTRA_KEY_CANCELABLE = "cancelable";

    public interface ActionListener {
        void onPositiveButtonClick(int requestId);

        void onNegativeButtonClick(int requestId);

        void onNeutralButtonClick(int requestId);

        void onCancel(int requestId);
    }

    public static CommonDialogFragment getInstance(
            int requestId, int messageId,
            int positiveButtonId, int negativeButtonId, boolean cancelable) {
        return getInstance(requestId, 0, messageId,
                positiveButtonId, negativeButtonId, 0, cancelable);
    }

    public static CommonDialogFragment getInstance(
            int requestId, int titleId, int messageId,
            int positiveButtonId, int negativeButtonId, int neutralButtonId,
            boolean cancelable) {
        CommonDialogFragment fragment = new CommonDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_KEY_REQUEST_ID, requestId);
        args.putInt(EXTRA_KEY_TITLE_ID, titleId);
        args.putInt(EXTRA_KEY_MESSAGE_ID, messageId);
        args.putInt(EXTRA_KEY_POSITIVE_ID, positiveButtonId);
        args.putInt(EXTRA_KEY_NEGATIVE_ID, negativeButtonId);
        args.putInt(EXTRA_KEY_NEUTRAL_ID, neutralButtonId);
        args.putBoolean(EXTRA_KEY_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    public static CommonDialogFragment getInstance(
            int requestId, int titleId, String message,
            int positiveButtonId, int negativeButtonId, int neutralButtonId,
            boolean cancelable) {
        CommonDialogFragment fragment = new CommonDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_KEY_REQUEST_ID, requestId);
        args.putInt(EXTRA_KEY_TITLE_ID, titleId);
        args.putString(EXTRA_KEY_MESSAGE, message);
        args.putInt(EXTRA_KEY_POSITIVE_ID, positiveButtonId);
        args.putInt(EXTRA_KEY_NEGATIVE_ID, negativeButtonId);
        args.putInt(EXTRA_KEY_NEUTRAL_ID, neutralButtonId);
        args.putBoolean(EXTRA_KEY_CANCELABLE, cancelable);
        fragment.setArguments(args);
        return fragment;
    }

    public static String getFragmentTAG() {
        return TAG;
    }

    private int mRequestId;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle args = getArguments();
        mRequestId = args.getInt(EXTRA_KEY_REQUEST_ID, 0);
        int titleId = args.getInt(EXTRA_KEY_TITLE_ID, 0);
        int messageId = args.getInt(EXTRA_KEY_MESSAGE_ID, 0);
        String message;
        if (messageId == 0) {
            message = args.getString(EXTRA_KEY_MESSAGE);
        } else {
            message = getString(messageId);
        }
        int positiveButtonId = args.getInt(EXTRA_KEY_POSITIVE_ID, 0);
        int negativeButtonId = args.getInt(EXTRA_KEY_NEGATIVE_ID, 0);
        int neutralButtonId = args.getInt(EXTRA_KEY_NEUTRAL_ID, 0);
        boolean cancelable = args.getBoolean(EXTRA_KEY_CANCELABLE);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        if (titleId > 0) {
            builder.setTitle(titleId);
        }
        if (!TextUtils.isEmpty(message)) {
            builder.setMessage(message);
        }
        if (positiveButtonId > 0) {
            builder.setPositiveButton(positiveButtonId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Activity activity = getActivity();
                    if (activity instanceof ActionListener) {
                        ((ActionListener) activity).onPositiveButtonClick(mRequestId);
                    } else {
                        LogUtils.w(TAG, "Action listener not implemented");
                    }
                }
            });
        }
        if (negativeButtonId > 0) {
            builder.setNegativeButton(negativeButtonId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Activity activity = getActivity();
                    if (activity instanceof ActionListener) {
                        ((ActionListener) activity).onNegativeButtonClick(mRequestId);
                    } else {
                        LogUtils.w(TAG, "Action listener not implemented");
                    }
                }
            });
        }
        if (neutralButtonId > 0) {
            builder.setNeutralButton(neutralButtonId, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Activity activity = getActivity();
                    if (activity instanceof ActionListener) {
                        ((ActionListener) activity).onNeutralButtonClick(mRequestId);
                    } else {
                        LogUtils.w(TAG, "Action listener not implemented");
                    }
                }
            });
        }
        Dialog dialog = builder.create();
        dialog.setCancelable(cancelable);
        dialog.setCanceledOnTouchOutside(cancelable);
        return dialog;
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        Activity activity = getActivity();
        if (activity instanceof ActionListener) {
            ((ActionListener) activity).onCancel(mRequestId);
        } else {
            LogUtils.w(TAG, "Action listener not implemented");
        }
    }
}
