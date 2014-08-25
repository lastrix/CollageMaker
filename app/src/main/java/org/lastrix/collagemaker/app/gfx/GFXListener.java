package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;

/**
 * Callback listener to receive screen capture.
 * Created by lastrix on 8/25/14.
 */
public interface GFXListener {

    /**
     * The screen has been captured
     *
     * @param bmp -- screen capture
     */
    void captured(Bitmap bmp);
}
