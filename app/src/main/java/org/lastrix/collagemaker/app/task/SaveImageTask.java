package org.lastrix.collagemaker.app.task;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 * Task for saving bitmaps to storage folder.<br/>
 * Use {@link org.lastrix.collagemaker.app.task.SaveImageTask.Listener} to handle result after completion.<br/>
 * If saving of any bitmap would fail - all already saved files would be removed, unless you called.
 * Created by lastrix on 8/26/14.
 */
public class SaveImageTask extends AsyncTask<Bitmap, Void, List<File>> implements DialogInterface.OnCancelListener {

    public static final String LOG_TAG = SaveImageTask.class.getSimpleName();
    public static final String LOG_MESSAGE_FAILED_DELETE = "Failed to delete file after exception [%s]";
    public static final String LOG_MESSAGE_FAILED_SAVE = "Save failed";
    private Listener mListener;
    private File mStorageDirectory;
    private ProgressDialog mProgressDialog;
    private String mFilenameTemplate;

    private Throwable mError;
    private volatile boolean mCanceled;
    private boolean mPreserve;

    public SaveImageTask(Listener mListener, File mStorageDirectory, ProgressDialog mProgressDialog, String filenameTemplate) {
        this.mListener = mListener;
        this.mStorageDirectory = mStorageDirectory;
        this.mProgressDialog = mProgressDialog;
        this.mProgressDialog.setOnCancelListener(this);
        this.mFilenameTemplate = filenameTemplate;
        this.mCanceled = false;
        this.mError = null;
        this.mPreserve = false;
    }

    @Override
    protected List<File> doInBackground(Bitmap... params) {
        List<File> files = new LinkedList<>();
        try {
            Random random = new Random();
            String filename;
            File file;
            for (Bitmap bitmap : params) {
                if (mCanceled) return files;
                //generate file
                filename = String.format(mFilenameTemplate, Integer.toHexString(random.nextInt()));
                file = new File(mStorageDirectory, filename);

                //protected save
                save(file, bitmap);

                files.add(file);
            }
        } catch (Exception e) {
            mError = e;
            Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SAVE, e);
            if (!mPreserve && files.size() > 0) {
                for (File f : files) {
                    if (!f.delete()) {
                        Log.w(LOG_TAG, String.format(LOG_MESSAGE_FAILED_DELETE, f.getAbsoluteFile()));
                    }
                }
            }
            return null;
        }
        return files;
    }

    /**
     * Tell task to keep already saved bitmaps if exception thrown.
     */
    public void preserve() {
        mPreserve = true;
    }

    private void save(File file, Bitmap bitmap) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

    @Override
    protected synchronized void onCancelled() {
        super.onCancelled();
        if (mCanceled) return;
        mCanceled = true;
        reset();
    }

    /**
     * Cleanup task
     */
    private void reset() {
        if (mProgressDialog != null) {
            if (mProgressDialog.isShowing()) {
                mProgressDialog.dismiss();
            }
            mProgressDialog.setOnCancelListener(null);
            mProgressDialog = null;
        }
        mError = null;
        mStorageDirectory = null;
        mListener = null;
    }

    @Override
    protected synchronized void onPostExecute(List<File> list) {
        super.onPostExecute(list);
        if (mCanceled) return;

        if (list != null) {
            mListener.onSavingCompleted(list);
        } else if (mError != null) {
            mListener.onSavingFailed(mError);
        }
        //there is no list=null, mError=null state because it is impossible!

        reset();
    }

    @Override
    protected synchronized void onPreExecute() {
        super.onPreExecute();
        if (mCanceled) return;
        if (!mProgressDialog.isShowing()) {
            mProgressDialog.show();
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        onCancelled();
    }

    /**
     * Results listener
     */
    public interface Listener {

        /**
         * Called on successful saving of all bitmap.
         *
         * @param files -- list of saved files
         */
        void onSavingCompleted(List<File> files);

        /**
         * Called on unsuccessful saving of any bitmap
         *
         * @param e -- thrown exception
         */
        void onSavingFailed(Throwable e);
    }
}
