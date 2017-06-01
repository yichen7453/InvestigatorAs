package com.biginnov.investigator.util;

import android.content.Context;
import android.text.TextUtils;

import com.biginnov.investigator.R;
import com.neurotec.biometrics.NBiographicDataElement;
import com.neurotec.biometrics.NBiographicDataSchema;
import com.neurotec.biometrics.NBiographicDataSchema.ElementCollection;
import com.neurotec.biometrics.NDBType;
import com.neurotec.biometrics.NMatchingResult;
import com.neurotec.biometrics.NSubject;

import java.util.HashMap;

public class BiographicData {
    private static final String TAG = BiographicData.class.getSimpleName();

    public static final String UUID = "uuid";
    public static final String NATIONALITY = "nationality";
    public static final String DOCUMENT_NUMBER = "document_number";
    public static final String NAME = "name";
    public static final String SEX = "sex";
    public static final String AGE = "age";
    public static final String ROLE = "role";

    public static final int SEX_FEMALE = 0;
    public static final int SEX_MALE = 1;
    public static final int SEX_UNSPECIFIED = 2;

    public static final int ROLE_ADMINISTRATOR = 0;
    public static final int ROLE_IMMIGRANT = 1;
    public static final int ROLE_CRIMINAL = 2;
    public static final int ROLE_UNKNOWN = 9;

    private static final HashMap<String, NDBType> TYPES = new HashMap<String, NDBType>();

    static {
        TYPES.put(UUID, NDBType.STRING);
        TYPES.put(NATIONALITY, NDBType.STRING);
        TYPES.put(DOCUMENT_NUMBER, NDBType.STRING);
        TYPES.put(NAME, NDBType.STRING);
        TYPES.put(SEX, NDBType.INTEGER);
        TYPES.put(AGE, NDBType.INTEGER);
        TYPES.put(ROLE, NDBType.INTEGER);
    }

    private static final String[] DEFAULT_SCHEMA = new String[]{
            UUID,
            NATIONALITY,
            DOCUMENT_NUMBER,
            NAME,
            SEX,
            AGE,
            ROLE
    };

    public static NBiographicDataSchema getSchema() {
        NBiographicDataSchema schema = new NBiographicDataSchema();
        ElementCollection elements = schema.getElements();
        for (String column : DEFAULT_SCHEMA) {
            elements.add(new NBiographicDataElement(column, column, TYPES.get(column)));
        }
        return schema;
    }

    public static String getUuid(NSubject subject) {
        String value = null;
        try {
            value = (String) subject.getProperty(UUID);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static String getNationality(NSubject subject) {
        String value = null;
        try {
            value = (String) subject.getProperty(NATIONALITY);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static String getDocumentNumber(NSubject subject) {
        String value = null;
        try {
            value = (String) subject.getProperty(DOCUMENT_NUMBER);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static String getName(NSubject subject) {
        String value = null;
        try {
            value = (String) subject.getProperty(NAME);
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static int getSex(NSubject subject) {
        int value = SEX_UNSPECIFIED;
        try {
            value = ((Long) subject.getProperty(SEX)).intValue();
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static String getDisplayedSex(Context context, NSubject subject) {
        String displayedGender;
        int sex = getSex(subject);
        switch (sex) {
            case SEX_FEMALE:
                displayedGender = context.getString(R.string.text_female);
                break;

            case SEX_MALE:
                displayedGender = context.getString(R.string.text_male);
                break;

            case SEX_UNSPECIFIED:
            default:
                displayedGender = context.getString(R.string.text_unspecified);
                break;

        }
        return displayedGender;
    }

    public static int getAge(NSubject subject) {
        int value = 0;
        try {
            value = ((Long) subject.getProperty(AGE)).intValue();
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static int getRole(NSubject subject) {
        int value = ROLE_UNKNOWN;
        try {
            value = ((Long) subject.getProperty(ROLE)).intValue();
        } catch (Exception e) {
            LogUtils.e(TAG, e.getMessage(), e);
        }
        return value;
    }

    public static boolean isAdmin(NSubject subject) {
        return getRole(subject) == ROLE_ADMINISTRATOR;
    }

    public static boolean isAdmin(NMatchingResult result) {
        return isAdmin(result.getSubject());
    }

    public static class Injector {

        NSubject subject;

        public Injector(NSubject subject) {
            this.subject = subject;
        }

        public Injector setId(String value) {
            if (!TextUtils.isEmpty(value)) {
                subject.setId(value);
            }
            return this;
        }

        public Injector setUuid(String value) {
            if (!TextUtils.isEmpty(value)) {
                subject.setProperty(UUID, value);
            }
            return this;
        }

        public Injector setNationality(String value) {
            if (!TextUtils.isEmpty(value)) {
                subject.setProperty(NATIONALITY, value);
            }
            return this;
        }

        public Injector setDocumentNumber(String value) {
            if (!TextUtils.isEmpty(value)) {
                subject.setProperty(DOCUMENT_NUMBER, value);
            }
            return this;
        }

        public Injector setName(String value) {
            if (!TextUtils.isEmpty(value)) {
                subject.setProperty(NAME, value);
            }
            return this;
        }

        public Injector setSex(int value) {
            subject.setProperty(SEX, value);
            return this;
        }

        public Injector setAge(int value) {
            subject.setProperty(AGE, value);
            return this;
        }

        public Injector setRole(int value) {
            subject.setProperty(ROLE, value);
            return this;
        }
    }
}
