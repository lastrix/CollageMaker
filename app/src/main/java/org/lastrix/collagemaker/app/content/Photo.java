package org.lastrix.collagemaker.app.content;

import android.content.ContentValues;
import android.database.Cursor;
import android.provider.BaseColumns;
import android.support.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Photo is object for storing info about image, data retrieved from instagram server.<br/>
 * Factory methods:<br/>
 * {@link #fromCursor(User, android.database.Cursor)}<br/>
 * {@link #fromJson(User, org.json.JSONObject)}<br/>
 * <br/>
 * Easy database storing:<br/>
 * {@link #asContentValues()}
 */
public class Photo {

    public final static String COLUMN_ID = BaseColumns._ID;
    public final static String COLUMN_USER_ID = "user_id";
    public final static String COLUMN_THUMBNAIL_URL = "thumbnail_url";
    public final static String COLUMN_IMAGE_URL = "image_url";
    public final static String COLUMN_LIKES = "likes";
    public final static String COLUMN_CHECKED = "checked";
    public final static String COLUMN_TIMESTAMP = "stamp";
    public final static int CACHE_EXPIRE = 12; //hours
    final static String TABLE_NAME = "photo";
    final static String SQL_CREATE = "CREATE TABLE " + TABLE_NAME + " ( " +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_USER_ID + " INTEGER NOT NULL, " +
            COLUMN_THUMBNAIL_URL + " TEXT NOT NULL, " +
            COLUMN_IMAGE_URL + " TEXT NOT NULL, " +
            COLUMN_LIKES + " INTEGER DEFAULT 0 NOT NULL, " +
            COLUMN_CHECKED + " INTEGER DEFAULT 0 NOT NULL, " +
            COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP NOT NULL" +
            ");";
    final static String SQL_DROP = "DROP TABLE IF EXISTS " + TABLE_NAME + ";";
    final static String SQL_FLUSH = "DELETE FROM " + TABLE_NAME + " WHERE " + COLUMN_TIMESTAMP + " <= datetime( 'now', '-" + CACHE_EXPIRE + " hours' );";
    private static final String FIELD_IMAGES = "images";
    private static final String FIELD_IMAGES_THUMBNAIL = "thumbnail";
    private static final String FIELD_IMAGES_STANDARD_RESOLUTION = "standard_resolution";
    private static final String FIELD_IMAGES__URL_ATTR = "url";
    private static final String FIELD_LIKES = "likes";
    private static final String FIELD_LIKES_COUNT_ATTR = "count";
    private final long mId;
    private final User mUser;
    private final String mThumbnailUrl;
    private final String mImageUrl;
    private final int mLikes;
    private boolean mChecked;

    /**
     * Create new photo
     *
     * @param user         -- the owner
     * @param thumbnailUrl -- thumbnail url
     * @param imageUrl     -- full-sized image
     * @param likes        -- amount of likes for this photo
     */
    public Photo(@NonNull User user, @NonNull String thumbnailUrl, @NonNull String imageUrl, int likes) {
        this(-1, user, thumbnailUrl, imageUrl, likes, false);
    }

    /**
     * Full constructor
     *
     * @param id           -- the database id of this photo
     * @param user         -- the owner
     * @param thumbnailUrl -- thumbnail url
     * @param imageUrl     -- full-sized image
     * @param likes        -- amount of likes for this photo
     * @param checked      -- photo should be included in collage if this parameter is true
     */
    public Photo(long id, @NonNull User user, @NonNull String thumbnailUrl, @NonNull String imageUrl, int likes, boolean checked) {
        this.mId = id;
        this.mUser = user;
        this.mThumbnailUrl = thumbnailUrl;
        this.mImageUrl = imageUrl;
        this.mLikes = likes;
        this.mChecked = checked;
    }

    /**
     * Construct photo from cursor object
     *
     * @param owner  -- owner of this photo
     * @param cursor -- data source
     * @return Photo object
     * @throws java.lang.IllegalArgumentException if 'owner' is not owner of this entry; or data corruption see {@link Cursor#getColumnIndexOrThrow(String)}
     */
    public static Photo fromCursor(@NonNull User owner, @NonNull Cursor cursor) throws IllegalArgumentException {
        final long id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        final long userId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_USER_ID));
        final String thumbUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_THUMBNAIL_URL));
        final String imageUrl = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IMAGE_URL));
        final int likes = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_LIKES));
        final boolean checked = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CHECKED)) != 0;

        //sanity check
        if (userId != owner.getId()) throw new IllegalArgumentException("userId != photo.ownerId");

        //construct object
        return new Photo(id, owner, thumbUrl, imageUrl, likes, checked);
    }

    /**
     * Construct photo from json document
     *
     * @param owner  -- photo owner
     * @param object -- json document
     * @return Photo
     * @throws JSONException
     */
    public static Photo fromJson(User owner, JSONObject object) throws JSONException {
        JSONObject images = object.getJSONObject(FIELD_IMAGES);
        final int likes = object.getJSONObject(FIELD_LIKES).getInt(FIELD_LIKES_COUNT_ATTR);
        final String thumbnailUrl = images.getJSONObject(FIELD_IMAGES_THUMBNAIL).getString(FIELD_IMAGES__URL_ATTR);
        final String imageUrl = images.getJSONObject(FIELD_IMAGES_STANDARD_RESOLUTION).getString(FIELD_IMAGES__URL_ATTR);
        //if still good - construct
        return new Photo(owner, thumbnailUrl, imageUrl, likes);
    }

    /**
     * Return photo id
     *
     * @return id
     */
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

    /**
     * Get owner
     *
     * @return user
     */
    public User getUser() {
        return mUser;
    }

    /**
     * Get likes count
     *
     * @return likes
     */
    public int getLikes() {
        return mLikes;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Photo && mId == ((Photo) o).mId;

    }

    @Override
    public int hashCode() {
        int result = mThumbnailUrl.hashCode();
        result = 31 * result + mImageUrl.hashCode();
        return result;
    }

    /**
     * Return checked state. This photo should be included into collage if set to true.
     *
     * @return checked
     */
    public boolean isChecked() {
        return mChecked;
    }

    /**
     * Set checked state
     *
     * @param state -- new state
     */
    public void setChecked(boolean state) {
        this.mChecked = state;
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

    /**
     * Convert this object to ContentValues
     *
     * @return ContentValues
     */
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
