package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.biginnov.investigator.R;

public class CommonInputDialogFragment extends DialogFragment {
    private static final String TAG = CommonInputDialogFragment.class.getSimpleName();
    private static final String EXTRA_REQUEST = "request";
    private static final String EXTRA_TITLE = "title_id";
    private static final String EXTRA_HINT = "hint_id";

    private int mRequestCode;
    private String mTitle;

    private AlertDialog mDialog;
    private View mView;
    private EditText mInput;

    public interface ActionListener {
        void onPositiveButtonClick(int requestCode, String value);

        void onNegativeButtonClick(int requestCode);
    }

    public static CommonInputDialogFragment newInstance(int requestCode, String title) {
        return newInstance(requestCode, title, null);
    }

    public static CommonInputDialogFragment newInstance(int requestCode, String title, String hint) {
        CommonInputDialogFragment fragment = new CommonInputDialogFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_REQUEST, requestCode);
        args.putString(EXTRA_TITLE, title);
        args.putString(EXTRA_HINT, hint);
        fragment.setArguments(args);
        return fragment;
    }

    public static String getFragmentTag() {
        return TAG;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mRequestCode = args.getInt(EXTRA_REQUEST);
        mTitle = args.getString(EXTRA_TITLE);
        String hint = args.getString(EXTRA_HINT);

        mView = LayoutInflater.from(getActivity()).inflate(R.layout.common_input, null);
        if (!TextUtils.isEmpty(hint)) {
            TextView hintView = (TextView) mView.findViewById(R.id.input_hint);
            hintView.setText(hint);
            hintView.setVisibility(View.VISIBLE);
        }
        mInput = (EditText) mView.findViewById(R.id.input_text);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        mDialog = new AlertDialog.Builder(getActivity())
                .setView(mView)
                .setPositiveButton(android.R.string.ok, mOnPositiveClickListener)
                .setNegativeButton(android.R.string.cancel, mOnNegativeClickListener)
                .create();
        if (!TextUtils.isEmpty(mTitle)) {
            mDialog.setTitle(mTitle);
        }
        mInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // Do nothing
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Do nothing
            }

            @Override
            public void afterTextChanged(Editable s) {
                final Button positiveButton = mDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                if (!TextUtils.isEmpty(s)) {
                    positiveButton.setEnabled(true);
                } else {
                    positiveButton.setEnabled(false);
                }
            }
        });
        return mDialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Need to get button after show dialog
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    private DialogInterface.OnClickListener mOnPositiveClickListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof ActionListener) {
                        ((ActionListener) activity).onPositiveButtonClick(
                                mRequestCode, mInput.getText().toString());
                    }
                }
            };

    private DialogInterface.OnClickListener mOnNegativeClickListener =
            new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Activity activity = getActivity();
                    if (activity != null && activity instanceof ActionListener) {
                        ((ActionListener) activity).onNegativeButtonClick(mRequestCode);
                    }
                }
            };
}
