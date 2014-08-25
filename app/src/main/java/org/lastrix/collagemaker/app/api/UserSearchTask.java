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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/25/14.
 */
public class UserSearchTask extends AsyncTask<String, Void, List<User>> implements DialogInterface.OnCancelListener {

    public static final String LOG_MESSAGE_FAILED_INSERT = "Failed to insert users to database";
    public static final boolean LOG_ALL = BuildConfig.LOG_ALL;
    private static final String LOG_TAG = UserSearchTask.class.getSimpleName();
    private volatile boolean mCanceled = false;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;
    private ContentResolver mContentResolver;

    public UserSearchTask(Listener mListener, ProgressDialog mProgressDialog, ContentResolver contentResolver) {
        this.mListener = mListener;
        this.mProgressDialog = mProgressDialog;
        mContentResolver = contentResolver;
        mProgressDialog.setOnCancelListener(this);
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
    protected void onPostExecute(List<User> users) {
        super.onPostExecute(users);
        if (mCanceled) {
            return;
        }
        mProgressDialog.dismiss();

        if (users == null) {
            mListener.onSearchFailed(mError);
        } else {
            mListener.onSearchCompleted(users);
        }
        mListener = null;
        mProgressDialog.setOnCancelListener(null);
        mProgressDialog = null;
        mError = null;
        mContentResolver = null;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected List<User> doInBackground(String... params) {
        List<User> users = new LinkedList<User>();
        List<User> list = new LinkedList<User>();
        try {
            for (String username : params) {
                if (fetchFromDatabase(users, username)) {
                    continue;
                }

                String next = API.getApiUserSearchUrl(username);
                while (next != null) {
                    if (mCanceled) return null;
                    next = fetch(next, list);
                }

                users.addAll(saveToDatabase(list, username));

                list.clear();
            }
        } catch (ApiException e) {
            mError = e;
            return null;
        } catch (JSONException e) {
            mError = e;
            return null;
        }
        return users;
    }

    private Collection<? extends User> saveToDatabase(List<User> list, String username) {
        int size = list.size();
        ContentValues[] values = new ContentValues[size];
        int idx = 0;
        for (User user : list) {
            values[idx++] = user.asContentValues();
        }

        //bulkInsert always better than insert
        if (size != mContentResolver.bulkInsert(ContentHelper.getUserUri(null), values)) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_INSERT);
            throw new IllegalStateException(LOG_MESSAGE_FAILED_INSERT);
        }

        //ids are same, so it's safe to use this objects.
        return list;
    }

    private boolean fetchFromDatabase(List<User> users, String username) {
        final Cursor cursor = mContentResolver.query(
                ContentHelper.getUserUri(null),
                null,
                String.format("%s LIKE ?", User.COLUMN_NICK),
                new String[]{username + "%"},
                Photo.COLUMN_ID);

        //if nothing found - just return false
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return false;
        }

        // convert data to internal objects
        cursor.moveToFirst();
        do {
            users.add(User.fromCursor(cursor));
        } while (cursor.moveToNext());
        cursor.close();

        //data was loaded - return true
        return true;
    }

    /**
     * Fetch data from url
     *
     * @param url -- the api call url
     * @return next url
     * @throws ApiException
     * @throws org.json.JSONException
     */
    private String fetch(String url, List<User> users) throws ApiException, JSONException {
        //safely fetch data
        JSONObject root = API.apiCall(url);

        //process data
        JSONArray data = root.getJSONArray(API.JSON_DATA);
        final int size = data.length();
        for (int i = 0; i < size; i++) {
            if (mCanceled) return null;
            users.add(User.fromJson(data.getJSONObject(i)));
        }

        //pagination control
        return API.nextUrl(root);
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        onCancelled();
    }

    public interface Listener {

        void onSearchCompleted(List<User> users);

        void onSearchFailed(Throwable e);
    }
}
