package org.lastrix.collagemaker.app.content;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.os.AsyncTask;

/**
 * Created by lastrix on 8/26/14.
 */
public class ResetSelectionTask extends AsyncTask<Void, Void, Void> {

    private ContentResolver mContentResolver;

    public ResetSelectionTask(ContentResolver contentResolver) {
        this.mContentResolver = contentResolver;
    }

    @Override
    protected Void doInBackground(Void... params) {
        final ContentValues values = new ContentValues(1);
        values.put(Photo.COLUMN_CHECKED, 0);
        mContentResolver.update(ContentHelper.getPhotoUri(null), values, null, null);
        return null;
    }
}
