package com.biginnov.investigator.fragment;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;

public class MatchingResultListFragment extends BaseFragment {
    private static final String TAG = MatchingResultListFragment.class.getSimpleName();

    public static MatchingResultListFragment getInstance() {
        return new MatchingResultListFragment();
    }

    private NSubject.MatchingResultCollection mMatchingResults;
    private ArrayList<NSubject> mMatchingSubjects;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getMatchingData();
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = null;
        if (mMatchingResults != null && mMatchingSubjects != null) {
            Activity activity = getActivity();
            view = inflater.inflate(R.layout.fragment_matching_result_list, container, false);
            RecyclerView list = (RecyclerView) view.findViewById(android.R.id.list);
            list.setHasFixedSize(true);
            list.setLayoutManager(new LinearLayoutManager(
                    activity, LinearLayoutManager.VERTICAL, false));
            ListAdapter adapter = new ListAdapter(activity);
            list.setAdapter(adapter);
        }
        return view; // Return null to throw null pointer exception
    }

    private void getMatchingData() {
        Activity activity = getActivity();
        if (activity != null && activity instanceof IdentificationActivity) {
            mMatchingResults =
                    ((IdentificationActivity) activity).getSubject().getMatchingResults();
            mMatchingSubjects = ((IdentificationActivity) activity).getMatchingSubjects();
        } else {
            LogUtils.w(TAG, "Activity is not BiometricActivity");
        }
    }

    private class ListAdapter
            extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_LABEL = 0;
        private static final int VIEW_TYPE_MATCHING_RESULT = 1;

        public class ViewHolderLabel extends RecyclerView.ViewHolder {

            private TextView label;

            public ViewHolderLabel(View v) {
                super(v);
                label = (TextView) v.findViewById(R.id.label);
            }
        }

        public class ViewHolderMatchingResult extends RecyclerView.ViewHolder {

            private View container;
            private TextView name;
            private TextView info;
            private TextView score;
            private SimpleDraweeView image;

            public ViewHolderMatchingResult(View v) {
                super(v);
                container = v;
                name = (TextView) v.findViewById(R.id.name);
                info = (TextView) v.findViewById(R.id.info);
                score = (TextView) v.findViewById(R.id.score);
                image = (SimpleDraweeView) v.findViewById(R.id.image);
            }
        }

        private LayoutInflater inflater;
        private int resultSize;

        public ListAdapter(Activity activity) {
            inflater = activity.getLayoutInflater();
            resultSize = mMatchingResults != null ? mMatchingResults.size() : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_LABEL) {
                View v = inflater.inflate(R.layout.item_label_matching_result, parent, false);
                return new ViewHolderLabel(v);
            } else {
                View v = inflater.inflate(R.layout.item_matching_result, parent, false);
                v.setOnClickListener(mOnItemClickListener);
                return new ViewHolderMatchingResult(v);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                ViewHolderLabel holderLabel = (ViewHolderLabel) holder;
                holderLabel.label.setText(getResources().getQuantityString(
                        R.plurals.matching_result_number, resultSize, resultSize));
            } else {
                int resultPosition = position - 1;
                ViewHolderMatchingResult holderResult = (ViewHolderMatchingResult) holder;
                NMatchingResult result = mMatchingResults.get(resultPosition);
                NSubject subject = mMatchingSubjects.get(resultPosition);
                holderResult.name.setText(BiographicData.getName(subject));
                holderResult.score.setText(String.valueOf(result.getScore()));

                Activity activity = getActivity();
                if (activity != null) {
                    holderResult.info.setText(getString(R.string.text_matching_result_info,
                            BiographicData.getDisplayedSex(activity, subject),
                            BiographicData.getAge(subject)));
                    holderResult.image.setImageURI(Uri.fromFile(
                            StorageUtils.getFaceImageFile(BiographicData.getUuid(subject))));
                }
                holderResult.container.setTag(resultPosition);
            }
        }

        @Override
        public int getItemCount() {
            return resultSize + 1;
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? VIEW_TYPE_LABEL : VIEW_TYPE_MATCHING_RESULT;
        }

        private View.OnClickListener mOnItemClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity activity = getActivity();
                if (activity != null && activity instanceof IdentificationActivity) {
                    int resultPosition = (int) view.getTag();
                    LogUtils.d(TAG, "onItemClick: ", resultPosition);
                    ((IdentificationActivity) activity).showMatchingResultDetail(resultPosition);
                }
            }
        };
    }
}
