package com.biginnov.investigator.activity;

import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.CommonInputDialogFragment;
import com.biginnov.investigator.fragment.ProgressDialogFragment;
import com.biginnov.investigator.util.LogUtils;

import net.sf.scuba.data.Gender;

import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.COMFile;
import org.jmrtd.lds.DG1File;
import org.jmrtd.lds.DG2File;
import org.jmrtd.lds.FaceImageInfo;
import org.jmrtd.lds.FaceInfo;
import org.jmrtd.lds.LDSFile;
import org.jmrtd.lds.LDSFileUtil;
import org.jmrtd.lds.MRZInfo;

import java.security.Security;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ReadPassportActivity extends AppCompatActivity implements
        CommonInputDialogFragment.ActionListener {
    private static final String TAG = ReadPassportActivity.class.getSimpleName();

    private LinearLayout mInfoContainer;
    private TextView mType;
    private TextView mCode;
    private TextView mPassportNumber;
    private TextView mName;
    private TextView mNationality;
    private TextView mPersonalNumber;
    private TextView mGender;
    private ArrayList<ImageView> mImages;

    private ProgressDialogFragment mProgressDialogFragment;
    private boolean mForegroundDispatchEnabled;

    private Mrz mMrz;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_passport);
        mInfoContainer = (LinearLayout) findViewById(R.id.info_container);
        mType = (TextView) findViewById(R.id.type);
        mCode = (TextView) findViewById(R.id.code);
        mPassportNumber = (TextView) findViewById(R.id.passport_number);
        mName = (TextView) findViewById(R.id.name);
        mNationality = (TextView) findViewById(R.id.nationality);
        mPersonalNumber = (TextView) findViewById(R.id.personal_number);
        mGender = (TextView) findViewById(R.id.gender);
        mImages = new ArrayList<ImageView>();
        showMrzInputDialog();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        dismissProgressDialog();
        disableForegroundDispatch();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.read_passport, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_new) {
            showMrzInputDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static final int DIALOG_REQUEST_INPUT_MRZ = 1;

    @Override
    public void onPositiveButtonClick(int requestCode, String value) {
        switch (requestCode) {
            case DIALOG_REQUEST_INPUT_MRZ:
                mMrz = Mrz.parse(value);
                if (mMrz != null) {
                    if (!mImages.isEmpty()) {
                        for (ImageView image : mImages) {
                            mInfoContainer.removeView(image);
                        }
                    }
                    showProgressDialog(R.string.text_waiting_passport);
                    enableForegroundDispatch();
                } else {
                    Toast.makeText(this, "Wrong MRZ", Toast.LENGTH_SHORT).show();
                }
                break;
        }

    }

    @Override
    public void onNegativeButtonClick(int requestCode) {
        switch (requestCode) {
            case DIALOG_REQUEST_INPUT_MRZ:
                // Do nothing
                break;
        }
    }

    private void showMrzInputDialog() {
        CommonInputDialogFragment fragment = CommonInputDialogFragment.newInstance(
                DIALOG_REQUEST_INPUT_MRZ, "Input MRZ");
        fragment.show(getSupportFragmentManager(), "input_mrz");
    }

    private void showProgressDialog(int messageId) {
        if (mProgressDialogFragment == null) {
            mProgressDialogFragment = ProgressDialogFragment.getInstance(messageId,
                    false);
            mProgressDialogFragment.setCancelable(false);
            mProgressDialogFragment.show(getSupportFragmentManager(),
                    ProgressDialogFragment.getFragmentTAG());
        }
    }

    private void dismissProgressDialog() {
        if (mProgressDialogFragment != null) {
            mProgressDialogFragment.dismiss();
            mProgressDialogFragment = null;
        }
    }

    private void enableForegroundDispatch() {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        Intent intent = new Intent(this, this.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pending = PendingIntent
                .getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        String[][] filter = new String[][]{
                new String[]{
                        "android.nfc.tech.IsoDep"
                }
        };
        nfcAdapter.enableForegroundDispatch(this, pending, null, filter);
        mForegroundDispatchEnabled = true;
        LogUtils.d(TAG, "Start reading");
    }

    private void disableForegroundDispatch() {
        if (mForegroundDispatchEnabled) {
            NfcAdapter.getDefaultAdapter(this).disableForegroundDispatch(this);
            mForegroundDispatchEnabled = false;
            LogUtils.d(TAG, "Stop reading");
        }
    }

    private void handleIntent(Intent intent) {
        if (NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction())) {
            LogUtils.d(TAG, "Receive intent");
            showProgressDialog(R.string.text_reading);
            new ParseTagTask(intent).execute();
        }
    }

    private class ParseTagTask extends
            AsyncTask<Object, Object, Boolean> {

        Intent intent;

        public ParseTagTask(Intent intent) {
            this.intent = intent;
        }

        @Override
        protected Boolean doInBackground(Object... params) {
            Boolean success = false;
            Tag tag = intent.getExtras().getParcelable(NfcAdapter.EXTRA_TAG);
            IsoDep isoDep = IsoDep.get(tag);
            if (isoDep != null) {
                LogUtils.d(TAG, "Got IsoDep");
                try {
                    Security.insertProviderAt(
                            new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
                    net.sf.scuba.smartcards.CardService cardService =
                            net.sf.scuba.smartcards.CardService.getInstance(isoDep);
                    PassportService passportService = new PassportService(cardService);
                    passportService.open();
                    passportService.sendSelectApplet(false);
                    passportService.doBAC(new BACKey(mMrz.documentNumber, mMrz.dateOfBirth,
                            mMrz.dateOfExpiry));

                    COMFile com = (COMFile) LDSFileUtil.getLDSFile(PassportService.EF_COM,
                            passportService.getInputStream(PassportService.EF_COM));

                    ArrayList<Integer> tags = new ArrayList<Integer>();
                    for (int id : com.getTagList()) {
                        LogUtils.d(TAG, "has: ", id);
                        tags.add(id);
                    }
                    if (tags.contains(LDSFile.EF_DG1_TAG)) {
                        DG1File dg1 = (DG1File) LDSFileUtil.getLDSFile(PassportService.EF_DG1,
                                passportService.getInputStream(PassportService.EF_DG1));
                        publishProgress(dg1.getMRZInfo());
                    }

                    if (tags.contains(LDSFile.EF_DG2_TAG)) {
                        DG2File dg2 = (DG2File) LDSFileUtil.getLDSFile(PassportService.EF_DG2,
                                passportService.getInputStream(PassportService.EF_DG2));
                        List<FaceInfo> faceInfos = dg2.getFaceInfos();
                        List<FaceImageInfo> faceImageInfos = new ArrayList<FaceImageInfo>();
                        for (FaceInfo faceInfo : faceInfos) {
                            faceImageInfos.clear();
                            faceImageInfos.addAll(faceInfo.getFaceImageInfos());
                            for (FaceImageInfo faceImageInfo : faceImageInfos) {
                                Bitmap image = BitmapFactory.decodeStream(faceImageInfo
                                        .getImageInputStream());
                                publishProgress(image);
                            }
                        }
                    }
                    success = true;
                } catch (Exception e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                }
            }
            return success;
        }

        @Override
        protected void onProgressUpdate(Object... values) {
            super.onProgressUpdate(values);
            if (values[0] instanceof MRZInfo) {
                MRZInfo info = (MRZInfo) values[0];
                mType.setText(info.getDocumentCode());
                mCode.setText(info.getIssuingState());
                mPassportNumber.setText(info.getDocumentNumber());
                mName.setText(info.getPrimaryIdentifier() + ", "
                        + TextUtils.join(" ", info.getSecondaryIdentifierComponents()));
                mNationality.setText(info.getNationality());
                mPersonalNumber.setText(info.getPersonalNumber());
                mGender.setText(getGender(info.getGender()));
            } else if (values[0] instanceof Bitmap) {
                ImageView imageView = new ImageView(ReadPassportActivity.this);
                imageView.setImageBitmap((Bitmap) values[0]);
                mInfoContainer.addView(imageView,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT);
                mImages.add(imageView);
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            if (!isFinishing()) {
                dismissProgressDialog();
                disableForegroundDispatch();
                if (!result) {
                    Toast.makeText(ReadPassportActivity.this, "Read error", Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private static final int DOCUMENT_TYPE_PASSPORT = 3;

    private static String getDocumentType(int type) {
        String typeString = null;
        switch (type) {
            case DOCUMENT_TYPE_PASSPORT:
                typeString = "P";
                break;
        }
        return typeString;
    }

    private static String getGender(Gender gender) {
        String genderString = null;
        switch (gender) {
            case MALE:
                genderString = "M";
                break;

            case FEMALE:
                genderString = "F";
                break;

            default:
                genderString = "U";
                break;
        }
        return genderString;
    }

    private static SimpleDateFormat sMrzDateFormat = new SimpleDateFormat("yyMMdd");

    private static class Mrz {
        private String documentNumber;
        private Date dateOfBirth;
        private Date dateOfExpiry;

        private static Mrz parse(String mrzString) {
            Mrz mrz = null;
            if (!TextUtils.isEmpty(mrzString) && mrzString.length() >= 28) {
                mrz = new Mrz();
                mrz.documentNumber = mrzString.substring(0, 9);
                String birth = mrzString.substring(13, 19);
                String expiry = mrzString.substring(21, 27);
                LogUtils.d(TAG, "number: ", mrz.documentNumber);
                LogUtils.d(TAG, "birth: ", birth);
                LogUtils.d(TAG, "expiry: ", expiry);
                try {
                    mrz.dateOfBirth = sMrzDateFormat.parse(birth);
                    mrz.dateOfExpiry = sMrzDateFormat.parse(expiry);
                } catch (ParseException e) {
                    LogUtils.e(TAG, e.getMessage(), e);
                    mrz = null;
                }
            }
            return mrz;
        }
    }
}
