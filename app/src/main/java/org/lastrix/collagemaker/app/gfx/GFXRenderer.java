package org.lastrix.collagemaker.app.gfx;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
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
    private final float[] mMvp = new float[16];
    private final List<GFXEntity> mEntities = new LinkedList<GFXEntity>();
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


    public GFXRenderer() {
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
        GLES20.glViewport(0, 0, width, height);

        float ratio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        Matrix.frustumM(mProjectionMatrix, 0, -ratio, ratio, -1, 1, 2, 20);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMvp, 0, mProjectionMatrix, 0, mViewMatrix, 0);
    }

    @Override
    public void onDrawFrame(GL10 unused) {
        if (mDestroy) {
            dispose();
            return;
        }

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        if (mEntities.size() == 0) return;

        glUseProgram(mProgram);

        // Enable a handle to the triangle vertices
        glEnableVertexAttribArray(mPositionHandle);

        // Prepare the triangle coordinate data
        glVertexAttribPointer(mPositionHandle, COORDS_PER_VERTEX, GL_FLOAT, false, VERTEX_STRIDE, mVertexBuffer);

        glEnableVertexAttribArray(mTexCoord);

        glVertexAttribPointer(mTexCoord, COORDS_PER_TEXTURE, GL_FLOAT, false, TEXTURE_STRIDE, mTextureBuffer);

        glUniform1i(mSamplerLoc, 0);

        for (GFXEntity entity : mEntities) {
            if (entity.isChecked()) drawEntity(unused, entity);
        }

        glDisableVertexAttribArray(mPositionHandle);
        glDisableVertexAttribArray(mTexCoord);

        glBindTexture(GL_TEXTURE_2D, 0);

        glUseProgram(0);

    }

    private void drawEntity(GL10 unused, GFXEntity entity) {
        Matrix.setIdentityM(mTransform, 0);
        Matrix.translateM(mTransform, 0, entity.getX(), entity.getY(), 0f);
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
        for (GFXEntity entity : mEntities) {
            entity.onDestroy();
        }
        mEntities.clear();
        glDeleteProgram(mProgram);
        glDeleteShader(mVertexShader);
        glDeleteShader(mTextureShader);
    }

    public void onDestroy() {
        mDestroy = true;
    }

    public void add(Bitmap bitmap) {
        if (!mDestroy) {
            mEntities.add(new GFXEntity(bitmap));
        }
    }

    public float getZoom() {
        return mZoom;
    }

    public void setZoom(float mZoom) {
        this.mZoom = mZoom;
    }

    public void setChecked(int position, boolean state) {
        mEntities.get(position).setChecked(state);
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

    private static class GFXEntity {
        private Bitmap mBitmap;
        private float mX, mY;
        private float mScale;
        private int textureId = -1;
        private volatile boolean mChecked;

        private GFXEntity(Bitmap bitmap) {
            this(bitmap, 0f, 0f, 1f);
        }

        private GFXEntity(Bitmap bitmap, float x, float y, float scale) {
            this.mBitmap = bitmap;
            this.mX = x;
            this.mY = y;
            this.mScale = scale;
        }

        public Bitmap getBitmap() {
            return mBitmap;
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

        public void onDestroy() {
            mBitmap = null;
            if (textureId > 0) {
                glDeleteTextures(1, new int[]{textureId}, 0);
            }
        }

        public boolean isChecked() {
            return mChecked;
        }

        public void setChecked(boolean checked) {
            this.mChecked = checked;
        }

        public void onSetup(GL10 unused) {
            //don't show this thing
            if (textureId <= 0) {
                //load texture
                int tex[] = new int[1];
                glGenTextures(1, tex, 0);
                textureId = tex[0];

                glBindTexture(GL_TEXTURE_2D, textureId);

                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                GLUtils.texImage2D(GL_TEXTURE_2D, 0, mBitmap, 0);
                mBitmap = null;
            } else {
                glBindTexture(GL_TEXTURE_2D, textureId);
            }
        }
    }
}
