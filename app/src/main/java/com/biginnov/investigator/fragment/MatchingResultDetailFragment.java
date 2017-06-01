package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.biginnov.investigator.R;
import com.biginnov.investigator.activity.IdentificationActivity;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.facebook.drawee.view.SimpleDraweeView;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NSubject;

public class MatchingResultDetailFragment extends BaseFragment {
    private static final String TAG = MatchingResultDetailFragment.class.getSimpleName();

    private static final String EXTRA_POSITION = "position";

    public static MatchingResultDetailFragment getInstance(int position) {
        MatchingResultDetailFragment fragment = new MatchingResultDetailFragment();
        Bundle args = new Bundle();
        args.putInt(EXTRA_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    // private NSubject mSubject;
    private NMatchingResult mMatchingResult;
    private NSubject mMatchingSubject;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        int position = args.getInt(EXTRA_POSITION);
        getMatchingData(position);
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        if (mMatchingResult != null) {
            view = inflater.inflate(R.layout.fragment_matching_result_detail, container, false);
            findViews(view);
        }
        return view; // Return null to throw null pointer exception
    }

    private void getMatchingData(int position) {
        Activity activity = getActivity();
        if (activity != null && activity instanceof IdentificationActivity) {
            mMatchingResult = ((IdentificationActivity) activity).getSubject()
                    .getMatchingResults().get(position);
            mMatchingSubject = ((IdentificationActivity) activity).getMatchingSubject(position);
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    private void findViews(View view) {
        SimpleDraweeView draweeTemplate =
                (SimpleDraweeView) view.findViewById(R.id.face_image_template);
        draweeTemplate.setImageURI(Uri.fromFile(
                StorageUtils.getTempFaceImageFile()));

        String uuid = BiographicData.getUuid(mMatchingSubject);
        SimpleDraweeView draweeResult =
                (SimpleDraweeView) view.findViewById(R.id.face_image_matching_result);
        draweeResult.setImageURI(Uri.fromFile(
                StorageUtils.getFaceImageFile(uuid)));

        ((TextView) view.findViewById(R.id.name)).setText(
                BiographicData.getName(mMatchingSubject));
        ((TextView) view.findViewById(R.id.info)).setText(
                getString(R.string.text_matching_result_info,
                        BiographicData.getDisplayedSex(getActivity(), mMatchingSubject),
                        BiographicData.getAge(mMatchingSubject)));
        ((TextView) view.findViewById(R.id.score)).setText(
                String.valueOf(mMatchingResult.getScore()));
    }
}
