package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.view.View;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static android.opengl.GLES20.*;

/**
 * Created by lastrix on 8/17/14.
 */
public class GFXRenderer implements GLSurfaceView.Renderer {
    public static final int COORDS_PER_TEXTURE = 2;
    public static final int TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;

    public static final String SHADER_VERTEX =
            "        uniform mat4 uMVPMatrix;\n" +
                    "        attribute vec4 vPosition;\n" +
                    "        attribute vec2 a_texCoord;\n" +
                    "        varying vec2 v_texCoord;\n" +
                    "\n" +
                    "        void main() {\n" +
                    "        gl_Position = uMVPMatrix * vPosition;\n" +
                    "        v_texCoord = a_texCoord;\n" +
                    "        }\n";

    public static final String SHADER_TEXTURE =
            "        precision mediump float;\n" +
                    "        varying vec2 v_texCoord;\n" +
                    "        uniform sampler2D s_texture;\n" +
                    "\n" +
                    "        void main() {\n" +
                    "        gl_FragColor = texture2D( s_texture, v_texCoord );\n" +
                    "        }\n";
    public static final String LOG_TAG = GFXRenderer.class.getSimpleName();
    private static final float TILE_MESH[] = {
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    private static final float TILE_TEX[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
    private static final short TILE_ORDER[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices
    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mScratch = new float[16];

    private final float[] mTransform = new float[16];
    private final float[] mScale = new float[16];
    private final float[] mTranslate = new float[16];

    private final float[] mMvp = new float[16];
    private final List<GFXEntity> mEntities = new LinkedList<GFXEntity>();
    private final Deque<GFXEntity> mDrawOrder = new LinkedList<GFXEntity>();
    private final GFXSurfaceView mGfxSurfaceView;
    private volatile boolean mDestroy = false;

    private float mZoom = 3;

    private int mProgram;

    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    private ShortBuffer mDrawListBuffer;
    private int mDrawListBufferSize;

    private int mPositionHandle;
    private int mTexCoord;
    private int mMVPMatrixHandle;
    private int mSamplerLoc;
    private int mVertexShader;
    private int mTextureShader;

    private final Object mLock = new Object();

    public GFXRenderer(GFXSurfaceView gfxSurfaceView) {
        mGfxSurfaceView = gfxSurfaceView;
    }

    private static FloatBuffer loadToBuffer(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer buffer1 = buffer.asFloatBuffer();
        buffer1.put(values);
        buffer1.position(0);
        return buffer1;
    }

    private static ShortBuffer loadToBuffer(short[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * 2);
        buffer.order(ByteOrder.nativeOrder());
        ShortBuffer buffer1 = buffer.asShortBuffer();
        buffer1.put(values);
        buffer1.position(0);
        return buffer1;
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     * <p/>
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(LOG_TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.3f, 0.5f, 0.1f, 1.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        // Set the camera position (View matrix)
        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, mZoom, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMvp, 0, mProjectionMatrix, 0, mViewMatrix, 0);

        loadProgram();

        loadBuffers();
    }

    private void loadBuffers() {
        mDrawListBufferSize = TILE_ORDER.length;
        mVertexBuffer = loadToBuffer(TILE_MESH);
        mTextureBuffer = loadToBuffer(TILE_TEX);
        mDrawListBuffer = loadToBuffer(TILE_ORDER);
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        synchronized (mMvp) {
            Matrix.orthoM(mProjectionMatrix, 0, -mZoom * ratio, mZoom * ratio, -mZoom, mZoom, 1, 10);

            // Calculate the projection and view transformation
            Matrix.multiplyMM(mMvp, 0, mProjectionMatrix, 0, mViewMatrix, 0);
        }
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (mDestroy) {
            dispose();
            return;
        }

        // Draw background color
        glClear(GL_COLOR_BUFFER_BIT);

        if (mEntities.size() == 0) return;

        glUseProgram(mProgram);

        // Enable a handle to the triangle vertices
        glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);

        glEnableVertexAttribArray(mTexCoord);

        glVertexAttribPointer(mTexCoord, COORDS_PER_TEXTURE, GL_FLOAT, false, TEXTURE_STRIDE, mTextureBuffer);

        glUniform1i(mSamplerLoc, 0);

        synchronized (mLock) {
                for (GFXEntity entity : mDrawOrder) {
                    if (entity.isChecked()) drawEntity(unused, entity);
                }
        }

        glDisableVertexAttribArray(mPositionHandle);
        glDisableVertexAttribArray(mTexCoord);

        glBindTexture(GL_TEXTURE_2D, 0);

        glUseProgram(0);

    }

    public float[] getMvp() {
        return mMvp;
    }

    public Bitmap takeScreen(int width, int height){
        int size = width * height;
        int[] array = new int[size];
        IntBuffer buf = IntBuffer.wrap(array);
        glReadPixels(0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, buf);

        for (int i = 0; i < size; ++i) {
            // The alpha and green channels' positions are preserved while the red and blue are swapped
            array[i] = ((array[i] & 0xff00ff00)) | ((array[i] & 0x000000ff) << 16) | ((array[i] & 0x00ff0000) >> 16);
        }

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bmp.setPixels(array, size - width, -width, 0, 0, width, height);
        return bmp;
    }

    private void drawEntity(GL10 unused, GFXEntity entity) {
        Matrix.setIdentityM(mTranslate, 0);
        Matrix.translateM(mTranslate, 0, entity.getX(), entity.getY(), 0.0f);
        Matrix.setIdentityM(mScale, 0);
        Matrix.scaleM(mScale, 0, entity.getRatio(), 1f, 1f);
        Matrix.multiplyMM(mTransform, 0, mTranslate, 0, mScale, 0);
        Matrix.multiplyMM(mScratch, 0, mMvp, 0, mTransform, 0);

        // Apply the projection and view transformation
        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mScratch, 0);
        checkGlError("glUniformMatrix4fv");
        //do nothing;
        entity.onSetup(unused);

        glDrawElements(
                GL_TRIANGLES, mDrawListBufferSize,
                GL_UNSIGNED_SHORT, mDrawListBuffer);
    }

    private void dispose() {
        mDrawOrder.clear();
        for (GFXEntity entity : mEntities) {
            entity.clean();
        }
        mEntities.clear();
        glDeleteProgram(mProgram);
        glDeleteShader(mVertexShader);
        glDeleteShader(mTextureShader);
    }

    public void onDestroy() {
        mDestroy = true;
    }


    public float getZoom() {
        return mZoom;
    }

    public void setZoom(float mZoom, int width, int height) {
        if ( mZoom >= 0.5f && mZoom < 10f) {
            this.mZoom = mZoom;
            float ratio = (float) width / height;
            synchronized (mLock) {
                // this projection matrix is applied to object coordinates
                // in the onDrawFrame() method

                Matrix.orthoM(mProjectionMatrix, 0, -mZoom * ratio, mZoom * ratio, -mZoom, mZoom, 1, 10);

                // Calculate the projection and view transformation
                Matrix.multiplyMM(mMvp, 0, mProjectionMatrix, 0, mViewMatrix, 0);
            }
        }
    }

    public void setChecked(int position, boolean state) {
        final GFXEntity e = mEntities.get(position);
        e.setChecked(state);
        putToTop(e);

        if ( e.mBitmap == null && e.mTextureId == 0 ){
            loadImage(e);
        }
    }

    private void loadImage(final GFXEntity e) {
        ImageLoader.getInstance().loadImage(e.mUrl, new SimpleImageLoadingListener(){
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                e.mBitmap = loadedImage;
                mGfxSurfaceView.requestRender();
            }
        });
    }

    void resume(){
        mGfxSurfaceView.queueEvent(new Runnable() {
            @Override
            public void run() {
                synchronized (mLock){
                    for(GFXEntity entity : mEntities){
                        entity.mTextureId = 0;
                        if ( entity.isChecked()) {
                            loadImage(entity);
                        }
                    }
                }
            }
        });
    }

    void putToTop(GFXEntity e) {
        synchronized (mLock) {
            mDrawOrder.remove(e);
            mDrawOrder.add(e);
        }
    }

    protected void loadProgram() {
        mVertexShader = loadShader(GL_VERTEX_SHADER, SHADER_VERTEX);

        mTextureShader = loadShader(GL_FRAGMENT_SHADER, SHADER_TEXTURE);

        mProgram = glCreateProgram();
        glAttachShader(mProgram, mVertexShader);
        glAttachShader(mProgram, mTextureShader);
        glLinkProgram(mProgram);

        int[] linkStatus = new int[1];
        glGetProgramiv(mProgram, GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            Log.e(LOG_TAG, "Failed to link program.");
            glDeleteProgram(mProgram);
            glDeleteShader(mVertexShader);
            glDeleteShader(mTextureShader);
            mDestroy = true;
        } else {
            glUseProgram(mProgram);

            mPositionHandle = glGetAttribLocation(mProgram, "vPosition");
            checkGlError("glGetAttribLocation");

            mTexCoord = glGetAttribLocation(mProgram, "a_texCoord");
            checkGlError("glGetAttribLocation");

            // get handle to shape's transformation matrix
            mMVPMatrixHandle = glGetUniformLocation(mProgram, "uMVPMatrix");
            checkGlError("glGetUniformLocation");

            mSamplerLoc = glGetUniformLocation(mProgram, "s_texture");
            checkGlError("glGetUniformLocation");

            glUseProgram(0);
        }
    }

    /**
     * Utility method for compiling a OpenGL shader.
     *
     * @param type       - Vertex or fragment shader type.
     * @param shaderCode - String containing the shader code.
     * @return - Returns an id for the shader.
     */
    private int loadShader(int type, String shaderCode) {

        int shader = glCreateShader(type);

        // add the source code to the shader and compile it
        glShaderSource(shader, shaderCode);

        glCompileShader(shader);
        checkGlError("glCompileShader");

        return shader;
    }

    public GFXEntity getEntityUnder(float x, float y) {
        synchronized (mDrawOrder){
            Iterator<GFXEntity> it = mDrawOrder.descendingIterator();
            GFXEntity e;
            float dx;
            float dy;
            while(it.hasNext()){
                e = it.next();
                dx = x - e.getX();
                dy = y - e.getY();
                if ( dx > 0f && dx < 1f && dy > 0f && dy < 1f) {
                    return e;
                }
            }
        }
        return null;
    }

    public void setImages(List<String> strings) {
        synchronized (mLock){
            mDrawOrder.clear();
            mEntities.clear();
            GFXEntity entity;
            for( String url : strings){
                entity = new GFXEntity(url);
                mEntities.add(entity);
                mDrawOrder.add(entity);
            }
        }
    }

    static class GFXEntity {
        private volatile Bitmap mBitmap;
        private float mX, mY;
        private float mScale;
        private int mTextureId;
        private volatile boolean mChecked;
        private float mRatio;
        private String mUrl;

        private GFXEntity(String url) {
            this(url, 0f, 0f, 1f);
        }

        private GFXEntity(String url, float x, float y, float scale) {
            this.mUrl = url;
            this.mBitmap = null;
            this.mX = x;
            this.mY = y;
            this.mScale = scale;
            this.mTextureId = 0;
            mRatio = 1f;
        }

        public float getX() {
            return mX;
        }

        public void setX(float mX) {
            this.mX = mX;
        }

        public float getY() {
            return mY;
        }

        public void setY(float mY) {
            this.mY = mY;
        }

        public float getScale() {
            return mScale;
        }

        public void setScale(float mScale) {
            this.mScale = mScale;
        }

        public boolean isChecked() {
            return mChecked && (mTextureId > 0 || mBitmap != null);
        }

        public void setChecked(boolean checked) {
            this.mChecked = checked;
        }

        public void clean(){
            if ( mTextureId != 0){
                glDeleteTextures(1, new int[]{mTextureId}, 0);
            }
            mTextureId = 0;
            mBitmap = null;
        }

        public void onSetup(GL10 unused) {
            //don't show this thing
            if (mTextureId == 0) {
                //load texture
                int tex[] = new int[1];
                glGenTextures(1, tex, 0);
                mTextureId = tex[0];

                glBindTexture(GL_TEXTURE_2D, mTextureId);

                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmap, 0);
                mRatio = (float)mBitmap.getWidth() / (float)mBitmap.getHeight();
                mBitmap = null;
            } else {
                glBindTexture(GL_TEXTURE_2D, mTextureId);
            }
        }

        public float getRatio() {
            return mRatio;
        }
    }
}
