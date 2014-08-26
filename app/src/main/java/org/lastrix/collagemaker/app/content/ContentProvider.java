package org.lastrix.collagemaker.app.content;

import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import org.lastrix.collagemaker.app.BuildConfig;

import java.util.Arrays;

/**
 * ContentProvider used for caching api calls results.<br/>
 * This is required because of strict limits on number of api calls per hour
 * and per node.<br/>
 * NOTICE:<br/>
 * This content provider does not provide per item insert or delete.<br/>
 * You may call {@link #call(String, String, android.os.Bundle)}
 * with method {@link #CALL_FLUSH} to remove obsolete entries.</br>
 * ContentProvider will automatically flush obsolete data at {@link #onCreate()} .
 */
public class ContentProvider extends android.content.ContentProvider {

    private static final String LOG_MESSAGE_FAILED_SQL = "Failed to execute sql";
    private static final String LOG_MESSAGE_CALLING_INSERT = "Calling inserting single record is not advisable.";
    private static final String LOG_TAG = ContentProvider.class.getSimpleName();
    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;


    public static final String CALL_FLUSH = "flush";
    public static final String CALL_FLUSH_RESULT = "RESULT";
    public static final String AUTHORITY = "org.lastrix.collagemaker.app.content";
    private static final UriMatcher sUriMatcher;
    private static final int CODE_USER = 1;
    private static final int CODE_PHOTO = 2;
    private static final int CODE_PHOTO_UPDATE = 3;
    private static final int CODE_USER_UPDATE = 4;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        sUriMatcher.addURI(AUTHORITY, User.TABLE_NAME, CODE_USER);
        sUriMatcher.addURI(AUTHORITY, Photo.TABLE_NAME, CODE_PHOTO);
        sUriMatcher.addURI(AUTHORITY, Photo.TABLE_NAME + "/#", CODE_PHOTO_UPDATE);
        sUriMatcher.addURI(AUTHORITY, User.TABLE_NAME + "/#", CODE_USER_UPDATE);
    }


    private DatabaseHelper mDatabaseHelper;

    public ContentProvider() {
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        //this method is not supposed to work with single entries.
        // calling this runs SQL_FLUSH
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                db.execSQL(User.SQL_FLUSH);
                return 1;

            case CODE_PHOTO:
                db.execSQL(Photo.SQL_FLUSH);
                return 1;

            default:
                throw new UnsupportedOperationException(String.format("Incorrect uri [%s]", uri.toString()));
        }
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                return "android.cursor.dir/" + AUTHORITY + User.TABLE_NAME;

            case CODE_PHOTO:
                return "android.cursor.dir/" + AUTHORITY + Photo.TABLE_NAME;

            default:
                throw new UnsupportedOperationException(String.format("Incorrect uri [%s]", uri.toString()));
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        //this method is not supposed to work with single entries.
        // you'd better you bulkInsert
        Log.w(LOG_TAG, LOG_MESSAGE_CALLING_INSERT);
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                db.insert(User.TABLE_NAME, null, values);
                return ContentHelper.getUserUri(null);

            case CODE_PHOTO:
                db.insert(Photo.TABLE_NAME, null, values);
                return ContentHelper.getPhotoUri(null);

            default:
                throw new UnsupportedOperationException(String.format("Incorrect uri [%s]", uri.toString()));
        }
    }

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DatabaseHelper(getContext());
        flush();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getReadableDatabase();
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                return db.query(User.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

            case CODE_PHOTO:
                return db.query(Photo.TABLE_NAME, projection, selection, selectionArgs, null, null, sortOrder);

            default:
                throw new UnsupportedOperationException(String.format("Incorrect uri [%s]", uri.toString()));
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        if ( LOG_ALL ) {
            Log.v(LOG_TAG, "Uri = " + uri.toString() + " contentValues=" + values + " selection=" + selection + " selectionArgs=" + Arrays.toString(selectionArgs));
        }
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                return db.update(User.TABLE_NAME, values, selection, selectionArgs);

            case CODE_PHOTO:
                return db.update(Photo.TABLE_NAME, values, selection, selectionArgs);

            case CODE_USER_UPDATE:
                return db.update(User.TABLE_NAME, values, selection, selectionArgs);

            case CODE_PHOTO_UPDATE:
                return db.update(Photo.TABLE_NAME, values, selection, selectionArgs);

            default:
                throw new UnsupportedOperationException(String.format("Incorrect uri [%s]", uri.toString()));
        }
    }

    @Override
    public int bulkInsert(Uri uri, @NonNull ContentValues[] values) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        switch (sUriMatcher.match(uri)) {
            case CODE_USER:
                return bulkInsertInner(values, db, User.TABLE_NAME);

            case CODE_PHOTO:
                return bulkInsertInner(values, db, Photo.TABLE_NAME);

            default:
                return super.bulkInsert(uri, values);
        }
    }

    private int bulkInsertInner(ContentValues[] values, SQLiteDatabase db, String tableName) {
        int numInserted = 0;
        db.beginTransaction();
        try {
            for (ContentValues v : values) {
                db.insertOrThrow(tableName, null, v);
                numInserted++;
            }
            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SQL, e);
            return 0;
        } finally {
            db.endTransaction();
        }
        return numInserted;
    }

    @Override
    public Bundle call(String method, String arg, Bundle extras) {
        if (CALL_FLUSH.equals(method)) {
            return flush();
        }
        return null;
    }

    private Bundle flush() {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        boolean failed = false;
        try {
            db.execSQL(Photo.SQL_FLUSH);
            db.execSQL(User.SQL_FLUSH);
        } catch (SQLException e) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SQL, e);
            failed = true;
        }
        Bundle bundle = new Bundle();
        bundle.putBoolean(CALL_FLUSH_RESULT, !failed);
        return bundle;
    }
}
