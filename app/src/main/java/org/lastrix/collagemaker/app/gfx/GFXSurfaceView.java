package org.lastrix.collagemaker.app.gfx;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

/**
 * Custom GLSurfaceView.
 * Created by lastrix on 8/17/14.
 */
public class GFXSurfaceView extends GLSurfaceView {

    private GFXRenderer mRenderer;

    public GFXSurfaceView(Context context) {
        super(context);
        init();
    }

    public GFXSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setEGLContextClientVersion(2);

        mRenderer = new GFXRenderer();
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

    /**
     * Add bitmap to renderer
     *
     * @param bitmap -- the bitmap
     */
    public void add(Bitmap bitmap) {
        mRenderer.add(bitmap);
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
}
