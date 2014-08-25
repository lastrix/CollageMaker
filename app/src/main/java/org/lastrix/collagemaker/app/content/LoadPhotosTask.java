package org.lastrix.collagemaker.app.content;

import android.content.ContentResolver;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Task for loading photos from database, only marked as 'checked' photos would be loaded,
 * all others would be ignored.<br/>
 * In order to receive data from this task use {@link org.lastrix.collagemaker.app.content.LoadPhotosTask.Listener} interface.
 * Created by lastrix on 8/25/14.
 */
public class LoadPhotosTask extends AsyncTask<Void, Void, List<Photo>> {

    private static final String LOG_MESSAGE_EXCEPTION = "Exception:";
    private static final String LOG_TAG = LoadPhotosTask.class.getSimpleName();

    private Listener mListener;
    private ContentResolver mContentResolver;
    private volatile boolean mCanceled;
    private Throwable mError;

    public LoadPhotosTask(@NonNull Listener listener, @NonNull ContentResolver contentResolver) {
        this.mListener = listener;
        this.mContentResolver = contentResolver;
        mCanceled = false;
    }

    @Override
    protected List<Photo> doInBackground(Void... params) {
        //do protected loading
        try {
            return load();
        } catch (Exception e) {
            Log.e(LOG_TAG, LOG_MESSAGE_EXCEPTION, e);
            mError = e;
            return null;
        }
    }

    private List<Photo> load() {
        final List<Photo> photos = new LinkedList<Photo>();
        final Cursor cursor = mContentResolver.query(
                ContentHelper.getPhotoUri(null),
                null,
                String.format("%s != 0 AND %s > datetime('now', '-%d hours')", Photo.COLUMN_CHECKED, Photo.COLUMN_TIMESTAMP, Photo.CACHE_EXPIRE),
                null,
                Photo.COLUMN_ID);

        //if nothing found - just return false
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return photos;
        }

        // convert data to internal objects
        cursor.moveToFirst();
        do {
            photos.add(Photo.fromCursor(null, cursor));
        } while (cursor.moveToNext());
        cursor.close();

        return photos;
    }

    @Override
    protected void onPostExecute(List<Photo> photos) {
        super.onPostExecute(photos);
        if (mCanceled) {
            return;
        }

        if (photos != null) {
            mListener.onFetchCompleted(new ArrayList<Photo>(photos));
            photos.clear();
        } else if (mError != null) {
            mListener.onFetchFailed(mError);
        }
        reset();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        if (mCanceled) {
            return;
        }
        mCanceled = true;
        reset();
    }

    private void reset() {
        mListener = null;
        mContentResolver = null;
        mError = null;
    }

    /**
     * Communication interface
     */
    public interface Listener {

        /**
         * Called when fetch successfully finished
         *
         * @param photos -- fetched photos
         */
        void onFetchCompleted(List<Photo> photos);

        /**
         * Called when an error occurred
         *
         * @param e -- raised exception
         */
        void onFetchFailed(Throwable e);
    }
}
