package kr.go.mobile.agent.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import kr.go.mobile.agent.v3.solution.DKI_LocalPushSolution;

public class PushMessageDBHelper extends SQLiteOpenHelper {
    static final int DB_VERSION = 1;
    static final String DB_LOCAL_PUSH = "CommonBasedSystem.db";
    private static final String TAG = PushMessageDBHelper.class.getSimpleName();

    public static class NoticeEntry implements BaseColumns {
        public static final String TABLE_NAME = "common_based_system_notice";
        public static final String COLUMN_NAME_REQID = "requestId";
        public static final String COLUMN_NAME_NOTICE_TYPE = "type";
        public static final String COLUMN_NAME_NOTICE_TITLE = "title";
        public static final String COLUMN_NAME_NOTICE_MESSAGE = "message";
        public static final String COLUMN_NAME_MESSAGE_ORIGINAL = "original";
        public static final String COLUMN_NAME_CHECKED_NOTICE = "checked";
    }

    public static int UNCHECKED = 0;
    public static int CHECKED = 1;

    private static PushMessageDBHelper helper;
    public static void newInstance(Context context) {
        if (helper == null) {
            helper = new PushMessageDBHelper(context);
        }
    }

    public static void insertNotice(DKI_LocalPushSolution.PushMessage message) throws SQLException {
        if (helper == null) return;
        helper.insertNewNotice(message);
    }

    public static void checkedNotice(String reqId) {
        helper.updateCheckedNotice(reqId);
    }

    public static List<String> unreadNotice() {
        if (helper == null) return new ArrayList<>();
        return helper.selectWithUnchecked();
    }

    public static String[] unreadNotice(String reqId) {
        return helper.selectWithUnchecked(reqId);
    }

    final String DROP_TABLE_NOTICE = "DROP TABLE IF EXISTS " + NoticeEntry.TABLE_NAME;
    final String CREATE_TABLE_NOTICE = "CREATE TABLE IF NOT EXISTS " + NoticeEntry.TABLE_NAME + " ( " +
            NoticeEntry.COLUMN_NAME_REQID + " INTEGER PRIMARY KEY, " +
            NoticeEntry.COLUMN_NAME_NOTICE_TITLE + " TEXT, " +
            NoticeEntry.COLUMN_NAME_NOTICE_TYPE + " INTEGER, " +
            NoticeEntry.COLUMN_NAME_NOTICE_MESSAGE + " TEXT, " +
            NoticeEntry.COLUMN_NAME_MESSAGE_ORIGINAL + " TEXT, " +
            NoticeEntry.COLUMN_NAME_CHECKED_NOTICE  + " INTEGER )";



    public PushMessageDBHelper(Context context) {
        super(context, DB_LOCAL_PUSH, null, DB_VERSION);
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_NOTICE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(DROP_TABLE_NOTICE);
        db.execSQL(CREATE_TABLE_NOTICE);
    }

    private void insertNewNotice(DKI_LocalPushSolution.PushMessage message) {
        if (message.getMessage().isEmpty()) {
            Log.e(TAG, "푸시 메시지를 등록할 수 없습니다. (메시지 값이 존재하지 않습니다.)");
            return;
        }
        ContentValues values = new ContentValues();
        values.put(NoticeEntry.COLUMN_NAME_REQID, message.getMessageID());
        values.put(NoticeEntry.COLUMN_NAME_NOTICE_TITLE, message.getMessageTitle());
        values.put(NoticeEntry.COLUMN_NAME_NOTICE_TYPE, message.getMessageType());
        values.put(NoticeEntry.COLUMN_NAME_NOTICE_MESSAGE, message.getMessage());
        try {
            values.put(NoticeEntry.COLUMN_NAME_MESSAGE_ORIGINAL, URLEncoder.encode(message.getMessageOriginal(), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            values.put(NoticeEntry.COLUMN_NAME_MESSAGE_ORIGINAL, "");
        }
        values.put(NoticeEntry.COLUMN_NAME_CHECKED_NOTICE, UNCHECKED);
        if (getWritableDatabase().insert(NoticeEntry.TABLE_NAME, null, values) < 0) {
            Log.e(TAG, "공지 메시지 등록 실패");
        }
    }


    void updateCheckedNotice (String reqId) {
        ContentValues values = new ContentValues();
        values.put(NoticeEntry.COLUMN_NAME_REQID, reqId);
        values.put(NoticeEntry.COLUMN_NAME_CHECKED_NOTICE, CHECKED);
        getWritableDatabase().update(
                NoticeEntry.TABLE_NAME,
                values,
                NoticeEntry.COLUMN_NAME_REQID + " = ? ",
                new String[] {String.valueOf(reqId)}
        );
    }

    // Define a projection that specifies which columns from the database
    // you will actually use after this query.
    String[] projection = {
            NoticeEntry.COLUMN_NAME_REQID,
            NoticeEntry.COLUMN_NAME_NOTICE_TITLE,
            NoticeEntry.COLUMN_NAME_NOTICE_TYPE,
            NoticeEntry.COLUMN_NAME_NOTICE_MESSAGE,
            NoticeEntry.COLUMN_NAME_CHECKED_NOTICE
    };

    // Filter results WHERE "title" = 'My Title'
    final String selection = NoticeEntry.COLUMN_NAME_CHECKED_NOTICE + " = ?";
    final String[] selectionArgs = { String.valueOf(UNCHECKED) };

    // How you want the results sorted in the resulting Cursor
    final String sortOrder = NoticeEntry.COLUMN_NAME_REQID + " DESC";

    private List<String> selectWithUnchecked() {
        Cursor cursor = getReadableDatabase().query(NoticeEntry.TABLE_NAME,
                projection,             // The array of columns to return (pass null to get all)
                selection,              // The columns for the WHERE clause
                selectionArgs,          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null//sortOrder               // The sort order
        );
        List<String> itemIds = new ArrayList<>();
        while(cursor.moveToNext()) {
            String title = cursor.getString(cursor.getColumnIndexOrThrow(NoticeEntry.COLUMN_NAME_NOTICE_TITLE));
            int reqId = cursor.getInt(cursor.getColumnIndexOrThrow(NoticeEntry.COLUMN_NAME_REQID));
            itemIds.add(String.valueOf(reqId));
        }
        return itemIds;
    }

    private String[] selectWithUnchecked(String reqId) {
        Cursor cursor = getReadableDatabase().query(NoticeEntry.TABLE_NAME,
                projection,             // The array of columns to return (pass null to get all)
                NoticeEntry.COLUMN_NAME_REQID + " = ? ",              // The columns for the WHERE clause
                new String[] {String.valueOf(reqId)},          // The values for the WHERE clause
                null,                   // don't group the rows
                null,                   // don't filter by row groups
                null//sortOrder               // The sort order
        );
        String[] ret = new String[2];
        while(cursor.moveToNext()) {
            ret[0] = cursor.getString(cursor.getColumnIndexOrThrow(NoticeEntry.COLUMN_NAME_NOTICE_TITLE));
            ret[1] = cursor.getString(cursor.getColumnIndexOrThrow(NoticeEntry.COLUMN_NAME_NOTICE_MESSAGE));
        }
        return ret;
    }

}
