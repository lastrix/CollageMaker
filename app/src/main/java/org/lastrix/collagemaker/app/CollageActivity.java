package org.lastrix.collagemaker.app;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import org.lastrix.collagemaker.app.content.LoadPhotosTask;
import org.lastrix.collagemaker.app.content.Photo;
import org.lastrix.collagemaker.app.gfx.GFXListener;
import org.lastrix.collagemaker.app.gfx.GFXSurfaceView;

import java.util.List;

/**
 * Manages creation of collage.
 * See {@link org.lastrix.collagemaker.app.gfx.GFXListener}, {@link org.lastrix.collagemaker.app.gfx.GFXSurfaceView},
 * {@link org.lastrix.collagemaker.app.gfx.GFXRenderer} for more details.
 * Created by lastrix on 8/21/14.
 */
public class CollageActivity extends ActionBarActivity implements GFXListener, LoadPhotosTask.Listener {
    private static final String LOG_MESSAGE_FAILED_FETCH = "Failed to fetch images";
    private static final String LOG_TAG = CollageActivity.class.getSimpleName();
    private static final boolean LOG_ALL = true;

    private static final String URI_SCREEN_SHOT = "content://lastrix.org/bmp/screen.bmp";
    private GFXSurfaceView mGfxSurfaceView;
    private LoadPhotosTask mTask;
    private boolean mCanceled;
    private volatile boolean mCapturing = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collage);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mGfxSurfaceView = (GFXSurfaceView) findViewById(R.id.surface_collage);

        mCanceled = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        mGfxSurfaceView.onResume();
        if (mCanceled) {
            mCanceled = false;
            mTask = new LoadPhotosTask(this, getContentResolver());
            mTask.execute();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mGfxSurfaceView.onPause();
        mCanceled = true;
        if (mTask != null) {
            mCanceled = true;
            mTask.cancel(true);
        }
    }

    @Override
    protected void onDestroy() {
        mGfxSurfaceView.onDestroy();
        mGfxSurfaceView = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_collage, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;

            case R.id.preview:
                //already working
                if (mCapturing) return true;
                //do preview
                mCapturing = true;
                mGfxSurfaceView.requestCapture();
                return true;

            case R.id.zoom_in:
                mGfxSurfaceView.zoom(-0.5f);
                return true;

            case R.id.zoom_out:
                mGfxSurfaceView.zoom(0.5f);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void captured(final Bitmap bmp) {
        ImageLoader.getInstance().getMemoryCache().put(URI_SCREEN_SHOT, bmp);
        Intent intent = new Intent(this, PreviewActivity.class);
        intent.putExtra(PreviewActivity.PARAMETER_URL, URI_SCREEN_SHOT);
        startActivity(intent);
        mCapturing = false;
    }

    @Override
    public void onFetchCompleted(List<Photo> photos) {
        mTask = null;
        mGfxSurfaceView.add(photos);
    }

    @Override
    public void onFetchFailed(Throwable e) {
        mTask = null;
        Log.e(LOG_TAG, LOG_MESSAGE_FAILED_FETCH, e);
        Toast.makeText(this, R.string.error_fetch_failed, Toast.LENGTH_LONG).show();
    }
}
