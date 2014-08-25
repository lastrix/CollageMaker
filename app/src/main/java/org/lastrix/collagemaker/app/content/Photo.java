package org.lastrix.collagemaker.app.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;

/**
 * Holds  image's urls.
 * See {@link #getImageUrl()} for further details.
 */
public class Photo {

    public final static String COLUMN_ID = BaseColumns._ID;
    public final static String COLUMN_USER_ID = "user_id";
    public final static String COLUMN_THUMBNAIL_URL = "thumbnail_url";
    public final static String COLUMN_IMAGE_URL = "image_url";
    public final static String COLUMN_LIKES = "likes";
    public final static String COLUMN_CHECKED = "checked";
    public final static String COLUMN_TIMESTAMP = "stamp";

    public final static String TABLE_NAME = "photo";
    public final static String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_ID + " INTEGER NOT NULL, " +
            COLUMN_THUMBNAIL_URL + " TEXT NOT NULL, " +
            COLUMN_IMAGE_URL + " TEXT NOT NULL, " +
            COLUMN_LIKES + " INTEGER DEFAULT 0, " +
            COLUMN_CHECKED + " INTEGER DEFAULT 0, " +
            COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL" +
            ");";
    public final static String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    public final static int CACHE_EXPIRE = 12; //hours
    public final static String SQL_FLUSH = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_TIMESTAMP + " <= datetime( 'now', '-" + CACHE_EXPIRE + " hours' );";
    private final long mId;
    private final User mUser;
    private final String mThumbnailUrl;
    private final String mImageUrl;
    private final int mLikes;
    private boolean mChecked;

    public Photo(User user, String thumbnailUrl, String imageUrl, int likes) {
        this(-1, user, thumbnailUrl, imageUrl, likes, false);
    }

    public Photo(long id, User user, String thumbnailUrl, String imageUrl, int likes) {
        this(id, user, thumbnailUrl, imageUrl, likes, false);
    }

    public Photo(long id, User user, String thumbnailUrl, String imageUrl, int likes, boolean checked) {
        this.mId = id;
        this.mUser = user;
        this.mThumbnailUrl = thumbnailUrl;
        this.mImageUrl = imageUrl;
        this.mLikes = likes;
        this.mChecked = checked;
    }

    public static Photo fromCursor(User owner, Cursor cursor) {
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        final long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        final String thumbUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THUMBNAIL_URL));
        final String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL));
        final int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
        final boolean checked = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_CHECKED)) != 0;

        if (userId != owner.getId()) throw new IllegalStateException("userId != photo.ownerId");

        return new Photo(id, owner, thumbUrl, imageUrl, likes, checked);
    }

    public long getId() {
        return mId;
    }

    /**
     * Return smallest image url
     *
     * @return url
     */
    public String getThumbnailUrl() {
        return mThumbnailUrl;
    }

    /**
     * Return image url
     *
     * @return url
     */
    public String getImageUrl() {
        return mImageUrl;
    }

    public User getUser() {
        return mUser;
    }

    public int getLikes() {
        return mLikes;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    @Override
    public int hashCode() {
        int result = mThumbnailUrl.hashCode();
        result = 31 * result + mImageUrl.hashCode();
        return result;
    }

    public boolean isChecked() {
        return mChecked;
    }

    public void setChecked(boolean mChecked) {
        this.mChecked = mChecked;
    }

    @Override
    public String toString() {
        return "Photo{" +
                "mUser='" + mUser + '\'' +
                ", mThumbnailUrl='" + mThumbnailUrl + '\'' +
                ", mImageUrl='" + mImageUrl + '\'' +
                ", mLikes='" + mLikes + '\'' +
                '}';
    }

    public ContentValues asContentValues() {
        ContentValues values = new ContentValues(6);
        if (mId != -1) {
            values.put(COLUMN_ID, mId);
        }
        values.put(COLUMN_USER_ID, mUser.getId());
        values.put(COLUMN_THUMBNAIL_URL, mThumbnailUrl);
        values.put(COLUMN_IMAGE_URL, mImageUrl);
        values.put(COLUMN_LIKES, mLikes);
        values.put(COLUMN_CHECKED, mChecked);
        return values;
    }


}
