package org.lastrix.collagemaker.app.content;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Handles creation and upgrade process of cache sqlite database.
 * Created by lastrix on 8/25/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    public static final String LOG_MESSAGE_FAILED_SQL = "Failed to execute sql.";
    public static final String LOG_TAG = DatabaseHelper.class.getSimpleName();
    private final static String DATABASE_NAME = "collage.sqlite";
    private final static int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();
        try {
            //install schema
            db.execSQL(Photo.SQL_CREATE);
            db.execSQL(User.SQL_CREATE);

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SQL, e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();
        try {
            //drop schema
            db.execSQL(Photo.SQL_DROP);
            db.execSQL(User.SQL_DROP);

            //install new one
            db.execSQL(Photo.SQL_CREATE);
            db.execSQL(User.SQL_CREATE);

            db.setTransactionSuccessful();
        } catch (SQLException e) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SQL, e);
        } finally {
            db.endTransaction();
        }
    }
}
