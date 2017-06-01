package com.biginnov.investigator.activity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.biginnov.investigator.Constants;
import com.biginnov.investigator.R;
import com.biginnov.investigator.fragment.BiometricFragment;
import com.biginnov.investigator.util.BiographicData;
import com.biginnov.investigator.util.CommonUtils;
import com.biginnov.investigator.util.LogUtils;
import com.ftdi.j2xx.D2xxManager;
import com.ftdi.j2xx.FT_Device;
import com.neurotec.biometrics.NSubject;
import com.neurotec.biometrics.client.NBiometricClient;
import com.neurotec.devices.NDeviceManager;
import com.neurotec.devices.NDeviceType;
import com.neurotec.lang.NCore;

import java.util.Collection;
import java.util.EnumSet;

import sensor.fpc.hst.fpcsensor.Fingerprint;

public abstract class BiometricActivity extends BaseActivity {

    private static final String TAG = BiometricActivity.class.getSimpleName();

    protected NBiometricClient mBiometricClient;
    protected NSubject mSubject;

    protected Handler mHandler;
    protected FragmentManager mFragmentManager;
    protected boolean mInitCompleted;

    private D2xxManager mD2xxManager;
    private FT_Device mFtDevice;

    private int devCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new Handler();
        mFragmentManager = getSupportFragmentManager();
        CommonUtils.executeAsyncTaskSerially(new InitTask());
    }

    @Override
    protected void onDestroy() {
        LogUtils.d(TAG, "onDestroy()............");
        release();
        super.onDestroy();
    }

    public NBiometricClient getBiometricClient() {
        return mBiometricClient;
    }

    public NSubject getSubject() {
        return mSubject;
    }

    public void setSubject(NSubject subjects) {
        this.mSubject = subjects;
    }

    public D2xxManager getD2xxManager() {
        return mD2xxManager;
    }

    public int getDevCount(D2xxManager d2xxManager) {
        int devCount = 0;
        if (d2xxManager != null) {
            devCount = d2xxManager.createDeviceInfoList(getApplicationContext());
            LogUtils.d(TAG, "devCount: " + devCount);
        }
        return devCount;
    }

    public void setFTDevice(FT_Device ftDevice) {
        this.mFtDevice = ftDevice;
    }

    public FT_Device getFtDevice() {
        if (mFtDevice != null) {
            return mFtDevice;
        }
        return null;
    }

    public abstract void onInitCompleted();

    public abstract void onCaptureCompleted(int requestId);

    public abstract void onCaptureCancelled(int requestId);

    protected void init() {
        LogUtils.d(TAG, "init()...................");
        mSubject = new NSubject();
        mBiometricClient = new NBiometricClient();
        mBiometricClient.setBiographicDataSchema(BiographicData.getSchema());
        mBiometricClient.setDatabaseConnectionToSQLite(CommonUtils.combinePath(
                NCore.getContext().getFilesDir().getAbsolutePath(), "Biometrics.db"));
        mBiometricClient.setUseDeviceManager(true);
        NDeviceManager deviceManager = mBiometricClient.getDeviceManager();
        // set type of the device used
        deviceManager.setDeviceTypes(EnumSet.copyOf(getDeviceTypes()));
        if (!isFinishing()) {
            mBiometricClient.initialize();
        }

        try {
            mD2xxManager = D2xxManager.getInstance(this);
        } catch (D2xxManager.D2xxException e) {
            LogUtils.e(TAG, e.toString());
        }

        mFtDevice = null;
    }

    protected void initAsync() {
        CommonUtils.executeAsyncTaskSerially(new InitTask());
    }

    protected class InitTask extends AsyncTask<Object, Object, Object> {

        @Override
        protected Object doInBackground(Object... params) {
            init();
            return null;
        }

        @Override
        protected void onPostExecute(Object result) {
            if (!isFinishing()) {
                mInitCompleted = true;
                onInitCompleted();
            } else {
                release();
            }
        }
    }

    protected Collection<NDeviceType> getDeviceTypes() {
        return Constants.DEVICE_TYPE_LIST;
    }

    protected void release() {
        if (mBiometricClient != null) {
            mBiometricClient.cancel();
            mBiometricClient.dispose();
            mBiometricClient = null;
        }
    }

    protected boolean removeCurrentFragment() {
        boolean removed = false;
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            mFragmentManager.beginTransaction().remove(fragment).commitAllowingStateLoss();
            removed = true;
        }
        return removed;
    }

    protected boolean stopCapture() {
        boolean stopped = false;
        Fragment fragment = mFragmentManager.findFragmentById(R.id.fragment_container);
        if (fragment != null && fragment instanceof BiometricFragment) {
            ((BiometricFragment) fragment).stopCapture();
            stopped = true;
        }
        return stopped;
    }
}
