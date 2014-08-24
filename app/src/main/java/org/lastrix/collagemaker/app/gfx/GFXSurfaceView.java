package org.lastrix.collagemaker.app.gfx;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.MotionEvent;

import java.util.List;

/**
 * Custom GLSurfaceView.
 * Created by lastrix on 8/17/14.
 */
public class GFXSurfaceView extends GLSurfaceView {

    public static final String LOG_TAG = GFXSurfaceView.class.getSimpleName();
    private GFXRenderer mRenderer;
    private GFXRenderer.GFXEntity mDragged;
    private float mPreviousX;
    private float mPreviousY;
    private GFXListener mGfxListener;

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

        mRenderer = new GFXRenderer(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            setPreserveEGLContextOnPause(true);
        }
        setEGLConfigChooser(8,8,8,8,0,0);
        setRenderer(mRenderer);

        getHolder().setFormat(PixelFormat.TRANSLUCENT);
        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    /**
     * Send
     */
    public void onDestroy() {
        mRenderer.onDestroy();
    }


    @Override
    public void onResume() {
        super.onResume();
        requestRender();
    }

    /**
     * Set checked state
     *
     * @param position -- element to change
     * @param state    -- new state
     */
    public void setChecked(int position, boolean state) {
        mRenderer.setChecked(position, state);
        requestRender();
    }

    public GFXListener getGfxListener() {
        return mGfxListener;
    }

    public void setGfxListener(GFXListener mGFXListener) {
        this.mGfxListener = mGFXListener;
    }

    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        float ratio = (float)getWidth()/(float)getHeight();
        float w = getWidth();
        float x = (event.getX()*2.0f - w)/w* mRenderer.getZoom()*ratio;
        float h = getHeight();
        float y = -(event.getY()*2.0f - h)/h* mRenderer.getZoom();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                mDragged = mRenderer.getEntityUnder(x, y);
                if ( mDragged != null ) {
                    mPreviousX = x;
                    mPreviousY = y;
                    mRenderer.putToTop(mDragged);
                    return true;
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if ( mDragged == null)
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
        mRenderer.setZoom(mRenderer.getZoom() + dZoom, getWidth(), getHeight());
        requestRender();
    }



    public void takeScreen() {
        if ( mGfxListener == null ) return;
        mRenderer.requestScreenShot();
        requestRender();
    }

    public void setImages(List<String> strings) {
        mRenderer.setImages(strings);
        requestRender();
    }

    public interface GFXListener {

        void screenShot(Bitmap bmp);

        void loading(int progress, int max);
    }
}
