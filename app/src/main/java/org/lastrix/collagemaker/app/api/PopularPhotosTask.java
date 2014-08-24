package org.lastrix.collagemaker.app.api;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/25/14.
 */
public class PopularPhotosTask extends AsyncTask<User, Void, List<Photo>> implements DialogInterface.OnCancelListener {

    private volatile boolean mCanceled = false;
    private ProgressDialog mProgressDialog;
    private Listener mListener;
    private Throwable mError;

    public PopularPhotosTask(Listener mListener, ProgressDialog mProgressDialog) {
        this.mListener = mListener;
        this.mProgressDialog = mProgressDialog;
        mProgressDialog.setOnCancelListener(this);
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
    protected List<Photo> doInBackground(User... params) {
        List<Photo> photos = new LinkedList<Photo>();
        for (User user : params) {
            try {
                String next = API.getApiPopularPhotosUrl(user);
                while (next != null) {
                    if (mCanceled) return null;
                    next = fetch(next, photos);
                }
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

    private String fetch(String next, List<Photo> photos) throws ApiException, JSONException {
        //do api call
        JSONObject root = API.apiCall(next);

        //process data
        JSONArray data = root.getJSONArray(API.JSON_DATA);
        final int size = data.length();
        JSONObject entry;
        for (int i = 0; i < size; i++) {
            if (mCanceled) return null;
            //create photo
            entry = data.getJSONObject(i).getJSONObject(API.JSON_DATA_IMAGES);
            photos.add(new Photo(
                    API.getImageUrl(entry, API.JSON_DATA_IMAGES_THUMBNAIL),
                    API.getImageUrl(entry, API.JSON_DATA_IMAGES_STANDARD_RESOLUTION)
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
