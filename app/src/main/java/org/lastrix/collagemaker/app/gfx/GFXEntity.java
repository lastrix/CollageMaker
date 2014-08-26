package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;
import android.opengl.GLUtils;
import android.opengl.Matrix;

import static android.opengl.GLES20.*;

/**
 * Entity under Render control.<br/>
 * Use factory method ( from openGL thread!) to create entity, see {@link #create(android.graphics.Bitmap)} .<br/>
 * The entity will automatically calculate it's model matrix, see {@link #getModel()}, {@link #resetModel()}.<br/>
 * Whenever you need to remove this entity call {@link #destroy()} from openGL thread.
 * Created by lastrix on 8/25/14.
 */
class GFXEntity {
    private static float[] sTranslate = new float[16];
    private static float[] sScale = new float[16];
    private static float[] sRotate = new float[16];
    private static float[] sScratch = new float[16];

    private float mX, mY;
    private float mScaleX, mScaleY;
    private int mTextureId;
    private float mRatio;
    private float[] mModel;

    /**
     * Private empty constructor
     */
    private GFXEntity() {

    }

    /**
     * Create new entity
     *
     * @param textureId -- texture id
     * @param ratio     -- width/height ratio
     */
    private GFXEntity(int textureId, float ratio) {
        mTextureId = textureId;
        mRatio = ratio;
        mX = 0f;
        mY = 0f;
        mScaleX = 1f;
        mScaleY = 1f;
        mModel = new float[16];
        resetModel();
    }

    /**
     * Create entity for bitmap<br/>
     * Call this method only from GL thread, see {@link android.opengl.GLSurfaceView#queueEvent(Runnable)}
     *
     * @param bmp -- the bitmap
     * @return an entity
     */
    public static GFXEntity create(Bitmap bmp) {
        int tex[] = new int[1];
        glGenTextures(1, tex, 0);
        int textureId = tex[0];

        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        GLUtils.texImage2D(GL_TEXTURE_2D, 0, bmp, 0);
        float ratio = (float) bmp.getWidth() / (float) bmp.getHeight();
        return new GFXEntity(textureId, ratio);
    }

    /**
     * Creates blank entity with no texture
     *
     * @return entity
     */
    public static GFXEntity create() {
        return new GFXEntity(0, 1f);
    }

    /**
     * Get x
     *
     * @return x
     */
    public float getX() {
        return mX;
    }

    /**
     * Get y
     *
     * @return y
     */
    public float getY() {
        return mY;
    }

    /**
     * Set scaleX (unused)
     *
     * @param scaleX -- new scale x
     * @param scaleY -- new scale y
     */
    public void setScale(float scaleX, float scaleY) {
        this.mScaleX = scaleX;
        this.mScaleY = scaleY;
        resetModel();
    }

    /**
     * Return texture id
     *
     * @return texture id
     */
    public int getTextureId() {
        return mTextureId;
    }

    /**
     * Reset model, calculate new matrix immediately
     */
    private void resetModel() {
        Matrix.setIdentityM(sTranslate, 0);
        Matrix.translateM(sTranslate, 0, mX, mY, 0.0f);

        Matrix.setIdentityM(sScale, 0);
        Matrix.scaleM(sScale, 0, mRatio * mScaleX, mScaleY, 1f);

        Matrix.multiplyMM(mModel, 0, sTranslate, 0, sScale, 0);
    }

    /**
     * Return model matrix
     *
     * @return model matrix
     */
    public float[] getModel() {
        return mModel;
    }

    /**
     * Remove this entity, delete texture.<br/>
     * Call this method only from GL thread, see {@link android.opengl.GLSurfaceView#queueEvent(Runnable)}
     */
    public void destroy() {
        int[] textures = new int[]{mTextureId};
        glDeleteTextures(1, textures, 0);
        mModel = null;
    }

    public void setPosition(float x, float y) {
        this.mX = x;
        this.mY = y;
        resetModel();
    }

    /**
     * Return scale y
     * @return scaleY
     */
    public float getScaleY() {
        return mScaleY;
    }

    /**
     * Return scale x
     * @return scaleX
     */
    public float getScaleX() {
        return mScaleX;
    }
}
