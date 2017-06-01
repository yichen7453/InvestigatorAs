package com.biginnov.investigator.provider.dto;

import android.content.ContentValues;
import android.database.Cursor;

import com.biginnov.investigator.provider.AdminTable;

import java.util.UUID;

public class Admin {

    private static final String TAG = Admin.class.getSimpleName();
    private long mId;
    private String mUuid;
    private String mName;
    private String mPassword = null;
    private int mFaceCount = 0;
    private int mFingerprintCount = 0;

    private Admin() {
        this(false);
    }

    private Admin(boolean randomUuid) {
        if (randomUuid) {
            mUuid = UUID.randomUUID().toString();
        }
    }

    public Admin(String name, String password, int faceCount, int fingerCount) {
        mUuid = UUID.randomUUID().toString();
        mName = name;
        mPassword = password;
        mFaceCount = faceCount;
        mFingerprintCount = fingerCount;
    }

    public static Admin fromCursor(Cursor cursor) {
        Admin admin = new Admin();
        admin.mId = cursor.getLong(AdminTable.ColumnIndexes.ID);
        admin.mUuid = cursor.getString(AdminTable.ColumnIndexes.UUID);
        admin.mName = cursor.getString(AdminTable.ColumnIndexes.NAME);
        admin.mPassword = cursor.getString(AdminTable.ColumnIndexes.PASSWORD);
        admin.mFaceCount = cursor.getInt(AdminTable.ColumnIndexes.FACE_COUNT);
        admin.mFingerprintCount = cursor.getInt(AdminTable.ColumnIndexes.FINGERPRINT_COUNT);
        return admin;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues();
        values.put(AdminTable.ColumnNames.UUID, mUuid);
        values.put(AdminTable.ColumnNames.NAME, mName);
        values.put(AdminTable.ColumnNames.PASSWORD, mPassword);
        values.put(AdminTable.ColumnNames.FACE_COUNT, mFaceCount);
        values.put(AdminTable.ColumnNames.FINGERPRINT_COUNT, mFingerprintCount);
        return values;
    }

    public long getId() {
        return mId;
    }

    public String getUuid() {
        return mUuid;
    }

    public String getName() {
        return mName;
    }

    public String getPassword() {
        return mPassword;
    }

    public void setmPassword(String newPassword){
        mPassword = newPassword;
    }

    public int getFaceCount() {
        return mFaceCount;
    }

    public void setFaceCount(int count) {
        mFaceCount = count;
    }

    public void addFaceCount() {
        mFaceCount++;
    }

    public int getFingerprintCount() {
        return mFingerprintCount;
    }

    public void setFingerprintCount(int count) {
        mFingerprintCount = count;
    }

    public void addFingerprintCount() {
        mFingerprintCount++;
    }
}