package org.lastrix.collagemaker.app;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.lastrix.collagemaker.app.task.SaveImageTask;

import java.io.File;
import java.util.List;

/**
 * Simple activity with {@link android.widget.ImageView} inside.<br/>
 * This activity require image as argument, see {@link #PARAMETER_URL} .<br/>
 * Whole project uses ImageLoader library - so caller must use it too, please place image
 * inside memory cache, as follows:<br/>
 * <pre>
 *     {@code
 *     ImageLoader.getInstance().getMemoryCache().put(URL, bmp);
 *     }
 * </pre><br/>
 * After that pass {@code URL} to this activity, this will ensure fast and safe interaction.
 */
public class PreviewActivity extends ActionBarActivity implements SaveImageTask.Listener {

    public static final String PARAMETER_URL = "FILE_URL";
    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;
    private static final String LOG_TAG = PreviewActivity.class.getSimpleName();
    private static final String LOG_MESSAGE_FAILED_SAVE = "Failed to save bitmap.";
    private static final String LOG_MESSAGE_FAILED_CREATE = "Failed to create storage.";
    private static final String FOLDER = "Pictures/CollageMaker";
    private static final String FILE_TEMPLATE_NAME = "%s.png";
    private static final String MIME_TYPE = "image/png";
    private String mUri;
    private ProgressDialog mProgressDialog = null;
    private SaveImageTask mTask = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        mUri = bundle.getString(PARAMETER_URL);
        if (mUri == null) {
            finish();
        } else {
            ImageView mImageView = (ImageView) findViewById(R.id.preview);
            //image is stored directly into memory.
            // may be there is a safer way? Who knows what could be there.
            mImageView.setImageBitmap(ImageLoader.getInstance().getMemoryCache().get(mUri));
        }
    }

    private ProgressDialog createProgressDialog() {
        ProgressDialog mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setTitle(R.string.title_saving);
        mProgressDialog.setCancelable(true);
        return mProgressDialog;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_preview, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.action_send:
                //the progress dialog does not created at onCreate() because
                // there is only possibility that this dialog would be needed.
                if (mProgressDialog == null) {
                    mProgressDialog = createProgressDialog();
                }
                mTask = new SaveImageTask(this, getStorage(), mProgressDialog, FILE_TEMPLATE_NAME);
                mTask.execute(ImageLoader.getInstance().getMemoryCache().get(mUri));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mTask != null) {
            mTask.cancel(true);
            mTask = null;
        }
        if (mProgressDialog != null && mProgressDialog.isShowing()) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    /**
     * Returns file pointing to storage directory. If it does not exist, it would be created.
     *
     * @return storage
     */
    private File getStorage() {
        File storage;
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            storage = new File(Environment.getExternalStorageDirectory(), FOLDER);
        } else {
            storage = getCacheDir();
        }
        if (!storage.exists()) {
            if (!storage.mkdirs()) {
                throw new IllegalStateException(LOG_MESSAGE_FAILED_CREATE);
            }
        }
        return storage;
    }

    @Override
    public void onSavingCompleted(List<File> files) {
        //send mail
        final Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(MIME_TYPE);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(files.get(0)));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(Intent.createChooser(intent, getString(R.string.title_send_image)));
    }

    @Override
    public void onSavingFailed(Throwable e) {
        //show error
        Log.e(LOG_TAG, LOG_MESSAGE_FAILED_SAVE, e);
        Toast.makeText(this, R.string.error_saving_failed, Toast.LENGTH_LONG).show();
    }

}
