package org.lastrix.collagemaker.app.api;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lastrix.collagemaker.app.BuildConfig;
import org.lastrix.collagemaker.app.content.ContentHelper;
import org.lastrix.collagemaker.app.content.Photo;
import org.lastrix.collagemaker.app.content.User;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/25/14.
 */
public class PopularPhotosTask extends AsyncTask<User, Void, List<Photo>> implements DialogInterface.OnCancelListener {
    public static final String LOG_TAG = PopularPhotosTask.class.getSimpleName();
    public static final String LOG_MESSAGE_FAILED_INSERT = "Failed to insert photos to database";
    public static final String LOG_MESSAGE_FAILED_DATABASE = "Failed to load data from database";
    public static final boolean LOG_ALL = BuildConfig.LOG_ALL;

    private volatile boolean mCanceled = false;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;
    private ContentResolver mContentResolver;

    public PopularPhotosTask(Listener mListener, ProgressDialog mProgressDialog, ContentResolver contentResolver) {
        this.mListener = mListener;
        this.mProgressDialog = mProgressDialog;
        this.mContentResolver = contentResolver;
        this.mProgressDialog.setOnCancelListener(this);
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected void onPostExecute(List<Photo> photos) {
        super.onPostExecute(photos);
        if (mCanceled) {
            return;
        }
        mProgressDialog.dismiss();

        if (photos == null) {
            mListener.onLoadingFailed(mError);
        } else {
            mListener.onLoadingCompleted(new ArrayList<Photo>(photos));
            photos.clear();
        }
        mListener = null;
        mProgressDialog.setOnCancelListener(null);
        mProgressDialog = null;
        mError = null;
        mContentResolver = null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        mCanceled = true;
        mProgressDialog.dismiss();
        mProgressDialog.setOnCancelListener(null);
        mProgressDialog = null;
        mListener = null;
        mContentResolver = null;
    }

    @Override
    protected List<Photo> doInBackground(User... params) {
        List<Photo> photos = new LinkedList<Photo>();
        List<Photo> userPhotos = new LinkedList<Photo>();
        for (User user : params) {
            try {
                if (mCanceled) return null;

                //check local cache, abusing server is not good idea
                if (fetchFromDatabase(photos, user)) {
                    continue;
                }
                //start fetching from server
                String next = API.getApiPopularPhotosUrl(user);
                while (next != null) {
                    if (mCanceled) return null;
                    next = fetch(next, userPhotos, user);
                }

                //store to database
                photos.addAll(saveToDatabase(userPhotos, user));

                userPhotos.clear();
            } catch (ApiException e) {
                mError = e;
                return null;
            } catch (JSONException e) {
                mError = e;
                return null;
            }
        }
        return photos;
    }

    private Collection<? extends Photo> saveToDatabase(List<Photo> userPhotos, User user) {
        int size = userPhotos.size();
        ContentValues[] values = new ContentValues[size];
        int idx = 0;
        for (Photo photo : userPhotos) {
            values[idx++] = photo.asContentValues();
        }

        //bulkInsert always better than insert
        if (size != mContentResolver.bulkInsert(ContentHelper.getPhotoUri(null), values)) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_INSERT);
            throw new IllegalStateException(LOG_MESSAGE_FAILED_INSERT);
        }

        //create new collection with mId field set
        List<Photo> resultSet = new ArrayList<Photo>(size);
        if (fetchFromDatabase(resultSet, user)) {
            return resultSet;
        }

        //since saving was not successful... return argument photos,
        // but log about some problem
        Log.w(LOG_TAG, LOG_MESSAGE_FAILED_DATABASE);
        return userPhotos;
    }

    private boolean fetchFromDatabase(List<Photo> userPhotos, User user) {
        final Cursor cursor = mContentResolver.query(
                ContentHelper.getPhotoUri(null),
                null,
                String.format("%s = ? AND %s > datetime('now', '-%d hours')", Photo.COLUMN_USER_ID, Photo.COLUMN_TIMESTAMP, Photo.CACHE_EXPIRE),
                new String[]{Long.toString(user.getId())},
                Photo.COLUMN_ID);

        //if nothing found - just return false
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return false;
        }

        // convert data to internal objects
        cursor.moveToFirst();
        do {
            userPhotos.add(Photo.fromCursor(user, cursor));
        } while (cursor.moveToNext());
        cursor.close();

        //data was loaded - return true
        return true;
    }

    private String fetch(String next, List<Photo> photos, User user) throws ApiException, JSONException {
        //do api call
        JSONObject root = API.apiCall(next);

        //process data
        JSONArray data = root.getJSONArray(API.JSON_DATA);
        final int size = data.length();
        JSONObject object, images;
        for (int i = 0; i < size; i++) {
            object = data.getJSONObject(i);
            images = object.getJSONObject(API.JSON_DATA_IMAGES);

            if (!API.isImage(object)) continue;

            //create photo
            photos.add(new Photo(user,
                    API.getImageUrl(images, API.JSON_DATA_IMAGES_THUMBNAIL),
                    API.getImageUrl(images, API.JSON_DATA_IMAGES_STANDARD_RESOLUTION),
                    API.getLikes(object)
            ));
        }
        //well done!
        return API.nextUrl(root);
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        onCancelled();
    }

    public interface Listener {

        void onLoadingCompleted(List<Photo> photos);

        void onLoadingFailed(Throwable e);
    }
}
