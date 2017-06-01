package com.biginnov.investigator.provider;

import android.net.Uri;

public class AdminTable {
    public static final String TABLE = "admin";
    public static final Uri CONTENT_URI = Uri.parse(
            "content://" + InvestigatorProvider.AUTHORITY + "/" + TABLE);

    public static String getCreateTableSql() {
        StringBuilder sqlCreate = new StringBuilder();
        sqlCreate.append("CREATE TABLE ").append(AdminTable.TABLE).append("(");
        sqlCreate.append(ColumnNames.ID).append(" INTEGER primary key AUTOINCREMENT,");
        sqlCreate.append(ColumnNames.UUID).append(" TEXT NOT NULL,");
        sqlCreate.append(ColumnNames.NAME).append(" TEXT NOT NULL,");
        sqlCreate.append(ColumnNames.PASSWORD).append(" TEXT,");
        sqlCreate.append(ColumnNames.FACE_COUNT).append(" INTEGER default 0,");
        sqlCreate.append(ColumnNames.FINGERPRINT_COUNT).append(" INTEGER default 0");
        sqlCreate.append(");");
        return sqlCreate.toString();
    }

    public static class ColumnNames {
        public static final String ID = "_id";
        public static final String UUID = "uuid";
        public static final String NAME = "name";
        public static final String PASSWORD = "password";
        public static final String FACE_COUNT = "face_count";
        public static final String FINGERPRINT_COUNT = "fingerprint_count";
    }

    public static final String[] PROJECTION = {
            ColumnNames.ID,
            ColumnNames.UUID,
            ColumnNames.NAME,
            ColumnNames.PASSWORD,
            ColumnNames.FACE_COUNT,
            ColumnNames.FINGERPRINT_COUNT
    };

    public static class ColumnIndexes {
        private static int index = 0;
        public static final int ID = index++;
        public static final int UUID = index++;
        public static final int NAME = index++;
        public static final int PASSWORD = index++;
        public static final int FACE_COUNT = index++;
        public static final int FINGERPRINT_COUNT = index++;
    }
}