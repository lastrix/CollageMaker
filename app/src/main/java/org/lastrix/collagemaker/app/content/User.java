package org.lastrix.collagemaker.app.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.BaseColumns;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Holds all necessary information about user.
 * Created by lastrix on 8/21/14.
 */
public class User {

    public static final String TABLE_NAME = "user";
    public final static String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    public final static String COLUMN_ID = BaseColumns._ID;
    public final static String COLUMN_NAME = "name";
    public final static String COLUMN_NICK = "nick";
    public final static String COLUMN_PHOTO_URL = "photo_url";
    public final static String COLUMN_TIMESTAMP = "stamp";
    public final static String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_NAME + " TEXT NOT NULL, " +
            COLUMN_NICK + " TEXT NOT NULL, " +
            COLUMN_PHOTO_URL + " TEXT NOT NULL, " +
            COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL" +
            ");";
    public final static int CACHE_EXPIRE = 48; //hours
    public final static String SQL_FLUSH = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_TIMESTAMP + " <= datetime( 'now', '-" + CACHE_EXPIRE + " hours' );";
    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "full_name";
    private static final String FIELD_USERNAME = "username";
    private static final String FIELD_PHOTO_URL = "profile_picture";

    private final long mId;
    private final String mName;
    private final String mUsername;
    private final String mPhotoUrl;

    public User(long id, String name, String nick, String photoUrl) {
        this.mId = id;
        this.mName = name;
        this.mUsername = nick;
        this.mPhotoUrl = photoUrl;
    }

    /**
     * Fetch user object from its json representation
     *
     * @param o -- source
     * @return user object
     * @throws JSONException in case of bad things happened.
     */
    public static User fromJson(JSONObject o) throws JSONException {
        final int id = o.getInt(FIELD_ID);
        final String name = o.getString(FIELD_NAME);
        final String username = o.getString(FIELD_USERNAME);
        final String photoUrl = o.getString(FIELD_PHOTO_URL);
        return new User(id, name, username, photoUrl);
    }

    /**
     * Recover user from bundle
     *
     * @param bundle -- source
     * @return user
     */
    public static User fromBundle(Bundle bundle) {
        final long id = bundle.getLong(FIELD_ID);
        final String name = bundle.getString(FIELD_NAME);
        final String username = bundle.getString(FIELD_USERNAME);
        final String photoUrl = bundle.getString(FIELD_PHOTO_URL);
        return new User(id, name, username, photoUrl);
    }

    public static User fromCursor(Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        final String photoUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PHOTO_URL));
        final String name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        final String nick = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NICK));
        return new User(id, name, nick, photoUrl);
    }

    /**
     * Get user id
     *
     * @return id
     */
    public long getId() {
        return mId;
    }

    /**
     * Get user name
     *
     * @return name
     */
    public String getName() {
        return mName;
    }

    /**
     * Get nick
     *
     * @return nick
     */
    public String getUsername() {
        return mUsername;
    }

    /**
     * Get user avatar url
     *
     * @return avatar url
     */
    public String getPhotoUrl() {
        return mPhotoUrl;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof User && mId == ((User) o).mId;

    }

    @Override
    public int hashCode() {
        return (int) (mId ^ (mId >>> 32));
    }

    @Override
    public String toString() {
        return "User{" +
                "mId=" + mId +
                ", mName='" + mName + '\'' +
                ", mUsername='" + mUsername + '\'' +
                ", mPhotoUrl='" + mPhotoUrl + '\'' +
                '}';
    }

    /**
     * Convert user to bundle
     *
     * @return bundle
     */
    public Bundle asBundle() {
        Bundle bundle = new Bundle(4);
        bundle.putLong(FIELD_ID, mId);
        bundle.putString(FIELD_NAME, mName);
        bundle.putString(FIELD_USERNAME, mUsername);
        bundle.putString(FIELD_PHOTO_URL, mPhotoUrl);
        return bundle;
    }

    public ContentValues asContentValues() {
        final ContentValues values = new ContentValues(4);
        values.put(COLUMN_ID, mId);
        values.put(COLUMN_NAME, mName);
        values.put(COLUMN_NICK, mUsername);
        values.put(COLUMN_PHOTO_URL, mPhotoUrl);
        return values;
    }
}
