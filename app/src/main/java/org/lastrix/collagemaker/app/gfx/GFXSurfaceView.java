package org.lastrix.collagemaker.app.gfx;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import org.lastrix.collagemaker.app.content.Photo;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Custom GLSurfaceView.
 * Created by lastrix on 8/17/14.
 */
public final class GFXSurfaceView extends GLSurfaceView {

    public static final String LOG_TAG = GFXSurfaceView.class.getSimpleName();
    private GFXRenderer mRenderer;
    private GFXEntity mDragged;
    private float mPreviousX;
    private float mPreviousY;

    public GFXSurfaceView(Context context) {
        super(context);
        init();
    }

    public GFXSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void init() {
        setEGLContextClientVersion(2);

        mRenderer = new GFXRenderer();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
        setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        setRenderer(mRenderer);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    /**
     * Send
     */
    public void onDestroy() {
        mRenderer.requestDestroy();
        requestRender();
    }


    @Override
    public void onResume() {
        super.onResume();
        mRenderer.setGfxListener((GFXListener) getContext());
        requestRender();
    }

    @Override
    public void onPause() {
        super.onPause();
        mRenderer.setGfxListener(null);
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float ratio = (float) getWidth() / (float) getHeight();
        float w = getWidth();
        float x = (event.getX() * 2.0f - w) / w * mRenderer.getZoom() * ratio;
        float h = getHeight();
        float y = -(event.getY() * 2.0f - h) / h * mRenderer.getZoom();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mDragged = mRenderer.getEntityUnder(x, y);
                if (mDragged != null) {
                    mPreviousX = x;
                    mPreviousY = y;
                    mRenderer.putToTop(mDragged);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (mDragged == null)
                    break;
                mDragged.setX(mDragged.getX() + (x - mPreviousX));
                mDragged.setY(mDragged.getY() + (y - mPreviousY));
                mPreviousX = x;
                mPreviousY = y;
                requestRender();
                return true;

            case MotionEvent.ACTION_UP:
                mDragged = null;
                return true;
        }
        return false;
    }

    public void zoom(float dZoom) {
        mRenderer.setZoom(mRenderer.getZoom() + dZoom);
        requestRender();
    }

    public void requestCapture() {
        mRenderer.requestCapture();
        requestRender();
    }

    public void add(final List<Photo> photos) {
        ImageLoader loader = ImageLoader.getInstance();
        final AtomicInteger mPending = new AtomicInteger(photos.size());
        for (Photo photo : photos) {
            loader.loadImage(photo.getImageUrl(), new GFXImageLoadingListener(this, mPending));
        }
    }

    private void onLoadingCompleted() {
        mRenderer.ready();
        requestRender();
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    private static class NewGFXEntityRunnable implements Runnable {
        private GFXSurfaceView mSurfaceView;
        private Bitmap mImage;
        private AtomicInteger mPending;

        public NewGFXEntityRunnable(GFXSurfaceView surfaceView, Bitmap image, AtomicInteger atomicInteger) {
            this.mSurfaceView = surfaceView;
            this.mImage = image;
            this.mPending = atomicInteger;
        }

        @Override
        public void run() {
            mSurfaceView.mRenderer.add(GFXEntity.create(mImage));
            if (mPending.decrementAndGet() == 0) {
                mSurfaceView.onLoadingCompleted();
            }
            mSurfaceView = null;
            mImage = null;
            mPending = null;
        }
    }

    private static class GFXImageLoadingListener implements ImageLoadingListener {
        private GFXSurfaceView mSurfaceView;
        private AtomicInteger mPending;

        public GFXImageLoadingListener(GFXSurfaceView surfaceView, AtomicInteger mPending) {
            this.mSurfaceView = surfaceView;
            this.mPending = mPending;
        }

        @Override
        public void onLoadingComplete(String imageUri, View view, final Bitmap loadedImage) {
            mSurfaceView.queueEvent(new NewGFXEntityRunnable(mSurfaceView, loadedImage, mPending));
            mSurfaceView = null;
            mPending = null;
        }

        @Override
        public void onLoadingStarted(String s, View view) {
            //nothing to do
        }

        @Override
        public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
            Log.w(LOG_TAG, String.format("Failed to load image [%s]", imageUri));
            reset();
        }

        private void reset() {
            if (mPending.decrementAndGet() == 0) {
                mSurfaceView.onLoadingCompleted();
            }
            mSurfaceView = null;
            mPending = null;
        }

        @Override
        public void onLoadingCancelled(String imageUri, View view) {
            reset();
        }
    }
}
