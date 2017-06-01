package com.biginnov.investigator.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.provider.ContentHelper;
import com.biginnov.investigator.provider.AdminTable;
import com.biginnov.investigator.provider.dto.Admin;
import com.biginnov.investigator.util.BitmapUtils;
import com.biginnov.investigator.util.LogUtils;
import com.biginnov.investigator.util.StorageUtils;
import com.neurotec.biometrics.NBiometricStatus;

import java.util.ArrayList;

public class AccountManagerActivity extends BiometricActivity implements LoaderManager.LoaderCallbacks<Cursor> {
	private static final String TAG = AccountManagerActivity.class.getSimpleName();
	private static final int LOADER_USERLIST = 1;
	private ListView mListView;
	private Toolbar mToolbar;
	private UserCursorAdapter mAdapter;
	private Resources mResource;
	ArrayList<UserViewHolder> mUserSelectedList = new ArrayList<UserViewHolder>();
	ContentHelper mHelper;

	@Override
	public void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(R.layout.activity_account_manager);
		mToolbar = (Toolbar) findViewById(R.id.toolbar);
		mToolbar.setTitle(R.string.text_account_management);
		setSupportActionBar(mToolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		mResource = this.getResources();

		mListView = (ListView) findViewById(R.id.listview);
		mAdapter = new UserCursorAdapter(this, null, 0);
		mListView.setAdapter(mAdapter);
		mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				UserViewHolder holder = (UserViewHolder) view.getTag();
				LogUtils.d(TAG, "Clicked", holder.mName.getText());

				if (!mUserSelectedList.contains(holder)) {
					mUserSelectedList.add(holder);
					holder.mLayout.setBackgroundResource(R.color.listview_item_selected);
                    if (holder.mFaceCount > 0) {
                        holder.mCheck.setVisibility(View.VISIBLE);
                    }
				} else {
					mUserSelectedList.remove(holder);
					holder.mLayout.setBackgroundResource(android.R.color.transparent);
                    holder.mCheck.setVisibility(View.INVISIBLE);
				}
				int size = mUserSelectedList.size();
				mEditMode = size > 0 ? true : false;
				invalidateOptionsMenu();
				if (mEditMode) {
					String selectedText = mResource.getString(R.string.text_admin_selected, size);
					mToolbar.setTitle(selectedText);
				} else {
					mToolbar.setTitle(R.string.text_account_management);
				}
				//mAdapter.notifyDataSetChanged();
			}
		});

		mHelper = new ContentHelper(this);
	}

	@Override
	public void onInitCompleted() {

	}

	@Override
	public void onCaptureCompleted(int requestId) {

	}

	@Override
	public void onCaptureCancelled(int requestId) {

	}


	@Override
    protected void onStart() {
        super.onStart();
        LogUtils.d(TAG, "onStart");
		// start loader
		getLoaderManager().restartLoader(LOADER_USERLIST, null, this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LogUtils.d(TAG, "onStop");
    }

    @Override
    protected void onPause() {
        super.onPause();
        LogUtils.d(TAG,"onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        LogUtils.d(TAG, "onResume");
    }

    private static final int MENU_ADDUSER = 0;
	private static final int MENU_EDITUSER = 1;
	private static final int MENU_DELETEUSER = 2;
    private boolean mEditMode = false;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
    	menu.add(0, MENU_ADDUSER, MENU_ADDUSER, "");
    	menu.add(0, MENU_EDITUSER, MENU_EDITUSER, "");
    	menu.add(0, MENU_DELETEUSER, MENU_DELETEUSER, "");
    	return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	if (mEditMode) {
			if (mUserSelectedList.size() != 1) {
				MenuItem item;
				item = menu.getItem(MENU_ADDUSER);
				item.setVisible(false);
				item = menu.getItem(MENU_EDITUSER);
				item.setVisible(false);
				item = menu.getItem(MENU_DELETEUSER);
				item.setVisible(true);
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				item.setIcon(mResource.getDrawable(R.drawable.ic_delete_white_24dp));
			} else {
				MenuItem item;
				item = menu.getItem(MENU_ADDUSER);
				item.setVisible(false);
				item = menu.getItem(MENU_EDITUSER);
				item.setVisible(true);
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				item.setIcon(mResource.getDrawable(R.drawable.ic_mode_edit_white_24dp));
				item = menu.getItem(MENU_DELETEUSER);
				item.setVisible(true);
				item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
				item.setIcon(mResource.getDrawable(R.drawable.ic_delete_white_24dp));
			}
    	} else {
    		MenuItem item;
    		item = menu.getItem(MENU_ADDUSER);
    		item.setVisible(true);
    		item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    		item.setIcon(mResource.getDrawable(R.drawable.ic_person_add_white_24dp));
    		item = menu.getItem(MENU_EDITUSER);
    		item.setVisible(false);
    		item = menu.getItem(MENU_DELETEUSER);
    		item.setVisible(false);
    	}
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent;
        switch (item.getItemId()) {
        // Respond to the action bar's Up/Home button
        case android.R.id.home:
           finish();
           break;
        case MENU_ADDUSER:
        	intent = new Intent(this, AddAccountActivity.class);
        	startActivity(intent);
        	break;
        case MENU_EDITUSER:
			if (mUserSelectedList.size() == 1) {
				String name = mUserSelectedList.get(0).mName.getText().toString();
				if (!TextUtils.isEmpty(name)) {
					LogUtils.d(TAG,"launch Modify acctount activity for name = " , name);
					intent = new Intent(this, ModifyAccountActivity.class);
					Bundle bundle = new Bundle();
					bundle.putString(Constants.BUNDLE_PARAMETER_NAME, name);
					intent.putExtras(bundle);
					startActivity(intent);
				}
			}
        	break;
        case MENU_DELETEUSER:
			if (mInitCompleted) {
				showDialog();
			}
        	break;
        }
        return true;
    }

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		LogUtils.d(TAG,"onCreateLoader id = " , id);

		Uri uri = null;
        switch (id) {
            case LOADER_USERLIST:
            	uri = AdminTable.CONTENT_URI;
            	return new CursorLoader(this, uri, null, null, null, null);
        }
		return null;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		LogUtils.d(TAG,"onLoadFinished");
		switch (loader.getId()) {
            case LOADER_USERLIST:
            	mAdapter.swapCursor(data);
				mEditMode = false;
				mUserSelectedList.clear();
				mToolbar.setTitle(R.string.text_account_management);
				invalidateOptionsMenu();
                break;
        }
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		LogUtils.d(TAG,"onLoaderReset");
		switch (loader.getId()) {
            case LOADER_USERLIST:
                mAdapter.swapCursor(null);
                break;
        }
	}

	public class UserCursorAdapter extends CursorAdapter  {
		private LayoutInflater inflater;

		public UserCursorAdapter(Context context, Cursor cursor, int flags) {
			super(context, cursor, 0);
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		}

		// The newView method is used to inflate a new view and return it,
		// you don't bind any data to the view at this point.
		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			View view = inflater.inflate(R.layout.account_item, null);
			UserViewHolder holder = new UserViewHolder();
			holder.mLayout = (RelativeLayout) view.findViewById(R.id.layout);
			holder.mImage1 = (ImageView) view.findViewById(R.id.image1);
			holder.mImage2 = (ImageView) view.findViewById(R.id.image2);
			holder.mCardView1 = (CardView) view.findViewById(R.id.card1);
			holder.mCardView2 = (CardView) view.findViewById(R.id.card2);
			holder.mName = (TextView) view.findViewById(R.id.name);
            holder.mCheck = (ImageView) view.findViewById(R.id.check);
			view.setTag(holder);
			return view;
		}

		// The bindView method is used to bind all data to a given view
		// such as setting the text on a TextView.
		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			// Find fields to populate in inflated template
			LogUtils.d(TAG,"bindView");
			UserViewHolder holder = (UserViewHolder) view.getTag();
			Admin admin = Admin.fromCursor(cursor);
			holder.mName.setText(admin.getName());
			int faceCount = admin.getFaceCount();
            holder.mFaceCount = faceCount;
			int fingerCount = admin.getFingerprintCount();
			if (faceCount > 0 && fingerCount > 0) {
				holder.mCardView1.setVisibility(View.VISIBLE);
				holder.mCardView2.setVisibility(View.VISIBLE);
				String imagePath = StorageUtils.getFaceImageFilePath(admin.getUuid());
				Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(imagePath, 100, 100, Bitmap.Config.RGB_565);
				holder.mImage2.setImageBitmap(bitmap);
				holder.mImage1.setImageResource(R.drawable.ic_fingerprint_white_24dp);
				holder.mImage1.setBackgroundResource(android.R.color.black);
			} else if (faceCount > 0) {
				holder.mCardView1.setVisibility(View.INVISIBLE);
				holder.mCardView2.setVisibility(View.VISIBLE);
				String imagePath = StorageUtils.getFaceImageFilePath(admin.getUuid());
				Bitmap bitmap = BitmapUtils.decodeSampledBitmapFromFile(imagePath, 100, 100, Bitmap.Config.RGB_565);
				holder.mImage2.setImageBitmap(bitmap);
			} else if (fingerCount > 0) {
				holder.mCardView1.setVisibility(View.INVISIBLE);
				holder.mCardView2.setVisibility(View.VISIBLE);
				holder.mImage2.setImageResource(R.drawable.ic_fingerprint_white_24dp);
				holder.mImage2.setBackgroundResource(android.R.color.black);
			} else {
				holder.mCardView1.setVisibility(View.INVISIBLE);
				holder.mCardView2.setVisibility(View.INVISIBLE);
			}
			if (mUserSelectedList.contains(holder)) {
				holder.mLayout.setBackgroundResource(R.color.listview_item_selected);
				if (holder.mFaceCount > 0) {
					holder.mCheck.setVisibility(View.VISIBLE);
				}
			} else {
				holder.mLayout.setBackgroundResource(android.R.color.transparent);
				holder.mCheck.setVisibility(View.INVISIBLE);
			}
		}
	}

	public class UserViewHolder {
		public RelativeLayout mLayout;
		public ImageView mImage1;
		public ImageView mImage2;
		public CardView mCardView1;
		public CardView mCardView2;
		public TextView mName;
        public ImageView mCheck;
        public int mFaceCount ;
	}


	public static class MyAlertDialogFragment extends DialogFragment {

	    public static MyAlertDialogFragment newInstance(int title , int number) {
	        MyAlertDialogFragment frag = new MyAlertDialogFragment();
	        Bundle args = new Bundle();
	        args.putInt("title", title);
	        args.putInt("number", number);
	        frag.setArguments(args);
	        return frag;
	    }

	    @Override
	    public Dialog onCreateDialog(Bundle savedInstanceState) {
	        final int title = getArguments().getInt("title");
	        final int number = getArguments().getInt("number");
	        String message = MyAlertDialogFragment.this.getString(R.string.alert_dialog_delete_content, number);
	        return new AlertDialog.Builder(getActivity(),R.style.AppCompatAlertDialogStyle)
	                .setMessage(message)
	                .setTitle(title)
	                .setPositiveButton(R.string.alert_dialog_ok,
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            ((AccountManagerActivity)getActivity()).doPositiveClick();
	                        }
	                    }
	                )
	                .setNegativeButton(R.string.alert_dialog_cancel,
	                    new DialogInterface.OnClickListener() {
	                        public void onClick(DialogInterface dialog, int whichButton) {
	                            ((AccountManagerActivity)getActivity()).doNegativeClick();
	                        }
	                    }
	                )
	                .create();
	    }
	}

	void showDialog() {
		int size = mUserSelectedList.size();
		if (size > 0) {
			DialogFragment newFragment = MyAlertDialogFragment.newInstance(
					R.string.alert_dialog_delete_title, size);
			newFragment.show(getFragmentManager(), "dialog");
		}
	}

	public void doPositiveClick() {
	    // Do stuff here.
	    LogUtils.d("FragmentAlertDialog", "Positive click!");
		Admin admin = null;
	    for (UserViewHolder holder: mUserSelectedList){
	    	String name = (String) holder.mName.getText();
	    	if ( mHelper != null){
				admin = mHelper.getAdmin(name);
	    		mHelper.deleteAdmin(admin);
                if ( admin.getFaceCount() > 0 ){
                    NBiometricStatus status = mBiometricClient.delete(admin.getUuid());
                    LogUtils.d(TAG,"status =" , status);
                    StorageUtils.deleteFaceImageFile(admin.getUuid());
                }
	    	}
	    }

	    mUserSelectedList.clear();
	    mEditMode = false;
	    invalidateOptionsMenu();
		mToolbar.setTitle(R.string.text_account_management);
	}

	public void doNegativeClick() {
	    // Do stuff here.
		LogUtils.d("FragmentAlertDialog", "Negative click!");
	}
}