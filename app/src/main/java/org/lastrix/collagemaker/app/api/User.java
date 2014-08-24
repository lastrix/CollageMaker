package org.lastrix.collagemaker.app.api;

import android.content.ContentValues;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Holds all necessary information about user.
 * Created by lastrix on 8/21/14.
 */
public class User {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "full_name";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_PHOTO_URL = "profile_picture";

    private final int mId;
    private final String mName;
    private final String mUsername;
    private final String mPhotoUrl;

    public User(int mId, String mName, String mUsername, String mPhotoUrl) {
        this.mId = mId;
        this.mName = mName;
        this.mUsername = mUsername;
        this.mPhotoUrl = mPhotoUrl;
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
        final int id = bundle.getInt(FIELD_ID);
        final String name = bundle.getString(FIELD_NAME);
        final String username = bundle.getString(FIELD_USERNAME);
        final String photoUrl = bundle.getString(FIELD_PHOTO_URL);
        return new User(id, name, username, photoUrl);
    }

    /**
     * Get user id
     *
     * @return id
     */
    public int getId() {
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
        return mId;
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
        bundle.putInt(FIELD_ID, mId);
        bundle.putString(FIELD_NAME, mName);
        bundle.putString(FIELD_USERNAME, mUsername);
        bundle.putString(FIELD_PHOTO_URL, mPhotoUrl);
        return bundle;
    }

    public ContentValues asContentValues() {
        final ContentValues values = new ContentValues(4);
        values.put(FIELD_ID, mId);
        values.put(FIELD_NAME, mName);
        values.put(FIELD_USERNAME, mUsername);
        values.put(FIELD_PHOTO_URL, mPhotoUrl);
        return values;
    }
}
