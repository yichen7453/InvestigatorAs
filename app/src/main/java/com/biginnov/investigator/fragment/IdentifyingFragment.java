package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;

import com.biginnov.investigator.R;
import com.biginnov.investigator.activity.IdentificationActivity;

public class IdentifyingFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = IdentifyingFragment.class.getSimpleName();

    public static IdentifyingFragment getInstance() {
        IdentifyingFragment fragment = new IdentifyingFragment();
        return fragment;
    }

    private View mProgress;
    private Interpolator mProgressInInterpolator = new DecelerateInterpolator();
    private Interpolator mProgressOutInterpolator = new LinearInterpolator();

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_identifying, container, false);
        view.findViewById(R.id.action_negative).setOnClickListener(this);
        mProgress = view.findViewById(R.id.progress_background);
        animateProgressIn();
        return view; // Return null to throw null pointer exception
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.action_negative:
                Activity activity = getActivity();
                if (activity != null && activity instanceof IdentificationActivity) {
                    ((IdentificationActivity) activity).cancelIdentification();
                }
                break;
        }
    }

    private void animateProgressIn() {
        mProgress.animate()
                .alpha(1.0f)
                .setStartDelay(250)
                .setDuration(1250)
                .setInterpolator(mProgressInInterpolator)
                .withEndAction(mProgressInEndAction)
                .start();
    }

    private void animateProgressOut() {
        mProgress.animate()
                .alpha(0.0f)
                .setStartDelay(250)
                .setDuration(1250)
                .setInterpolator(mProgressOutInterpolator)
                .withEndAction(mProgressOutEndAction)
                .start();
    }

    private Runnable mProgressInEndAction = new Runnable() {
        @Override
        public void run() {
            animateProgressOut();
        }
    };

    private Runnable mProgressOutEndAction = new Runnable() {
        @Override
        public void run() {
            animateProgressIn();
        }
    };
}
