package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;
import android.opengl.GLSurfaceView;
import android.util.Log;
import org.lastrix.collagemaker.app.BuildConfig;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by lastrix on 8/17/14.
 */
class GFXRenderer implements GLSurfaceView.Renderer {
    public static final String LOG_TAG = GFXRenderer.class.getSimpleName();

    private final static GFXListener DUMMY_LISTENER = new GFXListener() {
        @Override
        public void captured(Bitmap bmp) {

        }
    };

    private static final boolean LOG_ALL = BuildConfig.LOG_ALL;

    private final List<GFXEntity> mEntities = new LinkedList<GFXEntity>();
    private final Deque<GFXEntity> mDrawOrder = new LinkedList<GFXEntity>();
    private final Object mLock = new Object();
    private GFXListener mGfxListener;
    private boolean mDestroy = false;
    private boolean mDestroyed = false;
    private volatile boolean mLoading = true;
    private GFXResource mGfxResource;
    private volatile boolean mCapture = false;

    public GFXRenderer() {
        mGfxListener = DUMMY_LISTENER;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        mGfxResource = new GFXResource();
        if (!mGfxResource.init()) {
            mDestroy = true;
        }

        mGfxResource.surfaceCreated(gl, config);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        mGfxResource.surfaceChanged(gl, width, height);
    }

    public void setGfxListener(GFXListener gfxListener) {
        synchronized (mLock) {
            this.mGfxListener = gfxListener;
            if (mGfxListener == null) {
                mGfxListener = DUMMY_LISTENER;
            }
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (mDestroy && !mDestroyed) {
            dispose();
            return;
        }

        if (LOG_ALL) {
            Log.v(LOG_TAG, "Drawing new frame: " + mLoading + "  " + mEntities.size());
        }
        if (mLoading) {
            drawLoadingScreen(unused);
        } else if (mEntities.size() > 0) {
            drawScene(unused);
        }

        if (mCapture) {
            mCapture = false;
            mGfxListener.captured(mGfxResource.capture());
        }
    }

    private void drawScene(GL10 unused) {
        synchronized (mLock) {
            mGfxResource.bind(unused);
            for (GFXEntity entity : mDrawOrder) {
                mGfxResource.draw(unused, entity.getModel(), entity.getTextureId());
            }
            mGfxResource.unbind(unused);
        }
    }

    private void drawLoadingScreen(GL10 unused) {
        //TODO: think about loading screen
        mGfxResource.bind(unused);
        mGfxResource.unbind(unused);
    }


    private void dispose() {
        mDrawOrder.clear();
        for (GFXEntity entity : mEntities) {
            entity.destroy();
        }
        mEntities.clear();
        mDestroyed = true;
    }

    public void onDestroy() {
        mDestroy = true;
    }

    public void add(GFXEntity entity) {
        synchronized (mLock) {
            mEntities.add(entity);
            mDrawOrder.add(entity);
        }
    }

    void putToTop(GFXEntity e) {
        synchronized (mLock) {
            mDrawOrder.remove(e);
            mDrawOrder.add(e);
        }
    }

    public GFXEntity getEntityUnder(float x, float y) {
        synchronized (mLock) {
            Iterator<GFXEntity> it = mDrawOrder.descendingIterator();
            GFXEntity e;
            float dx;
            float dy;
            while (it.hasNext()) {
                e = it.next();
                dx = x - e.getX();
                dy = y - e.getY();
                if (dx > 0f && dx < 1f && dy > 0f && dy < 1f) {
                    return e;
                }
            }
        }
        return null;
    }

    public void requestCapture() {
        mCapture = true;
    }

    public float getZoom() {
        return mGfxResource.getZoom();
    }

    public void setZoom(float zoom) {
        mGfxResource.setZoom(zoom);
    }

    public void requestDestroy() {
        mDestroy = true;
    }

    public void ready() {
        mLoading = false;
    }
}
