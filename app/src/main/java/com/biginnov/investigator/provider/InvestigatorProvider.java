package com.biginnov.investigator.provider;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

import com.biginnov.investigator.util.LogUtils;


public class InvestigatorProvider extends ContentProvider {

    public static String TAG = InvestigatorProvider.class.getSimpleName();
    public static final String AUTHORITY = "com.biginnov.investigator.contentprovider";
    public static final String DATABASE_NAME = "investigator.db";
    public static final int DATABASE_VERSION = 2;

    private SQLiteDatabase mDataBase;

    /**
     * Helper class that actually creates and manages the provider's underlying data repository.
     */
    protected static final class MainDatabaseHelper extends SQLiteOpenHelper {

        /*
         * Instantiates an open helper for the provider's SQLite data repository
         * Do not do database creation and upgrade here.
         */
        public MainDatabaseHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
            super(context, name, factory, version);
        }

        /*
         * Creates the data repository. This is called when the provider attempts to open the
         * repository and SQLite reports that it doesn't exist.
         */
        @Override
        public void onCreate(SQLiteDatabase db) {
            createTable(db);
        }


        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            LogUtils.d(TAG, "DatabaseHelper onUpgrade, from ", oldVersion, " to ", newVersion);
        }

        private void createTable(SQLiteDatabase db) {
            // Creates the main table
            LogUtils.d(TAG, "Create new table");
            db.execSQL(AdminTable.getCreateTableSql());
        }
    }

    @Override
    public boolean onCreate() {
        LogUtils.d(TAG, "provider onCreate");
        MainDatabaseHelper dbHelper = new MainDatabaseHelper(getContext(), DATABASE_NAME, null, DATABASE_VERSION);
        mDataBase = dbHelper.getWritableDatabase();
        return mDataBase != null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        Cursor cursor;
        LogUtils.d(TAG, "query uri=", uri, "  selection=", selection);
        switch (sUriMatcher.match(uri)) {
            case CASE_USER_TABLE:
                cursor = mDataBase.query(AdminTable.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (cursor != null) {
            // Tell the cursor what uri to watch, so it knows when its source data changes
            cursor.setNotificationUri(getContext().getContentResolver(), uri);
        }

        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        return null;
    }

    @Override
    public int bulkInsert(Uri uri, ContentValues[] valuesArray) {
        int count = -1;
        switch (sUriMatcher.match(uri)) {

            case CASE_USER_TABLE:
                count = doDefaultBulkInsert(AdminTable.TABLE, null, valuesArray);
                break;

        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        } else if (count == -1) {
            count = super.bulkInsert(uri, valuesArray);
        }
        return count;
    }


    @Override
    public Uri insert(Uri uri, ContentValues values) {
        LogUtils.d(TAG, "insert uri=", uri);
        long rowId;
        switch (sUriMatcher.match(uri)) {
            case CASE_USER_TABLE:
                rowId = mDataBase.insert(AdminTable.TABLE, null, values);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (rowId > 0) {
            Uri rowUri = ContentUris.withAppendedId(uri, rowId);
            getContext().getContentResolver().notifyChange(rowUri, null);
            return rowUri;
        } else {
            throw new SQLException("Failed to insert row into " + uri);
        }
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        int count;
        switch (sUriMatcher.match(uri)) {
            case CASE_USER_TABLE:
                count = mDataBase.delete(AdminTable.TABLE, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        int count;
        switch (sUriMatcher.match(uri)) {
            case CASE_USER_TABLE:
                count = mDataBase.update(AdminTable.TABLE, values, selection, selectionArgs);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
        if (count > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return count;
    }

    private int doDefaultBulkInsert(String table, String nullColumnHack, ContentValues[] valuesArray) {
        int count = 0;
        mDataBase.beginTransaction();
        try {
            for (ContentValues values : valuesArray) {
                mDataBase.insert(table, nullColumnHack, values);
                count++;
            }
            mDataBase.setTransactionSuccessful();
        } catch (Exception e) {
            LogUtils.w(TAG, e.getMessage(), e);
            count = 0;
        } finally {
            mDataBase.endTransaction();
        }
        return count;
    }

    private static final int CASE_USER_TABLE = 1000;

    protected static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(AUTHORITY, AdminTable.TABLE, CASE_USER_TABLE);
    }

}