package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.*;

import static android.opengl.GLES20.*;

/**
 * GFX resources, holds, processes and manages shaders, meshes, surface.<br/>
 * Has control over world matrix.<br/>
 * Call {@link #init()} before any work, use {@link #destroy()} to free memory.<br/>
 * Use this class only from GL thread.<br/>
 * See {@link #bind(javax.microedition.khronos.opengles.GL10)}<br/>
 * {@link #draw(javax.microedition.khronos.opengles.GL10, float[], int)}<br/>
 * {@link #unbind(javax.microedition.khronos.opengles.GL10)}<br/>
 * Created by lastrix on 8/25/14.
 */
class GFXResource {
    public static final int COORDS_PER_TEXTURE = 2;
    public static final int TEXTURE_STRIDE = COORDS_PER_TEXTURE * 4;
    public static final int COORDS_PER_VERTEX = 3;
    public static final int VERTEX_STRIDE = COORDS_PER_VERTEX * 4;
    static final String SHADER_VERTEX =
            "        uniform mat4 uMVPMatrix;\n" +
                    "        attribute vec4 vPosition;\n" +
                    "        attribute vec2 a_texCoord;\n" +
                    "        varying vec2 v_texCoord;\n" +
                    "\n" +
                    "        void main() {\n" +
                    "        gl_Position = uMVPMatrix * vPosition;\n" +
                    "        v_texCoord = a_texCoord;\n" +
                    "        }\n";
    static final String SHADER_TEXTURE =
            "        precision mediump float;\n" +
                    "        varying vec2 v_texCoord;\n" +
                    "        uniform sampler2D s_texture;\n" +
                    "\n" +
                    "        void main() {\n" +
                    "        gl_FragColor = texture2D( s_texture, v_texCoord );\n" +
                    "        }\n";
    static final float TILE_MESH[] = {
            0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f
    };
    static final float TILE_TEX[] = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 1.0f,
            1.0f, 0.0f
    };
    static final short TILE_ORDER[] = {0, 1, 2, 0, 2, 3}; // order to draw vertices
    private static final String LOG_TAG = GFXResource.class.getSimpleName();

    public static final float ZOOM_MIN = 1f;
    public static final float ZOOM_MAX = 10f;

    private final float[] mProjectionMatrix = new float[16];
    private final float[] mViewMatrix = new float[16];
    private final float[] mWorld = new float[16];
    private final float[] mScratch = new float[16];
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;
    private ShortBuffer mDrawListBuffer;
    private int mDrawListBufferSize;
    private int mProgram;
    private int mPositionHandle;
    private int mTexCoord;
    private int mMVPMatrixHandle;
    private int mSamplerLoc;
    private int mVertexShader;
    private int mTextureShader;
    private boolean mInitializing;
    private float mZoom = 3;
    private int mWidth;
    private int mHeight;
    private float mRatio;

    public GFXResource() {
        mInitializing = true;
    }

    /**
     * Load values to buffer
     *
     * @param values -- the values
     * @return float buffer
     */
    private static FloatBuffer loadToBuffer(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(values.length * 4);
        buffer.order(ByteOrder.nativeOrder());
        FloatBuffer buffer1 = buffer.asFloatBuffer();
        buffer1.put(values);
        buffer1.position(0);
        return buffer1;
    }

    /**
     * Load values to buffer
     *
     * @param values -- the values
     * @return short buffer
     */
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

    /**
     * Initialize resources
     *
     * @return true if success
     */
    public boolean init() {
        mDrawListBufferSize = TILE_ORDER.length;
        mVertexBuffer = loadToBuffer(TILE_MESH);
        mTextureBuffer = loadToBuffer(TILE_TEX);
        mDrawListBuffer = loadToBuffer(TILE_ORDER);


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
            return false;
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

        mInitializing = false;
        return true;
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

    /**
     * Setup surface basic settings
     *
     * @param unused -- unused
     * @param config -- egl configuration
     */
    public void surfaceCreated(GL10 unused, EGLConfig config) {
        glClearColor(0.3f, 0.5f, 0.1f, 0.0f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
    }

    /**
     * Bind buffers
     *
     * @param unused -- not used
     */
    public void bind(GL10 unused) {
        if (mInitializing) {
            throw new IllegalStateException("Initializing...");
        }

        // Draw background color
        glClear(GL_COLOR_BUFFER_BIT);

        glUseProgram(mProgram);
        checkGlError("glUseProgram");

        // Enable a handle to the triangle vertices
        glEnableVertexAttribArray(mPositionHandle);
        checkGlError("glEnableVertexAttribArray");

        // Prepare the triangle coordinate data
        glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);
        checkGlError("glVertexAttribPointer");

        glEnableVertexAttribArray(mTexCoord);
        checkGlError("glEnableVertexAttribArray");

        glVertexAttribPointer(mTexCoord, COORDS_PER_TEXTURE, GL_FLOAT, false, TEXTURE_STRIDE, mTextureBuffer);
        checkGlError("glVertexAttribPointer");

        glUniform1i(mSamplerLoc, 0);
        checkGlError("glUniform1i");
    }

    /**
     * Unbind buffers
     *
     * @param unused -- not used
     */
    public void unbind(GL10 unused) {
        if (mInitializing) {
            throw new IllegalStateException("Initializing...");
        }

        glDisableVertexAttribArray(mPositionHandle);
        checkGlError("glDisableVertexAttribArray");

        glDisableVertexAttribArray(mTexCoord);
        checkGlError("glDisableVertexAttribArray");

        glBindTexture(GL_TEXTURE_2D, 0);
        checkGlError("glBindTexture");

        glUseProgram(0);
        checkGlError("glUseProgram");

    }

    /**
     * Destroy resources
     */
    public void destroy() {
        mInitializing = true;

        glDeleteProgram(mProgram);
        glDeleteShader(mVertexShader);
        glDeleteShader(mTextureShader);

        mProgram = 0;
        mVertexShader = 0;
        mTextureShader = 0;

        mVertexBuffer = null;
        mTextureBuffer = null;
        mDrawListBuffer = null;
        mDrawListBufferSize = 0;
    }

    /**
     * Called when surface rotated or like.
     *
     * @param unused -- not used
     * @param width  -- new width
     * @param height -- new height
     */
    public void surfaceChanged(GL10 unused, int width, int height) {
        mWidth = width;
        mHeight = height;
        glViewport(0, 0, mWidth, mHeight);

        resetViewMatrix();
    }

    /**
     * Calculate new view and projection matrices
     */
    private void resetViewMatrix() {
        mRatio = (float) mWidth / mHeight;

        Matrix.setLookAtM(mViewMatrix, 0, 0, 0, mZoom, 0f, 0f, 0f, 0f, 1.0f, 0.0f);

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method

        Matrix.orthoM(mProjectionMatrix, 0, -mZoom * mRatio, mZoom * mRatio, -mZoom, mZoom, ZOOM_MIN, ZOOM_MAX);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mWorld, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    /**
     * Return zoom
     *
     * @return zoom
     */
    public float getZoom() {
        return mZoom;
    }

    /**
     * Set zoom, recalculate view matrix.
     *
     * @param zoom -- new zoom
     */
    public void setZoom(float zoom) {
        if ( zoom >= ZOOM_MIN && zoom <= ZOOM_MAX) {
            mZoom = zoom;
            resetViewMatrix();
        }
    }

    /**
     * Draw object with custom model and texture
     *
     * @param unused    -- not used
     * @param mModel    -- model matrix
     * @param textureId -- the texture
     */
    public void draw(GL10 unused, float[] mModel, int textureId) {
        glBindTexture(GL_TEXTURE_2D, textureId);

        Matrix.multiplyMM(mScratch, 0, mWorld, 0, mModel, 0);

        glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mScratch, 0);
        checkGlError("glUniformMatrix4fv");

        glDrawElements(
                GL_TRIANGLES, mDrawListBufferSize,
                GL_UNSIGNED_SHORT, mDrawListBuffer);
    }

    /**
     * Make screen capture
     *
     * @return screen capture
     */
    public Bitmap capture() {
        int size = mWidth * mHeight;
        int[] array = new int[size];
        IntBuffer buf = IntBuffer.wrap(array);
        glReadPixels(0, 0, mWidth, mHeight, GL_RGBA, GL_UNSIGNED_BYTE, buf);

        for (int i = 0; i < size; ++i) {
            // The alpha and green channels' positions are preserved while the red and blue are swapped
            array[i] = ((array[i] & 0xff00ff00)) | ((array[i] & 0x000000ff) << 16) | ((array[i] & 0x00ff0000) >> 16);
        }

        Bitmap bmp = Bitmap.createBitmap(mWidth, mHeight, Bitmap.Config.ARGB_8888);
        bmp.setPixels(array, size - mWidth, -mWidth, 0, 0, mWidth, mHeight);
        return bmp;
    }

    public float getRatio() {
        return mRatio;
    }
}
