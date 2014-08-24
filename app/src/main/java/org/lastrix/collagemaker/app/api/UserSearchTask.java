package org.lastrix.collagemaker.app.api;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/25/14.
 */
public class UserSearchTask extends AsyncTask<String, Void, List<User>> implements DialogInterface.OnCancelListener {

    private volatile boolean mCanceled = false;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;

    public UserSearchTask(Listener mListener, ProgressDialog mProgressDialog) {
        this.mListener = mListener;
        this.mProgressDialog = mProgressDialog;
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
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mProgressDialog.show();
    }

    @Override
    protected List<User> doInBackground(String... params) {
        List<User> users = new LinkedList<User>();
        try {
            for (String username : params) {
                String next = API.getApiUserSearchUrl(username);
                while (next != null) {
                    if (mCanceled) return null;
                    next = fetch(next, users);
                }
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
