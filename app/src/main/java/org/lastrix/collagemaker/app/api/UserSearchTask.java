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
import org.lastrix.collagemaker.app.content.User;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Searches database or calls instagram api server for list of users matching pattern.</br>
 * Stores users in database to reduce amount of api calls.
 * More about storing here {@link org.lastrix.collagemaker.app.content.User} and
 * here {@link org.lastrix.collagemaker.app.content.ContentProvider} .
 * Created by lastrix on 8/25/14.
 */
public class UserSearchTask extends AsyncTask<String, Void, List<User>> implements DialogInterface.OnCancelListener {

    /**
     * Sentinel object to allow fetching cached users list.
     */
    public static final String SENTINEL = "...***SENTINEL***...";
    private static final String LOG_MESSAGE_FAILED_INSERT = "Failed to insert users to database";
    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;
    private static final String LOG_TAG = UserSearchTask.class.getSimpleName();
    private static final String LOG_MESSAGE_EXCEPTION = "Exception:";
    private volatile boolean mCanceled;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;
    private ContentResolver mContentResolver;

    public UserSearchTask(Listener listener, ProgressDialog progressDialog, ContentResolver contentResolver) {
        this.mListener = listener;
        this.mProgressDialog = progressDialog;
        this.mProgressDialog.setOnCancelListener(this);
        this.mContentResolver = contentResolver;
        this.mCanceled = false;
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

    @Override
    protected void onPostExecute(List<User> users) {
        super.onPostExecute(users);
        if (mCanceled) {
            return;
        }

        if (users != null) {
            mListener.onSearchCompleted(new ArrayList<User>(users));
            users.clear();
        } else if (mError != null) {
            mListener.onSearchFailed(mError);
        }
        reset();
    }

    private void reset() {
        if (mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
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
                //check database first
                if (get(users, username)) {
                    continue;
                }

                //do api calls
                String next = API.getApiUserSearchUrl(username);
                while (next != null) {
                    if (mCanceled) return null;
                    next = fetch(next, list);
                }

                //cache found users
                users.addAll(persist(list));

                //clear local cache
                list.clear();
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, LOG_MESSAGE_EXCEPTION, e);
            mError = e;
            return null;
        }
        return users;
    }

    /**
     * Store users in database.<br/>
     * This method returns passed argument if saving failed for some reason.
     *
     * @param users -- list of users
     * @return list of persisted users
     */
    private List<User> persist(@NonNull List<User> users) {
        int size = users.size();
        ContentValues[] values = new ContentValues[size];
        int idx = 0;
        for (User user : users) {
            values[idx++] = user.asContentValues();
        }

        //bulkInsert always better than insert
        if (size != mContentResolver.bulkInsert(ContentHelper.getUserUri(null), values)) {
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_INSERT);
            throw new IllegalStateException(LOG_MESSAGE_FAILED_INSERT);
        }

        //ids are same, so it's safe to use this objects.
        return users;
    }

    /**
     * Get data from database
     *
     * @param users    -- where users should be stored
     * @param username -- username to search
     * @return true if data loaded, false otherwise
     */
    private boolean get(List<User> users, String username) {
        final Cursor cursor;
        boolean defaultResult = false;
        if (SENTINEL.equals(username)) {
            defaultResult = true;
            cursor = mContentResolver.query(
                    ContentHelper.getUserUri(null),
                    null,
                    null,
                    null,
                    User.DEFAULT_SORT);
        } else {
            cursor = mContentResolver.query(
                    ContentHelper.getUserUri(null),
                    null,
                    User.DEFAULT_SEARCH_WHERE,
                    new String[]{"%" + username + "%"},
                    User.DEFAULT_SORT);
        }

        //if nothing found - just return false
        if (cursor == null || cursor.getCount() == 0) {
            if (cursor != null) cursor.close();
            return defaultResult;
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

    /**
     * Listener which should receive task results
     */
    public interface Listener {

        /**
         * Called when search successfully completed
         *
         * @param users -- list of found users
         */
        void onSearchCompleted(List<User> users);

        /**
         * Called when search failed due to exception
         *
         * @param e -- raised exception
         */
        void onSearchFailed(Throwable e);
    }
}
