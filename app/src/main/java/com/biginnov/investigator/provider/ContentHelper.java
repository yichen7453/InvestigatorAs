package com.biginnov.investigator.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.text.TextUtils;

import com.biginnov.investigator.provider.dto.Admin;
import com.biginnov.investigator.util.LogUtils;

import java.util.ArrayList;

public class ContentHelper {
    public static final String TAG = ContentHelper.class.getSimpleName();

    private ContentResolver mResolver = null;

    public ContentHelper(Context context) {
        mResolver = context.getContentResolver();
    }

    public long addAdmin(Admin admin) {
        long id = 0l;
        if (admin != null) {
            try {
                id = ContentUris.parseId(
                        mResolver.insert(AdminTable.CONTENT_URI, admin.toContentValues()));
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
        return id;
    }

    public void deleteAdmin(Admin admin) {
        if (admin != null) {
            String name = admin.getName();
            deleteAdmin(name);
        }
    }

    public void deleteAdmin(String name) {
        if (!TextUtils.isEmpty(name)) {
            try {
                String where = AdminTable.ColumnNames.NAME + " = ?";
                mResolver.delete(AdminTable.CONTENT_URI, where, new String[]{name});
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
    }

    public Admin getAdmin(String name) {
        Admin result = null;
        if (!TextUtils.isEmpty(name)) {
            String where = AdminTable.ColumnNames.NAME + " = ?";
            Cursor cursor = mResolver.query(AdminTable.CONTENT_URI, AdminTable.PROJECTION,
                    where, new String[]{name}, null);
            if (cursor != null) {
                if (cursor.moveToNext()) {
                    result = Admin.fromCursor(cursor);
                }
                cursor.close();
            }
        }
        return result;
    }

    public ArrayList<Admin> getAllAdmin() {
        ArrayList<Admin> result = new ArrayList<>();
        Cursor cursor = mResolver.query(
                AdminTable.CONTENT_URI, AdminTable.PROJECTION, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                Admin admin = Admin.fromCursor(cursor);
                result.add(admin);
            }
            cursor.close();
        }
        return result;
    }

    public boolean hasAdmin() {
        boolean hasAdmin = false;
        Cursor cursor = mResolver.query(
                AdminTable.CONTENT_URI, AdminTable.PROJECTION, null, null, null);
        if (cursor != null) {
            hasAdmin = cursor.moveToNext();
            cursor.close();
        }
        return hasAdmin;
    }

    public void updateAdmin(Admin admin) {
        if (admin != null) {
            try {
                String where = AdminTable.ColumnNames.UUID + " = ?";
                mResolver.update(AdminTable.CONTENT_URI, admin.toContentValues(), where,
                        new String[]{admin.getUuid()});
            } catch (Exception e) {
                LogUtils.e(TAG, e.getMessage(), e);
            }
        }
    }
}