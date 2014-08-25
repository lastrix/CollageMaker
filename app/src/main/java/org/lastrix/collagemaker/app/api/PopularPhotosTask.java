package org.lastrix.collagemaker.app.api;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.lastrix.collagemaker.app.BuildConfig;
import org.lastrix.collagemaker.app.content.ContentHelper;
import org.lastrix.collagemaker.app.content.Photo;
import org.lastrix.collagemaker.app.content.User;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Retrieves and stores popular photos index in database.<br/>
 * Returns list of retrieved photos to listener {@link org.lastrix.collagemaker.app.api.PopularPhotosTask.Listener}.<br/>
 * Index fetching is limited to {@link #PAGES_LIMIT} pages, which should be 5, since max requests per node is 15.
 * It's not wise to increase this value anymore. 5 pages should be almost 150 photos.<br/>
 * <br/>
 * More about storing info in database here {@link org.lastrix.collagemaker.app.content.Photo}
 * and here {@link org.lastrix.collagemaker.app.content.ContentProvider} .
 * <p/>
 * Created by lastrix on 8/25/14.
 */
public class PopularPhotosTask extends AsyncTask<User, Void, List<Photo>> implements DialogInterface.OnCancelListener {
    private static final int PAGES_LIMIT = 5;

    private static final String LOG_TAG = PopularPhotosTask.class.getSimpleName();
    private static final String LOG_MESSAGE_FAILED_INSERT = "Failed to insert photos to database";
    private static final String LOG_MESSAGE_FAILED_DATABASE = "Failed to load data from database";
    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;
    public static final String LOG_MESSAGE_EXCEPTION = "Exception:";

    private volatile boolean mCanceled;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;
    private ContentResolver mContentResolver;

    /**
     * Create task for fetching popular photos
     *
     * @param listener        -- event listener
     * @param progressDialog  -- progress dialog to display process
     * @param contentResolver -- content resolver for data saving
     */
    public PopularPhotosTask(@NonNull Listener listener, @NonNull ProgressDialog progressDialog, @NonNull ContentResolver contentResolver) {
        this.mListener = listener;
        this.mProgressDialog = progressDialog;
        this.mProgressDialog.setOnCancelListener(this);
        this.mContentResolver = contentResolver;
        this.mCanceled = false;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected synchronized void onPostExecute(List<Photo> photos) {
        super.onPostExecute(photos);
        if (mCanceled) {
            return;
        }

        if (photos != null) {
            mListener.onLoadingCompleted(new ArrayList<Photo>(photos));
            photos.clear();
        } else if ( mError != null ){
            mListener.onLoadingFailed(mError);
        }
        reset();
    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        if (mCanceled) {
            return;
        }
        mCanceled = true;
        reset();
    }

    private void reset() {
        if ( mProgressDialog != null ) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog.setOnCancelListener(null);
            mProgressDialog = null;
        }
        mListener = null;
        mContentResolver = null;
        mError = null;
    }

    @Override
    protected List<Photo> doInBackground(User... params) {
        List<Photo> photos = new LinkedList<Photo>();
        List<Photo> userPhotos = new LinkedList<Photo>();
        int pages;
        for (User user : params) {
            try {
                if (mCanceled) return null;

                //check local cache, abusing server is not good idea
                if (get(photos, user)) {
                    continue;
                }
                //start fetching from server
                pages = 0;
                String next = API.getApiPopularPhotosUrl(user);
                while (next != null && pages < PAGES_LIMIT) {
                    if (mCanceled) return null;
                    next = fetch(next, userPhotos, user);
                    pages++;
                }

                //store to database
                photos.addAll(persist(userPhotos, user));

                userPhotos.clear();
            } catch (Exception e) {
                Log.e(LOG_TAG, LOG_MESSAGE_EXCEPTION, e);
                mError = e;
                return null;
            }
        }
        return photos;
    }

    /**
     * Store photos in database, returns valid objects with valid id fields.
     *
     * @param userPhotos -- photos to save
     * @param user       -- photos owner
     * @return list of persisted photos or supplied list of persist failed for some reason.
     */
    private List<Photo> persist(@NonNull List<Photo> userPhotos, @NonNull User user) {
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
        if (get(resultSet, user)) {
            return resultSet;
        }

        //since saving was not successful... return argument photos,
        // but log about some problem
        Log.w(LOG_TAG, LOG_MESSAGE_FAILED_DATABASE);
        return userPhotos;
    }

    /**
     * Return list of photos for user
     *
     * @param userPhotos -- where to store photos
     * @param user       -- the photos owner
     * @return true of data loaded, false otherwise
     */
    private boolean get(@NonNull List<Photo> userPhotos, @NonNull User user) {
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

    /**
     * Fetch paged data from instagram server
     *
     * @param next   -- the api call url
     * @param photos -- where results should be stored
     * @param user   -- photos owner
     * @return api call url for next page
     * @throws ApiException
     * @throws JSONException
     */
    private String fetch(String next, @NonNull List<Photo> photos, @NonNull User user) throws ApiException, JSONException {
        //do api call
        JSONObject root = API.apiCall(next);

        //process data
        JSONArray data = root.getJSONArray(API.JSON_DATA);
        final int size = data.length();
        JSONObject object;
        for (int i = 0; i < size; i++) {
            object = data.getJSONObject(i);

            if (!API.isImage(object)) continue;

            //create photo
            photos.add(Photo.fromJson(user, object));
        }
        //well done!
        return API.nextUrl(root);
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        onCancelled();
    }

    /**
     * Listener for result processing of this task
     */
    public interface Listener {

        /**
         * Called when photos was correctly loaded
         *
         * @param photos -- loaded photos
         */
        void onLoadingCompleted(List<Photo> photos);

        /**
         * Called when an error occurred during task processing
         *
         * @param e -- raised exception
         */
        void onLoadingFailed(Throwable e);
    }
}
