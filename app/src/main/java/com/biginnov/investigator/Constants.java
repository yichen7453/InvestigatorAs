package com.biginnov.investigator;

import com.biginnov.investigator.util.LicenseManager;
import com.neurotec.devices.NDeviceType;

import java.util.ArrayList;
import java.util.Arrays;

public class Constants {
    public static final boolean DEVELOPER_MODE = true;

    public static final String[] LICENSE_ARRAY = new String[]{
            LicenseManager.LICENSE_DEVICES_CAMERAS,
            LicenseManager.LICENSE_FACE_EXTRACTION,
            LicenseManager.LICENSE_FACE_DETECTION,
            LicenseManager.LICENSE_FACE_MATCHING,
//            LicenseManager.LICENSE_FACE_MATCHING_FAST,
            LicenseManager.LICENSE_FINGER_EXTRACTION,
            LicenseManager.LICENSE_FINGER_DETECTION,
            LicenseManager.LICENSE_FINGER_MATCHING
//            LicenseManager.LICENSE_FINGER_MATCHING_FAST
    };

    public static final ArrayList<String> LICENSE_LIST =
            new ArrayList<>(Arrays.asList(LICENSE_ARRAY));

    public static final NDeviceType[] DEVICE_TYPE_ARRAY = new NDeviceType[]{
            NDeviceType.CAMERA,
            NDeviceType.FSCANNER,
            NDeviceType.FINGER_SCANNER
    };
    public static final ArrayList<NDeviceType> DEVICE_TYPE_LIST =
            new ArrayList<>(Arrays.asList(DEVICE_TYPE_ARRAY));

    //    public static final String DEFAULT_ADMIN_ACCOUNT = "tsadmin";
    //    public static final String DEFAULT_ADMIN_PASSWORD = "tsp@ssw0rd";
    public static final String DEFAULT_ADMIN_ACCOUNT = "a";
    public static final String DEFAULT_ADMIN_PASSWORD = "a";

    public static final String BUNDLE_PARAMETER_NAME = "name";
    public static final String BUNDLE_PARAMETER_PASSWORD = "password";
    public static final int REQUEST_CODE_MODIFY_PASSWORD = 100;
    public static final int SPINNER_AGE_START = 16;
    public static final int SPINNER_AGE_END = 70;
}
